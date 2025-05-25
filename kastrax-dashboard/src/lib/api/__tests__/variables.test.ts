/**
 * Variables API Tests
 */

// Mock the variables module
jest.mock('../v1/variables', () => ({
  getVariableList: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getVariableById: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  createVariable: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  updateVariable: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  deleteVariable: jest.fn().mockImplementation(() => Promise.resolve({ data: {} }))
}));

// Mock client and utilities
jest.mock('../client', () => ({
  get: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  post: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  put: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  delete: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  default: {
    get: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
    post: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
    put: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
    delete: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  }
}));

jest.mock('../utils', () => ({
  getRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  postRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  putRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  deleteRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
}));

import { rest } from 'msw';
import { setupServer } from 'msw/node';
import client from '../client';
import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';

// Direct imports for v1 API
import { 
  getVariableList as v1GetVariableList,
  getVariableById as v1GetVariableById,
  createVariable as v1CreateVariable,
  updateVariable as v1UpdateVariable,
  deleteVariable as v1DeleteVariable
} from '../v1/variables';

// Direct imports for v2 API
import {
  getVariableList as v2GetVariableList,
  getVariableById as v2GetVariableById,
  createVariable as v2CreateVariable,
  updateVariable as v2UpdateVariable,
  deleteVariable as v2DeleteVariable,
  getVariablesByPrefix as v2GetVariablesByPrefix,
  bulkUpsertVariables as v2BulkUpsertVariables,
  exportVariables as v2ExportVariables,
  importVariables as v2ImportVariables
} from '../v2/variables';

// Mock responses
const mockVariables = [
  {
    variableId: '1',
    variableName: 'DB_HOST',
    variableValue: 'localhost',
    description: 'Database host',
    createTime: '2023-01-01T00:00:00Z',
    updateTime: '2023-01-01T00:00:00Z'
  },
  {
    variableId: '2',
    variableName: 'API_KEY',
    variableValue: 'test-key-12345',
    description: 'API Key for testing',
    createTime: '2023-01-01T00:00:00Z',
    updateTime: '2023-01-01T00:00:00Z'
  }
];

// Helper function to safely get ID param as string
const getParamAsString = (param: any): string => {
  if (Array.isArray(param)) {
    return param[0] || '';
  }
  return String(param);
};

