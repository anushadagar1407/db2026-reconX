import { cleanup, render, screen } from '@testing-library/react';
import { StrictMode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useInfiniteScroll } from '../useInfiniteScroll.js';

class FakeIntersectionObserver {
  static instances = [];

  constructor(callback, options) {
    this.callback = callback;
    this.options = options;
    this.observe = vi.fn();
    this.unobserve = vi.fn();
    this.disconnect = vi.fn();
    FakeIntersectionObserver.instances.push(this);
  }

  trigger(...entries) {
    this.callback(entries, this);
  }
}

function Sentinel({ loadMore, nodeKey = 'sentinel' }) {
  const sentinelRef = useInfiniteScroll(loadMore);

  return <div key={nodeKey} data-testid="sentinel" ref={sentinelRef} />;
}

const originalIntersectionObserver = window.IntersectionObserver;

describe('useInfiniteScroll', () => {
  beforeEach(() => {
    FakeIntersectionObserver.instances = [];
    window.IntersectionObserver = FakeIntersectionObserver;
  });

  afterEach(() => {
    cleanup();
    window.IntersectionObserver = originalIntersectionObserver;
  });

  it('observes the sentinel and ignores non-intersecting entries', () => {
    const loadMore = vi.fn();

    render(<Sentinel loadMore={loadMore} />);

    const observer = FakeIntersectionObserver.instances[0];
    const sentinel = screen.getByTestId('sentinel');
    expect(observer.observe).toHaveBeenCalledWith(sentinel);

    observer.trigger({ isIntersecting: false });
    expect(loadMore).not.toHaveBeenCalled();

    observer.trigger({ isIntersecting: true });
    expect(loadMore).toHaveBeenCalledTimes(1);
  });

  it('uses the latest callback without recreating the observer', () => {
    const firstLoadMore = vi.fn();
    const latestLoadMore = vi.fn();
    const { rerender } = render(<Sentinel loadMore={firstLoadMore} />);
    const observer = FakeIntersectionObserver.instances[0];

    observer.trigger({ isIntersecting: true });
    expect(firstLoadMore).toHaveBeenCalledTimes(1);

    rerender(<Sentinel loadMore={latestLoadMore} />);

    // RTL's rerender commits the update and flushes the hook's callback effect.
    expect(FakeIntersectionObserver.instances).toHaveLength(1);
    observer.trigger({ isIntersecting: true });

    expect(firstLoadMore).toHaveBeenCalledTimes(1);
    expect(latestLoadMore).toHaveBeenCalledTimes(1);
  });

  it('unobserves the old node and observes its replacement', () => {
    const loadMore = vi.fn();
    const { rerender } = render(<Sentinel loadMore={loadMore} nodeKey="first" />);
    const observer = FakeIntersectionObserver.instances[0];
    const firstNode = screen.getByTestId('sentinel');

    rerender(<Sentinel loadMore={loadMore} nodeKey="second" />);

    const secondNode = screen.getByTestId('sentinel');
    expect(observer.unobserve).toHaveBeenCalledWith(firstNode);
    expect(observer.observe).toHaveBeenLastCalledWith(secondNode);
  });

  it('disconnects the observer on unmount', () => {
    const { unmount } = render(<Sentinel loadMore={vi.fn()} />);
    const observer = FakeIntersectionObserver.instances[0];

    unmount();

    expect(observer.disconnect).toHaveBeenCalledTimes(1);
  });

  it('leaves one active observer under StrictMode', () => {
    const loadMore = vi.fn();

    const { unmount } = render(
      <StrictMode>
        <Sentinel loadMore={loadMore} />
      </StrictMode>
    );

    const observers = FakeIntersectionObserver.instances;
    const activeObserver = observers[observers.length - 1];
    observers.slice(0, -1).forEach((observer) => {
      expect(observer.disconnect).toHaveBeenCalledTimes(1);
    });

    activeObserver.trigger({ isIntersecting: true });
    expect(loadMore).toHaveBeenCalledTimes(1);

    unmount();
    expect(activeObserver.disconnect).toHaveBeenCalledTimes(1);
  });
});
