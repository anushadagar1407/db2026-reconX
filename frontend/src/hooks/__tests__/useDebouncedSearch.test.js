import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useDebouncedSearch } from '../useDebouncedSearch.js';

describe('useDebouncedSearch', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('returns the current query before the debounce delay elapses', () => {
    const { result } = renderHook(() => useDebouncedSearch('AAPL', 300));

    expect(result.current).toBe('AAPL');
  });

  it('applies only the trailing value after rapid query changes', () => {
    const { result, rerender } = renderHook(
      ({ query, delay }) => useDebouncedSearch(query, delay),
      { initialProps: { query: '', delay: 300 } }
    );

    rerender({ query: 'A', delay: 300 });
    rerender({ query: 'AA', delay: 300 });
    rerender({ query: 'AAPL', delay: 300 });

    act(() => vi.advanceTimersByTime(299));
    expect(result.current).toBe('');

    act(() => vi.advanceTimersByTime(1));
    expect(result.current).toBe('AAPL');
  });

  it('cancels the old timer when the delay changes', () => {
    const { result, rerender } = renderHook(
      ({ query, delay }) => useDebouncedSearch(query, delay),
      { initialProps: { query: 'initial', delay: 300 } }
    );

    rerender({ query: 'updated', delay: 100 });

    act(() => vi.advanceTimersByTime(100));
    expect(result.current).toBe('updated');

    act(() => vi.advanceTimersByTime(200));
    expect(result.current).toBe('updated');
  });

  it('handles a zero delay as a trailing update', () => {
    const { result, rerender } = renderHook(
      ({ query, delay }) => useDebouncedSearch(query, delay),
      { initialProps: { query: 'before', delay: 0 } }
    );

    rerender({ query: 'after', delay: 0 });
    expect(result.current).toBe('before');

    act(() => vi.runOnlyPendingTimers());
    expect(result.current).toBe('after');
  });

  it('cancels pending work when unmounted', () => {
    const { result, rerender, unmount } = renderHook(
      ({ query, delay }) => useDebouncedSearch(query, delay),
      { initialProps: { query: 'before', delay: 300 } }
    );

    rerender({ query: 'after', delay: 300 });
    unmount();

    act(() => vi.runOnlyPendingTimers());
    expect(result.current).toBe('before');
  });
});
