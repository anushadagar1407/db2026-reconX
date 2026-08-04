import { Buffer } from 'node:buffer';
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { deflateSync } from 'node:zlib';
import { afterEach, describe, expect, it } from 'vitest';
import {
  CliUsageError,
  createManifest,
  createSlideImageMetadata,
  deriveSourceState,
  enumerateSlideCoordinates,
  exportExitCode,
  mergeManifestSlides,
  parseArguments,
  readPngDimensions,
  selectSlides,
  sha256,
  slideFilename,
  staleSlideFilenames,
  validatePartialRefreshState,
} from './slide-export-lib.mjs';

const crcTable = Array.from({ length: 256 }, (_, index) => {
  let value = index;
  for (let bit = 0; bit < 8; bit += 1) {
    value = (value & 1) ? (0xedb88320 ^ (value >>> 1)) : (value >>> 1);
  }
  return value >>> 0;
});

function crc32(buffer) {
  let value = 0xffffffff;
  for (const byte of buffer) value = crcTable[(value ^ byte) & 0xff] ^ (value >>> 8);
  return (value ^ 0xffffffff) >>> 0;
}

function pngChunk(type, data) {
  const typeBuffer = Buffer.from(type, 'ascii');
  const chunk = Buffer.alloc(data.length + 12);
  chunk.writeUInt32BE(data.length, 0);
  typeBuffer.copy(chunk, 4);
  data.copy(chunk, 8);
  chunk.writeUInt32BE(crc32(Buffer.concat([typeBuffer, data])), data.length + 8);
  return chunk;
}

function pngFixture(width, height, shade = 0) {
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width, 0);
  header.writeUInt32BE(height, 4);
  header.set([8, 0, 0, 0, 0], 8);
  const rows = Buffer.alloc((width + 1) * height);
  for (let row = 0; row < height; row += 1) {
    rows.fill(shade, row * (width + 1) + 1, (row + 1) * (width + 1));
  }
  return Buffer.concat([
    Buffer.from('89504e470d0a1a0a', 'hex'),
    pngChunk('IHDR', header),
    pngChunk('IDAT', deflateSync(rows)),
    pngChunk('IEND', Buffer.alloc(0)),
  ]);
}

const canonicalPng = pngFixture(1920, 1080);
const replacementPng = pngFixture(1920, 1080, 1);
const wrongDimensionsPng = pngFixture(1280, 720);
const testDirectories = new Set();

afterEach(async () => {
  await Promise.all([...testDirectories].map((directory) => (
    rm(directory, { force: true, recursive: true })
  )));
  testDirectories.clear();
});

function cleanDiagnostics() {
  const metrics = (width = 1920, height = 1080) => ({
    clientWidth: width,
    clientHeight: height,
    scrollWidth: width,
    scrollHeight: height,
    horizontal: false,
    vertical: false,
  });
  return {
    fragmentPolicy: 'all-visible',
    overflow: {
      detected: false,
      missingSlideCanvas: false,
      document: metrics(),
      slide: metrics(),
      canvas: metrics(),
      safeFrame: metrics(),
      safeArea: {
        ...metrics(1696, 904),
        inset: 'safe',
        enforcement: 'safe-content',
      },
      outsideSafeArea: [],
      autoFit: [],
    },
  };
}

function slideEntry(ordinal, overrides = {}) {
  return {
    ordinal,
    horizontalIndex: ordinal - 1,
    verticalIndex: 0,
    filename: slideFilename(ordinal, 2),
    title: `Slide ${ordinal}`,
    capturedAt: `2026-08-04T10:00:0${ordinal}.000Z`,
    sourceCommit: 'abc123',
    sourceDirty: false,
    image: {
      width: 1920,
      height: 1080,
      sha256: sha256(canonicalPng),
      bytes: canonicalPng.length,
    },
    diagnostics: cleanDiagnostics(),
    ...overrides,
  };
}

