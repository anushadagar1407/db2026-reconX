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

  it('returns a disabled value for an explicit null', () => {
    expect(getDemoUrl({ demoUrl: null })).toBeNull();
  });

  it('preserves a valid HTTPS runtime URL', () => {
    expect(getDemoUrl({ demoUrl: 'https://demo.example.test' })).toBe(
      'https://demo.example.test/',
    );
  });

  it('falls back for a malformed runtime URL', () => {
    expect(getDemoUrl({ demoUrl: 'https://[invalid' })).toBe('http://localhost:5173');
  });
});
