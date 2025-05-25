import client from './client';
import { AxiosResponse } from 'axios';
import { ApiResponse } from './types';

// 规则类型枚举
export enum RuleType {
  CONDITIONAL = 'conditional',
  SEQUENCE = 'sequence',
  PARALLEL = 'parallel',
  TRANSFORM = 'transform'
}

// 节点类型枚举
export enum NodeType {
  START = 'start',
  END = 'end',
  CONDITION = 'condition',
  ACTION = 'action',
  TRANSFORM = 'transform',
  SCRIPT = 'script',
  API = 'api',
  DELAY = 'delay',
  RESOURCE = 'resource'
}

// 连接类型
export enum EdgeType {
  DEFAULT = 'default',
  SUCCESS = 'success',
  FAILURE = 'failure',
  CONDITION = 'condition'
}

// 规则状态
export enum RuleStatus {
  DRAFT = 'draft',
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  DEPRECATED = 'deprecated'
}

// 节点接口
export interface RuleNode {
  id: string;
  type: NodeType;
  position: { x: number; y: number };
  data: {
    label: string;
    config?: any;
    description?: string;
  };
  style?: Record<string, any>;
}

// 连接接口
export interface RuleEdge {
  id: string;
  source: string;
  target: string;
  type: EdgeType;
  animated?: boolean;
  label?: string;
  style?: Record<string, any>;
}

// 规则图接口
export interface RuleGraph {
  nodes: RuleNode[];
  edges: RuleEdge[];
}

// 规则接口
export interface Rule {
  ruleId: string;
  ruleName: string;
  type: RuleType;
  description?: string;
  status: RuleStatus;
  graph: RuleGraph;
  version: number;
  createTime: string;
  updateTime: string;
  createdBy?: string;
  updatedBy?: string;
  tags?: string[];
}

// 规则查询参数
export interface RuleQuery {
  ruleName?: string;
  type?: RuleType;
  status?: RuleStatus;
  tags?: string[];
  page?: number;
  size?: number;
}

// 规则创建参数
export interface RuleCreateParams {
  ruleName: string;
  type: RuleType;
  description?: string;
  graph: RuleGraph;
  status?: RuleStatus;
  tags?: string[];
}

// 规则更新参数
export interface RuleUpdateParams {
  ruleId: string;
  ruleName?: string;
  type?: RuleType;
  description?: string;
  graph?: RuleGraph;
  status?: RuleStatus;
  tags?: string[];
}

// 规则执行参数
export interface RuleExecuteParams {
  ruleId: string;
  params?: Record<string, any>;
  timeout?: number;
}

// 规则执行结果
export interface RuleExecuteResult {
  success: boolean;
  executionTime: number;
  output: string;
  error?: string;
  result?: any;
  path?: string[]; // 执行路径，节点ID数组
  nodeResults?: Record<string, any>; // 各节点执行结果
}

// API endpoints
const API_ROOT = '/api/v1';

/**
 * Get the list of rules
 * @param params Query parameters
 * @returns Promise with rule list
 */
export function getRuleList(params: RuleQuery = {}): Promise<AxiosResponse<ApiResponse<Rule[]>>> {
  return client.get(`${API_ROOT}/rules`, { params });
}

/**
 * Get a rule by ID
 * @param ruleId Rule ID
 * @returns Promise with rule details
 */
export function getRuleById(ruleId: string): Promise<AxiosResponse<ApiResponse<Rule>>> {
  return client.get(`${API_ROOT}/rules/${ruleId}`);
}

/**
 * Create a new rule
 * @param params Rule creation parameters
 * @returns Promise with created rule ID
 */
export function createRule(params: RuleCreateParams): Promise<AxiosResponse<ApiResponse<{ruleId: string}>>> {
  return client.post(`${API_ROOT}/rules`, params);
}

/**
 * Update an existing rule
 * @param params Rule update parameters
 * @returns Promise with update status
 */
