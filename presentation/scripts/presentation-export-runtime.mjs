import { spawn } from 'node:child_process';
import {
  access,
  cp,
  mkdir,
  mkdtemp,
  readFile,
  readdir,
  rename,
  rm,
  stat,
} from 'node:fs/promises';
import { createServer } from 'node:http';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';
import {
  VIEWPORT,
  deriveSourceState,
  enumerateSlideCoordinates,
  isMissingChromiumError,
} from './slide-export-lib.mjs';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
export const presentationDirectory = path.resolve(scriptDirectory, '..');
export const repositoryDirectory = path.resolve(presentationDirectory, '..');

const deterministicCss = `
  html[data-presentation-export="true"] *,
  html[data-presentation-export="true"] *::before,
  html[data-presentation-export="true"] *::after {
    animation-delay: 0s !important;
    animation-duration: 0s !important;
    caret-color: transparent !important;
    scroll-behavior: auto !important;
    transition-delay: 0s !important;
    transition-duration: 0s !important;
  }
  html[data-presentation-export="true"] .reveal .controls,
  html[data-presentation-export="true"] .reveal .progress,
  html[data-presentation-export="true"] .reveal .slide-number {
    display: none !important;
  }
  html[data-presentation-export="true"] .reveal .fragment {
    opacity: 1 !important;
    transform: none !important;
    visibility: visible !important;
  }
`;

const mimeTypes = new Map([
  ['.css', 'text/css; charset=utf-8'],
  ['.html', 'text/html; charset=utf-8'],
  ['.ico', 'image/x-icon'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.png', 'image/png'],
  ['.svg', 'image/svg+xml'],
  ['.webp', 'image/webp'],
]);

export function run(command, arguments_, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, arguments_, {
      cwd: options.cwd ?? presentationDirectory,
      env: process.env,
      stdio: options.capture ? ['ignore', 'pipe', 'pipe'] : 'inherit',
    });
    let stdout = '';
    let stderr = '';
    if (options.capture) {
      child.stdout.setEncoding('utf8');
      child.stderr.setEncoding('utf8');
      child.stdout.on('data', (chunk) => { stdout += chunk; });
      child.stderr.on('data', (chunk) => { stderr += chunk; });
    }
    child.on('error', reject);
    child.on('close', (code) => {
      if (code === 0) resolve({ stdout, stderr });
      else reject(new Error(`${command} ${arguments_.join(' ')} exited with code ${code}.${stderr ? `\n${stderr}` : ''}`));
    });
  });
}

async function gitOutput(arguments_) {
  const { stdout } = await run('git', arguments_, { cwd: repositoryDirectory, capture: true });
  return stdout;
}

export async function readSourceState() {
  const [commit, worktreeStatus, sourceStatus] = await Promise.all([
    gitOutput(['rev-parse', 'HEAD']),
    gitOutput(['status', '--porcelain=v1', '--untracked-files=all']),
    gitOutput([
      'status',
      '--porcelain=v1',
      '--untracked-files=all',
      '--',
      '.',
      ':(exclude)presentation/slide-renders/**',
      ':(exclude)presentation/pdf-export/**',
    ]),
  ]);
  return deriveSourceState({ commit, sourceStatus, worktreeStatus });
}

export async function readPackageVersions() {
  const packageJson = JSON.parse(await readFile(path.join(presentationDirectory, 'package.json'), 'utf8'));
  return {
    packageVersion: packageJson.version,
    playwrightVersion: packageJson.devDependencies.playwright,
    pdfjsVersion: packageJson.devDependencies['pdfjs-dist'] ?? null,
  };
}

export async function runBuild() {
  console.log('Building the presentation...');
  await run(process.platform === 'win32' ? 'npm.cmd' : 'npm', ['run', 'build']);
}

