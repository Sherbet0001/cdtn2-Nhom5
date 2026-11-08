import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import authService from '../services/authService.js';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '../services/axiosClient.js';

const AuthContext = createContext(null);

function readStoredUser() {
  const rawUser = localStorage.getItem(USER_STORAGE_KEY);
  if (!rawUser) return null;

  try {
    return JSON.parse(rawUser);
  } catch {
    localStorage.removeItem(USER_STORAGE_KEY);
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY));
  const [user, setUser] = useState(readStoredUser);

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    setToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    window.addEventListener('partjob:session-expired', clearSession);
    return () => window.removeEventListener('partjob:session-expired', clearSession);
  }, [clearSession]);

  useEffect(() => {
    function syncStoredSession(event) {
      if (event.key !== null && event.key !== TOKEN_STORAGE_KEY && event.key !== USER_STORAGE_KEY) return;
      setToken(localStorage.getItem(TOKEN_STORAGE_KEY));
      setUser(readStoredUser());
    }

    window.addEventListener('storage', syncStoredSession);
    return () => window.removeEventListener('storage', syncStoredSession);
  }, []);

  const login = async (credentials) => {
    const result = await authService.login(credentials);
    localStorage.setItem(TOKEN_STORAGE_KEY, result.accessToken);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(result.user));
    setToken(result.accessToken);
    setUser(result.user);
    return result.user;
  };

  const register = (payload) => authService.register(payload);

  const logout = async () => {
    try {
      if (token) await authService.logout();
    } finally {
      clearSession();
    }
  };

  const syncProfile = (profile) => {
    if (!user) return;
    const updatedUser = { ...user, fullName: profile.fullName };
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(updatedUser));
    setUser(updatedUser);
  };

  const value = useMemo(() => ({
    token,
    user,
    isAuthenticated: Boolean(token && user),
    login,
    register,
    logout,
    syncProfile,
  }), [token, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth phải được sử dụng bên trong AuthProvider');
  return context;
}
