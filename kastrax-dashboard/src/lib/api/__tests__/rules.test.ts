import { rest } from 'msw';
import { setupServer } from 'msw/node';
import client from '../client';
import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';

// Direct imports for v1 API
import { 
  getRuleList as v1GetRuleList,
  getRuleById as v1GetRuleById,
  createRule as v1CreateRule,
  updateRule as v1UpdateRule,
  deleteRule as v1DeleteRule,
  executeRule as v1ExecuteRule,
  RuleType
} from '../rules';

// Direct imports for v2 API
import {
  getRuleList as v2GetRuleList,
  getRuleById as v2GetRuleById,
  createRule as v2CreateRule,
  updateRule as v2UpdateRule,
  deleteRule as v2DeleteRule,
  executeRule as v2ExecuteRule,
  getRuleExecutionHistory,
  cloneRule
} from '../v2/rules';

// Mock the client and utility functions
jest.mock('../client', () => ({
  __esModule: true,
  default: {
    get: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
    post: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
    put: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
    delete: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  },
}));

jest.mock('../utils', () => ({
  getRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  postRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  putRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  deleteRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
}));

// Test data
const mockRule = {
  ruleId: 'rule-123',
  ruleName: 'Test Rule',
  type: 'conditional',
  description: 'Test rule description',
  status: 'draft',
  version: 1,
  graph: {
    nodes: [],
    edges: []
  }
};

describe('Rules API', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('v1 Rules API', () => {
    test('getRuleList should call client.get with correct path', async () => {
      await v1GetRuleList();
      expect(client.get).toHaveBeenCalledWith('/api/v1/rules', { params: {} });
    });

    test('getRuleById should call client.get with correct path', async () => {
      await v1GetRuleById('rule-123');
      expect(client.get).toHaveBeenCalledWith('/api/v1/rules/rule-123');
    });

    test('createRule should call client.post with correct path and data', async () => {
      const ruleData = {
        ruleName: 'Test Rule',
        type: RuleType.CONDITIONAL,
        graph: { nodes: [], edges: [] }
      };
      await v1CreateRule(ruleData);
      expect(client.post).toHaveBeenCalledWith('/api/v1/rules', ruleData);
    });

    test('updateRule should call client.put with correct path and data', async () => {
      const updateData = {
        ruleId: 'rule-123',
        ruleName: 'Updated Rule'
      };
      await v1UpdateRule(updateData);
      expect(client.put).toHaveBeenCalledWith('/api/v1/rules/rule-123', updateData);
    });

    test('deleteRule should call client.delete with correct path', async () => {
      await v1DeleteRule('rule-123');
      expect(client.delete).toHaveBeenCalledWith('/api/v1/rules/rule-123');
    });
    
    test('executeRule should call client.post with correct path', async () => {
      const params = {
        ruleId: 'rule-123',
        params: { testParam: 'value' }
      };
      await v1ExecuteRule(params);
      expect(client.post).toHaveBeenCalledWith('/api/v1/rules/rule-123/execute', params);
    });
  });

  describe('v2 Rules API', () => {
    test('v2GetRuleList should use getRequest utility', async () => {
      await v2GetRuleList({ page: 1, size: 10 });
      expect(getRequest).toHaveBeenCalledWith('/api/v2/rules', { page: 1, size: 10 });
    });

    test('v2GetRuleById should use getRequest utility', async () => {
      await v2GetRuleById('rule-123');
      expect(getRequest).toHaveBeenCalledWith('/api/v2/rules/rule-123');
    });
    
    test('v2CreateRule should use postRequest utility', async () => {
      const ruleData = {
        ruleName: 'Test Rule',
        type: RuleType.CONDITIONAL,
        graph: { nodes: [], edges: [] }
      };
      await v2CreateRule(ruleData);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/rules', ruleData);
    });
    
    test('v2UpdateRule should use putRequest utility', async () => {
      const ruleId = 'rule-123';
      const updateData = {
        ruleName: 'Updated Rule',
        description: 'Updated description'
      };
      await v2UpdateRule(ruleId, updateData);
      expect(putRequest).toHaveBeenCalledWith('/api/v2/rules/rule-123', updateData);
    });
    
    test('v2DeleteRule should use deleteRequest utility', async () => {
      await v2DeleteRule('rule-123');
      expect(deleteRequest).toHaveBeenCalledWith('/api/v2/rules/rule-123');
    });
    
    test('v2ExecuteRule should use postRequest utility', async () => {
      await v2ExecuteRule('rule-123', { testParam: 'value' });
      expect(postRequest).toHaveBeenCalledWith('/api/v2/rules/rule-123/execute', { testData: { testParam: 'value' } });
    });
    
    test('getRuleExecutionHistory should use getRequest utility', async () => {
      await getRuleExecutionHistory('rule-123', { page: 1, size: 10 });
      expect(getRequest).toHaveBeenCalledWith('/api/v2/rules/rule-123/executions', { page: 1, size: 10 });
    });
    
    test('cloneRule should use postRequest utility', async () => {
      await cloneRule('rule-123', 'New Rule Clone');
      expect(postRequest).toHaveBeenCalledWith('/api/v2/rules/rule-123/clone', { newName: 'New Rule Clone' });
    });
  });
}); 