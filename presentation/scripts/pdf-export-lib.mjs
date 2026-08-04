import path from 'node:path';
import { CliUsageError, FRAGMENT_POLICY, VIEWPORT, validateExternalUrl } from './slide-export-lib.mjs';

export const PDF_EXPORTER_VERSION = '1.0.0';
export const PDF_MANIFEST_SCHEMA_VERSION = 1;
export const PDF_OUTPUT_DIRECTORY_NAME = 'pdf-export';
export const PDF_FILENAME = 'reconx-presentation.pdf';
export const PDF_MANIFEST_FILENAME = 'manifest.json';
export const PDF_PAGE_WIDTH_POINTS = 1_469.04;
export const PDF_PAGE_HEIGHT_POINTS = 826.08;
export const PDF_DIMENSION_TOLERANCE_POINTS = 0.05;

export class PdfValidationError extends Error {
  constructor(message) {
    super(message);
    this.name = 'PdfValidationError';
  }
}

export function parsePdfArguments(argv) {
  let help = false;
  let url = null;

  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];
    if (argument === '--help' || argument === '-h') {
      help = true;
      continue;
    }
    if (argument === '--url' || argument.startsWith('--url=')) {
      if (url !== null) throw new CliUsageError('--url may be provided only once.');
      const value = argument === '--url' ? argv[index + 1] : argument.slice('--url='.length);
      if (!value || value.startsWith('--')) throw new CliUsageError('--url requires a value.');
      if (argument === '--url') index += 1;
      url = validateExternalUrl(value);
      continue;
    }
    throw new CliUsageError(`Unknown option "${argument}".`);
  }

  return { help, url };
}

export function pdfUsageText() {
  return `Usage:
  npm run export:pdf
  npm run export:pdf -- --url <http-or-https-url>

Exports the complete Reveal deck to ${PDF_OUTPUT_DIRECTORY_NAME}/${PDF_FILENAME} and writes ${PDF_OUTPUT_DIRECTORY_NAME}/${PDF_MANIFEST_FILENAME}.
The default command builds and serves the local deck. --url inspects an already-running deck instead.
Reveal print mode uses one 1469.04x826.08pt margin-adjusted 16:9 page per slide, print backgrounds, and pdfSeparateFragments=false.
Live demos remain idle and are never activated.

Prerequisite:
  npx playwright install chromium
`;
}

export function selectPdfOutputPaths(outputDirectory) {
  const pdfPath = path.join(outputDirectory, PDF_FILENAME);
  const manifestPath = path.join(outputDirectory, PDF_MANIFEST_FILENAME);
  return { pdfPath, manifestPath };
}

function roundedPoints(value) {
  return Number(value.toFixed(3));
}

export function validatePdfSummary(summary, expectedSlideCount) {
  if (!Number.isInteger(expectedSlideCount) || expectedSlideCount < 1) {
    throw new PdfValidationError('The deck must contain at least one slide before PDF export.');
  }
  if (!Number.isInteger(summary.bytes) || summary.bytes < 1_024) {
    throw new PdfValidationError(`Generated PDF is unexpectedly small (${summary.bytes ?? 0} bytes).`);
  }
  if (summary.pageCount !== expectedSlideCount) {
    throw new PdfValidationError(
      `Generated PDF page count ${summary.pageCount} does not match discovered slide count ${expectedSlideCount}.`,
    );
  }
  if (!Array.isArray(summary.pages) || summary.pages.length !== summary.pageCount) {
    throw new PdfValidationError('PDF parser did not return dimensions for every page.');
  }

  const normalizedPages = summary.pages.map(({ widthPoints, heightPoints }, index) => {
    if (!Number.isFinite(widthPoints) || !Number.isFinite(heightPoints)) {
      throw new PdfValidationError(`PDF page ${index + 1} has invalid dimensions.`);
    }
    if (
      Math.abs(widthPoints - PDF_PAGE_WIDTH_POINTS) > PDF_DIMENSION_TOLERANCE_POINTS
      || Math.abs(heightPoints - PDF_PAGE_HEIGHT_POINTS) > PDF_DIMENSION_TOLERANCE_POINTS
    ) {
      throw new PdfValidationError(
        `PDF page ${index + 1} dimensions ${widthPoints}x${heightPoints}pt do not match the required ${PDF_PAGE_WIDTH_POINTS}x${PDF_PAGE_HEIGHT_POINTS}pt Reveal page contract.`,
      );
    }
    return {
      widthPoints: roundedPoints(widthPoints),
      heightPoints: roundedPoints(heightPoints),
    };
  });

  const firstPage = normalizedPages[0];
  const inconsistentPage = normalizedPages.find(({ widthPoints, heightPoints }) => (
    Math.abs(widthPoints - firstPage.widthPoints) > 0.5
    || Math.abs(heightPoints - firstPage.heightPoints) > 0.5
  ));
  if (inconsistentPage) throw new PdfValidationError('PDF pages do not share one canonical size.');

  return {
    bytes: summary.bytes,
    pageCount: summary.pageCount,
    pageDimensions: {
      widthPoints: firstPage.widthPoints,
      heightPoints: firstPage.heightPoints,
      unit: 'pt',
    },
  };
}

export function createPdfManifest({
  generatedAt,
  source,
  sourceMode,
  externalUrl,
  packageVersion,
  playwrightVersion,
  pdfjsVersion,
  slideCount,
  validation,
  sha256,
}) {
  return {
    schemaVersion: PDF_MANIFEST_SCHEMA_VERSION,
    exporter: {
      name: 'reconx-pdf-exporter',
      version: PDF_EXPORTER_VERSION,
      packageVersion,
      playwrightVersion,
      pdfjsVersion,
    },
    generatedAt,
    sourceCommit: source.sourceCommit,
    sourceDirty: source.sourceDirty,
    worktreeDirtyBeforeExport: source.worktreeDirtyBeforeExport,
    capture: {
      source: sourceMode,
      externalUrl: sourceMode === 'external-url' ? externalUrl : null,
      viewport: { width: VIEWPORT.width, height: VIEWPORT.height },
      deviceScaleFactor: VIEWPORT.deviceScaleFactor,
      printMode: 'reveal-print-pdf',
      fragmentPolicy: FRAGMENT_POLICY,
      pdfSeparateFragments: false,
      pageContract: {
        widthPoints: PDF_PAGE_WIDTH_POINTS,
        heightPoints: PDF_PAGE_HEIGHT_POINTS,
        tolerancePoints: PDF_DIMENSION_TOLERANCE_POINTS,
        derivation: '1920x1080 Reveal canvas with configured 0.02 print margin',
      },
      slideCount,
    },
    output: {
      filename: PDF_FILENAME,
      sha256,
      bytes: validation.bytes,
      pageCount: validation.pageCount,
      pageDimensions: validation.pageDimensions,
    },
  };
}
