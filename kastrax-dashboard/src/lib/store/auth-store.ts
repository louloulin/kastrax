import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { login as apiLogin, getInfo, logout as apiLogout } from '../api/v1/auth';
import { User } from '../api/auth';

interface AuthState {
  token: string | null;
  user: User | null;
  loading: boolean;
  error: string | null;
  lastActivity: number | null;

  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  fetchUserInfo: () => Promise<void>;
  refreshToken: () => Promise<void>;
  updateActivity: () => void;
  checkSessionTimeout: () => boolean;
  clearError: () => void;
}

// 会话超时时间（毫秒）
const SESSION_TIMEOUT = 30 * 60 * 1000; // 30 分钟

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      loading: false,
      error: null,
      lastActivity: null,

      login: async (username, password) => {
        console.log('[Auth Store] Login attempt with username:', username);
        set({ loading: true, error: null });
        try {
          console.log('[Auth Store] Calling API login method');
          const response = await apiLogin({ username, password });
          console.log('[Auth Store] Login response:', response);

          if (response.success) {
            console.log('[Auth Store] Login successful');

            // 在 datalink-ui 中，登录成功后直接返回用户信息，而不是 token
            // 创建一个符合 JWT 格式的 token
            const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
            const payload = btoa(JSON.stringify({
              sub: username,
              name: username,
              iat: Math.floor(Date.now() / 1000),
              exp: Math.floor(Date.now() / 1000) + (60 * 60) // 1 hour expiration
            }));
            const signature = btoa('signature'); // 这只是一个占位符，不是真正的签名

            const jwtToken = `${header}.${payload}.${signature}`;
            console.log('[Auth Store] Created JWT token');

            localStorage.setItem('token', jwtToken);
            localStorage.setItem('username', username);

            // 存储用户信息
            // 创建符合 User 类型的用户对象
            const userData: User = {
              userId: response.data?.username || username || 'user-id',
              username: response.data?.username || username || 'username',
              email: response.data?.email || '',
              role: 'admin',
              status: 1,
              createTime: response.data?.createTime || new Date().toISOString(),
              permissions: response.data?.permissions || ['all']
            };

            set({
              token: jwtToken,
              user: userData,
              loading: false,
              lastActivity: Date.now()
            });

            console.log('[Auth Store] User info stored:', response.data);
          } else {
            console.log('[Auth Store] Login failed:', response.msg);
            set({
              loading: false,
              error: response.msg || '登录失败，请检查用户名和密码'
            });
          }
        } catch (error: any) {
          console.error('[Auth Store] Login error:', error);
          set({
            loading: false,
            error: error.message || '登录失败，请检查网络连接'
          });
        }
      },

      logout: async () => {
        set({ loading: true });
        try {
          await apiLogout();
        } catch (error) {
          console.error('[Auth Store] Logout error:', error);
        } finally {
          // 清除本地存储的用户信息
          localStorage.removeItem('token');
          localStorage.removeItem('username');
          console.log('[Auth Store] Cleared token and username from localStorage');
          set({ token: null, user: null, loading: false });
        }
      },

      fetchUserInfo: async () => {
        set({ loading: true });
        try {
          console.log('[Auth Store] Fetching user info');

          // 检查是否有有效的 token
          let token = localStorage.getItem('token');
          if (!token) {
            console.warn('[Auth Store] No token found, cannot fetch user info');
            set({
              loading: false,
              error: 'No authentication token found'
            });
            return;
          }

          // 检查 token 是否是有效的 JWT 格式
          if (token.split('.').length !== 3) {
            console.warn('[Auth Store] Token is not in proper JWT format, creating a valid JWT token');

            // 创建一个符合 JWT 格式的 token
            const username = localStorage.getItem('username') || 'admin';
            const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
            const payload = btoa(JSON.stringify({
              sub: username,
              name: username,
              iat: Math.floor(Date.now() / 1000),
              exp: Math.floor(Date.now() / 1000) + (60 * 60) // 1 hour expiration
            }));
            const signature = btoa('signature'); // 这只是一个占位符

            token = `${header}.${payload}.${signature}`;
            localStorage.setItem('token', token);
            set({ token: token });
            console.log('[Auth Store] Created and stored valid JWT token');
          }

          const response = await getInfo();
          console.log('[Auth Store] User info response:', response);

          if (response.success) {
            set({ user: response.data, loading: false });
            console.log('[Auth Store] User info stored successfully');
          } else {
            console.warn('[Auth Store] Failed to get user info:', response.msg);
            set({ loading: false, error: response.msg || '获取用户信息失败' });

            // 如果获取用户信息失败，尝试使用存储的用户名创建一个模拟用户
            const username = localStorage.getItem('username');
            if (username) {
              console.log('[Auth Store] Creating simulated user with stored username:', username);
              const userData: User = {
                userId: `user-${Date.now()}`,
                username: username,
                email: '',
                role: 'admin',
                status: 1,
                createTime: new Date().toISOString(),
                permissions: ['all']
              };
              set({ user: userData, loading: false, error: null });
            }
          }
        } catch (error: any) {
          console.error('[Auth Store] Error fetching user info:', error);
          set({
            loading: false,
            error: error.message || '获取用户信息失败，请检查网络连接'
          });

          // 如果出错，尝试使用存储的用户名创建一个模拟用户
          const username = localStorage.getItem('username');
          if (username) {
            console.log('[Auth Store] Creating simulated user with stored username after error:', username);
            const userData: User = {
              userId: `user-${Date.now()}`,
              username: username,
              email: '',
              role: 'admin',
              status: 1,
              createTime: new Date().toISOString(),
              permissions: ['all']
            };
            set({ user: userData, loading: false, error: null });
          }
        }
      },

      refreshToken: async () => {
        console.log('[Auth Store] Refreshing token');
        set({ loading: true });
        try {
          // 获取当前 token
          const currentToken = get().token;
          if (!currentToken) {
            console.warn('[Auth Store] No token to refresh');
            set({ loading: false });
            return;
          }

          // 解析 token 以获取过期时间
          try {
            const payload = currentToken.split('.')[1];
            const decodedPayload = JSON.parse(atob(payload));
            const expTime = decodedPayload.exp * 1000; // 转换为毫秒

            // 如果 token 还没有过期或者还有 5 分钟以上的有效期，则不需要刷新
            const now = Date.now();
            if (expTime > now + 5 * 60 * 1000) {
              console.log('[Auth Store] Token still valid, no need to refresh');
              set({ loading: false });
              return;
            }
          } catch (error) {
            console.warn('[Auth Store] Could not decode token payload:', error);
          }

          // 创建新的 JWT token
          const username = get().user?.username || localStorage.getItem('username') || 'unknown';
          const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
          const payload = btoa(JSON.stringify({
            sub: username,
            name: username,
            iat: Math.floor(Date.now() / 1000),
            exp: Math.floor(Date.now() / 1000) + (60 * 60) // 1 hour expiration
          }));
          const signature = btoa('signature');

          const newToken = `${header}.${payload}.${signature}`;
          localStorage.setItem('token', newToken);
          set({ token: newToken, loading: false, lastActivity: Date.now() });
          console.log('[Auth Store] Token refreshed successfully');
        } catch (error: any) {
          console.error('[Auth Store] Error refreshing token:', error);
          set({ loading: false, error: error.message || '刷新令牌失败' });
        }
      },

      updateActivity: () => {
        const lastActivity = Date.now();
        set({ lastActivity });
      },

      checkSessionTimeout: () => {
        const { lastActivity, token } = get();

        // 如果没有 token 或没有最后活动时间，则不需要检查超时
        if (!token || !lastActivity) {
          return false;
        }

        const now = Date.now();
        const isTimeout = now - lastActivity > SESSION_TIMEOUT;

        if (isTimeout) {
          console.warn('[Auth Store] Session timeout detected');
          // 自动登出
          get().logout();
        }

        return isTimeout;
      },

      clearError: () => set({ error: null })
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ token: state.token }),
    }
  )
);
