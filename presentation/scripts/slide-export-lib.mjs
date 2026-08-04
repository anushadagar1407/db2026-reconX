import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import path from 'node:path';

export const EXPORTER_VERSION = '1.0.0';
export const MANIFEST_SCHEMA_VERSION = 1;
export const OUTPUT_DIRECTORY_NAME = 'slide-renders';
export const MANIFEST_FILENAME = 'manifest.json';
export const FRAGMENT_POLICY = 'all-visible';
export const VIEWPORT = Object.freeze({ width: 1920, height: 1080, deviceScaleFactor: 1 });

const INVALID_EXTERNAL_URL_MESSAGE = '--url must be a valid HTTP(S) URL without credentials.';

export class CliUsageError extends Error {
  constructor(message) {
    super(message);
    this.name = 'CliUsageError';
  }
}

function optionValue(argv, index, optionName) {
  const value = argv[index + 1];
  if (!value || value.startsWith('--')) {
    throw new CliUsageError(`${optionName} requires a value.`);
  }
  return value;
}

export function validateExternalUrl(value) {
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new CliUsageError(INVALID_EXTERNAL_URL_MESSAGE);
  }
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    throw new CliUsageError('--url must use http or https and must not include credentials.');
  }
  if (url.username || url.password) {
    throw new CliUsageError(
      '--url must not include embedded credentials; provide a credential-free HTTP(S) URL.',
    );
  }
  url.hash = '';
  return url.toString();
}

export function parseArguments(argv) {
  let slide = null;
  let url = null;
  let help = false;

  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];

    if (argument === '--help' || argument === '-h') {
      help = true;
      continue;
    }

    if (argument === '--slide' || argument.startsWith('--slide=')) {
      if (slide !== null) throw new CliUsageError('--slide may be provided only once.');
      const value = argument === '--slide'
        ? optionValue(argv, index, '--slide')
        : argument.slice('--slide='.length);
      if (argument === '--slide') index += 1;
      if (!/^[1-9]\d*$/.test(value)) {
        throw new CliUsageError(`--slide must be a positive 1-based integer; received "${value}".`);
      }
      slide = Number(value);
      continue;
    }

    if (argument === '--url' || argument.startsWith('--url=')) {
      if (url !== null) throw new CliUsageError('--url may be provided only once.');
      const value = argument === '--url'
        ? optionValue(argv, index, '--url')
        : argument.slice('--url='.length);
      if (argument === '--url') index += 1;
      url = validateExternalUrl(value);
      continue;
    }

    throw new CliUsageError(`Unknown option "${argument}".`);
  }

  return { help, slide, url };
}

export function usageText() {
  return `Usage:
  npm run export:slides
  npm run export:slides -- --slide <1-based-number>
  npm run export:slides -- --url <http-or-https-url> [--slide <number>]

Exports 1920x1080 PNGs to ${OUTPUT_DIRECTORY_NAME}/ and writes ${OUTPUT_DIRECTORY_NAME}/${MANIFEST_FILENAME}.
Slides are numbered in Reveal presentation order: each horizontal slide, with nested vertical slides in top-to-bottom order.
The default command builds and serves the local deck. --url inspects an already-running deck instead.
All fragments are forced into their visible final QA state. Live demos are never activated.

Prerequisite:
  npx playwright install chromium
`;
}

export function enumerateSlideCoordinates(verticalCounts) {
  const slides = [];

  verticalCounts.forEach((verticalCount, horizontalIndex) => {
    if (!Number.isInteger(verticalCount) || verticalCount < 0) {
      throw new TypeError('Vertical slide counts must be non-negative integers.');
    }
    const count = verticalCount === 0 ? 1 : verticalCount;
    for (let verticalIndex = 0; verticalIndex < count; verticalIndex += 1) {
      slides.push({
        ordinal: slides.length + 1,
        horizontalIndex,
        verticalIndex,
      });
    }
  });

  return slides;
}

export function selectSlides(slides, requestedSlide) {
  if (requestedSlide === null) return [...slides];
  const selected = slides.find(({ ordinal }) => ordinal === requestedSlide);
  if (!selected) {
    throw new CliUsageError(
      `Slide ${requestedSlide} is out of range; this deck contains ${slides.length} slide${slides.length === 1 ? '' : 's'}.`,
    );
  }
  return [selected];
}

