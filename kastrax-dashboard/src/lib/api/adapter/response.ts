/**
 * 响应处理工具
 *
 * 这个模块提供了统一的响应格式处理和错误处理功能。
 */

import { ApiResponse } from '../types';

/**
 * 转换响应格式
 * 将 Datalink-UI 的响应格式转换为 Dataflare-UI 的标准响应格式
 *
 * @param response Datalink-UI 的响应
 * @returns Dataflare-UI 的标准响应格式
 */
export function transformResponse<T = any>(response: any): ApiResponse<T> {
  console.log('[Response Transform] Original response:', response);

  if (!response) {
    console.log('[Response Transform] No response, returning error');
    return {
      code: 500,
      success: false,
      msg: '无响应',
      data: null as any
    };
  }

  try {
    // 处理不同的响应格式
    const result = {
      code: response.code || response.status || 500,
      success: response.code === 200 || response.status === 200 || response.success === true,
      msg: response.message || response.msg || response.error || '',
      data: response.data || response.result || null
    };

    console.log('[Response Transform] Transformed response:', result);
    return result;
  } catch (error) {
    // 捕获并重新抛出错误，添加更多上下文信息
    console.error('[Response Transform] Error processing response:', error);
    return {
      code: 500,
      success: false,
      msg: error instanceof Error ? error.message : '响应处理错误',
      data: null as any
    };
  }
}

/**
 * 处理 API 错误
 *
 * @param error 错误对象
 * @returns 标准化的错误响应
 */
export function handleApiError(error: any): ApiResponse<any> {
  console.error('API Error:', error);

  // 网络错误
  if (!error.response) {
    return {
      code: 503,
      success: false,
      msg: '网络连接失败，请检查网络',
      data: null
    };
  }

  // 服务器错误
  const { status, data } = error.response;

  // 认证错误处理
  if (status === 401) {
    // 触发登出逻辑
    localStorage.removeItem('token');
    window.location.href = '/login';

    return {
      code: 401,
      success: false,
      msg: '认证失败，请重新登录',
      data: null
    };
  }

  // 其他错误
  return {
    code: status || 500,
    success: false,
    msg: data?.message || '请求失败',
    data: null
  };
}

/**
 * 创建分页响应
 *
 * @param list 数据列表
 * @param total 总数
 * @param page 当前页码
 * @param size 每页大小
 * @returns 标准分页响应
 */
export function createPaginatedResponse<T>(
  list: T[],
  total: number,
  page: number = 1,
  size: number = 10
): ApiResponse<{ list: T[]; total: number; page: number; size: number }> {
  return {
    code: 200,
    success: true,
    msg: 'success',
    data: {
      list,
      total,
      page,
      size
    }
  };
}
