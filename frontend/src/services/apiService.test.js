import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from './apiService.js';

function jsonResponse(body, status = 200, statusText = 'OK') {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText,
    text: vi.fn().mockResolvedValue(JSON.stringify(body)),
  };
}

function textResponse(body, status, statusText) {
  return {
    ok: false,
    status,
    statusText,
    text: vi.fn().mockResolvedValue(body),
  };
}

describe('api service', () => {
  let fetchMock;

  beforeEach(() => {
    sessionStorage.clear();
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it('sends login credentials without an Authorization header', async () => {
    const response = {
      token: 'jwt-token',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      role: 'TRADER',
    };
    fetchMock.mockResolvedValue(jsonResponse(response));

    await expect(api.login('trader@db.com', 'trader123')).resolves.toEqual(response);

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'trader@db.com', password: 'trader123' }),
    });
  });

  it('attaches a stored Bearer token and normalizes query strings', async () => {
    const response = { items: [], page: 0, size: 5, totalElements: 0, totalPages: 0, last: true };
    sessionStorage.setItem('reconx-token', 'stored-token');
    fetchMock.mockResolvedValue(jsonResponse(response));

    await expect(api.listTrades('?page=0&size=5')).resolves.toEqual(response);

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/trades?page=0&size=5', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer stored-token',
      },
    });

    await api.listTrades();
    expect(fetchMock).toHaveBeenLastCalledWith('/api/v1/trades', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer stored-token',
      },
    });
  });

  it('uses the current trade, recon, and audit paths', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ accepted: true }));

    await api.createTrade({ tradeRef: 'ABC-20260802-0001' });
    await api.updateStatus(42, 'MATCHED');
    await api.deleteTrade(42);
    await api.runRecon({ from: '2026-08-01', to: '2026-08-02' });
    await api.reconResults('job-42');
    await api.audit('ABC-20260802-0001');

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      '/api/v1/trades',
      '/api/v1/trades/42/status',
      '/api/v1/trades/42',
      '/api/v1/recon/run',
      '/api/v1/recon/jobs/job-42/results',
      '/api/v1/audit/trades/ABC-20260802-0001',
    ]);
  });

  it('returns null for a bodyless 204 response without reading a body', async () => {
    const response = {
      ok: true,
      status: 204,
      text: vi.fn().mockRejectedValue(new Error('body must not be read')),
    };
    fetchMock.mockResolvedValue(response);

    await expect(api.deleteTrade(42)).resolves.toBeNull();
    expect(response.text).not.toHaveBeenCalled();

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/trades/42', {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
    });
  });

  it('prefers RFC 7807 detail and preserves the HTTP status', async () => {
    fetchMock.mockResolvedValue(jsonResponse({
      type: 'https://reconx.example/errors/invalid-trade',
      title: 'Invalid trade',
      status: 400,
      detail: 'quantity must be positive',
    }, 400, 'Bad Request'));

    await expect(api.createTrade({ quantity: -1 })).rejects.toMatchObject({
      message: 'HTTP 400: quantity must be positive',
      status: 400,
      statusCode: 400,
    });
  });

  it.each([401, 403])('handles a bodyless %s response safely', async (status) => {
    fetchMock.mockResolvedValue({
      ok: false,
      status,
      statusText: '',
      text: vi.fn().mockResolvedValue(''),
    });

    await expect(api.listTrades()).rejects.toMatchObject({
      message: `HTTP ${status}: Request failed`,
      status,
    });
  });

  it('uses a safe message for text and non-ProblemDetail JSON failures', async () => {
    fetchMock.mockResolvedValueOnce(textResponse('upstream failed', 502, 'Bad Gateway'));
    await expect(api.listTrades()).rejects.toMatchObject({
      message: 'HTTP 502: upstream failed',
      status: 502,
    });

    fetchMock.mockResolvedValueOnce(jsonResponse({ message: 'service unavailable' }, 503));
    await expect(api.listTrades()).rejects.toMatchObject({
      message: 'HTTP 503: service unavailable',
      status: 503,
    });
  });
});
