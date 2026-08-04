#!/usr/bin/env node

import { writeFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { getDocument } from 'pdfjs-dist/legacy/build/pdf.mjs';
import { CliUsageError, sha256 } from './slide-export-lib.mjs';
import {
  PDF_PAGE_HEIGHT_POINTS,
  PDF_PAGE_WIDTH_POINTS,
  PDF_OUTPUT_DIRECTORY_NAME,
  PdfValidationError,
  createPdfManifest,
  parsePdfArguments,
  pdfUsageText,
  selectPdfOutputPaths,
  validatePdfSummary,
} from './pdf-export-lib.mjs';
import {
  createExportPage,
  discoverSlides,
  initializeDeck,
  initializePrintDeck,
  launchChromium,
  presentationDirectory,
  readPackageVersions,
  readSourceState,
  runBuild,
  runGenerationTransaction,
  startStaticServer,
} from './presentation-export-runtime.mjs';

// PDF validation uses Mozilla PDF.js (Apache-2.0): https://github.com/mozilla/pdf.js
const outputDirectory = path.join(presentationDirectory, PDF_OUTPUT_DIRECTORY_NAME);

async function parsePdf(pdfBuffer) {
  const loadingTask = getDocument({
    data: new Uint8Array(pdfBuffer),
    isEvalSupported: false,
    useWorkerFetch: false,
  });
  const document = await loadingTask.promise;
  try {
    const pages = [];
    for (let pageNumber = 1; pageNumber <= document.numPages; pageNumber += 1) {
      const page = await document.getPage(pageNumber);
      const viewport = page.getViewport({ scale: 1 });
      pages.push({ widthPoints: viewport.width, heightPoints: viewport.height });
      page.cleanup();
    }
    return { pageCount: document.numPages, pages };
  } finally {
    await document.destroy();
  }
}

async function exportPdf(options) {
  const source = await readSourceState();
  const versions = await readPackageVersions();
  let preview = null;
  let browser = null;

  try {
    let url = options.url;
    if (!url) {
      await runBuild();
      preview = await startStaticServer();
      url = preview.url;
    }

    browser = await launchChromium();
    const { page, browserErrors } = await createExportPage(browser);
    await initializeDeck(page, url);
    const slides = await discoverSlides(page);
    if (slides.length === 0) throw new PdfValidationError('Reveal did not expose any slides to export.');

    await initializePrintDeck(page, url, slides.length);
    if (browserErrors.length > 0) {
      throw new Error(`Browser errors occurred during PDF export:\n${browserErrors.join('\n')}`);
    }

    const pdfBuffer = await page.pdf({
      displayHeaderFooter: false,
      height: `${PDF_PAGE_HEIGHT_POINTS / 72}in`,
      margin: { top: 0, right: 0, bottom: 0, left: 0 },
      preferCSSPageSize: true,
      printBackground: true,
      width: `${PDF_PAGE_WIDTH_POINTS / 72}in`,
    });
    if (pdfBuffer.subarray(0, 5).toString('ascii') !== '%PDF-') {
      throw new PdfValidationError('Chromium output does not start with a valid PDF signature.');
    }
    const parsed = await parsePdf(pdfBuffer);
    const validation = validatePdfSummary({
      bytes: pdfBuffer.length,
      pageCount: parsed.pageCount,
      pages: parsed.pages,
    }, slides.length);

    const generatedAt = new Date().toISOString();
    const manifest = createPdfManifest({
      generatedAt,
      source,
      sourceMode: options.url ? 'external-url' : 'local-preview',
      externalUrl: options.url,
      packageVersion: versions.packageVersion,
      playwrightVersion: versions.playwrightVersion,
      pdfjsVersion: versions.pdfjsVersion,
      slideCount: slides.length,
      validation,
      sha256: sha256(pdfBuffer),
    });
    const outputPaths = selectPdfOutputPaths(outputDirectory);
    await runGenerationTransaction({
      outputDirectory,
      generate: async (workspace) => {
        await writeFile(path.join(workspace, path.basename(outputPaths.pdfPath)), pdfBuffer);
        await writeFile(
          path.join(workspace, path.basename(outputPaths.manifestPath)),
          `${JSON.stringify(manifest, null, 2)}\n`,
        );
      },
    });
    console.log(
      `Wrote ${outputPaths.pdfPath} (${validation.pageCount} page${validation.pageCount === 1 ? '' : 's'}, ${validation.bytes} bytes).`,
    );
  } finally {
    await browser?.close();
    await preview?.close();
  }
}

async function main() {
  let options;
  try {
    options = parsePdfArguments(process.argv.slice(2));
  } catch (error) {
    if (error instanceof CliUsageError) {
      console.error(error.message);
      console.error('Run "npm run export:pdf -- --help" for usage.');
      process.exitCode = 2;
      return;
    }
    throw error;
  }

  if (options.help) {
    console.log(pdfUsageText());
    return;
  }
  await exportPdf(options);
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 2;
});