export function updateRule(params: RuleUpdateParams): Promise<AxiosResponse<ApiResponse<boolean>>> {
  return client.put(`${API_ROOT}/rules/${params.ruleId}`, params);
}

/**
 * Delete a rule
 * @param ruleId Rule ID to delete
 * @returns Promise with deletion status
 */
export function deleteRule(ruleId: string): Promise<AxiosResponse<ApiResponse<boolean>>> {
  return client.delete(`${API_ROOT}/rules/${ruleId}`);
}

/**
 * Start a rule (activate)
 * @param ruleId Rule ID to start
 * @returns Promise with start status
 */
export function startRule(ruleId: string): Promise<AxiosResponse<ApiResponse<boolean>>> {
  return client.post(`${API_ROOT}/rules/${ruleId}/start`);
}

/**
 * Stop a rule (deactivate)
 * @param ruleId Rule ID to stop
 * @returns Promise with stop status
 */
export function stopRule(ruleId: string): Promise<AxiosResponse<ApiResponse<boolean>>> {
  return client.post(`${API_ROOT}/rules/${ruleId}/stop`);
}

/**
 * Get available rule types
 * @returns Promise with rule types
 */
export function getRuleTypes(): Promise<AxiosResponse<ApiResponse<string[]>>> {
  return client.get(`${API_ROOT}/rule-types`);
}

// V2 API functions (using async/await and better error handling)
export const getRuleListV2 = async (params?: RuleQuery) => {
  const response = await client.get(`${API_ROOT}/rules`, { params });
  return response.data;
};

export const getRuleByIdV2 = async (ruleId: string) => {
  const response = await client.get(`${API_ROOT}/rules/${ruleId}`);
  return response.data;
};

export const createRuleV2 = async (params: RuleCreateParams) => {
  const response = await client.post(`${API_ROOT}/rules`, params);
  return response.data;
};

export const updateRuleV2 = async (params: RuleUpdateParams) => {
  const response = await client.put(`${API_ROOT}/rules/${params.ruleId}`, params);
  return response.data;
};

export const deleteRuleV2 = async (ruleId: string) => {
  const response = await client.delete(`${API_ROOT}/rules/${ruleId}`);
  return response.data;
};

export const executeRule = async (params: RuleExecuteParams) => {
  const response = await client.post(`${API_ROOT}/rules/${params.ruleId}/execute`, params);
  return response.data;
};

export const getRuleHistory = async (ruleId: string) => {
  const response = await client.get(`${API_ROOT}/rules/${ruleId}/history`);
  return response.data;
};

export const restoreRuleVersion = async (ruleId: string, version: number) => {
  const response = await client.post(`${API_ROOT}/rules/${ruleId}/restore/${version}`);
  return response.data;
};