async function writeActiveGeneration(slides, images = new Map()) {
  const parentDirectory = await mkdtemp(path.join(os.tmpdir(), 'reconx-slide-integrity-'));
  testDirectories.add(parentDirectory);
  const outputDirectory = path.join(parentDirectory, 'slide-renders');
  await mkdir(outputDirectory);
  const manifest = {
    schemaVersion: 1,
    capture: { discoveredSlideCount: slides.length },
    slides,
  };
  await Promise.all(slides.map((slide) => writeFile(
    path.join(outputDirectory, slide.filename),
    images.get(slide.ordinal) ?? canonicalPng,
  )));
  await writeFile(path.join(outputDirectory, 'manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);
  return { manifest, outputDirectory };
}

describe('slide exporter arguments and selection', () => {
  it('selects all slides by default and one 1-based slide on request', () => {
    const slides = enumerateSlideCoordinates([0, 0, 0]);

    expect(parseArguments([])).toEqual({ help: false, slide: null, url: null });
    expect(selectSlides(slides, null)).toEqual(slides);
    expect(parseArguments(['--slide', '2']).slide).toBe(2);
    expect(parseArguments(['--slide=2']).slide).toBe(2);
    expect(selectSlides(slides, 2)).toEqual([slides[1]]);
  });

  it.each([
    ['--slide', '0'],
    ['--slide', '-1'],
    ['--slide', '1.5'],
    ['--slide', 'abc'],
  ])('rejects invalid slide option %s %s', (...arguments_) => {
    expect(() => parseArguments(arguments_)).toThrow(CliUsageError);
  });

  it('rejects missing and out-of-range slide numbers', () => {
    expect(() => parseArguments(['--slide'])).toThrow('--slide requires a value');
    expect(() => selectSlides(enumerateSlideCoordinates([0, 0]), 3)).toThrow(
      'Slide 3 is out of range; this deck contains 2 slides.',
    );
  });

  it('validates external deck URLs and removes their fragments', () => {
    expect(parseArguments(['--url', 'https://deck.example.test/path?mode=qa#slide']).url).toBe(
      'https://deck.example.test/path?mode=qa',
    );
    expect(() => parseArguments(['--url', 'file:///tmp/deck'])).toThrow('must use http or https');
    expect(() => parseArguments(['--url', 'not-a-url'])).toThrow(
      '--url must be a valid HTTP(S) URL without credentials.',
    );
  });

  it.each([
    { label: 'a parseable username', url: 'https://build-user@deck.example.test/' },
    { label: 'a parseable username and password', url: 'https://build-user:super-secret@deck.example.test/' },
    { label: 'percent-encoded userinfo', url: 'https://%75ser:%73ecret@deck.example.test/' },
  ])('rejects $label without reflecting its contents', ({ url }) => {
    let error;
    try {
      parseArguments(['--url', url]);
    } catch (caught) {
      error = caught;
    }
    expect(error).toBeInstanceOf(CliUsageError);
    expect(error.message).toBe(
      '--url must not include embedded credentials; provide a credential-free HTTP(S) URL.',
    );
    expect(error.message).not.toContain('build-user');
    expect(error.message).not.toContain('super-secret');
    expect(error.message).not.toContain('%75ser');
    expect(error.message).not.toContain('%73ecret');
  });

  it.each([
    { label: 'a malformed username and password', url: 'https://user:secret@' },
    { label: 'a malformed username', url: 'https://username-only@' },
    { label: 'malformed percent-encoded userinfo', url: 'https://%75ser:%73ecret@' },
  ])('rejects $label with a generic non-disclosing parse error', ({ url }) => {
    let error;
    try {
      parseArguments(['--url', url]);
    } catch (caught) {
      error = caught;
    }

    expect(error).toBeInstanceOf(CliUsageError);
    expect(error.message).toBe('--url must be a valid HTTP(S) URL without credentials.');
    expect(String(error)).not.toContain(url);
    for (const sensitivePart of ['user', 'secret', 'username-only', '%75ser', '%73ecret']) {
      expect(error.message).not.toContain(sensitivePart);
    }
  });
});

describe('Reveal slide ordering and output policy', () => {
  it('orders horizontal and nested vertical slides in presentation order', () => {
    expect(enumerateSlideCoordinates([0, 2, 0])).toEqual([
      { ordinal: 1, horizontalIndex: 0, verticalIndex: 0 },
      { ordinal: 2, horizontalIndex: 1, verticalIndex: 0 },
      { ordinal: 3, horizontalIndex: 1, verticalIndex: 1 },
      { ordinal: 4, horizontalIndex: 2, verticalIndex: 0 },
    ]);
  });

  it('uses stable zero-padded filenames and removes only stale full-export images', () => {
    expect(slideFilename(7, 10)).toBe('slide-007.png');
    expect(staleSlideFilenames(
      ['slide-001.png', 'slide-002.png', 'slide-003.png', 'manifest.json', 'notes.png'],
      ['slide-001.png', 'slide-002.png'],
    )).toEqual(['slide-003.png']);
  });
});

describe('manifest behavior', () => {
  it('preserves untouched entries during a single-slide refresh', () => {
    const previousSlides = [slideEntry(1), slideEntry(2)];
    const refreshed = slideEntry(2, {
      capturedAt: '2026-08-05T12:00:00.000Z',
      image: { width: 1920, height: 1080, sha256: 'new-hash' },
      sourceCommit: 'def456',
      sourceDirty: true,
    });

    const merged = mergeManifestSlides(previousSlides, [refreshed], 'single');

    expect(merged[0]).toBe(previousSlides[0]);
    expect(merged[0].capturedAt).toBe('2026-08-04T10:00:01.000Z');
    expect(merged[1]).toEqual(refreshed);
  });

  it('accepts a partial refresh only after validating real retained PNG metadata and diagnostics', async () => {
    const discoveredSlides = enumerateSlideCoordinates([0, 0]);
    const { manifest: previousManifest, outputDirectory } = await writeActiveGeneration([
      slideEntry(1),
      slideEntry(2),
    ]);

    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest,
      existingFilenames: ['manifest.json', 'slide-001.png', 'slide-002.png'],
      outputDirectory,
      selectedOrdinal: 2,
    })).resolves.toBe(previousManifest);
  });

  it('rejects a partial refresh after slide deletion or reorder', async () => {
    const current = enumerateSlideCoordinates([0]);
    const twoSlideManifest = {
      schemaVersion: 1,
      capture: { discoveredSlideCount: 2 },
      slides: [slideEntry(1), slideEntry(2)],
    };
    await expect(validatePartialRefreshState({
      discoveredSlides: current,
      previousManifest: twoSlideManifest,
      existingFilenames: ['slide-001.png', 'slide-002.png'],
      outputDirectory: '/unused',
      selectedOrdinal: 1,
    })).rejects.toThrow('discovered slide count differs');

    const nested = enumerateSlideCoordinates([0, 2]);
    const reorderedManifest = {
      schemaVersion: 1,
      capture: { discoveredSlideCount: 3 },
      slides: [
        slideEntry(1),
        slideEntry(2, { horizontalIndex: 1, verticalIndex: 1, filename: 'slide-002.png' }),
        slideEntry(3, { horizontalIndex: 1, verticalIndex: 0, filename: 'slide-003.png' }),
      ],
    };
    await expect(validatePartialRefreshState({
      discoveredSlides: nested,
      previousManifest: reorderedManifest,
      existingFilenames: ['slide-001.png', 'slide-002.png', 'slide-003.png'],
      outputDirectory: '/unused',
      selectedOrdinal: 1,
    })).rejects.toThrow('slide 2 coordinates no longer match');
  });

  it('rejects missing manifests, incomplete manifests, and missing retained files', async () => {
    const discoveredSlides = enumerateSlideCoordinates([0, 0]);
    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest: null,
      existingFilenames: [],
      outputDirectory: '/unused',
      selectedOrdinal: 1,
    })).rejects.toThrow('manifest is missing or invalid');
    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest: { schemaVersion: 1, slides: [slideEntry(1), slideEntry(2)] },
      existingFilenames: ['slide-001.png', 'slide-002.png'],
      outputDirectory: '/unused',
      selectedOrdinal: 1,
    })).rejects.toThrow('discovered slide count differs');
    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest: {
        schemaVersion: 1,
        capture: { discoveredSlideCount: 2 },
        slides: [slideEntry(1), slideEntry(2)],
      },
      existingFilenames: ['slide-001.png'],
      outputDirectory: '/unused',
      selectedOrdinal: 1,
    })).rejects.toThrow('retained file slide-002.png is missing');
  });

  it('rejects partial publication while an untouched slide retains overflow', async () => {
    const discoveredSlides = enumerateSlideCoordinates([0, 0]);
    const overflowDiagnostics = cleanDiagnostics();
    overflowDiagnostics.overflow.detected = true;
    const { manifest: previousManifest, outputDirectory } = await writeActiveGeneration([
      slideEntry(1),
      slideEntry(2, { diagnostics: overflowDiagnostics }),
    ]);

    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest,
      existingFilenames: ['slide-001.png', 'slide-002.png'],
      outputDirectory,
      selectedOrdinal: 1,
    })).rejects.toThrow('retained slide 2 still has unresolved overflow');
  });

  it('rejects replaced retained bytes without mutating the active generation', async () => {
    const discoveredSlides = enumerateSlideCoordinates([0, 0]);
    const { manifest: previousManifest, outputDirectory } = await writeActiveGeneration(
      [slideEntry(1), slideEntry(2)],
      new Map([[2, replacementPng]]),
    );
    const activePaths = [
      path.join(outputDirectory, 'manifest.json'),
      path.join(outputDirectory, 'slide-001.png'),
      path.join(outputDirectory, 'slide-002.png'),
    ];
    const hashesBeforeRejection = await Promise.all(
      activePaths.map(async (filePath) => sha256(await readFile(filePath))),
    );

    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest,
      existingFilenames: ['manifest.json', 'slide-001.png', 'slide-002.png'],
      outputDirectory,
      selectedOrdinal: 1,
    })).rejects.toThrow('retained file slide-002.png does not match its SHA-256');

    const hashesAfterRejection = await Promise.all(
      activePaths.map(async (filePath) => sha256(await readFile(filePath))),
    );
    expect(hashesAfterRejection).toEqual(hashesBeforeRejection);
  });

  it('rejects corrupt retained PNG data', async () => {
    const discoveredSlides = enumerateSlideCoordinates([0, 0]);
    const { manifest: previousManifest, outputDirectory } = await writeActiveGeneration(
      [slideEntry(1), slideEntry(2)],
      new Map([[2, Buffer.from('not-a-png')]]),
    );

    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest,
      existingFilenames: ['slide-001.png', 'slide-002.png'],
      outputDirectory,
      selectedOrdinal: 1,
    })).rejects.toThrow('retained file slide-002.png is not a valid PNG');
  });

  it('rejects retained PNGs with non-canonical dimensions', async () => {
    const discoveredSlides = enumerateSlideCoordinates([0, 0]);
    const wrongImage = {
      width: 1920,
      height: 1080,
      sha256: sha256(wrongDimensionsPng),
      bytes: wrongDimensionsPng.length,
    };
    const { manifest: previousManifest, outputDirectory } = await writeActiveGeneration(
      [slideEntry(1), slideEntry(2, { image: wrongImage })],
      new Map([[2, wrongDimensionsPng]]),
    );

    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest,
      existingFilenames: ['slide-001.png', 'slide-002.png'],
      outputDirectory,
      selectedOrdinal: 1,
    })).rejects.toThrow('retained file slide-002.png is not 1920x1080');
  });

  it('rejects a retained byte-length mismatch even when its hash matches', async () => {
    const discoveredSlides = enumerateSlideCoordinates([0, 0]);
    const { manifest: previousManifest, outputDirectory } = await writeActiveGeneration([
      slideEntry(1),
      slideEntry(2, {
        image: {
          width: 1920,
          height: 1080,
          sha256: sha256(canonicalPng),
          bytes: canonicalPng.length + 1,
        },
      }),
    ]);

    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest,
      existingFilenames: ['slide-001.png', 'slide-002.png'],
      outputDirectory,
      selectedOrdinal: 1,
    })).rejects.toThrow('does not match its recorded byte length');
  });

  it.each([
    { label: 'missing', bytes: undefined },
    { label: 'zero', bytes: 0 },
    { label: 'non-integer', bytes: 1.5 },
    { label: 'unsafe integer', bytes: Number.MAX_SAFE_INTEGER + 1 },
  ])('rejects $label retained byte metadata with full-refresh guidance', async ({ bytes }) => {
    const discoveredSlides = enumerateSlideCoordinates([0, 0]);
    const image = { ...slideEntry(2).image };
    if (bytes === undefined) delete image.bytes;
    else image.bytes = bytes;
    const { manifest: previousManifest, outputDirectory } = await writeActiveGeneration([
      slideEntry(1),
      slideEntry(2, { image }),
    ]);

    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest,
      existingFilenames: ['slide-001.png', 'slide-002.png'],
      outputDirectory,
      selectedOrdinal: 1,
    })).rejects.toThrow(
      'retained slide 2 has incomplete or invalid image metadata. Run "npm run export:slides" for a full refresh.',
    );
  });

  it.each([
    {
      label: 'missing image metadata',
      overrides: { image: undefined },
      message: 'incomplete or invalid image metadata',
    },
    {
      label: 'missing diagnostics',
      overrides: { diagnostics: undefined },
      message: 'missing or malformed diagnostics',
    },
    {
      label: 'malformed overflow diagnostics',
      overrides: {
        diagnostics: {
          fragmentPolicy: 'all-visible',
          overflow: { detected: false, missingSlideCanvas: false },
        },
      },
      message: 'missing or malformed diagnostics',
    },
  ])('rejects retained entries with $label', async ({ overrides, message }) => {
    const discoveredSlides = enumerateSlideCoordinates([0, 0]);
    const { manifest: previousManifest, outputDirectory } = await writeActiveGeneration([
      slideEntry(1),
      slideEntry(2, overrides),
    ]);

    await expect(validatePartialRefreshState({
      discoveredSlides,
      previousManifest,
      existingFilenames: ['slide-001.png', 'slide-002.png'],
      outputDirectory,
      selectedOrdinal: 1,
    })).rejects.toThrow(message);
  });

  it('records dirty source state honestly without rewriting untouched slide provenance', () => {
    const previous = {
      schemaVersion: 1,
      slides: [slideEntry(1), slideEntry(2)],
    };
    const source = deriveSourceState({
      commit: 'def456\n',
      sourceStatus: ' M presentation/src/App.tsx\n',
      worktreeStatus: ' M presentation/src/App.tsx\n?? presentation/slide-renders/slide-001.png\n',
    });
    const refreshed = slideEntry(2, { sourceCommit: source.sourceCommit, sourceDirty: source.sourceDirty });

    const manifest = createManifest({
      previousManifest: previous,
      capturedSlides: [refreshed],
      generatedAt: '2026-08-05T12:00:00.000Z',
      mode: 'single',
      source,
      sourceMode: 'local-preview',
      externalUrl: null,
      packageVersion: '0.1.0',
      playwrightVersion: '1.61.0',
      discoveredSlideCount: 2,
    });

    expect(manifest.sourceCommit).toBe('def456');
    expect(manifest.sourceDirty).toBe(true);
    expect(manifest.worktreeDirtyBeforeExport).toBe(true);
    expect(manifest.capture.refreshedSlides).toEqual([2]);
    expect(manifest.capture.externalUrl).toBeNull();
    expect(manifest.slides[0].sourceCommit).toBe('abc123');
    expect(manifest.slides[0].capturedAt).toBe('2026-08-04T10:00:01.000Z');
    expect(manifest.slides[1].sourceCommit).toBe('def456');
    expect(manifest.slides[1].sourceDirty).toBe(true);
  });
});

