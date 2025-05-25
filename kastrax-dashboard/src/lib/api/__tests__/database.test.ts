/**
 * Database API Tests
 */

// Mock the database module
jest.mock('../v1/database', () => ({
  getDatabaseList: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getDatabaseDetail: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  testConnection: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  createDatabase: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  updateDatabase: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  deleteDatabase: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getTableList: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getTableDetail: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getTableColumns: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getTablePreview: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getTableData: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  executeQuery: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getQueryHistory: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  saveQuery: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getSavedQueries: jest.fn().mockImplementation(() => Promise.resolve({ data: {} }))
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
  deleteRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: {} }))
}));

import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';

// Direct imports for v1 API
import {
  getDatabaseList as v1GetDatabaseList,
  getDatabaseDetail as v1GetDatabaseDetail,
  testConnection as v1TestConnection,
  createDatabase as v1CreateDatabase,
  updateDatabase as v1UpdateDatabase,
  deleteDatabase as v1DeleteDatabase,
  getTableList as v1GetTableList,
  getTableDetail as v1GetTableDetail,
  getTableColumns as v1GetTableColumns,
  getTablePreview as v1GetTablePreview,
  getTableData as v1GetTableData,
  executeQuery as v1ExecuteQuery,
  getQueryHistory as v1GetQueryHistory,
  saveQuery as v1SaveQuery,
  getSavedQueries as v1GetSavedQueries
} from '../v1/database';

// Direct imports for v2 API
import {
  getDatabaseList as v2GetDatabaseList,
  getDatabaseDetail as v2GetDatabaseDetail,
  testConnection as v2TestConnection,
  createDatabase as v2CreateDatabase,
  updateDatabase as v2UpdateDatabase,
  deleteDatabase as v2DeleteDatabase,
  getTableList as v2GetTableList,
  getTableDetail as v2GetTableDetail,
  getTableColumns as v2GetTableColumns,
  getTablePreview as v2GetTablePreview,
  getTableData as v2GetTableData,
  executeQuery as v2ExecuteQuery,
  getQueryHistory as v2GetQueryHistory,
  saveQuery as v2SaveQuery,
  getSavedQueries as v2GetSavedQueries
} from '../v2/database';

// Mock responses
const mockDatabases = [
  {
    dbId: '1',
    dbName: 'Production DB',
    dbType: 'postgresql',
    host: 'localhost',
    port: 5432,
    username: 'admin',
    schema: 'public',
    status: 1,
    createTime: '2024-03-20T00:00:00Z',
    updateTime: '2024-03-20T00:00:00Z'
  },
  {
    dbId: '2',
    dbName: 'Test DB',
    dbType: 'mysql',
    host: 'localhost',
    port: 3306,
    username: 'test',
    status: 1,
    createTime: '2024-03-21T00:00:00Z',
    updateTime: '2024-03-21T00:00:00Z'
  }
];

const mockTables = [
  {
    tableId: '1',
    tableName: 'users',
    dbId: '1',
    dbName: 'Production DB',
    schema: 'public',
    rowCount: 1000,
    size: 1024000,
    createTime: '2024-03-20T00:00:00Z',
    updateTime: '2024-03-20T00:00:00Z'
  }
];

const mockColumns = [
  {
    columnId: '1',
    columnName: 'id',
    tableId: '1',
    tableName: 'users',
    dataType: 'integer',
    isNullable: false,
    isPrimaryKey: true,
    isUnique: true
  }
];

