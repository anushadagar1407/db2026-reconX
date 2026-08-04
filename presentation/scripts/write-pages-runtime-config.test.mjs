import { mkdtemp, readFile, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import {
  normalizePagesDemoUrl,
  writePagesRuntimeConfig,
} from './write-pages-runtime-config.mjs';

const temporaryDirectories = [];

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => (
    rm(directory, { force: true, recursive: true })
  )));
});

async function temporaryOutputPath() {
  const directory = await mkdtemp(path.join(os.tmpdir(), 'reconx-pages-config-'));
  temporaryDirectories.push(directory);
  return path.join(directory, 'nested', 'config.js');
}

describe('Pages runtime configuration', () => {
  it('serializes an empty demo URL as an explicit disabled value', async () => {
    const outputPath = await temporaryOutputPath();

    await writePagesRuntimeConfig(outputPath, '');

    await expect(readFile(outputPath, 'utf8')).resolves.toBe(
      'window.__RECONX_PRESENTATION_CONFIG__ = Object.freeze({"demoUrl":null});\n',
    );
  });

  it('serializes an HTTPS URL as JSON without executable interpolation', async () => {
    const outputPath = await temporaryOutputPath();

    await writePagesRuntimeConfig(outputPath, 'https://demo.example.test/?q=%22%3C%2Fscript%3E');

    const content = await readFile(outputPath, 'utf8');
    expect(content).toContain('"demoUrl":"https://demo.example.test/?q=%22%3C%2Fscript%3E"');
    expect(content).not.toContain('<script>');
  });

  it.each([
    'http://demo.example.test',
    '/demo',
    'not a URL',
    'https://user:password@demo.example.test',
  ])('rejects unsafe demo URL %s', (candidate) => {
    expect(() => normalizePagesDemoUrl(candidate)).toThrow(/absolute HTTPS URL/);
  });
});