describe('diagnostic and image helpers', () => {
  it('constructs production image metadata from the captured PNG buffer', () => {
    expect(createSlideImageMetadata(canonicalPng)).toEqual({
      width: 1920,
      height: 1080,
      sha256: sha256(canonicalPng),
      bytes: canonicalPng.length,
    });
  });

  it('returns a non-zero result for viewport or unresolved AutoFit overflow', () => {
    const clean = slideEntry(1);
    const viewportOverflow = slideEntry(1, {
      diagnostics: { overflow: { detected: true, autoFit: [], missingSlideCanvas: false } },
    });
    const autoFitOverflow = slideEntry(1, {
      diagnostics: { overflow: { detected: false, autoFit: [{ scale: '0.72' }], missingSlideCanvas: false } },
    });

    expect(exportExitCode([clean])).toBe(0);
    expect(exportExitCode([viewportOverflow])).toBe(1);
    expect(exportExitCode([autoFitOverflow])).toBe(1);
  });

  it('reads actual dimensions from the PNG header', () => {
    expect(readPngDimensions(canonicalPng)).toEqual({ width: 1920, height: 1080 });
    expect(() => readPngDimensions(Buffer.from('not-png'))).toThrow('not a valid PNG');
    expect(() => readPngDimensions(canonicalPng.subarray(0, 40))).toThrow('not a valid PNG');
  });
});
