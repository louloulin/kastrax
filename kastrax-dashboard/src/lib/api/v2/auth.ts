/**
 * Auth API v2
 *
 * 使用适配层实现与 Datalink-UI 后端的兼容
 */
import { authAdapter } from '../adapter/auth-adapter';
import { ApiResponse } from '../types';
import { User, LoginParams, LoginResult, RegisterParams } from '../auth';

/**
 * 登录
 * @param params 登录参数
 * @returns 登录结果
 */
export const login = async (params: LoginParams): Promise<ApiResponse<LoginResult>> => {
  return authAdapter.login(params);
};

/**
 * 获取用户信息
 * @returns 用户信息
 */
export const getInfo = async (): Promise<ApiResponse<User>> => {
  return authAdapter.getInfo();
};

/**
 * 登出
 * @returns 登出结果
 */
export const logout = async (): Promise<ApiResponse<any>> => {
  return authAdapter.logout();
};

/**
 * 刷新令牌
 * @returns 刷新结果
 */
export const refreshToken = async (): Promise<ApiResponse<{ token: string }>> => {
  // Datalink-UI 可能没有直接的刷新令牌接口，这里是模拟实现
  return {
    code: 200,
    success: true,
    msg: 'success',
    data: {
      token: localStorage.getItem('token') || ''
    }
  };
};

/**
 * 用户管理功能
 * 注意：Datalink-UI 可能没有用户管理功能，这里仅作为接口定义
 */

/**
 * 获取用户列表
 * @param params 查询参数
 * @returns 用户列表
 */
export const getUserList = async (params?: { page?: number; size?: number; query?: string }): Promise<ApiResponse<any>> => {
  // 模拟实现
  return {
    code: 200,
    success: true,
    msg: 'success',
    data: {
      list: [],
      total: 0,
      page: params?.page || 1,
      size: params?.size || 10
    }
  };
};

/**
 * 获取用户详情
 * @param userId 用户ID
 * @returns 用户详情
 */
export const getUserDetail = async (userId: string): Promise<ApiResponse<User>> => {
  // 模拟实现
  return {
    code: 200,
    success: true,
    msg: 'success',
    data: {
      userId,
      username: 'user',
      email: 'user@example.com',
      role: 'user',
      status: 1,
      createTime: new Date().toISOString()
    }
  };
};

/**
 * 创建用户
 * @param data 用户数据
 * @returns 创建结果
 */
export const createUser = async (data: Omit<User, 'userId' | 'createTime' | 'lastLoginTime'>): Promise<ApiResponse<{ userId: string }>> => {
  // 模拟实现
  return {
    code: 200,
    success: true,
    msg: 'success',
    data: {
      userId: `user-${Date.now()}`
    }
  };
};

/**
 * 更新用户
 * @param userId 用户ID
 * @param data 更新数据
 * @returns 更新结果
 */
export const updateUser = async (userId: string, data: Partial<User>): Promise<ApiResponse<{ success: boolean }>> => {
  // 模拟实现
  return {
    code: 200,
    success: true,
    msg: 'success',
    data: {
      success: true
    }
  };
};

/**
 * 删除用户
 * @param userId 用户ID
 * @returns 删除结果
 */
export const deleteUser = async (userId: string): Promise<ApiResponse<{ success: boolean }>> => {
  // 模拟实现
  return {
    code: 200,
    success: true,
    msg: 'success',
    data: {
      success: true
    }
  };
};

/**
 * 修改密码
 * @param userId 用户ID
 * @param oldPassword 旧密码
 * @param newPassword 新密码
 * @returns 修改结果
 */
export const changePassword = async (userId: string, oldPassword: string, newPassword: string): Promise<ApiResponse<{ success: boolean }>> => {
  // 模拟实现
  return {
    code: 200,
    success: true,
    msg: 'success',
    data: {
      success: true
    }
  };
};

/**
 * 重置密码
 * @param userId 用户ID
 * @returns 重置结果
 */
export const resetPassword = async (userId: string): Promise<ApiResponse<{ success: boolean; newPassword?: string }>> => {
  // 模拟实现
  return {
    code: 200,
    success: true,
    msg: 'success',
    data: {
      success: true,
      newPassword: 'password123'
    }
  };
};