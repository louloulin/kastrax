/**
 * 系统适配器
 *
 * 这个模块提供了系统相关 API 的适配器，负责将 Dataflare-UI 的 API 调用
 * 转换为 Datalink-UI 后端能够理解的格式。
 */

import { apiMapping } from '../config/api-mapping';
import { transformResponse } from './response';
import { featureFlags } from '../config/feature-flags';
import { getApiClient } from '../client-factory';
import { transformSystemParams } from './transform';

// 获取客户端实例
const apiClient = getApiClient();

/**
 * 系统适配器
 */
export const systemAdapter = {
  /**
   * 获取系统信息
   * @returns 系统信息
   */
  getSystemInfo: async () => {
    try {
      // 转换参数
      const params = transformSystemParams.info({});

      // 发送请求
      const response = await apiClient.get(apiMapping.system.info.legacy, { params });
      return transformResponse(response.data);
    } catch (error) {
      console.error('获取系统信息失败:', error);
      throw error;
    }
  },

  /**
   * 获取系统统计信息
   * @returns 系统统计信息
   */
  getSystemStatistics: async () => {
    try {
      if (!featureFlags.systemStatistics) {
        // 如果后端不支持，返回模拟数据
        return transformResponse({
          code: 200,
          data: {
            ruleCount: 0,
            resourceCount: 0,
            scriptCount: 0,
            activeRuleCount: 0
          },
          message: 'success'
        });
      }

      // 转换参数
      const params = transformSystemParams.statistics({});

      // 发送请求
      const response = await apiClient.get(apiMapping.system.statistics.legacy, { params });
      return transformResponse(response.data);
    } catch (error) {
      console.error('获取系统统计信息失败:', error);
      throw error;
    }
  }
};
