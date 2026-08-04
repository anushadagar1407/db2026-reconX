import { describe, expect, it, vi } from 'vitest';
import { announcePresentationReady } from './presentationBridge.js';

describe('announcePresentationReady', () => {
  it('sends the readiness handshake only to the configured presentation origin', () => {
    const parentWindow = { postMessage: vi.fn() };

    expect(announcePresentationReady({
      search: '?presentationOrigin=http%3A%2F%2Flocalhost%3A4173',
      parentWindow,
      currentWindow: {},
    })).toBe(true);
    expect(parentWindow.postMessage).toHaveBeenCalledWith(
      { type: 'reconx:ready', version: 1 },
      'http://localhost:4173',
    );
  });

  it('does not message a parent without a valid http origin', () => {
    const parentWindow = { postMessage: vi.fn() };

    expect(announcePresentationReady({
      search: '?presentationOrigin=javascript%3Aalert(1)',
      parentWindow,
      currentWindow: {},
    })).toBe(false);
    expect(parentWindow.postMessage).not.toHaveBeenCalled();
  });

  it('does nothing outside an iframe', () => {
    const currentWindow = { postMessage: vi.fn() };

    expect(announcePresentationReady({
      search: '?presentationOrigin=http%3A%2F%2Flocalhost%3A4173',
      parentWindow: currentWindow,
      currentWindow,
    })).toBe(false);
    expect(currentWindow.postMessage).not.toHaveBeenCalled();
  });
});
