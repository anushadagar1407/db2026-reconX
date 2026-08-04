import { describe, expect, it } from 'vitest';
import { getDemoUrl } from './runtime';

describe('getDemoUrl', () => {
  it('supports a same-origin path in runtime configuration', () => {
    expect(getDemoUrl({ demoUrl: '/demo' }, 'http://localhost:4173')).toBe(
      'http://localhost:4173/demo',
    );
  });

  it('rejects non-http iframe URLs', () => {
    expect(getDemoUrl(
      { demoUrl: 'javascript:alert(1)' },
      'http://localhost:4173',
    )).toBe('http://localhost:5173');
  });
});
