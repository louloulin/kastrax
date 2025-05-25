/**
 * 规则适配器
 *
 * 这个模块提供了规则相关 API 的适配器，负责将 Dataflare-UI 的 API 调用
 * 转换为 Datalink-UI 后端能够理解的格式。
 */

import { apiMapping } from '../config/api-mapping';
import { transformResponse } from './response';
import { transformRuleParams } from './transform';
import { featureFlags } from '../config/feature-flags';
import { getApiClient } from '../client-factory';

// 获取客户端实例
const apiClient = getApiClient();

/**
 * 规则适配器
 */
export const ruleAdapter = {
  /**
   * 获取规则列表
   * @param params 查询参数
   * @returns 规则列表
   */
  getRuleList: async (params: any = {}) => {
    const transformedParams = transformRuleParams.list(params);
    const response = await apiClient.post(apiMapping.rule.list.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 获取规则详情
   * @param ruleId 规则ID
   * @returns 规则详情
   */
  getRuleById: async (ruleId: string) => {
    const response = await apiClient.get(apiMapping.rule.info.legacy, { params: { ruleId } });

    // 处理 graphJson 字段，转换为 graph 对象
    const transformedResponse = transformResponse(response.data);
    if (transformedResponse.data && transformedResponse.data.graphJson) {
      try {
        transformedResponse.data.graph = JSON.parse(transformedResponse.data.graphJson);
      } catch (e) {
        console.error('解析 graphJson 失败:', e);
        transformedResponse.data.graph = { nodes: [], edges: [] };
      }
    }

    return transformedResponse;
  },

  /**
   * 创建规则
   * @param params 创建参数
   * @returns 创建结果
   */
  createRule: async (params: any) => {
    const transformedParams = transformRuleParams.create(params);
    const response = await apiClient.post(apiMapping.rule.add.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 更新规则
   * @param ruleId 规则ID
   * @param params 更新参数
   * @returns 更新结果
   */
  updateRule: async (ruleId: string, params: any) => {
    const transformedParams = transformRuleParams.update({ ...params, ruleId });
    const response = await apiClient.put(apiMapping.rule.update.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 删除规则
   * @param ruleId 规则ID
   * @returns 删除结果
   */
  deleteRule: async (ruleId: string) => {
    const response = await apiClient.post(apiMapping.rule.remove.legacy, { ruleId });
    return transformResponse(response.data);
  },

  /**
   * 启动规则
   * @param ruleId 规则ID
   * @returns 启动结果
   */
  startRule: async (ruleId: string) => {
    const response = await apiClient.post(apiMapping.rule.start.legacy, { ruleId });
    return transformResponse(response.data);
  },

  /**
   * 停止规则
   * @param ruleId 规则ID
   * @returns 停止结果
   */
  stopRule: async (ruleId: string) => {
    const response = await apiClient.post(apiMapping.rule.stop.legacy, { ruleId });
    return transformResponse(response.data);
  },

  /**
   * 启动并保存规则
   * @param params 规则参数
   * @returns 启动并保存结果
   */
  startAndSaveRule: async (params: any) => {
    const transformedParams = transformRuleParams.update(params);
    const response = await apiClient.post(apiMapping.rule.startAndSave.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 重启规则
   * @param ruleId 规则ID
   * @returns 重启结果
   */
  restartRule: async (ruleId: string) => {
    const response = await apiClient.post(apiMapping.rule.restart.legacy, { ruleId });
    return transformResponse(response.data);
  },

  /**
   * 获取规则运行时信息
   * @param ruleId 规则ID
   * @returns 运行时信息
   */
  getRuleRuntime: async (ruleId: string) => {
    const response = await apiClient.get(apiMapping.rule.runtime.legacy, { params: { ruleId } });
    return transformResponse(response.data);
  },

  /**
   * 执行规则
   * @param ruleId 规则ID
   * @param testData 测试数据
   * @returns 执行结果
   */
  executeRule: async (ruleId: string, testData?: any) => {
    const transformedParams = transformRuleParams.execute({ ruleId, testData });
    const response = await apiClient.post(apiMapping.rule.execute.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 获取规则执行历史
   * @param ruleId 规则ID
   * @param params 查询参数
   * @returns 执行历史
   */
  getRuleExecutionHistory: async (ruleId: string, params: any = {}) => {
    if (featureFlags.ruleHistory) {
      // 如果后端支持历史记录功能，直接调用
      // 实际上 Datalink-UI 不支持，所以这里是模拟实现
    }

    // 模拟历史记录
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
  }
};
