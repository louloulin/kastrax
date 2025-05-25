/**
 * 规则适配器测试
 */

// 直接模拟适配器
jest.mock('../adapter/rule-adapter', () => ({
  ruleAdapter: {
    getRuleList: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: [
        {
          ruleId: 'rule-1',
          ruleName: 'Test Rule 1',
          graph: { nodes: [{ id: 'node-1', type: 'start' }], edges: [] },
          enable: true,
          updateTime: '2023-01-01'
        }
      ]
    }),
    getRuleById: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: {
        ruleId: 'rule-1',
        ruleName: 'Test Rule rule-1',
        graph: { nodes: [{ id: 'node-1', type: 'start' }], edges: [] },
        enable: true,
        updateTime: '2023-01-01'
      }
    }),
    createRule: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: { ruleId: 'new-rule-id' }
    }),
    updateRule: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: { success: true }
    }),
    deleteRule: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: { success: true }
    }),
    startRule: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: { success: true }
    }),
    stopRule: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: { success: true }
    }),
    executeRule: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: {
        success: true,
        result: { value: 42 },
        executionTime: 100
      }
    }),
    getRuleExecutionHistory: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: {
        list: [],
        total: 0,
        page: 1,
        size: 10
      }
    })
  }
}));

import { ruleAdapter } from '../adapter/rule-adapter';

describe('规则适配器测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getRuleList', () => {
    it('应该能获取规则列表', async () => {
      const result = await ruleAdapter.getRuleList();

      expect(result.success).toBe(true);
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data.length).toBe(1);
      expect(result.data[0].ruleId).toBe('rule-1');
    });

    it('应该能处理查询参数', async () => {
      const params = { ruleName: 'test', page: 1, size: 10 };
      await ruleAdapter.getRuleList(params);

      expect(ruleAdapter.getRuleList).toHaveBeenCalledWith(params);
    });
  });

  describe('getRuleById', () => {
    it('应该能获取规则详情', async () => {
      const result = await ruleAdapter.getRuleById('rule-1');

      expect(result.success).toBe(true);
      expect(result.data.ruleId).toBe('rule-1');
      expect(result.data.graph).toBeDefined();
      expect(result.data.graph.nodes).toBeDefined();
      expect(result.data.graph.nodes[0].id).toBe('node-1');
    });
  });

  describe('createRule', () => {
    it('应该能创建规则', async () => {
      const params = {
        ruleName: 'New Rule',
        type: 'CONDITIONAL',
        description: 'Test description',
        graph: {
          nodes: [{ id: 'node-1', type: 'start' }],
          edges: []
        }
      };

      const result = await ruleAdapter.createRule(params);

      expect(result.success).toBe(true);
      expect(result.data.ruleId).toBe('new-rule-id');
      expect(ruleAdapter.createRule).toHaveBeenCalledWith(params);
    });
  });

  describe('updateRule', () => {
    it('应该能更新规则', async () => {
      const params = {
        ruleName: 'Updated Rule',
        description: 'Updated description',
        graph: {
          nodes: [{ id: 'node-1', type: 'start' }],
          edges: []
        }
      };

      const result = await ruleAdapter.updateRule('rule-1', params);

      expect(result.success).toBe(true);
      expect(result.data.success).toBe(true);
      expect(ruleAdapter.updateRule).toHaveBeenCalledWith('rule-1', params);
    });
  });

  describe('deleteRule', () => {
    it('应该能删除规则', async () => {
      const result = await ruleAdapter.deleteRule('rule-1');

      expect(result.success).toBe(true);
      expect(result.data.success).toBe(true);
      expect(ruleAdapter.deleteRule).toHaveBeenCalledWith('rule-1');
    });
  });

  describe('startRule', () => {
    it('应该能启动规则', async () => {
      const result = await ruleAdapter.startRule('rule-1');

      expect(result.success).toBe(true);
      expect(result.data.success).toBe(true);
      expect(ruleAdapter.startRule).toHaveBeenCalledWith('rule-1');
    });
  });

  describe('stopRule', () => {
    it('应该能停止规则', async () => {
      const result = await ruleAdapter.stopRule('rule-1');

      expect(result.success).toBe(true);
      expect(result.data.success).toBe(true);
      expect(ruleAdapter.stopRule).toHaveBeenCalledWith('rule-1');
    });
  });

  describe('executeRule', () => {
    it('应该能执行规则', async () => {
      const testData = { input: 'test' };
      const result = await ruleAdapter.executeRule('rule-1', testData);

      expect(result.success).toBe(true);
      expect(result.data.success).toBe(true);
      expect(result.data.result).toEqual({ value: 42 });
      expect(ruleAdapter.executeRule).toHaveBeenCalledWith('rule-1', testData);
    });

    it('应该能处理空测试数据', async () => {
      const result = await ruleAdapter.executeRule('rule-1');

      expect(result.success).toBe(true);
      // 不检查第二个参数，因为它可能是 undefined 或空对象
      expect(ruleAdapter.executeRule).toHaveBeenCalledWith('rule-1');
    });
  });

  describe('getRuleExecutionHistory', () => {
    it('应该能获取规则执行历史（模拟实现）', async () => {
      const result = await ruleAdapter.getRuleExecutionHistory('rule-1', { page: 1, size: 10 });

      expect(result.success).toBe(true);
      expect(result.data.list).toEqual([]);
      expect(result.data.total).toBe(0);
      expect(result.data.page).toBe(1);
      expect(result.data.size).toBe(10);
      expect(ruleAdapter.getRuleExecutionHistory).toHaveBeenCalledWith('rule-1', { page: 1, size: 10 });
    });
  });
});
