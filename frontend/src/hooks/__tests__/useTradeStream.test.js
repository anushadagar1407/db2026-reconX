import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useTradeStream } from '../useTradeStream.js';

class FakeEventSource {
  static instances = [];

  constructor(url) {
    this.url = url;
    this.closeCalls = 0;
    FakeEventSource.instances.push(this);
  }

  close() {
    this.closeCalls += 1;
  }

  open() {
    this.onopen?.();
  }

  message(data) {
    this.onmessage?.({ data });
  }

  error() {
    this.onerror?.({ type: 'error' });
  }
}

describe('useTradeStream', () => {
  beforeEach(() => {
    FakeEventSource.instances = [];
    vi.stubGlobal('EventSource', FakeEventSource);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('opens the documented default stream and tracks connection state', () => {
    const { result, unmount } = renderHook(() => useTradeStream());
    const source = FakeEventSource.instances[0];

    expect(FakeEventSource.instances).toHaveLength(1);
    expect(source.url).toBe('/api/v1/trades/stream');
    expect(result.current.isConnected).toBe(false);

    act(() => source.open());
    expect(result.current.isConnected).toBe(true);

    act(() => source.error());
    expect(result.current.isConnected).toBe(false);

    unmount();
    expect(source.closeCalls).toBe(1);
  });

  it('parses messages, prepends new trades, and ignores malformed events', () => {
    const { result, unmount } = renderHook(() => useTradeStream('/stream'));
    const source = FakeEventSource.instances[0];
    const firstTrade = { id: 1, status: 'MATCHED' };
    const secondTrade = { id: 2, status: 'UNMATCHED' };

    act(() => source.message(JSON.stringify(firstTrade)));
    expect(result.current.trades).toEqual([firstTrade]);
    const tradesAfterValidEvent = result.current.trades;

    act(() => source.message('{malformed'));
    expect(result.current.trades).toBe(tradesAfterValidEvent);

    act(() => source.message(JSON.stringify(secondTrade)));
    expect(result.current.trades).toEqual([secondTrade, firstTrade]);
    expect(result.current.trades).not.toBe(tradesAfterValidEvent);

    unmount();
  });

  it('keeps only the newest 200 entries', () => {
    const { result, unmount } = renderHook(() => useTradeStream('/stream'));
    const source = FakeEventSource.instances[0];

    act(() => {
      for (let id = 0; id < 205; id += 1) {
        source.message(JSON.stringify({ id }));
      }
    });

    expect(result.current.trades).toHaveLength(200);
    expect(result.current.trades[0]).toEqual({ id: 204 });
    expect(result.current.trades[199]).toEqual({ id: 5 });

    unmount();
  });

  it('creates one source per URL, closes on change, and ignores stale events', () => {
    const { result, rerender, unmount } = renderHook(
      ({ url }) => useTradeStream(url),
      { initialProps: { url: '/one' } },
    );
    const first = FakeEventSource.instances[0];

    act(() => {
      first.open();
      first.message('{"id":"old"}');
    });
    expect(result.current.isConnected).toBe(true);

    rerender({ url: '/one' });
    expect(FakeEventSource.instances).toHaveLength(1);

    rerender({ url: '/two' });
    const second = FakeEventSource.instances[1];
    expect(first.closeCalls).toBe(1);
    expect(second.url).toBe('/two');
    expect(result.current.isConnected).toBe(false);

    act(() => second.open());
    act(() => {
      first.open();
      first.message('{"id":"stale"}');
      first.error();
    });
    expect(result.current.isConnected).toBe(true);
    expect(result.current.trades).toEqual([{ id: 'old' }]);

    act(() => second.message('{"id":"new"}'));
    expect(result.current.trades).toEqual([{ id: 'new' }, { id: 'old' }]);

    unmount();
    expect(second.closeCalls).toBe(1);
  });

  it('ignores events after unmount', () => {
    const { unmount } = renderHook(() => useTradeStream('/stream'));
    const source = FakeEventSource.instances[0];
    const staleMessageHandler = source.onmessage;

    act(() => source.open());
    unmount();

    let dataReads = 0;
    const staleEvent = {};
    Object.defineProperty(staleEvent, 'data', {
      get() {
        dataReads += 1;
        throw new Error('stale event data should not be read');
      },
    });

    act(() => {
      source.open();
      staleMessageHandler?.(staleEvent);
      source.error();
    });

    expect(source.closeCalls).toBe(1);
    expect(dataReads).toBe(0);
  });
});
