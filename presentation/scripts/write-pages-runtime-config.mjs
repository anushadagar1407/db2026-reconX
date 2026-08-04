import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

export function normalizePagesDemoUrl(rawValue) {
  if (rawValue === undefined || rawValue === '') return null;
  if (rawValue !== rawValue.trim()) {
    throw new Error('RECONX_DEMO_URL must be an absolute HTTPS URL.');
  }

  let url;
  try {
    url = new URL(rawValue);
  } catch {
    throw new Error('RECONX_DEMO_URL must be an absolute HTTPS URL.');
  }

  if (url.protocol !== 'https:' || url.username || url.password) {
    throw new Error('RECONX_DEMO_URL must be an absolute HTTPS URL without credentials.');
  }

  return url.toString();
}

export function renderPagesRuntimeConfig(rawValue) {
  return `window.__RECONX_PRESENTATION_CONFIG__ = Object.freeze(${JSON.stringify({
    demoUrl: normalizePagesDemoUrl(rawValue),
  })});\n`;
}

export async function writePagesRuntimeConfig(outputPath, rawValue = process.env.RECONX_DEMO_URL) {
  if (!outputPath) throw new Error('An output path is required.');

  const resolvedOutputPath = path.resolve(outputPath);
  await mkdir(path.dirname(resolvedOutputPath), { recursive: true });
  await writeFile(resolvedOutputPath, renderPagesRuntimeConfig(rawValue), 'utf8');
  return resolvedOutputPath;
}

const scriptPath = fileURLToPath(import.meta.url);
const invokedPath = process.argv[1] ? path.resolve(process.argv[1]) : null;

if (invokedPath === scriptPath) {
  try {
    await writePagesRuntimeConfig(process.argv[2]);
  } catch (error) {
    console.error(error instanceof Error ? error.message : 'Could not write Pages runtime configuration.');
    process.exitCode = 1;
  }
}
