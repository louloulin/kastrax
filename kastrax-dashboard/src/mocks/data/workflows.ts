import { TaskStatus } from '../../lib/api/types';

/**
 * 工作流类型
 */
export enum WorkflowType {
  SEQUENTIAL = 'SEQUENTIAL',
  PARALLEL = 'PARALLEL',
  CONDITIONAL = 'CONDITIONAL',
  EVENT_DRIVEN = 'EVENT_DRIVEN',
  CUSTOM = 'CUSTOM'
}

/**
 * 工作流状态
 */
export enum WorkflowStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  ARCHIVED = 'ARCHIVED'
}

/**
 * 工作流步骤类型
 */
export enum WorkflowStepType {
  SCRIPT = 'SCRIPT',
  RULE = 'RULE',
  API = 'API',
  WEBHOOK = 'WEBHOOK',
  EMAIL = 'EMAIL',
  NOTIFICATION = 'NOTIFICATION',
  APPROVAL = 'APPROVAL',
  CONDITION = 'CONDITION',
  DELAY = 'DELAY',
  KAFKA = 'KAFKA',
  DATABASE = 'DATABASE'
}

/**
 * 工作流步骤接口
 */
export interface WorkflowStep {
  stepId: string;
  name: string;
  type: WorkflowStepType;
  description?: string;
  config: {
    [key: string]: any;
  };
  nextSteps?: string[];
  conditions?: {
    condition: string;
    nextStep: string;
  }[];
}

/**
 * 工作流接口
 */
export interface Workflow {
  workflowId: string;
  workflowName: string;
  description: string;
  type: WorkflowType;
  status: WorkflowStatus;
  trigger: {
    type: string;
    config: any;
  };
  steps: WorkflowStep[];
  createTime: string;
  updateTime: string;
  tags?: string[];
}

/**
 * 工作流执行记录接口
 */
export interface WorkflowExecution {
  executionId: string;
  workflowId: string;
  status: TaskStatus;
  startTime: string;
  endTime?: string;
  duration?: number;
  steps: {
    [stepId: string]: {
      status: TaskStatus;
      startTime: string;
      endTime?: string;
      output?: any;
      error?: string;
    };
  };
  variables: {
    [key: string]: any;
  };
}

/**
 * 模拟工作流数据
 */
export const mockWorkflows: Workflow[] = [
  {
    workflowId: 'workflow-001',
    workflowName: '新用户注册流程',
    description: '处理新用户注册，验证信息并发送欢迎邮件',
    type: WorkflowType.SEQUENTIAL,
    status: WorkflowStatus.ACTIVE,
    trigger: {
      type: 'EVENT',
      config: {
        eventType: 'USER_REGISTERED'
      }
    },
    steps: [
      {
        stepId: 'step-001',
        name: '验证用户信息',
        type: WorkflowStepType.SCRIPT,
        config: {
          scriptId: 'script-001',
          timeout: 30
        },
        nextSteps: ['step-002']
      },
      {
        stepId: 'step-002',
        name: '创建用户账户',
        type: WorkflowStepType.API,
        config: {
          url: '/api/users',
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        },
        nextSteps: ['step-003']
      },
      {
        stepId: 'step-003',
        name: '发送欢迎邮件',
        type: WorkflowStepType.EMAIL,
        config: {
          template: 'welcome-email',
          from: 'noreply@example.com',
          subject: '欢迎加入我们的平台'
        }
      }
    ],
    createTime: '2023-02-01T00:00:00Z',
    updateTime: '2023-02-01T00:00:00Z',
    tags: ['用户', '注册', '自动化']
  },
  {
    workflowId: 'workflow-002',
    workflowName: '订单处理流程',
    description: '新订单处理，包括库存检查、支付处理和发货通知',
    type: WorkflowType.CONDITIONAL,
    status: WorkflowStatus.ACTIVE,
    trigger: {
      type: 'EVENT',
      config: {
        eventType: 'ORDER_CREATED'
      }
    },
    steps: [
      {
        stepId: 'step-001',
        name: '检查库存',
        type: WorkflowStepType.RULE,
        config: {
          ruleId: 'rule-002'
        },
        conditions: [
          {
            condition: 'result.inStock === true',
            nextStep: 'step-002'
          },
          {
            condition: 'result.inStock === false',
            nextStep: 'step-004'
          }
        ]
      },
      {
        stepId: 'step-002',
        name: '处理支付',
        type: WorkflowStepType.API,
        config: {
          url: '/api/payments',
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        },
        nextSteps: ['step-003']
      },
      {
        stepId: 'step-003',
        name: '创建发货单',
        type: WorkflowStepType.API,
        config: {
          url: '/api/shipments',
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        }
      },
      {
        stepId: 'step-004',
        name: '缺货通知',
        type: WorkflowStepType.NOTIFICATION,
        config: {
          channel: 'email',
          template: 'out-of-stock',
          to: '${order.customerEmail}'
        }
      }
    ],
    createTime: '2023-02-05T00:00:00Z',
    updateTime: '2023-02-10T00:00:00Z',
    tags: ['订单', '库存', '支付']
  },
  {
    workflowId: 'workflow-003',
    workflowName: '数据处理管道',
    description: '从多个源获取、转换和加载数据的ETL流程',
    type: WorkflowType.PARALLEL,
    status: WorkflowStatus.DRAFT,
    trigger: {
      type: 'SCHEDULE',
      config: {
        cron: '0 0 * * *' // 每天午夜执行
      }
    },
    steps: [
      {
        stepId: 'step-001',
        name: '从API获取数据',
        type: WorkflowStepType.API,
        config: {
          url: '/api/data/source1',
          method: 'GET',
          headers: {
            'Authorization': 'Bearer ${API_TOKEN}'
          }
        },
        nextSteps: ['step-003']
      },
      {
        stepId: 'step-002',
        name: '从数据库获取数据',
        type: WorkflowStepType.DATABASE,
        config: {
          connectionId: 'conn-001',
          query: 'SELECT * FROM orders WHERE created_at > ${lastRunDate}'
        },
        nextSteps: ['step-003']
      },
      {
        stepId: 'step-003',
        name: '数据转换',
        type: WorkflowStepType.SCRIPT,
        config: {
          scriptId: 'script-002',
          timeout: 120
        },
        nextSteps: ['step-004']
      },
      {
        stepId: 'step-004',
        name: '加载到数据仓库',
        type: WorkflowStepType.DATABASE,
        config: {
          connectionId: 'conn-002',
          query: 'INSERT INTO data_warehouse.facts (${columns}) VALUES (${values})'
        }
      }
    ],
    createTime: '2023-02-15T00:00:00Z',
    updateTime: '2023-02-15T00:00:00Z',
    tags: ['ETL', '数据', '自动化']
  }
]; 