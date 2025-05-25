import client, { ApiResponse } from './client';
import { AxiosResponse } from 'axios';
import { getBaseUrl } from "./base";

// Workflow Component Types
export enum ComponentType {
  SOURCE = 'source',
  TRANSFORM = 'transform',
  SINK = 'sink'
}

// Source Component Subtypes
export enum SourceType {
  DATABASE = 'DATABASE',
  FILE = 'FILE',
  API = 'API',
  SCHEDULE = 'SCHEDULE',
  WEBHOOK = 'WEBHOOK',
  RESOURCE = 'RESOURCE',
}

// Transform Component Subtypes
export enum TransformType {
  FILTER = 'FILTER',
  MAP = 'MAP',
  AGGREGATE = 'AGGREGATE',
  JOIN = 'JOIN',
  SCRIPT = 'SCRIPT',
  SPLIT = 'SPLIT',
}

// Sink Component Subtypes
export enum SinkType {
  DATABASE = 'DATABASE',
  FILE = 'FILE',
  API = 'API',
  MESSAGE = 'MESSAGE',
  EMAIL = 'EMAIL',
  SERVICE = 'SERVICE',
}

// Edge Type
export enum ConnectionType {
  DEFAULT = 'default',
  SUCCESS = 'success',
  ERROR = 'error',
  CONDITION = 'condition'
}

// Workflow Status
export enum WorkflowStatus {
  DRAFT = 'draft',
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  DEPRECATED = 'deprecated'
}

// Component Interface
export interface WorkflowComponent {
  id: string;
  type: ComponentType;
  subType: SourceType | TransformType | SinkType;
  position: {
    x: number;
    y: number;
  };
  data: {
    label: string;
    config: any;
  };
}

// Connection Interface
export interface WorkflowConnection {
  id: string;
  source: string;
  target: string;
  type: ConnectionType;
  animated?: boolean;
  label?: string;
}

// Workflow Graph
export interface WorkflowGraph {
  components: WorkflowComponent[];
  connections: WorkflowConnection[];
}

// Workflow Interface
export interface Workflow {
  workflowId: string;
  workflowName: string;
  description?: string;
  type: string;
  status: WorkflowStatus;
  graph: WorkflowGraph;
  tags?: string[];
  version: number;
  createTime: string;
  updateTime: string;
}

// Query Parameters
export interface WorkflowQuery {
  workflowName?: string;
  type?: string;
  status?: WorkflowStatus;
  tags?: string[];
  page?: number;
  size?: number;
}

// Create Parameters
export interface WorkflowCreateParams {
  workflowName: string;
  description?: string;
  type: string;
  status?: WorkflowStatus;
  graph: WorkflowGraph;
  tags?: string[];
}

// Update Parameters
export interface WorkflowUpdateParams {
  workflowId: string;
  workflowName?: string;
  description?: string;
  type?: string;
  status?: WorkflowStatus;
  graph?: WorkflowGraph;
  tags?: string[];
}

// Execute Parameters
export interface WorkflowExecuteParams {
  workflowId: string;
  params?: Record<string, any>;
  timeout?: number;
}

// Workflow Execution Result
export interface WorkflowExecuteResult {
  success: boolean;
  executionTime: number;
  output: string;
  error?: string;
  result?: any;
  componentResults?: Record<string, any>;
}

// Workflow History Record
export interface WorkflowHistoryRecord {
  workflowId: string;
  version: number;
  graph: WorkflowGraph;
  updatedBy: string;
  updateTime: string;
  comment?: string;
}

// API Functions

// List workflows with pagination
export function getWorkflowList(params: WorkflowQuery = {}): Promise<AxiosResponse<ApiResponse<Workflow[]>>> {
  const baseUrl = getBaseUrl();
  return client.post('/workflow/list', params);
}

// Get workflow by ID
export function getWorkflowById(workflowId: string): Promise<AxiosResponse<ApiResponse<Workflow>>> {
  const baseUrl = getBaseUrl();
  return client.get(`/workflow/${workflowId}`);
}

