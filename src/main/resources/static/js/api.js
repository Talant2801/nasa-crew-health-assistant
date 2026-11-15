// Thin wrapper around the backend REST API (relative paths, served by Spring Boot).

async function request(method, path, body) {
  const opts = { method, headers: {} };
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(path, opts);
  if (!res.ok) throw new Error(`${method} ${path} → ${res.status}`);
  return res.status === 204 ? null : res.json();
}

export const api = {
  mission: () => request('GET', '/api/mission'),
  setContext: (env) => request('POST', '/api/mission/context', { env }),
  crew: () => request('GET', '/api/crew'),
  modules: (id) => request('GET', `/api/crew/${encodeURIComponent(id)}/modules`),
  readings: (id, indicator, days = 30) =>
    request('GET', `/api/crew/${encodeURIComponent(id)}/readings?indicator=${encodeURIComponent(indicator)}&days=${days}`),
  alerts: (crewId) => request('GET', crewId ? `/api/alerts?crewId=${encodeURIComponent(crewId)}` : '/api/alerts'),
  ack: (id) => request('POST', `/api/alerts/${encodeURIComponent(id)}/ack`),
  scenario: (name) => request('POST', `/api/scenarios/${encodeURIComponent(name)}`),
};
