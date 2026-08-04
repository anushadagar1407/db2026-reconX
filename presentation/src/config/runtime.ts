const DEFAULT_DEMO_URL = 'http://localhost:5173';

function safeHttpUrl(candidate: string | undefined, baseUrl: string): string | null {
  if (!candidate?.trim()) return null;

  try {
    const url = new URL(candidate.trim(), baseUrl);
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.toString() : null;
  } catch {
    return null;
  }
}

export function getDemoUrl(
  runtimeConfig = window.__RECONX_PRESENTATION_CONFIG__,
  baseUrl = window.location.origin,
): string | null {
  if (runtimeConfig?.demoUrl === null) return null;

  const configuredUrl = safeHttpUrl(runtimeConfig?.demoUrl, baseUrl);
  return configuredUrl ?? DEFAULT_DEMO_URL;
}
