import { mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { runGenerationTransaction } from './presentation-export-runtime.mjs';

let testDirectory;
let outputDirectory;

beforeEach(async () => {
  testDirectory = await mkdtemp(path.join(os.tmpdir(), 'reconx-export-'));
  outputDirectory = path.join(testDirectory, 'slide-renders');
  await runGenerationTransaction({
    outputDirectory,
    generate: async (workspace) => {
      await writeFile(path.join(workspace, 'slide-001.png'), 'old-image');
      await writeFile(path.join(workspace, 'manifest.json'), 'old-manifest');
    },
  });
});

afterEach(async () => {
  await rm(testDirectory, { force: true, recursive: true });
});

async function expectOriginalGeneration() {
  await expect(readFile(path.join(outputDirectory, 'slide-001.png'), 'utf8'))
    .resolves.toBe('old-image');
  await expect(readFile(path.join(outputDirectory, 'manifest.json'), 'utf8'))
    .resolves.toBe('old-manifest');
  expect((await readdir(testDirectory)).sort()).toEqual(['slide-renders']);
}

describe('generation publication', () => {
  it('removes the workspace and preserves the active generation when retained copying fails', async () => {
    let generateCalled = false;
    let copyAttempts = 0;

    await expect(runGenerationTransaction({
      outputDirectory,
      copyExisting: true,
      copyEntry: async (_source, destination) => {
        copyAttempts += 1;
        if (copyAttempts === 2) throw new Error('injected retained-copy failure');
        await writeFile(destination, 'partial-copy');
      },
      generate: async () => {
        generateCalled = true;
      },
    })).rejects.toThrow('injected retained-copy failure');

    expect(generateCalled).toBe(false);
    expect(copyAttempts).toBe(2);
    await expectOriginalGeneration();
  });

  it('leaves the existing generation unchanged when validation fails before publication', async () => {
    await expect(runGenerationTransaction({
      outputDirectory,
      copyExisting: true,
      generate: async (workspace) => {
        await writeFile(path.join(workspace, 'slide-001.png'), 'candidate-image');
        throw new Error('injected validation failure');
      },
    })).rejects.toThrow('injected validation failure');

    await expectOriginalGeneration();
  });

  it('rolls back both output and manifest when publication fails after backup', async () => {
    await expect(runGenerationTransaction({
      outputDirectory,
      generate: async (workspace) => {
        await writeFile(path.join(workspace, 'slide-001.png'), 'candidate-image');
        await writeFile(path.join(workspace, 'manifest.json'), 'candidate-manifest');
      },
      publicationHook: (stage) => {
        if (stage === 'after-backup') throw new Error('injected publication failure');
      },
    })).rejects.toThrow('injected publication failure');

    await expectOriginalGeneration();
  });

  it('publishes a complete generation and removes stale files as one directory swap', async () => {
    await runGenerationTransaction({
      outputDirectory,
      generate: async (workspace) => {
        await writeFile(path.join(workspace, 'slide-002.png'), 'new-image');
        await writeFile(path.join(workspace, 'manifest.json'), 'new-manifest');
      },
    });

    expect((await readdir(outputDirectory)).sort()).toEqual(['manifest.json', 'slide-002.png']);
    await expect(readFile(path.join(outputDirectory, 'manifest.json'), 'utf8'))
      .resolves.toBe('new-manifest');
    expect((await readdir(testDirectory)).sort()).toEqual(['slide-renders']);
  });
});
