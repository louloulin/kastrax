/**
 * 规则类型
 */
export enum RuleType {
  CONDITIONAL = 'CONDITIONAL',
  SEQUENCE = 'SEQUENCE',
  TRANSFORM = 'TRANSFORM',
  DECISION = 'DECISION',
  CUSTOM = 'CUSTOM'
}

/**
 * 规则状态
 */
export enum RuleStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  ARCHIVED = 'ARCHIVED'
}

/**
 * 节点类型
 */
export enum NodeType {
  START = 'START',
  END = 'END',
  CONDITION = 'CONDITION',
  ACTION = 'ACTION',
  TRANSFORM = 'TRANSFORM',
  DECISION = 'DECISION',
  API = 'API',
  SCRIPT = 'SCRIPT',
  KAFKA = 'KAFKA',
  DATABASE = 'DATABASE'
}

/**
 * 边类型
 */
export enum EdgeType {
  DEFAULT = 'DEFAULT',
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE',
  CONDITION = 'CONDITION'
}

/**
 * 图形节点接口
 */
export interface GraphNode {
  id: string;
  type: NodeType;
  position: {
    x: number;
    y: number;
  };
  data: {
    label: string;
    config?: any;
  };
}

/**
 * 图形边接口
 */
export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  type: EdgeType;
  label?: string;
}

/**
 * 规则图形接口
 */
export interface RuleGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

/**
 * 规则接口
 */
export interface Rule {
  ruleId: string;
  ruleName: string;
  type: RuleType;
  description: string;
  status: RuleStatus;
  graph: RuleGraph;
  version: number;
  createTime: string;
  updateTime: string;
  tags?: string[];
}

/**
 * 模拟规则数据
 */
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
            label: '数据转换',
            config: {
              transform: 'convertFormat',
              params: { outputFormat: 'XML' }
            }
          }
        },
        {
          id: 'node-4',
          type: NodeType.END,
          position: { x: 250, y: 300 },
          data: { label: '结束' }
        }
      ],
      edges: [
        { id: 'edge-1-2', source: 'node-1', target: 'node-2', type: EdgeType.DEFAULT },
        { id: 'edge-2-3', source: 'node-2', target: 'node-3', type: EdgeType.DEFAULT },
        { id: 'edge-3-4', source: 'node-3', target: 'node-4', type: EdgeType.DEFAULT }
      ]
    },
    version: 2,
    createTime: '2023-01-20T00:00:00Z',
    updateTime: '2023-01-25T00:00:00Z'
  }
]; 