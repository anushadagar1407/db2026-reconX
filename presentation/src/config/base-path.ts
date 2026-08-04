const SAFE_BASE_PATH = /^\/(?:[A-Za-z0-9._~-]+\/)*$/;

export function normalizeBasePath(candidate: string | undefined = '/'): string {
  if (candidate.length > 512 || !SAFE_BASE_PATH.test(candidate)) {
    throw new Error('PRESENTATION_BASE_PATH must be a safe path beginning and ending with /.');
  }

  return candidate;
}
