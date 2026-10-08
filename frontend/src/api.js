const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(status, message, fieldErrors = []) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request(path, { method = 'GET', body, token } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, `Não foi possível conectar ao backend em ${API_URL}`);
  }

  if (response.status === 204) return null;

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, data?.message ?? `Erro HTTP ${response.status}`, data?.errors ?? []);
  }
  return data;
}

export const api = {
  login: (username, password) => request('/auth/login', { method: 'POST', body: { username, password } }),
  listPeople: (token) => request('/list', { token }),
  createPerson: (token, person) => request('/registrarName', { method: 'POST', body: person, token }),
  deletePerson: (token, id) => request(`/list/${id}`, { method: 'DELETE', token }),
  findNationality: (token, id) => request(`/findNacionalityByPerson/${id}`, { token }),
};
