const ACCESS_TOKEN_KEY = 'boardgame_tracker_access_token';
const REFRESH_TOKEN_KEY = 'boardgame_tracker_refresh_token';

function readStorage(key: string): string | null {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string | null) {
  if (typeof window === 'undefined') {
    return;
  }

  try {
    if (value) {
      window.localStorage.setItem(key, value);
    } else {
      window.localStorage.removeItem(key);
    }
  } catch {
    // Ignore storage write errors.
  }
}

export function getAccessToken() {
  return readStorage(ACCESS_TOKEN_KEY);
}

export function getRefreshToken() {
  return readStorage(REFRESH_TOKEN_KEY);
}

export function setAccessToken(token: string | null) {
  writeStorage(ACCESS_TOKEN_KEY, token);
}

export function setRefreshToken(token: string | null) {
  writeStorage(REFRESH_TOKEN_KEY, token);
}

export function setAuthTokens(accessToken: string, refreshToken: string) {
  setAccessToken(accessToken);
  setRefreshToken(refreshToken);
}

export function clearAuthTokens() {
  setAccessToken(null);
  setRefreshToken(null);
}

export function hasStoredSession() {
  return Boolean(getAccessToken() && getRefreshToken());
}
