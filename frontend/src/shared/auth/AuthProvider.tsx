import { type ReactNode, useCallback, useEffect, useMemo, useState } from 'react';

import { getAuthProfile, postAuthLogin, postAuthLogout, postAuthRegister } from '../api/generated/auth/auth';
import type { GetAuthProfile200 } from '../api/generated/model';
import { AuthContext, type AuthContextValue, type AuthStatus } from './context';
import { notifyAuthSessionCleared, subscribeAuthSessionCleared } from './events';
import { clearAuthTokens, getRefreshToken, hasStoredSession, setAuthTokens } from './storage';

type AuthProviderProps = {
  children: ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<GetAuthProfile200 | null>(null);

  const applyAuthenticatedProfile = useCallback((profile: GetAuthProfile200) => {
    setUser(profile);
    setStatus('authenticated');
  }, []);

  useEffect(() => {
    const unsubscribe = subscribeAuthSessionCleared(() => {
      setUser(null);
      setStatus('unauthenticated');
    });

    return unsubscribe;
  }, []);

  useEffect(() => {
    let canceled = false;

    async function bootstrapAuth() {
      if (!hasStoredSession()) {
        if (!canceled) {
          setStatus('unauthenticated');
        }

        return;
      }

      try {
        const profile = await getAuthProfile();

        if (!canceled) {
          applyAuthenticatedProfile(profile);
        }
      } catch {
        clearAuthTokens();
        notifyAuthSessionCleared();
      }
    }

    void bootstrapAuth();

    return () => {
      canceled = true;
    };
  }, [applyAuthenticatedProfile]);

  const login = useCallback(async ({ email, password }: { email: string; password: string }) => {
    const response = await postAuthLogin({ email, password });
    setAuthTokens(response.accessToken, response.refreshToken);
    try {
      const profile = await getAuthProfile();
      applyAuthenticatedProfile(profile);
    } catch (error) {
      clearAuthTokens();
      notifyAuthSessionCleared();
      throw error;
    }
  }, [applyAuthenticatedProfile]);

  const register = useCallback(async ({ name, email, password }: { name: string; email: string; password: string }) => {
    const response = await postAuthRegister({ name, email, password });
    setAuthTokens(response.accessToken, response.refreshToken);
    try {
      const profile = await getAuthProfile();
      applyAuthenticatedProfile(profile);
    } catch (error) {
      clearAuthTokens();
      notifyAuthSessionCleared();
      throw error;
    }
  }, [applyAuthenticatedProfile]);

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();

    try {
      if (refreshToken) {
        await postAuthLogout({ refreshToken });
      }
    } catch {
      // Ignore logout request errors and always cleanup local auth state.
    } finally {
      clearAuthTokens();
      notifyAuthSessionCleared();
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      isAuthenticated: status === 'authenticated',
      user,
      login,
      register,
      logout,
    }),
    [status, user, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
