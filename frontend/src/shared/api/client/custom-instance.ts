import axios, { type AxiosRequestConfig } from 'axios';

import { notifyAuthSessionCleared } from '../../auth/events';
import { clearAuthTokens, getAccessToken, getRefreshToken, setAuthTokens } from '../../auth/storage';
import { API_BASE_URL } from './config';

type RetriableConfig = AxiosRequestConfig & {
  _retry?: boolean;
};

type RefreshResponse = {
  accessToken: string;
  refreshToken: string;
};

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

let refreshRequest: Promise<string | null> | null = null;

async function refreshAccessToken() {
  if (refreshRequest) {
    return refreshRequest;
  }

  const refreshToken = getRefreshToken();

  if (!refreshToken) {
    return null;
  }

  refreshRequest = apiClient
    .post<RefreshResponse>('/auth/refresh', { refreshToken })
    .then((refreshResponse) => {
      setAuthTokens(refreshResponse.data.accessToken, refreshResponse.data.refreshToken);
      return refreshResponse.data.accessToken;
    })
    .catch(() => {
      clearAuthTokens();
      notifyAuthSessionCleared();
      return null;
    })
    .finally(() => {
      refreshRequest = null;
    });

  return refreshRequest;
}

function isPublicAuthUrl(url: string | undefined) {
  if (!url) {
    return false;
  }

  const pathname = new URL(url, 'http://boardgame-tracker.local').pathname;

  return pathname === '/auth/login' || pathname === '/auth/register' || pathname === '/auth/refresh';
}

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();

  if (token && !isPublicAuthUrl(config.url)) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const statusCode = error.response?.status;
    const originalRequest = error.config as RetriableConfig | undefined;

    if (!originalRequest || statusCode !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    if (isPublicAuthUrl(originalRequest.url)) {
      clearAuthTokens();
      notifyAuthSessionCleared();
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    const newAccessToken = await refreshAccessToken();

    if (!newAccessToken) {
      return Promise.reject(error);
    }

    originalRequest.headers = {
      ...originalRequest.headers,
      Authorization: `Bearer ${newAccessToken}`,
    };

    return apiClient.request(originalRequest);
  },
);

export async function customInstance<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.request<T>(config);
  return response.data;
}
