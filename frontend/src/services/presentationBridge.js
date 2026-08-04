const READY_MESSAGE = Object.freeze({ type: 'reconx:ready', version: 1 });

export function announcePresentationReady({
  search = window.location.search,
  parentWindow = window.parent,
  currentWindow = window,
} = {}) {
  if (parentWindow === currentWindow) return false;

  const configuredOrigin = new URLSearchParams(search).get('presentationOrigin');
  if (!configuredOrigin) return false;

  try {
    const target = new URL(configuredOrigin);
    if (!['http:', 'https:'].includes(target.protocol)) return false;

    parentWindow.postMessage(READY_MESSAGE, target.origin);
    return true;
  } catch {
    return false;
  }
}
