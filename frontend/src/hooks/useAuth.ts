import { useCallback, useEffect, useState } from 'react';
import {
  getCurrentUser,
  login as apiLogin,
  logout as apiLogout,
  register as apiRegister,
  UNAUTHORIZED_EVENT,
} from '../api/client';
import type { User } from '../api/types';

export function useAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // Checked once on load: a session cookie from an earlier visit may still
  // be valid, so start by asking rather than assuming logged-out.
  useEffect(() => {
    let cancelled = false;
    getCurrentUser()
      .then((u) => {
        if (!cancelled) setUser(u);
      })
      .catch(() => {
        if (!cancelled) setUser(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // A 401 from anywhere in the app (session expired mid-use) drops back to
  // the login screen instead of leaving every panel stuck on stale errors.
  useEffect(() => {
    function handleUnauthorized() {
      setUser(null);
    }
    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    setUser(await apiLogin(email, password));
  }, []);

  const register = useCallback(async (email: string, password: string) => {
    setUser(await apiRegister(email, password));
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiLogout();
    } finally {
      setUser(null);
    }
  }, []);

  return { user, loading, login, register, logout };
}
