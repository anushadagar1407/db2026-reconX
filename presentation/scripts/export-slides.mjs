#!/usr/bin/env node

import { readFile, readdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import {
  CliUsageError,
  FRAGMENT_POLICY,
  MANIFEST_FILENAME,
  OUTPUT_DIRECTORY_NAME,
  createManifest,
  createSlideImageMetadata,
  exportExitCode,
  isValidManifest,
  parseArguments,
  selectSlides,
  slideFilename,
  usageText,
  validatePartialRefreshState,
} from './slide-export-lib.mjs';
import {
  createExportPage,
  discoverSlides,
  initializeDeck,
  launchChromium,
  presentationDirectory,
  readPackageVersions,
  readSourceState,
  runBuild,
  runGenerationTransaction,
  startStaticServer,
} from './presentation-export-runtime.mjs';
import { inspectSlideDom } from './slide-export-dom.mjs';

const outputDirectory = path.join(presentationDirectory, OUTPUT_DIRECTORY_NAME);
const manifestPath = path.join(outputDirectory, MANIFEST_FILENAME);

async function waitForImages(page, horizontalIndex, verticalIndex) {
  await page.evaluate(async ({ h, v }) => {
    const horizontal = [...document.querySelector('.reveal .slides').children]
      .filter((element) => element.tagName === 'SECTION')[h];
    const vertical = horizontal
      ? [...horizontal.children].filter((element) => element.tagName === 'SECTION')
      : [];
    const slide = vertical.length > 0 ? vertical[v] : horizontal;
    if (!slide) throw new Error(`Reveal slide ${h}/${v} is missing.`);

    const images = [...slide.querySelectorAll('img')];
    for (const image of images) image.loading = 'eager';
    await Promise.all(images.map(async (image) => {
      if (!image.complete) {
        await new Promise((resolve) => {
          image.addEventListener('load', resolve, { once: true });
          image.addEventListener('error', resolve, { once: true });
        });
      }
      if (typeof image.decode === 'function') await image.decode().catch(() => undefined);
    }));
  }, { h: horizontalIndex, v: verticalIndex });
}

async function settleLayout(page, horizontalIndex, verticalIndex) {
  await page.evaluate(async ({ h, v }) => {
    const getSlide = () => {
      const horizontal = [...document.querySelector('.reveal .slides').children]
        .filter((element) => element.tagName === 'SECTION')[h];
      const vertical = horizontal
        ? [...horizontal.children].filter((element) => element.tagName === 'SECTION')
        : [];
      return vertical.length > 0 ? vertical[v] : horizontal;
    };
    let previous = '';
    let stableFrames = 0;

    for (let frame = 0; frame < 120 && stableFrames < 4; frame += 1) {
      await new Promise(requestAnimationFrame);
      const slide = getSlide();
      const signature = JSON.stringify([
        document.documentElement.scrollWidth,
        document.documentElement.scrollHeight,
        slide?.scrollWidth,
        slide?.scrollHeight,
        ...[...slide.querySelectorAll('[data-scale], img')].flatMap((element) => {
          const rect = element.getBoundingClientRect();
          return [rect.width, rect.height, element.getAttribute('data-scale')];
        }),
      ]);
      stableFrames = signature === previous ? stableFrames + 1 : 0;
      previous = signature;
    }
    if (stableFrames < 4) throw new Error(`Slide ${h}/${v} did not reach a stable layout.`);
  }, { h: horizontalIndex, v: verticalIndex });
}

async function showSlide(page, slide) {
  await page.evaluate(({ h, v }) => {
    window.location.hash = `#/${h}/${v}`;
  }, { h: slide.horizontalIndex, v: slide.verticalIndex });

  await page.waitForFunction(({ h, v }) => {
    const horizontal = [...document.querySelector('.reveal .slides').children]
      .filter((element) => element.tagName === 'SECTION')[h];
    const vertical = horizontal
      ? [...horizontal.children].filter((element) => element.tagName === 'SECTION')
      : [];
    const target = vertical.length > 0 ? vertical[v] : horizontal;
    return target?.classList.contains('present');
  }, { h: slide.horizontalIndex, v: slide.verticalIndex });

  await page.evaluate(({ h, v }) => {
    const horizontal = [...document.querySelector('.reveal .slides').children]
      .filter((element) => element.tagName === 'SECTION')[h];
    const vertical = horizontal
      ? [...horizontal.children].filter((element) => element.tagName === 'SECTION')
      : [];
    const target = vertical.length > 0 ? vertical[v] : horizontal;
    for (const fragment of target.querySelectorAll('.fragment')) {
      fragment.classList.add('visible');
      fragment.classList.remove('current-fragment');
    }
  }, { h: slide.horizontalIndex, v: slide.verticalIndex });

  await waitForImages(page, slide.horizontalIndex, slide.verticalIndex);
  await settleLayout(page, slide.horizontalIndex, slide.verticalIndex);
}

async function inspectSlide(page, slide) {
  return page.evaluate(inspectSlideDom, { h: slide.horizontalIndex, v: slide.verticalIndex });
}

async function readPreviousManifest() {
  try {
    const manifest = JSON.parse(await readFile(manifestPath, 'utf8'));
    return isValidManifest(manifest) ? manifest : null;
  } catch {
    return null;
  }
}

async function captureSlide(page, slide, totalSlides, source, destinationDirectory) {
  await showSlide(page, slide);
  const diagnostics = await inspectSlide(page, slide);
  const image = await page.screenshot({
    animations: 'disabled',
    caret: 'hide',
    fullPage: false,
    scale: 'css',
    type: 'png',
  });
  const filename = slideFilename(slide.ordinal, totalSlides);
  await writeFile(path.join(destinationDirectory, filename), image);

  return {
    ordinal: slide.ordinal,
    horizontalIndex: slide.horizontalIndex,
    verticalIndex: slide.verticalIndex,
    filename,
    title: diagnostics.title,
    capturedAt: new Date().toISOString(),
    sourceCommit: source.sourceCommit,
    sourceDirty: source.sourceDirty,
    image: createSlideImageMetadata(image),
    diagnostics: {
      fragmentPolicy: FRAGMENT_POLICY,
      overflow: diagnostics.overflow,
    },
  };
}

async function exportSlides(options) {
  const source = await readSourceState();
  const versions = await readPackageVersions();
  const previousManifest = await readPreviousManifest();
  let preview = null;
  let browser = null;
  let browserErrors = [];

  try {
    let url = options.url;
    if (!url) {
      await runBuild();
      preview = await startStaticServer();
      url = preview.url;
    }

    browser = await launchChromium();
    const exportPage = await createExportPage(browser);
    const { page } = exportPage;
    browserErrors = exportPage.browserErrors;

    await initializeDeck(page, url);
    const allSlides = await discoverSlides(page);
    const selectedSlides = selectSlides(allSlides, options.slide);
    const partialRefresh = options.slide !== null;
    if (partialRefresh) {
      const existingFilenames = await readdir(outputDirectory).catch(() => []);
      await validatePartialRefreshState({
        discoveredSlides: allSlides,
        previousManifest,
        existingFilenames,
        outputDirectory,
        selectedOrdinal: options.slide,
      });
    }

    const generation = await runGenerationTransaction({
      outputDirectory,
      copyExisting: partialRefresh,
      generate: async (workspace) => {
        const capturedSlides = [];
        for (const slide of selectedSlides) {
          console.log(`Capturing slide ${slide.ordinal}/${allSlides.length}...`);
          capturedSlides.push(await captureSlide(
            page,
            slide,
            allSlides.length,
            source,
            workspace,
          ));
        }

        if (browserErrors.length > 0) {
          throw new Error(`Browser errors occurred during export:\n${browserErrors.join('\n')}`);
        }

        const manifest = createManifest({
          previousManifest,
          capturedSlides,
          generatedAt: new Date().toISOString(),
          mode: partialRefresh ? 'single' : 'full',
          source,
          sourceMode: options.url ? 'external-url' : 'local-preview',
          externalUrl: options.url,
          packageVersion: versions.packageVersion,
          playwrightVersion: versions.playwrightVersion,
          discoveredSlideCount: allSlides.length,
        });
        await writeFile(
          path.join(workspace, MANIFEST_FILENAME),
          `${JSON.stringify(manifest, null, 2)}\n`,
        );
        return { capturedSlides, manifest };
      },
    });

    const exitCode = exportExitCode(generation.manifest.slides);
    for (const slide of generation.manifest.slides.filter(
      ({ diagnostics }) => diagnostics.overflow.detected,
    )) {
      console.warn(`Slide ${slide.ordinal} has overflow diagnostics; inspect ${slide.filename} and ${MANIFEST_FILENAME}.`);
    }
    console.log(`Wrote ${generation.capturedSlides.length} slide render${generation.capturedSlides.length === 1 ? '' : 's'} to ${OUTPUT_DIRECTORY_NAME}/.`);
    return exitCode;
  } finally {
    await browser?.close();
    await preview?.close();
  }
}

async function main() {
  let options;
  try {
    options = parseArguments(process.argv.slice(2));
  } catch (error) {
    if (error instanceof CliUsageError) {
      console.error(error.message);
      console.error('Run "npm run export:slides -- --help" for usage.');
      process.exitCode = 2;
      return;
    }
    throw error;
  }

  if (options.help) {
    console.log(usageText());
    return;
  }

  process.exitCode = await exportSlides(options);
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 2;
});
