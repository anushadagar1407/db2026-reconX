import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AutoFit } from './AutoFit';

class ResizeObserverMock {
  static instances: ResizeObserverMock[] = [];

  readonly observe = vi.fn();
  readonly disconnect = vi.fn();
  readonly unobserve = vi.fn();

  constructor(private readonly callback: ResizeObserverCallback) {
    ResizeObserverMock.instances.push(this);
  }

  trigger() {
    this.callback([], this as unknown as ResizeObserver);
  }
}

function setDimensions(
  element: Element,
  dimensions: Partial<Record<'clientHeight' | 'clientWidth' | 'scrollHeight' | 'scrollWidth', number>>,
) {
  for (const [property, value] of Object.entries(dimensions)) {
    Object.defineProperty(element, property, { configurable: true, value });
  }
}

describe('AutoFit', () => {
  beforeEach(() => {
    ResizeObserverMock.instances = [];
    vi.stubGlobal('ResizeObserver', ResizeObserverMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('fits against both axes and responds to observer notifications', () => {
    const { container } = render(
      <AutoFit data-testid="autofit">
        <div>Dense content</div>
      </AutoFit>,
    );
    const frame = screen.getByTestId('autofit');
    const content = container.querySelector('.ds-autofit__content')!;
    setDimensions(frame, { clientWidth: 900, clientHeight: 300 });
    setDimensions(content, { scrollWidth: 1_000, scrollHeight: 400 });

    act(() => ResizeObserverMock.instances[0].trigger());

    expect(frame).toHaveAttribute('data-scale', '0.75');
    expect(frame).toHaveAttribute('data-overflow', 'false');
    expect(ResizeObserverMock.instances[0].observe).toHaveBeenCalledWith(frame);
    expect(ResizeObserverMock.instances[0].observe).toHaveBeenCalledWith(content);
  });

  it('holds the readable minimum, signals overflow, and recovers after content shrinks', () => {
    const onOverflowChange = vi.fn();
    const warning = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    const { container } = render(
      <AutoFit data-testid="autofit" debugName="trade matrix" onOverflowChange={onOverflowChange}>
        <div>Overfull content</div>
      </AutoFit>,
    );
    const frame = screen.getByTestId('autofit');
    const content = container.querySelector('.ds-autofit__content')!;
    setDimensions(frame, { clientWidth: 600, clientHeight: 300 });
    setDimensions(content, { scrollWidth: 1_200, scrollHeight: 800 });

    act(() => ResizeObserverMock.instances[0].trigger());

    expect(frame).toHaveAttribute('data-scale', '0.72');
    expect(frame).toHaveAttribute('data-overflow', 'true');
    expect(onOverflowChange).toHaveBeenLastCalledWith(true);
    expect(warning).toHaveBeenCalledWith(expect.stringContaining('AutoFit "trade matrix"'));

    setDimensions(content, { scrollWidth: 500, scrollHeight: 200 });
    act(() => ResizeObserverMock.instances[0].trigger());

    expect(frame).toHaveAttribute('data-scale', '1');
    expect(frame).toHaveAttribute('data-overflow', 'false');
    expect(onOverflowChange).toHaveBeenLastCalledWith(false);
  });

  it('will not accept a minimum scale below the legibility floor', () => {
    const warning = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    const { container } = render(
      <AutoFit data-testid="autofit" minimumScale={0.3}>
        <div>Overfull content</div>
      </AutoFit>,
    );
    const frame = screen.getByTestId('autofit');
    const content = container.querySelector('.ds-autofit__content')!;
    setDimensions(frame, { clientWidth: 500, clientHeight: 500 });
    setDimensions(content, { scrollWidth: 1_000, scrollHeight: 500 });

    act(() => ResizeObserverMock.instances[0].trigger());

    expect(frame).toHaveAttribute('data-scale', '0.6');
    expect(frame).toHaveAttribute('data-overflow', 'true');
    expect(warning).toHaveBeenCalledWith(expect.stringContaining('readable minimum 0.6'));
  });

  it.each([Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY])(
    'normalizes non-finite minimum scale %s to the readable default',
    (minimumScale) => {
      const warning = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
      const { container } = render(
        <AutoFit data-testid="autofit" minimumScale={minimumScale}>
          <div>Overfull content</div>
        </AutoFit>,
      );
      const frame = screen.getByTestId('autofit');
      const content = container.querySelector('.ds-autofit__content')!;
      setDimensions(frame, { clientWidth: 500, clientHeight: 500 });
      setDimensions(content, { scrollWidth: 1_000, scrollHeight: 500 });

      act(() => ResizeObserverMock.instances[0].trigger());

      expect(frame).toHaveAttribute('data-scale', '0.72');
      expect(frame).toHaveAttribute('data-overflow', 'true');
      expect(warning).toHaveBeenCalledWith(expect.stringContaining('readable minimum 0.72'));
    },
  );

  it('falls back deterministically to window resize when ResizeObserver is unavailable', () => {
    vi.stubGlobal('ResizeObserver', undefined);
    const { container } = render(
      <AutoFit data-testid="autofit">
        <div>Fallback content</div>
      </AutoFit>,
    );
    const frame = screen.getByTestId('autofit');
    const content = container.querySelector('.ds-autofit__content')!;
    setDimensions(frame, { clientWidth: 800, clientHeight: 500 });
    setDimensions(content, { scrollWidth: 1_000, scrollHeight: 500 });

    fireEvent(window, new Event('resize'));

    expect(frame).toHaveAttribute('data-scale', '0.8');
    expect(frame).toHaveAttribute('data-overflow', 'false');
  });
});
