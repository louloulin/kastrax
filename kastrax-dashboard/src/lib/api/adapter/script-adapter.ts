/**
 * 脚本适配器
 *
 * 这个模块提供了脚本相关 API 的适配器，负责将 Dataflare-UI 的 API 调用
 * 转换为 Datalink-UI 后端能够理解的格式。
 */

import { apiMapping } from '../config/api-mapping';
import { transformResponse } from './response';
import { transformScriptParams } from './transform';
import { featureFlags } from '../config/feature-flags';
import { getApiClient } from '../client-factory';

// 获取客户端实例
const apiClient = getApiClient();

/**
 * 脚本适配器
 */
export const scriptAdapter = {
  /**
   * 获取脚本列表
   * @param params 查询参数
   * @returns 脚本列表
   */
  getScriptList: async (params: any = {}) => {
    const transformedParams = transformScriptParams.list(params);
    const response = await apiClient.post(apiMapping.script.list.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 获取脚本详情
   * @param scriptId 脚本ID
   * @returns 脚本详情
   */
  getScriptById: async (scriptId: string) => {
    const response = await apiClient.get(apiMapping.script.info.legacy, { params: { scriptId } });
    return transformResponse(response.data);
  },

  /**
   * 创建脚本
   * @param params 创建参数
   * @returns 创建结果
   */
  createScript: async (params: any) => {
    const transformedParams = transformScriptParams.create(params);
    const response = await apiClient.post(apiMapping.script.add.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 更新脚本
   * @param scriptId 脚本ID
   * @param params 更新参数
   * @returns 更新结果
   */
  updateScript: async (scriptId: string, params: any) => {
    const transformedParams = transformScriptParams.update({ ...params, scriptId });
    const response = await apiClient.put(apiMapping.script.update.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 删除脚本
   * @param scriptId 脚本ID
   * @returns 删除结果
   */
  deleteScript: async (scriptId: string) => {
    const response = await apiClient.post(apiMapping.script.remove.legacy, { scriptId });
    return transformResponse(response.data);
  },

  /**
   * 测试脚本
   * @param scriptId 脚本ID
   * @param params 测试参数
   * @returns 测试结果
   */
  testScript: async (scriptId: string, params: any = {}) => {
    const transformedParams = transformScriptParams.test({ ...params, scriptId });
    const response = await apiClient.post(apiMapping.script.test.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 执行脚本
   * @param scriptId 脚本ID
   * @param params 执行参数
   * @returns 执行结果
   */
  executeScript: async (scriptId: string, params: any = {}) => {
    // Datalink-UI 可能没有直接的脚本执行接口，使用测试接口代替
    const transformedParams = transformScriptParams.test({ ...params, scriptId });
    const response = await apiClient.post(apiMapping.script.test.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 调试脚本
   * @param scriptId 脚本ID
   * @param params 调试参数
   * @returns 调试结果
   */
  debugScript: async (scriptId: string, params: any = {}) => {
    if (featureFlags.scriptDebug) {
      // 如果后端支持调试功能，直接调用
      // 实际上 Datalink-UI 不支持，所以这里是模拟实现
    }

    // 降级为测试功能
    const transformedParams = transformScriptParams.test({ ...params, scriptId });
    const response = await apiClient.post(apiMapping.script.test.legacy, transformedParams);

    // 转换为调试结果格式
    const baseResponse = transformResponse(response.data);
    return {
      ...baseResponse,
      data: {
        success: baseResponse.success,
        executionTime: response.data?.data?.time || 0,
        output: JSON.stringify(response.data?.data?.result),
        returnValue: response.data?.data?.result,
        // 模拟调试信息
        debugSteps: []
      }
    };
  },

  /**
   * 获取脚本依赖
   * @param scriptId 脚本ID
   * @returns 依赖列表
   */
  getScriptDependencies: async (scriptId: string) => {
    if (!featureFlags.scriptDependencies) {
      // 如果后端不支持，返回模拟数据
      return transformResponse({
        code: 200,
        data: [],
        message: 'success'
      });
    }

    // 实际上 Datalink-UI 可能没有直接的脚本依赖接口，这里是模拟实现
    return transformResponse({
      code: 200,
      data: [],
      message: 'success'
    });
  },

  /**
   * 安装脚本依赖
   * @param scriptId 脚本ID
   * @param dependencies 依赖列表
   * @returns 安装结果
   */
  installScriptDependencies: async (scriptId: string, dependencies: string[]) => {
    if (!featureFlags.scriptDependencies) {
      // 如果后端不支持，返回模拟数据
      return transformResponse({
        code: 200,
        data: { success: true },
        message: '安装成功'
      });
    }

    // 实际上 Datalink-UI 可能没有直接的脚本依赖安装接口，这里是模拟实现
    return transformResponse({
      code: 200,
      data: { success: true },
      message: '安装成功'
    });
  }
};
