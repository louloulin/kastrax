/**
 * API 客户端工厂
 *
 * 这个模块提供了一个统一的 API 客户端工厂，确保所有适配器使用相同的客户端实例。
 */

import axios from 'axios';
import client from './client';

/**
 * 获取 API 客户端实例
 * @returns API 客户端实例
 */
export const getApiClient = () => {
  // 在测试环境中使用 axios.create()，在其他环境中使用预配置的客户端
  return process.env.NODE_ENV === 'test' ? axios.create() : client;
};

// 导出默认客户端实例，便于直接导入使用
export default getApiClient();
