import React, { createContext, useContext, useEffect, ReactNode } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/auth-store';
import { usePermissionStore } from '../store/permission-store';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: any;
  error: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { token, user, loading, error, login, logout, fetchUserInfo, refreshToken, updateActivity, checkSessionTimeout, clearError } = useAuthStore();

  // 获取权限存储
  const { setPermissions, clearPermissions } = usePermissionStore();

  // Check authentication status on mount and when token changes
  useEffect(() => {
    console.log('[Auth Context] Checking authentication status, token:', token ? '(exists)' : '(missing)', 'user:', user ? '(exists)' : '(missing)');

    const checkAuth = async () => {
      // If we have a token but no user, fetch user info
      if (token && !user) {
        console.log('[Auth Context] Token exists but no user, fetching user info');
        await fetchUserInfo();
      } else if (token && user) {
        // 如果有 token 和 user，设置权限
        console.log('[Auth Context] Setting permissions:', user.permissions);
        setPermissions(user.permissions || []);
      }
    };

    checkAuth();

    // 如果没有 token，清除权限
    if (!token) {
      clearPermissions();
    }
  }, [token, user, fetchUserInfo, setPermissions, clearPermissions]);

  // Handle redirects based on authentication status
  useEffect(() => {
    console.log('[Auth Context] Handling redirects, path:', location.pathname, 'loading:', loading, 'token:', token ? '(exists)' : '(missing)');

    // Skip for login page
    if (location.pathname === '/login') {
      console.log('[Auth Context] On login page, skipping redirect');
      return;
    }

    // Redirect to login if not authenticated
    if (!loading && !token) {
      console.log('[Auth Context] Not authenticated, redirecting to login');
      const returnUrl = encodeURIComponent(location.pathname + location.search);
      navigate(`/login?returnUrl=${returnUrl}`);
    }
  }, [token, loading, location, navigate]);

  // Check session timeout and refresh token periodically
  useEffect(() => {
    if (!token) return;

    // Update activity on user interaction
    const handleUserActivity = () => {
      updateActivity();
    };

    // Add event listeners for user activity
    window.addEventListener('click', handleUserActivity);
    window.addEventListener('keypress', handleUserActivity);
    window.addEventListener('scroll', handleUserActivity);
    window.addEventListener('mousemove', handleUserActivity);

    // Set initial activity timestamp
    updateActivity();

    // Check session timeout every minute
    const timeoutInterval = setInterval(() => {
      checkSessionTimeout();
    }, 60 * 1000);

    // Refresh token every 30 minutes
    const refreshInterval = setInterval(() => {
      refreshToken();
    }, 30 * 60 * 1000);

    return () => {
      // Clean up event listeners and intervals
      window.removeEventListener('click', handleUserActivity);
      window.removeEventListener('keypress', handleUserActivity);
      window.removeEventListener('scroll', handleUserActivity);
      window.removeEventListener('mousemove', handleUserActivity);
      clearInterval(timeoutInterval);
      clearInterval(refreshInterval);
    };
  }, [token, updateActivity, checkSessionTimeout, refreshToken]);

  const value = {
    isAuthenticated: !!token,
    isLoading: loading,
    user,
    error,
    login,
    logout,
    clearError
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