// Create a new workflow
export function createWorkflow(params: WorkflowCreateParams): Promise<AxiosResponse<ApiResponse<{workflowId: string}>>> {
  const baseUrl = getBaseUrl();
  return client.post('/workflow/add', params);
}

// Update an existing workflow
export function updateWorkflow(params: WorkflowUpdateParams): Promise<AxiosResponse<ApiResponse<boolean>>> {
  const baseUrl = getBaseUrl();
  return client.post('/workflow/update', params);
}

// Delete a workflow
export function deleteWorkflow(workflowId: string): Promise<AxiosResponse<ApiResponse<boolean>>> {
  const baseUrl = getBaseUrl();
  return client.post('/workflow/delete', { workflowId });
}

// Execute a workflow
export function executeWorkflow(params: WorkflowExecuteParams): Promise<AxiosResponse<ApiResponse<WorkflowExecuteResult>>> {
  const baseUrl = getBaseUrl();
  return client.post('/workflow/execute', params);
}

// Get workflow history
export function getWorkflowHistory(workflowId: string): Promise<AxiosResponse<ApiResponse<WorkflowHistoryRecord[]>>> {
  const baseUrl = getBaseUrl();
  return client.get(`/workflow/history/${workflowId}`);
}

// Restore workflow to a specific version
export function restoreWorkflowVersion(workflowId: string, version: number): Promise<AxiosResponse<ApiResponse<boolean>>> {
  const baseUrl = getBaseUrl();
  return client.post('/workflow/restore', { workflowId, version });
}

// Mock data for development
const mockWorkflows: Workflow[] = [
  {
    workflowId: 'wf-1',
    workflowName: '数据ETL流程',
    description: '从数据库获取数据，进行处理后写入到数据仓库',
    type: 'etl',
    status: WorkflowStatus.ACTIVE,
    graph: {
      components: [
        {
          id: 'component-1',
          type: ComponentType.SOURCE,
          subType: SourceType.DATABASE,
          position: { x: 100, y: 100 },
          data: {
            label: '数据库源',
            config: {
              resourceId: 'resource-1',
              query: 'SELECT * FROM users WHERE created_at > :lastRunTime'
            }
          }
        },
        {
          id: 'component-2',
          type: ComponentType.TRANSFORM,
          subType: TransformType.MAP,
          position: { x: 400, y: 100 },
          data: {
            label: '数据转换',
            config: {
              mappings: {
                'user_id': 'id',
                'username': 'name',
                'email': 'email',
                'created': 'created_at'
              }
            }
          }
        },
        {
          id: 'component-3',
          type: ComponentType.SINK,
          subType: SinkType.DATABASE,
          position: { x: 700, y: 100 },
          data: {
            label: '数据仓库',
            config: {
              resourceId: 'resource-2',
              table: 'user_analytics',
              batchSize: 1000
            }
          }
        }
      ],
      connections: [
        {
          id: 'connection-1',
          source: 'component-1',
          target: 'component-2',
          type: ConnectionType.DEFAULT
        },
        {
          id: 'connection-2',
          source: 'component-2',
          target: 'component-3',
          type: ConnectionType.DEFAULT
        }
      ]
    },
    tags: ['ETL', '用户数据'],
    version: 1,
    createTime: '2023-03-15T00:00:00Z',
    updateTime: '2023-03-15T00:00:00Z'
  },
  {
    workflowId: 'wf-2',
    workflowName: 'API数据同步',
    description: '定期从外部API获取数据并存储',
    type: 'sync',
    status: WorkflowStatus.ACTIVE,
    graph: {
      components: [
        {
          id: 'component-1',
          type: ComponentType.SOURCE,
          subType: SourceType.SCHEDULE,
          position: { x: 100, y: 100 },
          data: {
            label: '定时触发',
            config: {
              schedule: '0 0 * * *' // 每天0点触发
            }
          }
        },
        {
          id: 'component-2',
          type: ComponentType.SOURCE,
          subType: SourceType.API,
          position: { x: 400, y: 100 },
          data: {
            label: 'API请求',
            config: {
              url: 'https://api.example.com/data',
              method: 'GET',
              headers: {
                'Authorization': 'Bearer ${variables.API_TOKEN}'
              }
            }
          }
        },
        {
          id: 'component-3',
          type: ComponentType.TRANSFORM,
          subType: TransformType.FILTER,
          position: { x: 700, y: 100 },
          data: {
            label: '数据过滤',
            config: {
              condition: 'item.status === "active"'
            }
          }
        },
        {
          id: 'component-4',
          type: ComponentType.SINK,
          subType: SinkType.DATABASE,
          position: { x: 1000, y: 100 },
          data: {
            label: '数据库存储',
            config: {
              resourceId: 'resource-3',
              table: 'external_data',
              mode: 'upsert',
              keyFields: ['id']
            }
          }
        }
      ],
      connections: [
        {
          id: 'connection-1',
          source: 'component-1',
          target: 'component-2',
          type: ConnectionType.DEFAULT
        },
        {
          id: 'connection-2',
          source: 'component-2',
          target: 'component-3',
          type: ConnectionType.DEFAULT
        },
        {
          id: 'connection-3',
          source: 'component-3',
          target: 'component-4',
          type: ConnectionType.DEFAULT
        }
      ]
    },
    tags: ['API', '数据同步'],
    version: 3,
    createTime: '2023-02-20T00:00:00Z',
    updateTime: '2023-03-10T00:00:00Z'
  }
];

