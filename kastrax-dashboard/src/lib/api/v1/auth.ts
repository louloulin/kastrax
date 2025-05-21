/**
 * Auth API V1
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
 * 获取导航菜单
 * @returns 导航菜单
 */
export const getNav = async (): Promise<ApiResponse<any>> => {
  return authAdapter.getNav();
};

/**
 * 登出
 * @returns 登出结果
 */
export const logout = async (): Promise<ApiResponse<any>> => {
  return authAdapter.logout();
};