export function slideFilename(ordinal, totalSlides) {
  const width = Math.max(3, String(totalSlides).length);
  return `slide-${String(ordinal).padStart(width, '0')}.png`;
}

export function staleSlideFilenames(existingFilenames, expectedFilenames) {
  const expected = new Set(expectedFilenames);
  return existingFilenames
    .filter((filename) => /^slide-\d+\.png$/.test(filename) && !expected.has(filename))
    .sort();
}

export function mergeManifestSlides(previousSlides, capturedSlides, mode) {
  if (mode === 'full') return [...capturedSlides].sort((a, b) => a.ordinal - b.ordinal);

  const byOrdinal = new Map(
    (Array.isArray(previousSlides) ? previousSlides : [])
      .filter((slide) => Number.isInteger(slide?.ordinal))
      .map((slide) => [slide.ordinal, slide]),
  );
  for (const slide of capturedSlides) byOrdinal.set(slide.ordinal, slide);
  return [...byOrdinal.values()].sort((a, b) => a.ordinal - b.ordinal);
}

export function deriveSourceState({ commit, sourceStatus, worktreeStatus }) {
  const sourceCommit = commit.trim();
  if (!sourceCommit) throw new Error('Git did not return a source commit.');
  return {
    sourceCommit,
    sourceDirty: sourceStatus.trim().length > 0,
    worktreeDirtyBeforeExport: worktreeStatus.trim().length > 0,
  };
}

export function isValidManifest(value) {
  return Boolean(
    value
    && value.schemaVersion === MANIFEST_SCHEMA_VERSION
    && Array.isArray(value.slides),
  );
}

function partialRefreshError(reason) {
  return new CliUsageError(
    `Single-slide export cannot preserve the current render set: ${reason} Run "npm run export:slides" for a full refresh.`,
  );
}

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function isMetricShape(value) {
  return isObject(value)
    && ['clientWidth', 'clientHeight', 'scrollWidth', 'scrollHeight']
      .every((key) => Number.isInteger(value[key]) && value[key] >= 0)
    && typeof value.horizontal === 'boolean'
    && typeof value.vertical === 'boolean';
}

function isNullableString(value) {
  return value === null || typeof value === 'string';
}

export function hasCompleteDiagnostics(diagnostics) {
  if (!isObject(diagnostics) || diagnostics.fragmentPolicy !== FRAGMENT_POLICY) return false;
  const overflow = diagnostics.overflow;
  if (
    !isObject(overflow)
    || typeof overflow.detected !== 'boolean'
    || typeof overflow.missingSlideCanvas !== 'boolean'
    || !['document', 'slide', 'canvas', 'safeFrame'].every((key) => isMetricShape(overflow[key]))
    || !isMetricShape(overflow.safeArea)
    || !['safe', 'compact', 'edge'].includes(overflow.safeArea.inset)
    || !['safe-content', 'full-bleed'].includes(overflow.safeArea.enforcement)
    || !Array.isArray(overflow.outsideSafeArea)
    || !Array.isArray(overflow.autoFit)
  ) {
    return false;
  }

  const outsideSafeAreaIsValid = overflow.outsideSafeArea.every((entry) => (
    isObject(entry)
    && typeof entry.tag === 'string'
    && isNullableString(entry.id)
    && isNullableString(entry.className)
  ));
  const autoFitIsValid = overflow.autoFit.every((entry) => (
    isObject(entry)
    && isNullableString(entry.id)
    && typeof entry.className === 'string'
    && typeof entry.scale === 'string'
  ));
  return outsideSafeAreaIsValid && autoFitIsValid;
}

