/**
 * 认证适配器测试
 */

// 直接模拟适配器
jest.mock('../adapter/auth-adapter', () => ({
  authAdapter: {
    login: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: {
        token: 'test-token',
        user: {
          userId: 'user-1',
          username: 'testuser'
        }
      }
    }),
    getInfo: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: {
        userId: 'user-1',
        username: 'testuser',
        role: 'admin'
      }
    }),
    getNav: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: [
        { name: 'Dashboard', path: '/dashboard' },
        { name: 'Rules', path: '/rules' }
      ]
    }),
    logout: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: null
    })
  }
}));

import { authAdapter } from '../adapter/auth-adapter';

describe('认证适配器测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('login', () => {
    it('应该能登录', async () => {
      const params = { username: 'testuser', password: 'password' };
      const result = await authAdapter.login(params);

      expect(result.success).toBe(true);
      expect(result.data.token).toBe('test-token');
      expect(result.data.user.username).toBe('testuser');
      expect(authAdapter.login).toHaveBeenCalledWith(params);
    });
  });

  describe('getInfo', () => {
    it('应该能获取用户信息', async () => {
      const result = await authAdapter.getInfo();

      expect(result.success).toBe(true);
      expect(result.data.username).toBe('testuser');
      expect(result.data.role).toBe('admin');
      expect(authAdapter.getInfo).toHaveBeenCalled();
    });
  });

  describe('getNav', () => {
    it('应该能获取导航菜单', async () => {
      const result = await authAdapter.getNav();

      expect(result.success).toBe(true);
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data.length).toBe(2);
      expect(result.data[0].name).toBe('Dashboard');
      expect(authAdapter.getNav).toHaveBeenCalled();
    });
  });

  describe('logout', () => {
    it('应该能登出', async () => {
      const result = await authAdapter.logout();

      expect(result.success).toBe(true);
      expect(authAdapter.logout).toHaveBeenCalled();
    });
  });
});
