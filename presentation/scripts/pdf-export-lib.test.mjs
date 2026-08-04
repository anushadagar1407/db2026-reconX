import path from 'node:path';
import { describe, expect, it } from 'vitest';
import {
  createPdfManifest,
  parsePdfArguments,
  selectPdfOutputPaths,
  validatePdfSummary,
} from './pdf-export-lib.mjs';
import { printModeUrl } from './presentation-export-runtime.mjs';

const validSummary = {
  bytes: 25_000,
  pageCount: 2,
  pages: [
    { widthPoints: 1_469.04, heightPoints: 826.08 },
    { widthPoints: 1_469.04, heightPoints: 826.08 },
  ],
};

describe('PDF exporter CLI', () => {
  it('supports self-contained and validated external URL modes', () => {
    expect(parsePdfArguments([])).toEqual({ help: false, url: null });
    expect(parsePdfArguments(['--help'])).toEqual({ help: true, url: null });
    expect(parsePdfArguments(['--url', 'https://deck.example.test/demo#ignored'])).toEqual({
      help: false,
      url: 'https://deck.example.test/demo',
    });
  });

  it('rejects unsupported, duplicate, missing, and unsafe options', () => {
    expect(() => parsePdfArguments(['--slide', '1'])).toThrow('Unknown option');
    expect(() => parsePdfArguments(['--url'])).toThrow('--url requires a value');
    expect(() => parsePdfArguments(['--url', 'file:///tmp/deck'])).toThrow('must use http or https');
    expect(() => parsePdfArguments([
      '--url',
      'https://one.example.test',
      '--url',
      'https://two.example.test',
    ])).toThrow('only once');
  });

  it.each([
    'https://pdf-user@deck.example.test/',
    'https://pdf-user:pdf-secret@deck.example.test/',
  ])('rejects credential-bearing shared URL parsing for %s', (url) => {
    expect(() => parsePdfArguments(['--url', url])).toThrow(
      '--url must not include embedded credentials; provide a credential-free HTTP(S) URL.',
    );
  });
});

describe('PDF validation and provenance', () => {
  it('validates page count, canonical dimensions, and byte size', () => {
    expect(validatePdfSummary(validSummary, 2)).toEqual({
      bytes: 25_000,
      pageCount: 2,
      pageDimensions: { widthPoints: 1_469.04, heightPoints: 826.08, unit: 'pt' },
    });
  });

  it('fails on page-count mismatch and malformed output', () => {
    expect(() => validatePdfSummary(validSummary, 3)).toThrow(
      'page count 2 does not match discovered slide count 3',
    );
    expect(() => validatePdfSummary({ ...validSummary, bytes: 20 }, 2)).toThrow(
      'unexpectedly small',
    );
    expect(() => validatePdfSummary({
      ...validSummary,
      pages: [
        { widthPoints: 1_440, heightPoints: 810 },
        { widthPoints: 1_440, heightPoints: 810 },
      ],
    }, 2)).toThrow('do not match the required 1469.04x826.08pt');
  });

  it('records dirty source state and validated PDF metadata', () => {
    const manifest = createPdfManifest({
      generatedAt: '2026-08-05T10:00:00.000Z',
      source: {
        sourceCommit: 'abc123',
        sourceDirty: true,
        worktreeDirtyBeforeExport: true,
      },
      sourceMode: 'external-url',
      externalUrl: 'https://deck.example.test/',
      packageVersion: '0.1.0',
      playwrightVersion: '1.61.0',
      pdfjsVersion: '5.4.296',
      slideCount: 2,
      validation: validatePdfSummary(validSummary, 2),
      sha256: 'pdf-hash',
    });

    expect(manifest.sourceCommit).toBe('abc123');
    expect(manifest.sourceDirty).toBe(true);
    expect(manifest.capture).toMatchObject({
      source: 'external-url',
      externalUrl: 'https://deck.example.test/',
      fragmentPolicy: 'all-visible',
      pdfSeparateFragments: false,
      pageContract: {
        widthPoints: 1_469.04,
        heightPoints: 826.08,
        tolerancePoints: 0.05,
        derivation: '1920x1080 Reveal canvas with configured 0.02 print margin',
      },
      slideCount: 2,
    });
    expect(manifest.output).toEqual({
      filename: 'reconx-presentation.pdf',
      sha256: 'pdf-hash',
      bytes: 25_000,
      pageCount: 2,
      pageDimensions: { widthPoints: 1_469.04, heightPoints: 826.08, unit: 'pt' },
    });
  });
});

describe('shared PDF runtime paths', () => {
  it('selects distinct final paths for generation-level atomic publication', () => {
    const outputDirectory = path.join('presentation', 'pdf-export');
    expect(selectPdfOutputPaths(outputDirectory)).toEqual({
      pdfPath: path.join(outputDirectory, 'reconx-presentation.pdf'),
      manifestPath: path.join(outputDirectory, 'manifest.json'),
    });
  });

  it('adds Reveal print mode without losing an existing query', () => {
    const printUrl = new URL(printModeUrl('https://deck.example.test/path?mode=qa#old'));
    expect(printUrl.pathname).toBe('/path');
    expect(printUrl.searchParams.get('mode')).toBe('qa');
    expect(printUrl.searchParams.has('print-pdf')).toBe(true);
    expect(printUrl.hash).toBe('');
  });
});