function validateRetainedEntryShape(recorded, discovered, expectedFilename) {
  if (
    !isObject(recorded)
    || !Number.isInteger(recorded.ordinal)
    || !Number.isInteger(recorded.horizontalIndex)
    || !Number.isInteger(recorded.verticalIndex)
    || recorded.ordinal !== discovered.ordinal
    || recorded.horizontalIndex !== discovered.horizontalIndex
    || recorded.verticalIndex !== discovered.verticalIndex
    || recorded.filename !== expectedFilename
  ) {
    throw partialRefreshError(
      `retained slide ${discovered.ordinal} has incomplete or stale structural metadata.`,
    );
  }

  if (
    !isObject(recorded.image)
    || recorded.image.width !== VIEWPORT.width
    || recorded.image.height !== VIEWPORT.height
    || !/^[a-f0-9]{64}$/.test(recorded.image.sha256)
    || !Number.isSafeInteger(recorded.image.bytes)
    || recorded.image.bytes < 1
  ) {
    throw partialRefreshError(
      `retained slide ${recorded.ordinal} has incomplete or invalid image metadata.`,
    );
  }

  if (!hasCompleteDiagnostics(recorded.diagnostics)) {
    throw partialRefreshError(
      `retained slide ${recorded.ordinal} has missing or malformed diagnostics.`,
    );
  }
}

async function validateRetainedImage(recorded, outputDirectory) {
  let image;
  try {
    image = await readFile(path.join(outputDirectory, recorded.filename));
  } catch {
    throw partialRefreshError(`retained file ${recorded.filename} is missing or unreadable.`);
  }

  let dimensions;
  try {
    dimensions = readPngDimensions(image);
  } catch {
    throw partialRefreshError(`retained file ${recorded.filename} is not a valid PNG.`);
  }
  if (dimensions.width !== VIEWPORT.width || dimensions.height !== VIEWPORT.height) {
    throw partialRefreshError(
      `retained file ${recorded.filename} is not ${VIEWPORT.width}x${VIEWPORT.height}.`,
    );
  }
  if (sha256(image) !== recorded.image.sha256) {
    throw partialRefreshError(`retained file ${recorded.filename} does not match its SHA-256.`);
  }
  if (image.length !== recorded.image.bytes) {
    throw partialRefreshError(
      `retained file ${recorded.filename} does not match its recorded byte length.`,
    );
  }
}

export async function validatePartialRefreshState({
  discoveredSlides,
  previousManifest,
  existingFilenames,
  outputDirectory,
  selectedOrdinal,
}) {
  if (!isValidManifest(previousManifest)) {
    throw partialRefreshError('the existing manifest is missing or invalid.');
  }
  if (
    previousManifest.slides.length !== discoveredSlides.length
    || previousManifest.capture?.discoveredSlideCount !== discoveredSlides.length
  ) {
    throw partialRefreshError('the discovered slide count differs from the existing manifest.');
  }

  const existing = new Set(existingFilenames);
  const expectedFilenames = new Set();
  for (let index = 0; index < discoveredSlides.length; index += 1) {
    const discovered = discoveredSlides[index];
    const recorded = previousManifest.slides[index];
    const expectedFilename = slideFilename(discovered.ordinal, discoveredSlides.length);
    if (
      recorded?.ordinal !== discovered.ordinal
      || recorded.horizontalIndex !== discovered.horizontalIndex
      || recorded.verticalIndex !== discovered.verticalIndex
    ) {
      throw partialRefreshError(
        `slide ${discovered.ordinal} coordinates no longer match the existing manifest.`,
      );
    }
    if (recorded.filename !== expectedFilename) {
      throw partialRefreshError(`slide ${discovered.ordinal} filename is not structurally current.`);
    }
    if (recorded.ordinal !== selectedOrdinal && !existing.has(recorded.filename)) {
      throw partialRefreshError(`retained file ${recorded.filename} is missing.`);
    }
    expectedFilenames.add(recorded.filename);
  }

  const unexpectedImage = existingFilenames.find((filename) => (
    /^slide-\d+\.png$/.test(filename) && !expectedFilenames.has(filename)
  ));
  if (unexpectedImage) {
    throw partialRefreshError(`stale file ${unexpectedImage} is present.`);
  }

  for (let index = 0; index < discoveredSlides.length; index += 1) {
    const discovered = discoveredSlides[index];
    if (discovered.ordinal === selectedOrdinal) continue;
    const recorded = previousManifest.slides[index];
    const expectedFilename = slideFilename(discovered.ordinal, discoveredSlides.length);
    validateRetainedEntryShape(recorded, discovered, expectedFilename);
    if (hasOverflowDiagnostics(recorded.diagnostics)) {
      throw partialRefreshError(
        `retained slide ${recorded.ordinal} still has unresolved overflow diagnostics.`,
      );
    }
    await validateRetainedImage(recorded, outputDirectory);
  }

  return previousManifest;
}