// Mock数据支持
// 示例规则数据
export const mockRules: Rule[] = [
  {
    ruleId: 'rule-001',
    ruleName: '客户信用评估规则',
    type: RuleType.CONDITIONAL,
    description: '根据客户信息评估信用等级',
    status: RuleStatus.ACTIVE,
    graph: {
      nodes: [
        {
          id: 'node-1',
          type: NodeType.START,
          position: { x: 250, y: 5 },
          data: { label: '开始' }
        },
        {
          id: 'node-2',
          type: NodeType.CONDITION,
          position: { x: 250, y: 100 },
          data: { 
            label: '检查信用分数', 
            config: {
              condition: 'creditScore > 700'
            }
          }
        },
        {
          id: 'node-3',
          type: NodeType.ACTION,
          position: { x: 100, y: 200 },
          data: { 
            label: '高信用额度',
            config: {
              action: 'setCreditLimit',
              params: { limit: 50000 }
            }
          }
        },
        {
          id: 'node-4',
          type: NodeType.ACTION,
          position: { x: 400, y: 200 },
          data: { 
            label: '标准信用额度',
            config: {
              action: 'setCreditLimit',
              params: { limit: 10000 }
            }
          }
        },
        {
          id: 'node-5',
          type: NodeType.END,
          position: { x: 250, y: 300 },
          data: { label: '结束' }
        }
      ],
      edges: [
        { id: 'edge-1-2', source: 'node-1', target: 'node-2', type: EdgeType.DEFAULT },
        { id: 'edge-2-3', source: 'node-2', target: 'node-3', type: EdgeType.SUCCESS, label: '是' },
        { id: 'edge-2-4', source: 'node-2', target: 'node-4', type: EdgeType.FAILURE, label: '否' },
        { id: 'edge-3-5', source: 'node-3', target: 'node-5', type: EdgeType.DEFAULT },
        { id: 'edge-4-5', source: 'node-4', target: 'node-5', type: EdgeType.DEFAULT }
      ]
    },
    version: 1,
    createTime: '2023-01-10T00:00:00Z',
    updateTime: '2023-01-10T00:00:00Z'
  },
  {
    ruleId: 'rule-002',
    ruleName: '订单处理流程',
    type: RuleType.SEQUENCE,
    description: '订单验证、处理和发货流程',
    status: RuleStatus.ACTIVE,
    graph: {
      nodes: [
        {
          id: 'node-1',
          type: NodeType.START,
          position: { x: 250, y: 5 },
          data: { label: '开始' }
        },
        {
          id: 'node-2',
          type: NodeType.ACTION,
          position: { x: 250, y: 100 },
          data: { 
            label: '验证订单', 
            config: {
              action: 'validateOrder'
            }
          }
        },
        {
          id: 'node-3',
          type: NodeType.CONDITION,
          position: { x: 250, y: 200 },
          data: { 
            label: '库存检查',
            config: {
              condition: 'inventory > orderQuantity'
            }
          }
        },
        {
          id: 'node-4',
          type: NodeType.ACTION,
          position: { x: 100, y: 300 },
          data: { 
            label: '处理订单',
            config: {
              action: 'processOrder'
            }
          }
        },
        {
          id: 'node-5',
          type: NodeType.ACTION,
          position: { x: 400, y: 300 },
          data: { 
            label: '通知库存不足',
            config: {
              action: 'notifyOutOfStock'
            }
          }
        },
        {
          id: 'node-6',
          type: NodeType.END,
          position: { x: 250, y: 400 },
          data: { label: '结束' }
        }
      ],
      edges: [
        { id: 'edge-1-2', source: 'node-1', target: 'node-2', type: EdgeType.DEFAULT },
        { id: 'edge-2-3', source: 'node-2', target: 'node-3', type: EdgeType.DEFAULT },
        { id: 'edge-3-4', source: 'node-3', target: 'node-4', type: EdgeType.SUCCESS, label: '充足' },
        { id: 'edge-3-5', source: 'node-3', target: 'node-5', type: EdgeType.FAILURE, label: '不足' },
        { id: 'edge-4-6', source: 'node-4', target: 'node-6', type: EdgeType.DEFAULT },
        { id: 'edge-5-6', source: 'node-5', target: 'node-6', type: EdgeType.DEFAULT }
      ]
    },
    version: 3,
    createTime: '2023-01-15T00:00:00Z',
    updateTime: '2023-02-05T00:00:00Z'
  },
  {
    ruleId: 'rule-003',
    ruleName: '数据转换流程',
    type: RuleType.TRANSFORM,
    description: '输入数据格式转换与处理',
    status: RuleStatus.DRAFT,
    graph: {
      nodes: [
        {
          id: 'node-1',
          type: NodeType.START,
          position: { x: 250, y: 5 },
          data: { label: '开始' }
        },
        {
          id: 'node-2',
          type: NodeType.TRANSFORM,
          position: { x: 250, y: 100 },
          data: { 
            label: '数据解析', 
            config: {
              transform: 'parseJSON'
            }
          }
        },
        {
          id: 'node-3',
          type: NodeType.TRANSFORM,
          position: { x: 250, y: 200 },
          data: { 
            label: '字段映射',
            config: {
              transform: 'mapFields',
              mapping: {
                'id': 'userId',
                'name': 'fullName',
                'email': 'contactEmail'
              }
            }
          }
        },
        {
          id: 'node-4',
          type: NodeType.SCRIPT,
          position: { x: 250, y: 300 },
          data: { 
            label: '数据验证脚本',
            config: {
              scriptId: 'script-001',
              params: { validateEmail: true }
            }
          }
        },
        {
          id: 'node-5',
          type: NodeType.END,
          position: { x: 250, y: 400 },
          data: { label: '结束' }
        }
      ],
      edges: [
        { id: 'edge-1-2', source: 'node-1', target: 'node-2', type: EdgeType.DEFAULT },
        { id: 'edge-2-3', source: 'node-2', target: 'node-3', type: EdgeType.DEFAULT },
        { id: 'edge-3-4', source: 'node-3', target: 'node-4', type: EdgeType.DEFAULT },
        { id: 'edge-4-5', source: 'node-4', target: 'node-5', type: EdgeType.DEFAULT }
      ]
    },
    version: 2,
    createTime: '2023-02-10T00:00:00Z',
    updateTime: '2023-02-12T00:00:00Z'
  }
];

