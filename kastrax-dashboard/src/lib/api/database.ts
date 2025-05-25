import client from './client';

export interface Database {
  dbId: string;
  dbName: string;
  dbType: string;
  host: string;
  port: number;
  username: string;
  schema?: string;
  status: number;
  createTime: string;
  updateTime: string;
  connectionInfo?: string;
}

export interface Table {
  tableId: string;
  tableName: string;
  dbId: string;
  dbName: string;
  schema?: string;
  rowCount: number;
  size: number;
  createTime: string;
  updateTime: string;
  description?: string;
}

export interface Column {
  columnId: string;
  columnName: string;
  tableId: string;
  tableName: string;
  dataType: string;
  isNullable: boolean;
  isPrimaryKey: boolean;
  isUnique: boolean;
  defaultValue?: string;
  description?: string;
}

// Database functions
export const getDatabaseList = () => {
  return client.get('/api/v1/database/list');
};

export const getDatabaseDetail = (dbId: string) => {
  return client.get(`/api/v1/database/detail/${dbId}`);
};

export const testConnection = (data: Partial<Database>) => {
  return client.post('/api/v1/database/test', data);
};

export const createDatabase = (data: Partial<Database>) => {
  return client.post('/api/v1/database/create', data);
};

export const updateDatabase = (dbId: string, data: Partial<Database>) => {
  return client.put(`/api/v1/database/update/${dbId}`, data);
};

export const deleteDatabase = (dbId: string) => {
  return client.delete(`/api/v1/database/delete/${dbId}`);
};

// Table functions
export const getTableList = (dbId?: string) => {
  return client.get('/api/v1/table/list', { params: { dbId } });
};

export const getTableDetail = (tableId: string) => {
  return client.get(`/api/v1/table/detail/${tableId}`);
};

export const getTableColumns = (tableId: string) => {
  return client.get(`/api/v1/table/columns/${tableId}`);
};

export const getTablePreview = (tableId: string, limit: number = 100) => {
  return client.get(`/api/v1/table/preview/${tableId}`, { params: { limit } });
};

export const getTableData = (tableId: string, page: number = 1, pageSize: number = 20, filters?: any, sorts?: any) => {
  return client.post(`/api/v1/table/data/${tableId}`, { page, pageSize, filters, sorts });
};

// SQL Query functions
export const executeQuery = (dbId: string, sql: string) => {
  return client.post('/api/v1/query/execute', { dbId, sql });
};

export const getQueryHistory = () => {
  return client.get('/api/v1/query/history');
};

export const saveQuery = (name: string, dbId: string, sql: string) => {
  return client.post('/api/v1/query/save', { name, dbId, sql });
};

export const getSavedQueries = () => {
  return client.get('/api/v1/query/saved');
}; 