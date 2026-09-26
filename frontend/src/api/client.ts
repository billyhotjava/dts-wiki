import axios from 'axios';

// Single axios instance for all backend calls (design 05 S1):
// same-origin cookies, JHipster CSRF header, full-page login redirect on 401.
export const api = axios.create({
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

api.interceptors.response.use(
  response => response,
  error => {
    if (error?.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
      window.location.href = '/oauth2/authorization/oidc';
    }
    return Promise.reject(error);
  },
);

export interface Account {
  login: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  authorities: string[];
}

export async function fetchAccount(): Promise<Account> {
  const { data } = await api.get<Account>('/api/account');
  return data;
}
