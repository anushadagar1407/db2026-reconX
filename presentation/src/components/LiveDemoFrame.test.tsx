import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { LiveDemoFrame } from './LiveDemoFrame';

describe('LiveDemoFrame', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('loads the configured app only after explicit activation', () => {
    render(<LiveDemoFrame src="http://localhost:5173" />);

    expect(screen.queryByTitle('ReconX live demo')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Embedded demo idle state')).toHaveTextContent(
      'No runtime capture is claimed here.',
    );
    fireEvent.click(screen.getByRole('button', { name: 'Launch embedded demo' }));

    const frame = screen.getByTitle('ReconX live demo');
    expect(screen.queryByLabelText('Embedded demo idle state')).not.toBeInTheDocument();
    expect(frame).toHaveAttribute(
      'src',
      'http://localhost:5173/?presentationOrigin=http%3A%2F%2Flocalhost%3A3000',
    );
    expect(frame).toHaveAttribute(
      'sandbox',
      'allow-forms allow-popups allow-same-origin allow-scripts',
    );
    act(() => window.dispatchEvent(new MessageEvent('message', {
      data: { type: 'reconx:ready' },
      origin: 'https://unexpected.example.test',
      source: (frame as HTMLIFrameElement).contentWindow,
    })));
    expect(screen.getByRole('status')).toHaveTextContent('Demo: loading');
    act(() => window.dispatchEvent(new MessageEvent('message', {
      data: { type: 'reconx:ready' },
      origin: 'http://localhost:5173',
      source: window,
    })));
    expect(screen.getByRole('status')).toHaveTextContent('Demo: loading');
    act(() => window.dispatchEvent(new MessageEvent('message', {
      data: { type: 'reconx:ready' },
      origin: 'http://localhost:5173',
      source: (frame as HTMLIFrameElement).contentWindow,
    })));
    expect(screen.getByRole('status')).toHaveTextContent('Demo: ready');
  });

  it('offers recovery when the app does not load in time', () => {
    vi.useFakeTimers();
    render(<LiveDemoFrame src="http://localhost:5173" loadTimeoutMs={100} />);

    fireEvent.click(screen.getByRole('button', { name: 'Launch embedded demo' }));
    act(() => vi.advanceTimersByTime(100));

    expect(screen.getByRole('alert')).toHaveTextContent('did not become available');
    expect(screen.getByRole('alert')).toHaveTextContent('full-demo link or switch to a source/API walkthrough');
    expect(screen.queryByText(/prepared fallback/i)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open full demo' })).toHaveAttribute(
      'href',
      'http://localhost:5173',
    );
  });
});
