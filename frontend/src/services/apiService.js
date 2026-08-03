const BASE = '/api';

function authHeaders() {
  const token = sessionStorage.getItem('reconx-token');

  return token
    ? { Authorization: `Bearer ${token}` }
    : {};
}

async function request(method, path, body) {
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
    },
    body: body === undefined
      ? undefined
      : JSON.stringify(body),
  });

  if (!response.ok) {
    let detail = response.statusText;

    try {
      const errorBody = await response.json();
      detail =
        errorBody.detail ??
        errorBody.message ??
        JSON.stringify(errorBody);
    } catch {
      // Keep the HTTP status text when no JSON error body exists.
    }

    throw new Error(`HTTP ${response.status}: ${detail}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

function buildQueryString(params) {
  if (typeof params === 'string') {
    if (!params) {
      return '';
    }

    return params.startsWith('?') ? params : `?${params}`;
  }

  const query = new URLSearchParams();

  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value));
    }
  });

  const result = query.toString();
  return result ? `?${result}` : '';
}

export const api = {
  login: (email, password) =>
    request('POST', '/auth/login', { email, password }),

  listTrades: (params = {}) =>
    request('GET', `/v1/trades${buildQueryString(params)}`),

  createTrade: (trade) =>
    request('POST', '/v1/trades', trade),

  updateStatus: (id, status) =>
    request('PATCH', `/v1/trades/${id}/status`, { status }),

  deleteTrade: (id) =>
    request('DELETE', `/v1/trades/${id}`),

  runRecon: (reconRequest) =>
    request('POST', '/v1/recon/run', reconRequest),

  reconResults: (jobId) =>
    request('GET', `/v1/recon/jobs/${jobId}/results`),

  audit: (tradeRef) =>
    request('GET', `/v1/audit/trades/${tradeRef}`),
};