export function createManifest({
  previousManifest,
  capturedSlides,
  generatedAt,
  mode,
  source,
  sourceMode,
  externalUrl,
  packageVersion,
  playwrightVersion,
  discoveredSlideCount,
}) {
  const previousSlides = isValidManifest(previousManifest) ? previousManifest.slides : [];
  const slides = mergeManifestSlides(previousSlides, capturedSlides, mode);

  return {
    schemaVersion: MANIFEST_SCHEMA_VERSION,
    exporter: {
      name: 'reconx-slide-exporter',
      version: EXPORTER_VERSION,
      packageVersion,
      playwrightVersion,
    },
    generatedAt,
    exportMode: mode,
    sourceCommit: source.sourceCommit,
    sourceDirty: source.sourceDirty,
    worktreeDirtyBeforeExport: source.worktreeDirtyBeforeExport,
    capture: {
      source: sourceMode,
      externalUrl: sourceMode === 'external-url' ? externalUrl : null,
      viewport: { width: VIEWPORT.width, height: VIEWPORT.height },
      deviceScaleFactor: VIEWPORT.deviceScaleFactor,
      fragmentPolicy: FRAGMENT_POLICY,
      discoveredSlideCount,
      refreshedSlides: capturedSlides.map(({ ordinal }) => ordinal),
    },
    slides,
  };
}

export function createSlideImageMetadata(image) {
  const dimensions = readPngDimensions(image);
  return {
    width: dimensions.width,
    height: dimensions.height,
    sha256: sha256(image),
    bytes: image.length,
  };
}

export function readPngDimensions(buffer) {
  const pngSignature = '89504e470d0a1a0a';
  if (
    buffer.length < 45
    || buffer.subarray(0, 8).toString('hex') !== pngSignature
    || buffer.readUInt32BE(8) !== 13
    || buffer.subarray(12, 16).toString('ascii') !== 'IHDR'
  ) {
    throw new Error('Screenshot output is not a valid PNG.');
  }
  const dimensions = {
    width: buffer.readUInt32BE(16),
    height: buffer.readUInt32BE(20),
  };
  if (dimensions.width < 1 || dimensions.height < 1) {
    throw new Error('Screenshot output is not a valid PNG.');
  }

  let offset = 8;
  let sawImageData = false;
  while (offset + 12 <= buffer.length) {
    const chunkLength = buffer.readUInt32BE(offset);
    const chunkEnd = offset + 12 + chunkLength;
    if (chunkEnd > buffer.length) throw new Error('Screenshot output is not a valid PNG.');
    const chunkType = buffer.subarray(offset + 4, offset + 8).toString('ascii');
    if (chunkType === 'IDAT') sawImageData = true;
    if (chunkType === 'IEND') {
      if (chunkLength !== 0 || chunkEnd !== buffer.length || !sawImageData) {
        throw new Error('Screenshot output is not a valid PNG.');
      }
      return dimensions;
    }
    offset = chunkEnd;
  }
  throw new Error('Screenshot output is not a valid PNG.');
}

export function sha256(buffer) {
  return createHash('sha256').update(buffer).digest('hex');
}

export function hasOverflowDiagnostics(diagnostics) {
  return Boolean(
    diagnostics?.overflow?.detected
    || diagnostics?.overflow?.autoFit?.length
    || diagnostics?.overflow?.missingSlideCanvas,
  );
}

export function exportExitCode(slides) {
  return slides.some(({ diagnostics }) => hasOverflowDiagnostics(diagnostics)) ? 1 : 0;
}

export function isMissingChromiumError(error) {
  const message = error instanceof Error ? error.message : String(error);
  return message.includes('Executable doesn\'t exist')
    || message.includes('playwright install chromium')
    || message.includes('browserType.launch: Executable');
}
