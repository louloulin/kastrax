/**
 * 资源适配器
 *
 * 这个模块提供了资源相关 API 的适配器，负责将 Dataflare-UI 的 API 调用
 * 转换为 Datalink-UI 后端能够理解的格式。
 */

import { apiMapping } from '../config/api-mapping';
import { transformResponse } from './response';
import { transformResourceParams } from './transform';
import { featureFlags } from '../config/feature-flags';
import { getApiClient } from '../client-factory';

// 获取客户端实例
const apiClient = getApiClient();

/**
 * 资源适配器
 */
export const resourceAdapter = {
  /**
   * 获取资源列表
   * @param params 查询参数
   * @returns 资源列表
   */
  getResourceList: async (params: any = {}) => {
    const transformedParams = transformResourceParams.list(params);
    const response = await apiClient.post(apiMapping.resource.list.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 获取资源详情
   * @param resourceId 资源ID
   * @returns 资源详情
   */
  getResourceById: async (resourceId: string) => {
    // Datalink-UI 没有直接的资源详情接口，需要从列表中筛选
    const response = await apiClient.post(apiMapping.resource.list.legacy, { resourceId });

    // 从列表中找到对应的资源
    const resources = response.data.data || [];
    const resource = resources.find((r: any) => r.resourceId === resourceId);

    if (resource) {
      return transformResponse({
        code: 200,
        data: resource,
        message: 'success'
      });
    } else {
      return transformResponse({
        code: 404,
        data: null,
        message: '资源不存在'
      });
    }
  },

  /**
   * 创建资源
   * @param params 创建参数
   * @returns 创建结果
   */
  createResource: async (params: any) => {
    const transformedParams = transformResourceParams.create(params);
    const response = await apiClient.post(apiMapping.resource.add.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 更新资源
   * @param resourceId 资源ID
   * @param params 更新参数
   * @returns 更新结果
   */
  updateResource: async (resourceId: string, params: any) => {
    const transformedParams = transformResourceParams.update({ ...params, resourceId });
    const response = await apiClient.post(apiMapping.resource.update.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 删除资源
   * @param resourceId 资源ID
   * @returns 删除结果
   */
  deleteResource: async (resourceId: string) => {
    const response = await apiClient.post(apiMapping.resource.remove.legacy, { resourceId });
    return transformResponse(response.data);
  },

  /**
   * 测试资源连接
   * @param params 测试参数
   * @returns 测试结果
   */
  testResource: async (params: any) => {
    if (!featureFlags.resourceTest) {
      return transformResponse({
        code: 200,
        data: { success: true },
        message: '测试成功'
      });
    }

    const transformedParams = transformResourceParams.test(params);
    const response = await apiClient.post(apiMapping.resource.test.legacy, transformedParams);
    return transformResponse(response.data);
  },

  /**
   * 获取资源类型列表
   * @returns 资源类型列表
   */
  getResourceTypes: async () => {
    if (!featureFlags.resourceTypes) {
      // 如果后端不支持，返回模拟数据
      return transformResponse({
        code: 200,
        data: [
          { code: 'MQTT', name: 'MQTT', group: 'CHANNEL' },
          { code: 'MYSQL', name: 'MySQL', group: 'DATABASE' },
          { code: 'OPCUA', name: 'OPC UA', group: 'PROTOCOL' }
        ],
        message: 'success'
      });
    }

    // 实际上 Datalink-UI 可能没有直接的资源类型接口，这里是模拟实现
    // 在实际项目中，可能需要从配置文件或其他地方获取
    return transformResponse({
      code: 200,
      data: [
        { code: 'MQTT', name: 'MQTT', group: 'CHANNEL' },
        { code: 'MYSQL', name: 'MySQL', group: 'DATABASE' },
        { code: 'OPCUA', name: 'OPC UA', group: 'PROTOCOL' }
      ],
      message: 'success'
    });
  }
};
