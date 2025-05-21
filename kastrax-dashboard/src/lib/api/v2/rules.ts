/**
 * Rules API v2
 *
 * 使用适配层实现与 Datalink-UI 后端的兼容
 */
import { ruleAdapter } from '../adapter/rule-adapter';
import { ApiResponse, PaginatedResponse } from '../types';
import { Rule, RuleCreateParams, RuleUpdateParams, RuleQuery, RuleExecuteResult } from '../rules';

/**
 * 获取规则列表
 * @param params 查询参数
 * @returns 规则列表，带分页
 */
export const getRuleList = async (params?: RuleQuery): Promise<ApiResponse<PaginatedResponse<Rule>>> => {
  const response = await ruleAdapter.getRuleList(params);

  // 处理分页格式
  if (response.success && Array.isArray(response.data)) {
    const page = params?.page || 1;
    const size = params?.size || 10;

    return {
      ...response,
      data: {
        list: response.data,
        total: response.data.length,
        page,
        size
      }
    };
  }

  return response as any;
};

/**
 * 获取规则详情
 * @param ruleId 规则ID
 * @returns 规则详情
 */
export const getRuleById = async (ruleId: string): Promise<ApiResponse<Rule>> => {
  return ruleAdapter.getRuleById(ruleId);
};

/**
 * 创建规则
 * @param params 创建参数
 * @returns 创建结果
 */
export const createRule = async (params: RuleCreateParams): Promise<ApiResponse<{ ruleId: string }>> => {
  return ruleAdapter.createRule(params);
};

/**
 * 更新规则
 * @param ruleId 规则ID
 * @param params 更新参数
 * @returns 更新结果
 */
export const updateRule = async (ruleId: string, params: RuleUpdateParams): Promise<ApiResponse<{ success: boolean }>> => {
  return ruleAdapter.updateRule(ruleId, params);
};

/**
 * 删除规则
 * @param ruleId 规则ID
 * @returns 删除结果
 */
export const deleteRule = async (ruleId: string): Promise<ApiResponse<{ success: boolean }>> => {
  return ruleAdapter.deleteRule(ruleId);
};

/**
 * 执行规则
 * @param ruleId 规则ID
 * @param testData 测试数据
 * @returns 执行结果
 */
export const executeRule = async (ruleId: string, testData?: any): Promise<ApiResponse<RuleExecuteResult>> => {
  return ruleAdapter.executeRule(ruleId, testData);
};

/**
 * 获取规则执行历史
 * @param ruleId 规则ID
 * @param params 查询参数
 * @returns 执行历史
 */
export const getRuleExecutionHistory = async (
  ruleId: string,
  params?: { page?: number; size?: number }
): Promise<ApiResponse<PaginatedResponse<{
  executionId: string;
  ruleId: string;
  ruleName: string;
  status: string;
  startTime: string;
  endTime: string;
  executionTime: number;
  result: any;
}>>> => {
  return ruleAdapter.getRuleExecutionHistory(ruleId, params);
};

/**
 * 启动规则
 * @param ruleId 规则ID
 * @returns 启动结果
 */
export const startRule = async (ruleId: string): Promise<ApiResponse<{ success: boolean }>> => {
  return ruleAdapter.startRule(ruleId);
};

/**
 * 停止规则
 * @param ruleId 规则ID
 * @returns 停止结果
 */
export const stopRule = async (ruleId: string): Promise<ApiResponse<{ success: boolean }>> => {
  return ruleAdapter.stopRule(ruleId);
};

/**
 * 重启规则
 * @param ruleId 规则ID
 * @returns 重启结果
 */
export const restartRule = async (ruleId: string): Promise<ApiResponse<{ success: boolean }>> => {
  return ruleAdapter.restartRule(ruleId);
};

/**
 * 获取规则运行时信息
 * @param ruleId 规则ID
 * @returns 运行时信息
 */
export const getRuleRuntime = async (ruleId: string): Promise<ApiResponse<any>> => {
  return ruleAdapter.getRuleRuntime(ruleId);
};

/**
 * 克隆规则
 * @param ruleId 规则ID
 * @param newName 新规则名称
 * @returns 克隆结果
 */
export const cloneRule = async (ruleId: string, newName: string): Promise<ApiResponse<{ ruleId: string }>> => {
  // 获取原规则详情
  const ruleResponse = await ruleAdapter.getRuleById(ruleId);

  if (!ruleResponse.success) {
    return ruleResponse as any;
  }

  const originalRule = ruleResponse.data;

  // 创建新规则
  const createParams = {
    ruleName: newName,
    type: originalRule.type,
    description: `Clone of ${originalRule.ruleName}: ${originalRule.description || ''}`,
    graph: originalRule.graph
  };

  return ruleAdapter.createRule(createParams);
};