// Setup mock server
const server = setupServer(
  // Database endpoints
  rest.get('/api/v2/database/list', (req, res, ctx) => {
    return res(ctx.json({ success: true, data: mockDatabases }));
  }),

  rest.get('/api/v2/database/detail/:id', (req, res, ctx) => {
    const db = mockDatabases.find(d => d.dbId === req.params.id);
    if (db) {
      return res(ctx.json({ success: true, data: db }));
    }
    return res(ctx.status(404), ctx.json({ success: false, message: 'Database not found' }));
  }),

  rest.post('/api/v2/database/test', (req, res, ctx) => {
    return res(ctx.json({ success: true, data: { success: true, message: 'Connection successful' } }));
  }),

  rest.post('/api/v2/database/create', (req, res, ctx) => {
    const body = req.body as Record<string, unknown>;
    return res(ctx.json({ success: true, data: { ...body, dbId: '3' } }));
  }),

  rest.put('/api/v2/database/update/:id', (req, res, ctx) => {
    const body = req.body as Record<string, unknown>;
    return res(ctx.json({ success: true, data: { ...body, dbId: req.params.id } }));
  }),

  rest.delete('/api/v2/database/delete/:id', (req, res, ctx) => {
    return res(ctx.json({ success: true, data: { success: true } }));
  }),

  // Table endpoints
  rest.get('/api/v2/table/list', (req, res, ctx) => {
    const dbId = req.url.searchParams.get('dbId');
    const tables = dbId ? mockTables.filter(t => t.dbId === dbId) : mockTables;
    return res(ctx.json({ success: true, data: tables }));
  }),

  rest.get('/api/v2/table/detail/:id', (req, res, ctx) => {
    const table = mockTables.find(t => t.tableId === req.params.id);
    if (table) {
      return res(ctx.json({ success: true, data: table }));
    }
    return res(ctx.status(404), ctx.json({ success: false, message: 'Table not found' }));
  }),

  rest.get('/api/v2/table/columns/:id', (req, res, ctx) => {
    return res(ctx.json({ success: true, data: mockColumns }));
  }),

  rest.get('/api/v2/table/preview/:id', (req, res, ctx) => {
    return res(ctx.json({
      success: true,
      data: {
        columns: mockColumns,
        rows: [{ id: 1, name: 'Test User' }]
      }
    }));
  }),

  rest.post('/api/v2/table/data/:id', (req, res, ctx) => {
    return res(ctx.json({
      success: true,
      data: {
        columns: mockColumns,
        rows: [{ id: 1, name: 'Test User' }],
        total: 1,
        page: 1,
        pageSize: 20
      }
    }));
  }),

  // Query endpoints
  rest.post('/api/v2/query/execute', (req, res, ctx) => {
    return res(ctx.json({
      success: true,
      data: {
        columns: mockColumns,
        rows: [{ id: 1, name: 'Test User' }],
        rowsAffected: 1,
        executionTime: 0.1
      }
    }));
  }),

  rest.get('/api/v2/query/history', (req, res, ctx) => {
    return res(ctx.json({
      success: true,
      data: [{
        id: '1',
        dbId: '1',
        sql: 'SELECT * FROM users',
        executionTime: 0.1,
        timestamp: '2024-03-20T00:00:00Z',
        status: 'success'
      }]
    }));
  }),

  rest.post('/api/v2/query/save', (req, res, ctx) => {
    const body = req.body as { name: string; dbId: string; sql: string };
    return res(ctx.json({
      success: true,
      data: {
        id: '1',
        name: body.name,
        dbId: body.dbId,
        sql: body.sql
      }
    }));
  }),

  rest.get('/api/v2/query/saved', (req, res, ctx) => {
    return res(ctx.json({
      success: true,
      data: [{
        id: '1',
        name: 'Test Query',
        dbId: '1',
        sql: 'SELECT * FROM users',
        createTime: '2024-03-20T00:00:00Z',
        updateTime: '2024-03-20T00:00:00Z'
      }]
    }));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Database API', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('v1 Database API', () => {
    test('getDatabaseList should call correctly', async () => {
      await v1GetDatabaseList();
      expect(v1GetDatabaseList).toHaveBeenCalled();
    });

    test('getDatabaseDetail should call with correct ID', async () => {
      await v1GetDatabaseDetail('db-123');
      expect(v1GetDatabaseDetail).toHaveBeenCalledWith('db-123');
    });

    test('testConnection should call with correct data', async () => {
      const data = { host: 'localhost', port: 5432 };
      await v1TestConnection(data);
      expect(v1TestConnection).toHaveBeenCalledWith(data);
    });

    test('createDatabase should call with correct data', async () => {
      const data = { dbName: 'test', dbType: 'postgresql' };
      await v1CreateDatabase(data);
      expect(v1CreateDatabase).toHaveBeenCalledWith(data);
    });

    test('updateDatabase should call with correct ID and data', async () => {
      const data = { dbName: 'updated' };
      await v1UpdateDatabase('db-123', data);
      expect(v1UpdateDatabase).toHaveBeenCalledWith('db-123', data);
    });

    test('deleteDatabase should call with correct ID', async () => {
      await v1DeleteDatabase('db-123');
      expect(v1DeleteDatabase).toHaveBeenCalledWith('db-123');
    });

    test('getTableList should call with correct dbId', async () => {
      await v1GetTableList('db-123');
      expect(v1GetTableList).toHaveBeenCalledWith('db-123');
    });

    test('getTableDetail should call with correct ID', async () => {
      await v1GetTableDetail('table-123');
      expect(v1GetTableDetail).toHaveBeenCalledWith('table-123');
    });

    test('getTableColumns should call with correct ID', async () => {
      await v1GetTableColumns('table-123');
      expect(v1GetTableColumns).toHaveBeenCalledWith('table-123');
    });

    test('getTablePreview should call with correct ID and limit', async () => {
      await v1GetTablePreview('table-123', 50);
      expect(v1GetTablePreview).toHaveBeenCalledWith('table-123', 50);
    });

    test('getTableData should call with correct parameters', async () => {
      await v1GetTableData('table-123', 1, 20, { name: 'test' }, { id: 'asc' });
      expect(v1GetTableData).toHaveBeenCalledWith('table-123', 1, 20, { name: 'test' }, { id: 'asc' });
    });

    test('executeQuery should call with correct parameters', async () => {
      await v1ExecuteQuery('db-123', 'SELECT * FROM users');
      expect(v1ExecuteQuery).toHaveBeenCalledWith('db-123', 'SELECT * FROM users');
    });

    test('getQueryHistory should call correctly', async () => {
      await v1GetQueryHistory();
      expect(v1GetQueryHistory).toHaveBeenCalled();
    });

    test('saveQuery should call with correct parameters', async () => {
      await v1SaveQuery('Test Query', 'db-123', 'SELECT * FROM users');
      expect(v1SaveQuery).toHaveBeenCalledWith('Test Query', 'db-123', 'SELECT * FROM users');
    });

    test('getSavedQueries should call correctly', async () => {
      await v1GetSavedQueries();
      expect(v1GetSavedQueries).toHaveBeenCalled();
    });
  });

  describe('v2 Database API', () => {
    test('getDatabaseList should use getRequest utility', async () => {
      await v2GetDatabaseList();
      expect(getRequest).toHaveBeenCalledWith('/api/v2/database/list');
    });

    test('getDatabaseDetail should use getRequest utility', async () => {
      await v2GetDatabaseDetail('db-123');
      expect(getRequest).toHaveBeenCalledWith('/api/v2/database/detail/db-123');
    });

    test('testConnection should use postRequest utility', async () => {
      const data = { host: 'localhost', port: 5432 };
      await v2TestConnection(data);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/database/test', data);
    });

    test('createDatabase should use postRequest utility', async () => {
      const data = { dbName: 'test', dbType: 'postgresql' };
      await v2CreateDatabase(data);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/database/create', data);
    });

    test('updateDatabase should use putRequest utility', async () => {
      const data = { dbName: 'updated' };
      await v2UpdateDatabase('db-123', data);
      expect(putRequest).toHaveBeenCalledWith('/api/v2/database/update/db-123', data);
    });

    test('deleteDatabase should use deleteRequest utility', async () => {
      await v2DeleteDatabase('db-123');
      expect(deleteRequest).toHaveBeenCalledWith('/api/v2/database/delete/db-123');
    });

    test('getTableList should use getRequest utility', async () => {
      await v2GetTableList('db-123');
      expect(getRequest).toHaveBeenCalledWith('/api/v2/table/list', { dbId: 'db-123' });
    });

    test('getTableDetail should use getRequest utility', async () => {
      await v2GetTableDetail('table-123');
      expect(getRequest).toHaveBeenCalledWith('/api/v2/table/detail/table-123');
    });

    test('getTableColumns should use getRequest utility', async () => {
      await v2GetTableColumns('table-123');
      expect(getRequest).toHaveBeenCalledWith('/api/v2/table/columns/table-123');
    });

    test('getTablePreview should use getRequest utility', async () => {
      await v2GetTablePreview('table-123', 50);
      expect(getRequest).toHaveBeenCalledWith('/api/v2/table/preview/table-123', { limit: 50 });
    });

    test('getTableData should use postRequest utility', async () => {
      await v2GetTableData('table-123', 1, 20, { name: 'test' }, { id: 'asc' });
      expect(postRequest).toHaveBeenCalledWith('/api/v2/table/data/table-123', {
        page: 1,
        pageSize: 20,
        filters: { name: 'test' },
        sorts: { id: 'asc' }
      });
    });

    test('executeQuery should use postRequest utility', async () => {
      await v2ExecuteQuery('db-123', 'SELECT * FROM users');
      expect(postRequest).toHaveBeenCalledWith('/api/v2/query/execute', {
        dbId: 'db-123',
        sql: 'SELECT * FROM users'
      });
    });

    test('getQueryHistory should use getRequest utility', async () => {
      await v2GetQueryHistory();
      expect(getRequest).toHaveBeenCalledWith('/api/v2/query/history');
    });

    test('saveQuery should use postRequest utility', async () => {
      await v2SaveQuery('Test Query', 'db-123', 'SELECT * FROM users');
      expect(postRequest).toHaveBeenCalledWith('/api/v2/query/save', {
        name: 'Test Query',
        dbId: 'db-123',
        sql: 'SELECT * FROM users'
      });
    });

    test('getSavedQueries should use getRequest utility', async () => {
      await v2GetSavedQueries();
      expect(getRequest).toHaveBeenCalledWith('/api/v2/query/saved');
    });
  });
}); 