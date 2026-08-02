// TICKET-ADV112 — HTTP boundary for the versioned ReconX API.
const BASE = '/api';

function authHeaders() {
  const token = typeof sessionStorage === 'undefined'
    ? null
    : sessionStorage.getItem('reconx-token');

  return token ? { Authorization: `Bearer ${token}` } : {};
}

function normalizeQuery(params) {
  if (!params) return '';
  if (typeof params === 'string') return params.replace(/^[?&]+/, '');
  return new window.URLSearchParams(params).toString();
}

function withQuery(path, params) {
  const query = normalizeQuery(params);
  return query ? `${path}?${query}` : path;
}

async function readBody(response) {
  if (typeof response.text === 'function') {
    try {
      const text = await response.text();
      if (!text.trim()) return null;

      try {
        return JSON.parse(text);
      } catch {
        return text;
      }
    } catch {
      return null;
    }
  }

  if (typeof response.json === 'function') {
    try {
      return await response.json();
    } catch {
      return null;
    }
  }

  return null;
}

function errorDetail(body, statusText) {
  if (body && typeof body === 'object') {
    for (const property of ['detail', 'message', 'error', 'title']) {
      if (typeof body[property] === 'string' && body[property].trim()) {
        return body[property];
      }
    }

    try {
      return JSON.stringify(body);
    } catch {
      return 'Request failed';
    }
  }

  if (typeof body === 'string' && body.trim()) return body;
  return statusText || 'Request failed';
}

async function request(method, path, body, { authenticated = true } = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(authenticated ? authHeaders() : {}),
  };
  const options = { method, headers };

  if (body !== undefined) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`${BASE}${path}`, options);

  if (response.status === 204) return null;

  if (!response.ok) {
    const bodyContent = await readBody(response);
    const error = new Error(
      `HTTP ${response.status}: ${errorDetail(bodyContent, response.statusText)}`
    );
    error.status = response.status;
    error.statusCode = response.status;
    error.body = bodyContent;
    throw error;
  }

  return readBody(response);
}

export const api = {
  login: (email, password) => request(
    'POST',
    '/auth/login',
    { email, password },
    { authenticated: false }
  ),
  listTrades: (params = '') => request('GET', withQuery('/v1/trades', params)),
  createTrade: (req) => request('POST', '/v1/trades', req),
  updateStatus: (id, status) => request('PATCH', `/v1/trades/${encodeURIComponent(id)}/status`, { status }),
  deleteTrade: (id) => request('DELETE', `/v1/trades/${encodeURIComponent(id)}`),
  runRecon: (req) => request('POST', '/v1/recon/run', req),
  reconResults: (jobId) => request('GET', `/v1/recon/jobs/${encodeURIComponent(jobId)}/results`),
  audit: (tradeRef) => request('GET', `/v1/audit/trades/${encodeURIComponent(tradeRef)}`),
};
