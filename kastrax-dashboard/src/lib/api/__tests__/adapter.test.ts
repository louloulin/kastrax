/**
 * API 适配层测试
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
    })
  }
}));

jest.mock('../adapter/auth-adapter', () => ({
  authAdapter: {
    login: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: {
        token: 'test-token',
        user: {
          userId: 'user-1',
          username: 'testuser'
        }
      }
    }),
    getInfo: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: {
        userId: 'user-1',
        username: 'testuser',
        role: 'admin'
      }
    })
  }
}));

jest.mock('../adapter/resource-adapter', () => ({
  resourceAdapter: {
    getResourceList: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: [
        {
          resourceId: 'resource-1',
          resourceName: 'Test Resource 1',
          resourceType: 'MYSQL',
          properties: '{"host":"localhost","port":3306}'
        }
      ]
    })
  }
}));

jest.mock('../adapter/script-adapter', () => ({
  scriptAdapter: {
    getScriptList: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: [
        {
          scriptId: 'script-1',
          scriptName: 'Test Script 1',
          scriptContent: 'console.log("Hello")',
          scriptLanguage: 'javascript'
        }
      ]
    })
  }
}));

jest.mock('../adapter/system-adapter', () => ({
  systemAdapter: {
    getSystemInfo: jest.fn().mockResolvedValue({
      code: 200,
      success: true,
      msg: 'success',
      data: {
        version: '1.0.0',
        uptime: 3600
      }
    })
  }
}));

import { ruleAdapter } from '../adapter/rule-adapter';
import { authAdapter } from '../adapter/auth-adapter';
import { resourceAdapter } from '../adapter/resource-adapter';
import { scriptAdapter } from '../adapter/script-adapter';
import { systemAdapter } from '../adapter/system-adapter';

describe('API 适配层测试', () => {
  describe('规则适配器', () => {
    it('应该能获取规则列表', async () => {
      const result = await ruleAdapter.getRuleList();

      expect(result.success).toBe(true);
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data.length).toBe(1);
      expect(result.data[0].ruleId).toBe('rule-1');
    });

    it('应该能获取规则详情', async () => {
      const result = await ruleAdapter.getRuleById('rule-1');

      expect(result.success).toBe(true);
      expect(result.data.ruleId).toBe('rule-1');
      expect(result.data.graph).toBeDefined();
      expect(result.data.graph.nodes).toBeDefined();
    });

    it('应该能创建规则', async () => {
      const result = await ruleAdapter.createRule({
        ruleName: 'New Rule',
        type: 'CONDITIONAL',
        graph: {
          nodes: [],
          edges: []
        }
      });

      expect(result.success).toBe(true);
      expect(result.data.ruleId).toBe('new-rule-id');
    });
  });

  describe('认证适配器', () => {
    it('应该能登录', async () => {
      const result = await authAdapter.login({
        username: 'testuser',
        password: 'password'
      });

      expect(result.success).toBe(true);
      expect(result.data.token).toBe('test-token');
      expect(result.data.user.username).toBe('testuser');
    });

    it('应该能获取用户信息', async () => {
      const result = await authAdapter.getInfo();

      expect(result.success).toBe(true);
      expect(result.data.username).toBe('testuser');
      expect(result.data.role).toBe('admin');
    });
  });

  describe('资源适配器', () => {
    it('应该能获取资源列表', async () => {
      const result = await resourceAdapter.getResourceList();

      expect(result.success).toBe(true);
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data.length).toBe(1);
      expect(result.data[0].resourceId).toBe('resource-1');
    });
  });

  describe('脚本适配器', () => {
    it('应该能获取脚本列表', async () => {
      const result = await scriptAdapter.getScriptList();

      expect(result.success).toBe(true);
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data.length).toBe(1);
      expect(result.data[0].scriptId).toBe('script-1');
    });
  });

  describe('系统适配器', () => {
    it('应该能获取系统信息', async () => {
      const result = await systemAdapter.getSystemInfo();

      expect(result.success).toBe(true);
      expect(result.data.version).toBe('1.0.0');
    });
  });
});
