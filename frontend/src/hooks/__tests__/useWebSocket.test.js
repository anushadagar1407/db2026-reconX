import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useWebSocket } from '../useWebSocket.js';

class FakeWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;
  static instances = [];

  constructor(url) {
    this.url = url;
    this.readyState = FakeWebSocket.CONNECTING;
    this.sent = [];
    this.closeCalls = 0;
    FakeWebSocket.instances.push(this);
  }

  send(payload) {
    this.sent.push(payload);
  }

  close() {
    this.closeCalls += 1;
    this.readyState = FakeWebSocket.CLOSED;
    this.onclose?.({ code: 1000 });
  }

  open() {
    this.readyState = FakeWebSocket.OPEN;
    this.onopen?.();
  }

  message(data) {
    this.onmessage?.({ data });
  }

  error() {
    this.onerror?.({ type: 'error' });
  }

  serverClose() {
    this.readyState = FakeWebSocket.CLOSED;
    this.onclose?.({ code: 1006 });
  }
}

function advanceTimers(milliseconds) {
  act(() => {
    vi.advanceTimersByTime(milliseconds);
  });
}

describe('useWebSocket', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    FakeWebSocket.instances = [];
    vi.stubGlobal('WebSocket', FakeWebSocket);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('opens one socket, tracks state, parses JSON and falls back to raw data', () => {
    const { result, unmount } = renderHook(() => useWebSocket('/socket'));
    const socket = FakeWebSocket.instances[0];

    expect(FakeWebSocket.instances).toHaveLength(1);
    expect(result.current.status).toBe('connecting');

    act(() => socket.open());
    expect(result.current.status).toBe('open');

    act(() => socket.message('{"tradeRef":"TRD-1"}'));
    expect(result.current.data).toEqual({ tradeRef: 'TRD-1' });

    act(() => socket.message('not-json'));
    expect(result.current.data).toBe('not-json');

    unmount();
    expect(socket.closeCalls).toBe(1);
  });

  it('guards send until OPEN and stringifies non-string payloads', () => {
    const { result, unmount } = renderHook(() => useWebSocket('/socket'));
    const socket = FakeWebSocket.instances[0];

    act(() => result.current.send({ id: 1 }));
    expect(socket.sent).toEqual([]);

    act(() => socket.open());
    const send = result.current.send;
    act(() => {
      result.current.send('raw');
      result.current.send({ id: 1 });
    });

    expect(result.current.send).toBe(send);
    expect(socket.sent).toEqual(['raw', '{"id":1}']);

    act(() => socket.serverClose());
    act(() => result.current.send('after-close'));
    expect(socket.sent).toEqual(['raw', '{"id":1}']);

    unmount();
  });

  it('reports errors and closed state without reconnecting when disabled', () => {
    const { result, unmount } = renderHook(() => useWebSocket('/socket', { reconnect: false }));
    const socket = FakeWebSocket.instances[0];

    act(() => socket.error());
    expect(result.current.status).toBe('error');

    act(() => socket.serverClose());
    expect(result.current.status).toBe('closed');
    advanceTimers(30_000);
    expect(FakeWebSocket.instances).toHaveLength(1);

    unmount();
  });

  it('reconnects with exponential delays and stops at maxRetries', () => {
    const { result, unmount } = renderHook(() => useWebSocket('/socket', { maxRetries: 2 }));
    const first = FakeWebSocket.instances[0];

    act(() => first.serverClose());
    expect(result.current.status).toBe('closed');
    advanceTimers(499);
    expect(FakeWebSocket.instances).toHaveLength(1);
    advanceTimers(1);
    expect(FakeWebSocket.instances).toHaveLength(2);

    act(() => FakeWebSocket.instances[1].serverClose());
    advanceTimers(999);
    expect(FakeWebSocket.instances).toHaveLength(2);
    advanceTimers(1);
    expect(FakeWebSocket.instances).toHaveLength(3);

    act(() => FakeWebSocket.instances[2].serverClose());
    advanceTimers(30_000);
    expect(FakeWebSocket.instances).toHaveLength(3);

    unmount();
  });

  it('defaults to five reconnect attempts', () => {
    const { unmount } = renderHook(() => useWebSocket('/socket'));

    for (let attempt = 0; attempt < 5; attempt += 1) {
      act(() => FakeWebSocket.instances[attempt].serverClose());
      advanceTimers(500 * 2 ** attempt);
    }

    expect(FakeWebSocket.instances).toHaveLength(6);
    act(() => FakeWebSocket.instances[5].serverClose());
    advanceTimers(30_000);
    expect(FakeWebSocket.instances).toHaveLength(6);

    unmount();
  });

  it('caps backoff at 30 seconds', () => {
    const { unmount } = renderHook(() => useWebSocket('/socket', { maxRetries: 7 }));

    const delays = [500, 1_000, 2_000, 4_000, 8_000, 16_000];
    delays.forEach((delay, index) => {
      act(() => FakeWebSocket.instances[index].serverClose());
      advanceTimers(delay);
      expect(FakeWebSocket.instances).toHaveLength(index + 2);
    });

    act(() => FakeWebSocket.instances[6].serverClose());
    advanceTimers(29_999);
    expect(FakeWebSocket.instances).toHaveLength(7);
    advanceTimers(1);
    expect(FakeWebSocket.instances).toHaveLength(8);

    unmount();
  });

  it('cancels a pending reconnect timer and closes on unmount', () => {
    const { result, unmount } = renderHook(() => useWebSocket('/socket'));
    const socket = FakeWebSocket.instances[0];

    act(() => socket.serverClose());
    unmount();
    advanceTimers(30_000);

    expect(FakeWebSocket.instances).toHaveLength(1);
    expect(result.current.status).toBe('closed');
  });

  it('closes the old socket and ignores stale events when URL or options change', () => {
    const { result, rerender, unmount } = renderHook(
      ({ url, options }) => useWebSocket(url, options),
      { initialProps: { url: '/one', options: { reconnect: true } } },
    );
    const first = FakeWebSocket.instances[0];
    const sendBeforeChange = result.current.send;

    rerender({ url: '/two', options: { reconnect: false } });
    const second = FakeWebSocket.instances[1];

    expect(first.closeCalls).toBe(1);
    expect(second.url).toBe('/two');
    expect(result.current.status).toBe('connecting');
    expect(result.current.send).toBe(sendBeforeChange);

    act(() => {
      first.open();
      first.message('{"stale":true}');
      first.serverClose();
    });
    expect(result.current.status).toBe('connecting');
    expect(result.current.data).toBeNull();
    expect(FakeWebSocket.instances).toHaveLength(2);

    act(() => second.open());
    act(() => second.message('{"fresh":true}'));
    expect(result.current.status).toBe('open');
    expect(result.current.data).toEqual({ fresh: true });

    unmount();
    expect(second.closeCalls).toBe(1);
  });

  it('cancels a pending retry when the URL changes', () => {
    const { rerender, unmount } = renderHook(
      ({ url }) => useWebSocket(url),
      { initialProps: { url: '/one' } },
    );
    const first = FakeWebSocket.instances[0];

    act(() => first.serverClose());
    rerender({ url: '/two' });
    advanceTimers(30_000);

    expect(FakeWebSocket.instances).toHaveLength(2);
    expect(FakeWebSocket.instances[1].url).toBe('/two');

    unmount();
  });
});