async function resolveStaticFile(distDirectory, requestUrl) {
  const pathname = decodeURIComponent(new URL(requestUrl, 'http://127.0.0.1').pathname);
  const relativePath = pathname === '/' ? 'index.html' : pathname.replace(/^\/+/, '');
  const candidate = path.resolve(distDirectory, relativePath);
  if (candidate !== distDirectory && !candidate.startsWith(`${distDirectory}${path.sep}`)) return null;

  try {
    const candidateStat = await stat(candidate);
    if (candidateStat.isFile()) return candidate;
    if (candidateStat.isDirectory()) return path.join(candidate, 'index.html');
  } catch {
    if (!path.extname(relativePath)) return path.join(distDirectory, 'index.html');
  }
  return null;
}

export async function startStaticServer() {
  const distDirectory = path.join(presentationDirectory, 'dist');
  await access(path.join(distDirectory, 'index.html'));

  const server = createServer(async (request, response) => {
    try {
      const filePath = await resolveStaticFile(distDirectory, request.url ?? '/');
      if (!filePath) {
        response.writeHead(404).end('Not found');
        return;
      }
      const body = await readFile(filePath);
      response.writeHead(200, {
        'Cache-Control': 'no-store',
        'Content-Type': mimeTypes.get(path.extname(filePath)) ?? 'application/octet-stream',
      });
      response.end(request.method === 'HEAD' ? undefined : body);
    } catch {
      response.writeHead(404).end('Not found');
    }
  });

  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  const address = server.address();
  if (!address || typeof address === 'string') throw new Error('Could not determine local preview port.');
  return {
    url: `http://127.0.0.1:${address.port}/`,
    close: () => new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve())),
  };
}

export async function launchChromium() {
  try {
    return await chromium.launch({ headless: true });
  } catch (error) {
    if (isMissingChromiumError(error)) {
      throw new Error('Playwright Chromium is not installed. Run exactly: npx playwright install chromium');
    }
    throw error;
  }
}

export async function createExportPage(browser) {
  const browserErrors = [];
  const context = await browser.newContext({
    viewport: { width: VIEWPORT.width, height: VIEWPORT.height },
    deviceScaleFactor: VIEWPORT.deviceScaleFactor,
    reducedMotion: 'reduce',
    colorScheme: 'light',
    locale: 'en-US',
    timezoneId: 'UTC',
  });
  const page = await context.newPage();
  page.setDefaultTimeout(15_000);
  page.on('console', (message) => {
    if (message.type() === 'error') browserErrors.push(`console: ${message.text()}`);
  });
  page.on('pageerror', (error) => browserErrors.push(`pageerror: ${error.message}`));
  return { page, browserErrors };
}

async function applyDeterministicStyles(page) {
  await page.evaluate(async (css) => {
    document.documentElement.dataset.presentationExport = 'true';
    const style = document.createElement('style');
    style.dataset.presentationExportStyle = 'true';
    style.textContent = css;
    document.head.append(style);
    await document.fonts?.ready;
    for (const animation of document.getAnimations()) animation.cancel();
  }, deterministicCss);
}

export async function initializeDeck(page, url) {
  await page.goto(url, { waitUntil: 'load' });
  await page.waitForSelector('.reveal.ready .slides > section', { state: 'attached' });
  await applyDeterministicStyles(page);
}

export async function discoverSlides(page) {
  const verticalCounts = await page.evaluate(() => {
    const slides = document.querySelector('.reveal .slides');
    if (!slides) throw new Error('Reveal slide container was not found.');
    return [...slides.children]
      .filter((element) => element.tagName === 'SECTION')
      .map((horizontal) => [...horizontal.children]
        .filter((element) => element.tagName === 'SECTION').length);
  });
  return enumerateSlideCoordinates(verticalCounts);
}

export function printModeUrl(url) {
  const printUrl = new URL(url);
  printUrl.hash = '';
  printUrl.searchParams.set('print-pdf', '');
  return printUrl.toString();
}

export async function waitForAllImages(page) {
  await page.evaluate(async () => {
    const images = [...document.images];
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
  });
}

