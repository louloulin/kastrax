import { rest } from 'msw';
import { setupServer } from 'msw/node';
import client from '../client';
import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';

// Direct imports for v1 API
import {
  login as v1Login,
  logout as v1Logout,
  getInfo as v1GetInfo,
  refreshToken as v1RefreshToken
} from '../auth';

// Direct imports for v2 API
import {
  login as v2Login,
  logout as v2Logout,
  getInfo as v2GetInfo,
  refreshToken as v2RefreshToken
} from '../v2/auth';

// Mock the client and utility functions
jest.mock('../client', () => ({
  __esModule: true,
  default: {
    get: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
    post: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
    put: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
    delete: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  },
}));

jest.mock('../utils', () => ({
  getRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  postRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  putRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  deleteRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
}));

describe('Auth API', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('v1 Auth API', () => {
    test('login should call client.post with correct path and data', async () => {
      const loginParams = { username: 'test', password: 'test' };
      await v1Login(loginParams);
      expect(client.post).toHaveBeenCalledWith('/api/v1/auth/login', loginParams);
    });

    test('logout should call client.post with correct path', async () => {
      await v1Logout();
      expect(client.post).toHaveBeenCalledWith('/api/v1/auth/logout');
    });

    test('getInfo should call client.get with correct path', async () => {
      await v1GetInfo();
      expect(client.get).toHaveBeenCalledWith('/api/v1/auth/info');
    });

    test('refreshToken should call client.post with correct path', async () => {
      await v1RefreshToken();
      expect(client.post).toHaveBeenCalledWith('/api/v1/auth/refresh-token');
    });
  });

  describe('v2 Auth API', () => {
    test('v2Login should use postRequest utility', async () => {
      const loginParams = { username: 'test', password: 'test' };
      await v2Login(loginParams);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/auth/login', loginParams);
    });

    test('v2Logout should use postRequest utility', async () => {
      await v2Logout();
      expect(postRequest).toHaveBeenCalledWith('/api/v2/auth/logout');
    });

    test('v2GetInfo should use getRequest utility', async () => {
      await v2GetInfo();
      expect(getRequest).toHaveBeenCalledWith('/api/v2/auth/info');
    });

    test('v2RefreshToken should use postRequest utility', async () => {
      await v2RefreshToken();
      expect(postRequest).toHaveBeenCalledWith('/api/v2/auth/refresh-token');
    });
  });
}); 