// Mock 数据支持
// Use a function to determine if mock is enabled instead of directly using import.meta.env
function isMockEnabled() {
  try {
    // Check if we're in a browser environment
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
  console.log('[MOCK] 启用工作流Mock数据模式');
  
  // 获取工作流列表
  (getWorkflowList as any) = (params?: WorkflowQuery) => {
    console.log('[MOCK] 获取工作流列表，参数:', params);
    
    let filteredWorkflows = [...mockWorkflows];
    
    // 筛选
    if (params?.workflowName) {
      filteredWorkflows = filteredWorkflows.filter(w => 
        w.workflowName.toLowerCase().includes(params.workflowName!.toLowerCase())
      );
    }
    
    if (params?.type) {
      filteredWorkflows = filteredWorkflows.filter(w => w.type === params.type);
    }
    
    if (params?.status) {
      filteredWorkflows = filteredWorkflows.filter(w => w.status === params.status);
    }
    
    if (params?.tags && params.tags.length > 0) {
      filteredWorkflows = filteredWorkflows.filter(w => 
        w.tags?.some(tag => params.tags!.includes(tag))
      );
    }
    
    // 分页
    const page = params?.page || 1;
    const size = params?.size || 10;
    const start = (page - 1) * size;
    const end = start + size;
    const pagedWorkflows = filteredWorkflows.slice(start, end);
    
    return Promise.resolve(createMockResponse({
      list: pagedWorkflows,
      total: filteredWorkflows.length,
      page,
      size
    }));
  };
  
  // 获取工作流详情
  (getWorkflowById as any) = (workflowId: string) => {
    console.log('[MOCK] 获取工作流详情，ID:', workflowId);
    const workflow = mockWorkflows.find(w => w.workflowId === workflowId);
    
    if (workflow) {
      return Promise.resolve(createMockResponse(workflow));
    } else {
      return Promise.reject({
        response: {
          data: {
            code: 404,
            success: false,
            msg: '工作流不存在',
            data: null
          }
        }
      });
    }
  };
  
  // 创建工作流
  (createWorkflow as any) = (params: WorkflowCreateParams) => {
    console.log('[MOCK] 创建工作流，参数:', params);
    const newWorkflow: Workflow = {
      workflowId: `wf-${Date.now()}`,
      workflowName: params.workflowName,
      description: params.description,
      type: params.type,
      status: params.status || WorkflowStatus.DRAFT,
      graph: params.graph,
      tags: params.tags,
      version: 1,
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    };
    
    mockWorkflows.push(newWorkflow);
    return Promise.resolve(createMockResponse({ workflowId: newWorkflow.workflowId }));
  };
  
  // 更新工作流
  (updateWorkflow as any) = (params: WorkflowUpdateParams) => {
    console.log('[MOCK] 更新工作流，参数:', params);
    const index = mockWorkflows.findIndex(w => w.workflowId === params.workflowId);
    
    if (index !== -1) {
      const updatedWorkflow = {
        ...mockWorkflows[index],
        ...params,
        version: mockWorkflows[index].version + 1,
        updateTime: new Date().toISOString()
      };
      
      mockWorkflows[index] = updatedWorkflow;
      return Promise.resolve(createMockResponse(true));
    } else {
      return Promise.reject({
        response: {
          data: {
            code: 404,
            success: false,
            msg: '工作流不存在',
            data: null
          }
        }
      });
    }
  };
  
  // 删除工作流
  (deleteWorkflow as any) = (workflowId: string) => {
    console.log('[MOCK] 删除工作流，ID:', workflowId);
    const index = mockWorkflows.findIndex(w => w.workflowId === workflowId);
    
    if (index !== -1) {
      mockWorkflows.splice(index, 1);
      return Promise.resolve(createMockResponse(true));
    } else {
      return Promise.reject({
        response: {
          data: {
            code: 404,
            success: false,
            msg: '工作流不存在',
            data: null
          }
        }
      });
    }
  };
  
  // 执行工作流
  (executeWorkflow as any) = (params: WorkflowExecuteParams) => {
    console.log('[MOCK] 执行工作流，参数:', params);
    const workflow = mockWorkflows.find(w => w.workflowId === params.workflowId);
    
    if (workflow) {
      return new Promise(resolve => {
        setTimeout(() => {
          // 模拟执行结果
          const result: WorkflowExecuteResult = {
            success: true,
            executionTime: Math.floor(Math.random() * 1500) + 500,
            output: `执行工作流: ${workflow.workflowName}\n组件数: ${workflow.graph.components.length}`,
            result: { status: 'completed', data: params.params || {} },
            componentResults: workflow.graph.components.reduce((acc, component) => {
              acc[component.id] = {
                success: true,
                executionTime: Math.floor(Math.random() * 300),
                output: `执行组件: ${component.data.label}`
              };
              return acc;
            }, {} as Record<string, any>)
          };
          
          resolve(createMockResponse(result));
        }, Math.random() * 2000 + 1000);
      });
    } else {
      return Promise.reject({
        response: {
          data: {
            code: 404,
            success: false,
            msg: '工作流不存在',
            data: null
          }
        }
      });
    }
  };
  
  // 工作流历史
  (getWorkflowHistory as any) = (workflowId: string) => {
    console.log('[MOCK] 获取工作流历史，ID:', workflowId);
    const workflow = mockWorkflows.find(w => w.workflowId === workflowId);
    
    if (workflow) {
      // 生成模拟历史版本
      const history: WorkflowHistoryRecord[] = [];
      
      for (let i = 1; i <= workflow.version; i++) {
        const date = new Date();
        date.setDate(date.getDate() - (workflow.version - i));
        
        history.push({
          workflowId: workflow.workflowId,
          version: i,
          graph: workflow.graph,
          updatedBy: 'admin@example.com',
          updateTime: date.toISOString(),
          comment: i === 1 ? '初始创建' : `更新版本 ${i}`
        });
      }
      
      return Promise.resolve(createMockResponse(history));
    } else {
      return Promise.reject({
        response: {
          data: {
            code: 404,
            success: false,
            msg: '工作流不存在',
            data: null
          }
        }
      });
    }
  };
  
  // 恢复工作流版本
  (restoreWorkflowVersion as any) = (workflowId: string, version: number) => {
    console.log('[MOCK] 恢复工作流版本，ID:', workflowId, '版本:', version);
    const index = mockWorkflows.findIndex(w => w.workflowId === workflowId);
    
    if (index !== -1) {
      // 现实中这里应该从历史版本获取数据
      mockWorkflows[index] = {
        ...mockWorkflows[index],
        version: mockWorkflows[index].version + 1,
        updateTime: new Date().toISOString()
      };
      
      return Promise.resolve(createMockResponse(true));
    } else {
      return Promise.reject({
        response: {
          data: {
            code: 404,
            success: false,
            msg: '工作流不存在',
            data: null
          }
        }
      });
    }
  };
} 