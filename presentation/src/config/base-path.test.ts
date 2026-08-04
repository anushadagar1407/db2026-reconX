import { describe, expect, it } from 'vitest';
import { normalizeBasePath } from './base-path';

describe('normalizeBasePath', () => {
  it('defaults to the root path when no environment value is provided', () => {
    expect(normalizeBasePath()).toBe('/');
  });

  it('accepts a safe repository project path', () => {
    expect(normalizeBasePath('/db2026-reconX/')).toBe('/db2026-reconX/');
  });

  it.each(['', 'db2026-reconX/', '/db2026-reconX', '/db2026-reconX//', '/db?query/'])('rejects unsafe or unnormalized path %s', (candidate) => {
    expect(() => normalizeBasePath(candidate)).toThrow(
      'PRESENTATION_BASE_PATH must be a safe path beginning and ending with /.',
    );
  });
});