export async function initializePrintDeck(page, url, expectedSlideCount) {
  await page.goto(printModeUrl(url), { waitUntil: 'load' });
  await page.waitForSelector('html.reveal-print .reveal.ready .pdf-page', { state: 'attached' });
  await page.emulateMedia({ media: 'print', reducedMotion: 'reduce', colorScheme: 'light' });
  await applyDeterministicStyles(page);
  await waitForAllImages(page);
  await page.waitForFunction((expected) => (
    document.querySelectorAll('.reveal .slides .pdf-page').length === expected
  ), expectedSlideCount);

  await page.evaluate(async () => {
    let previous = '';
    let stableFrames = 0;
    for (let frame = 0; frame < 120 && stableFrames < 4; frame += 1) {
      await new Promise(requestAnimationFrame);
      const pages = [...document.querySelectorAll('.reveal .slides .pdf-page')];
      const signature = JSON.stringify([
        document.documentElement.scrollWidth,
        document.documentElement.scrollHeight,
        ...pages.flatMap((pdfPage) => {
          const rect = pdfPage.getBoundingClientRect();
          return [rect.width, rect.height];
        }),
        ...[...document.querySelectorAll('[data-scale], img')].flatMap((element) => {
          const rect = element.getBoundingClientRect();
          return [rect.width, rect.height, element.getAttribute('data-scale')];
        }),
      ]);
      stableFrames = signature === previous ? stableFrames + 1 : 0;
      previous = signature;
    }
    if (stableFrames < 4) throw new Error('The print layout did not reach a stable state.');
  });
}

async function pathExists(filePath) {
  try {
    await access(filePath);
    return true;
  } catch {
    return false;
  }
}

export async function createGenerationWorkspace(outputDirectory, copyExisting = false, copyEntry = cp) {
  const parentDirectory = path.dirname(outputDirectory);
  const outputName = path.basename(outputDirectory);
  await mkdir(parentDirectory, { recursive: true });
  const workspace = await mkdtemp(path.join(parentDirectory, `.${outputName}-generation-`));

  try {
    if (copyExisting && await pathExists(outputDirectory)) {
      const entries = await readdir(outputDirectory, { withFileTypes: true });
      for (const entry of entries) {
        await copyEntry(
          path.join(outputDirectory, entry.name),
          path.join(workspace, entry.name),
          { recursive: entry.isDirectory() },
        );
      }
    }
    return workspace;
  } catch (error) {
    await cleanupGenerationWorkspace(workspace).catch(() => undefined);
    throw error;
  }
}

export async function cleanupGenerationWorkspace(workspace) {
  if (workspace) await rm(workspace, { force: true, recursive: true });
}

export async function publishGeneration({ outputDirectory, workspace, publicationHook }) {
  const parentDirectory = path.dirname(outputDirectory);
  const outputName = path.basename(outputDirectory);
  const backupDirectory = path.join(
    parentDirectory,
    `.${outputName}-backup-${process.pid}-${Date.now()}`,
  );
  let movedExisting = false;
  let published = false;

  try {
    await publicationHook?.('before-backup');
    if (await pathExists(outputDirectory)) {
      await rename(outputDirectory, backupDirectory);
      movedExisting = true;
    }
    await publicationHook?.('after-backup');
    await rename(workspace, outputDirectory);
    published = true;
    await publicationHook?.('after-publish');
    if (movedExisting) {
      await rm(backupDirectory, { force: true, recursive: true });
      movedExisting = false;
    }
  } catch (error) {
    if (published && await pathExists(outputDirectory)) {
      await rename(outputDirectory, workspace).catch(async () => {
        await rm(outputDirectory, { force: true, recursive: true });
      });
      published = false;
    }
    if (movedExisting && await pathExists(backupDirectory)) {
      await rename(backupDirectory, outputDirectory);
      movedExisting = false;
    }
    await cleanupGenerationWorkspace(workspace);
    await rm(backupDirectory, { force: true, recursive: true });
    throw error;
  }
}

export async function runGenerationTransaction({
  outputDirectory,
  copyExisting = false,
  copyEntry,
  generate,
  publicationHook,
}) {
  const workspace = await createGenerationWorkspace(outputDirectory, copyExisting, copyEntry);
  try {
    const result = await generate(workspace);
    await publishGeneration({ outputDirectory, workspace, publicationHook });
    return result;
  } finally {
    await cleanupGenerationWorkspace(workspace);
  }
}