// 添加Mock API支持
// Use a function to determine if mock is enabled instead of directly using import.meta.env
function isMockEnabled() {
  try {
    // Check if we're in a browser environment with import.meta available
    return typeof window !== 'undefined' && 
           process.env.VITE_USE_MOCK_DATA === 'true';
  } catch (e) {
    // In test environment, return false by default
    return false;
  }
}

// 创建一个模拟响应
const createMockResponse = <T>(data: T) => {
  return {
    data: {
      code: 200,
      success: true,
      msg: 'success',
      data
    }
  };
};

// 如果启用了模拟数据，覆盖API函数
if (isMockEnabled()) {
  console.log('[MOCK] 启用规则Mock数据模式');
  
  // 创建一个mock实现包装器
  const mock = {
    // 获取规则列表
    getRuleList: (params?: RuleQuery) => {
      console.log('[MOCK] 获取规则列表，参数:', params);
      
      let filteredRules = [...mockRules];
      
      // 根据参数筛选规则
      if (params?.ruleName) {
        filteredRules = filteredRules.filter(rule => 
          rule.ruleName.toLowerCase().includes(params.ruleName!.toLowerCase())
        );
      }
      
      if (params?.type) {
        filteredRules = filteredRules.filter(rule => rule.type === params.type);
      }
      
      if (params?.status) {
        filteredRules = filteredRules.filter(rule => rule.status === params.status);
      }
      
      // 分页处理
      const page = params?.page || 1;
      const size = params?.size || 10;
      const start = (page - 1) * size;
      const end = start + size;
      const pagedRules = filteredRules.slice(start, end);
      
      return Promise.resolve(createMockResponse({
        list: pagedRules,
        total: filteredRules.length,
        page,
        size
      }));
    },
    
    // 获取规则详情
    getRuleById: (ruleId: string) => {
      console.log('[MOCK] 获取规则详情，ID:', ruleId);
      const rule = mockRules.find(r => r.ruleId === ruleId);
      
      if (rule) {
        return Promise.resolve(createMockResponse(rule));
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '规则不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 创建规则
    createRule: (params: RuleCreateParams) => {
      console.log('[MOCK] 创建规则，参数:', params);
      const newRule: Rule = {
        ruleId: `rule-${Date.now()}`,
        ruleName: params.ruleName,
        type: params.type,
        description: params.description || '',
        status: params.status || RuleStatus.DRAFT,
        graph: params.graph,
        version: 1,
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString(),
        tags: params.tags || []
      };
      
      mockRules.push(newRule);
      return Promise.resolve(createMockResponse({ ruleId: newRule.ruleId }));
    },
    
    // 更新规则
    updateRule: (params: RuleUpdateParams) => {
      console.log('[MOCK] 更新规则，参数:', params);
      const index = mockRules.findIndex(r => r.ruleId === params.ruleId);
      
      if (index !== -1) {
        // 增加版本号
        const updatedRule = {
          ...mockRules[index],
          ...params,
          version: mockRules[index].version + 1,
          updateTime: new Date().toISOString()
        };
        
        mockRules[index] = updatedRule;
        return Promise.resolve(createMockResponse(true));
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '规则不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 删除规则
    deleteRule: (ruleId: string) => {
      console.log('[MOCK] 删除规则，ID:', ruleId);
      const index = mockRules.findIndex(r => r.ruleId === ruleId);
      
      if (index !== -1) {
        mockRules.splice(index, 1);
        return Promise.resolve(createMockResponse(true));
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '规则不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 执行规则
    executeRule: (params: RuleExecuteParams) => {
      console.log('[MOCK] 执行规则，参数:', params);
      const rule = mockRules.find(r => r.ruleId === params.ruleId);
      
      if (rule) {
        return new Promise(resolve => {
          setTimeout(() => {
            // 模拟执行路径（简单示例）
            const path = rule.graph.nodes
              .filter(node => [NodeType.START, NodeType.ACTION, NodeType.END].includes(node.type))
              .map(node => node.id);
              
            const result: RuleExecuteResult = {
              success: true,
              executionTime: Math.floor(Math.random() * 500) + 50,
              output: `执行规则: ${rule.ruleName}\n路径节点数: ${path.length}`,
              result: { status: 'completed', data: params.params || {} },
              path,
              nodeResults: path.reduce((acc, nodeId) => {
                const node = rule.graph.nodes.find(n => n.id === nodeId);
                acc[nodeId] = {
                  success: true,
                  executionTime: Math.floor(Math.random() * 100),
                  output: `执行节点: ${node?.data.label || nodeId}`
                };
                return acc;
              }, {} as Record<string, any>)
            };
            
            resolve(createMockResponse(result));
          }, Math.random() * 1000 + 500);
        });
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '规则不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 规则版本历史
    getRuleHistory: (ruleId: string) => {
      console.log('[MOCK] 获取规则历史版本，ID:', ruleId);
      const rule = mockRules.find(r => r.ruleId === ruleId);
      
      if (rule) {
        // 生成模拟的历史版本
        const history = [];
        for (let i = 1; i <= rule.version; i++) {
          const versionDate = new Date(rule.createTime);
          versionDate.setDate(versionDate.getDate() + (i - 1) * 2); // 每个版本间隔2天
          
          history.push({
            ruleId: rule.ruleId,
            version: i,
            createdBy: 'admin',
            createTime: versionDate.toISOString(),
            comment: i === 1 ? '初始版本' : `版本更新 ${i}`
          });
        }
        
        return Promise.resolve(createMockResponse(history));
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '规则不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 恢复规则版本
    restoreRuleVersion: (ruleId: string, version: number) => {
      console.log('[MOCK] 恢复规则版本，ID:', ruleId, '版本:', version);
      const index = mockRules.findIndex(r => r.ruleId === ruleId);
      
      if (index !== -1) {
        const rule = mockRules[index];
        
        if (version > 0 && version <= rule.version) {
          // 模拟版本恢复（实际应该从历史中获取版本数据）
          const restoredRule = {
            ...rule,
            version: rule.version + 1,
            updateTime: new Date().toISOString()
          };
          
          mockRules[index] = restoredRule;
          return Promise.resolve(createMockResponse(true));
        } else {
          return Promise.reject({
            response: {
              data: {
                code: 400,
                success: false,
                msg: '无效的版本号',
                data: null
              }
            }
          });
        }
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '规则不存在',
              data: null
            }
          }
        });
      }
    }
  };
  
  // 使用模块模式覆盖原始导出函数
  Object.assign(module.exports, mock);
} 