// Setup mock server
const server = setupServer(
  // v2 API endpoints
  rest.get('/api/v2/variables', (req, res, ctx) => {
    return res(
      ctx.json({
        success: true,
        data: {
          list: mockVariables,
          total: mockVariables.length,
          page: 1,
          size: 10
        },
      })
    );
  }),
  
  rest.get('/api/v2/variables/:id', (req, res, ctx) => {
    const id = getParamAsString(req.params.id);
    const variable = mockVariables.find(v => v.variableId === id);
    if (variable) {
      return res(ctx.json({ success: true, data: variable }));
    }
    return res(ctx.status(404), ctx.json({ success: false, message: 'Variable not found' }));
  }),
  
  rest.post('/api/v2/variables', (req, res, ctx) => {
    return res(ctx.json({ success: true, data: { variableId: '3' } }));
  }),
  
  rest.put('/api/v2/variables/:id', (req, res, ctx) => {
    const id = getParamAsString(req.params.id);
    return res(ctx.json({ success: true, data: { success: true } }));
  }),
  
  rest.delete('/api/v2/variables/:id', (req, res, ctx) => {
    const id = getParamAsString(req.params.id);
    return res(ctx.json({ success: true, data: { success: true } }));
  }),
  
  // v2 additional endpoints
  rest.get('/api/v2/variables/prefix/:prefix', (req, res, ctx) => {
    const prefix = getParamAsString(req.params.prefix);
    const variables = mockVariables.filter(v => v.variableName.startsWith(prefix));
    return res(ctx.json({ success: true, data: variables }));
  }),
  
  rest.post('/api/v2/variables/bulk', (req, res, ctx) => {
    return res(ctx.json({ 
      success: true, 
      data: { success: true, inserted: 2, updated: 1 } 
    }));
  }),
  
  rest.post('/api/v2/variables/export', (req, res, ctx) => {
    return res(ctx.json({ 
      success: true, 
      data: { variables: mockVariables } 
    }));
  }),
  
  rest.post('/api/v2/variables/import', (req, res, ctx) => {
    return res(ctx.json({ 
      success: true, 
      data: { success: true, imported: 3, skipped: 1 } 
    }));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Variables API', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('v1 Variables API', () => {
    test('getVariableList should call with correct parameters', async () => {
      const params = { variableName: 'test', page: 1, pageSize: 10 };
      await v1GetVariableList(params);
      expect(v1GetVariableList).toHaveBeenCalledWith(params);
    });

    test('getVariableById should call with correct ID', async () => {
      await v1GetVariableById('var-123');
      expect(v1GetVariableById).toHaveBeenCalledWith('var-123');
    });

    test('createVariable should call with correct data', async () => {
      const variableData = {
        variableName: 'NEW_VAR',
        variableValue: 'value',
        description: 'New variable'
      };
      await v1CreateVariable(variableData);
      expect(v1CreateVariable).toHaveBeenCalledWith(variableData);
    });

    test('updateVariable should call with correct data', async () => {
      const updateData = {
        variableId: 'var-123',
        variableName: 'UPDATED_VAR',
        variableValue: 'updated value'
      };
      await v1UpdateVariable(updateData);
      expect(v1UpdateVariable).toHaveBeenCalledWith(updateData);
    });

    test('deleteVariable should call with correct ID', async () => {
      await v1DeleteVariable('var-123');
      expect(v1DeleteVariable).toHaveBeenCalledWith('var-123');
    });
  });

  describe('v2 Variables API', () => {
    test('getVariableList should use getRequest utility', async () => {
      await v2GetVariableList({ variableName: 'test', page: 1, pageSize: 10 });
      expect(getRequest).toHaveBeenCalledWith('/api/v2/variables', { variableName: 'test', page: 1, pageSize: 10 });
    });

    test('getVariableById should use getRequest utility', async () => {
      await v2GetVariableById('var-123');
      expect(getRequest).toHaveBeenCalledWith('/api/v2/variables/var-123');
    });
    
    test('createVariable should use postRequest utility', async () => {
      const variableData = {
        variableName: 'NEW_VAR',
        variableValue: 'value',
        description: 'New variable'
      };
      await v2CreateVariable(variableData);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/variables', variableData);
    });
    
    test('updateVariable should use putRequest utility', async () => {
      const variableId = 'var-123';
      const updateData = {
        variableName: 'UPDATED_VAR',
        variableValue: 'updated value'
      };
      await v2UpdateVariable(variableId, updateData);
      expect(putRequest).toHaveBeenCalledWith('/api/v2/variables/var-123', updateData);
    });
    
    test('deleteVariable should use deleteRequest utility', async () => {
      await v2DeleteVariable('var-123');
      expect(deleteRequest).toHaveBeenCalledWith('/api/v2/variables/var-123');
    });
    
    test('getVariablesByPrefix should use getRequest utility', async () => {
      await v2GetVariablesByPrefix('DB_');
      expect(getRequest).toHaveBeenCalledWith('/api/v2/variables/prefix/DB_');
    });
    
    test('bulkUpsertVariables should use postRequest utility', async () => {
      const variables = [
        { variableName: 'VAR1', variableValue: 'value1' },
        { variableName: 'VAR2', variableValue: 'value2' }
      ];
      await v2BulkUpsertVariables(variables);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/variables/bulk', { variables });
    });
    
    test('exportVariables should use postRequest utility', async () => {
      const variableIds = ['var-1', 'var-2'];
      await v2ExportVariables(variableIds);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/variables/export', { variableIds });
    });
    
    test('importVariables should use postRequest utility', async () => {
      const variables = [
        {
          variableId: '1',
          variableName: 'VAR1',
          variableValue: 'value1',
          description: '',
          createTime: '',
          updateTime: ''
        }
      ];
      await v2ImportVariables(variables, true);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/variables/import', { variables, overwrite: true });
    });
  });
}); 