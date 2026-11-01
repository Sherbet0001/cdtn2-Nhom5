import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import authService from '../services/authService';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '../services/axiosClient';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY) || null);
  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem(USER_STORAGE_KEY);
      return savedUser ? JSON.parse(savedUser) : null;
    } catch {
      return null;
    }
  });
  const [authBannerMessage, setAuthBannerMessage] = useState('');

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    setToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    const handleUnauthorized = (event) => {
      clearSession();
      setAuthBannerMessage(
        event.detail || 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.'
      );
    };

    const handleForbidden = (event) => {
      setAuthBannerMessage(
        event.detail || 'Bạn không có quyền truy cập vào tài nguyên này.'
      );
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    window.addEventListener('auth:forbidden', handleForbidden);

    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
      window.removeEventListener('auth:forbidden', handleForbidden);
    };
  }, [clearSession]);

  const login = async (credentials) => {
    setAuthBannerMessage('');
    const response = await authService.login(credentials);
    const { accessToken, user: userData } = response.data;

    localStorage.setItem(TOKEN_STORAGE_KEY, accessToken);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(userData));

    setToken(accessToken);
    setUser(userData);
    return userData;
  };

  const register = async (payload) => {
    setAuthBannerMessage('');
    const response = await authService.register(payload);
    return response.data;
  };

  const logout = async () => {
    try {
      if (token) {
        await authService.logout();
      }
    } catch {
      // Bỏ qua lỗi mạng khi đăng xuất vì JWT là stateless
    } finally {
      clearSession();
    }
  };

  const syncUserProfileName = (newFullName) => {
    if (!user || !newFullName) return;
    const updatedUser = { ...user, fullName: newFullName };
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(updatedUser));
    setUser(updatedUser);
  };

  const value = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    authBannerMessage,
    setAuthBannerMessage,
    login,
    register,
    logout,
    syncUserProfileName,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth phải được sử dụng bên trong AuthProvider');
  }
  return context;
};

