/**
 * Database API v2
 * 
 * 数据库管理相关的API
 */
import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';
import { Database, Table, Column } from '../database';

/**
 * API Endpoints
 */
const BASE_PATH = '/api/v2';
const DATABASE_PATH = `${BASE_PATH}/database`;
const TABLE_PATH = `${BASE_PATH}/table`;
const QUERY_PATH = `${BASE_PATH}/query`;

/**
 * Database Management APIs
 */

/**
 * Get database list
 * @returns Promise with database list
 */
export const getDatabaseList = () => {
  return getRequest<Database[]>(`${DATABASE_PATH}/list`);
};

/**
 * Get database details
 * @param dbId Database ID
 * @returns Promise with database details
 */
export const getDatabaseDetail = (dbId: string) => {
  return getRequest<Database>(`${DATABASE_PATH}/detail/${dbId}`);
};

/**
 * Test database connection
 * @param data Database connection parameters
 * @returns Promise with test result
 */
export const testConnection = (data: Partial<Database>) => {
  return postRequest<{ success: boolean; message: string }>(
    `${DATABASE_PATH}/test`,
    data
  );
};

/**
 * Create a new database connection
 * @param data Database creation parameters
 * @returns Promise with created database
 */
export const createDatabase = (data: Partial<Database>) => {
  return postRequest<Database>(`${DATABASE_PATH}/create`, data);
};

/**
 * Update database connection
 * @param dbId Database ID
 * @param data Database update parameters
 * @returns Promise with updated database
 */
export const updateDatabase = (dbId: string, data: Partial<Database>) => {
  return putRequest<Database>(`${DATABASE_PATH}/update/${dbId}`, data);
};

/**
 * Delete database connection
 * @param dbId Database ID
 * @returns Promise with deletion status
 */
export const deleteDatabase = (dbId: string) => {
  return deleteRequest<{ success: boolean }>(
    `${DATABASE_PATH}/delete/${dbId}`
  );
};

/**
 * Table Management APIs
 */

/**
 * Get table list
 * @param dbId Optional database ID to filter tables
 * @returns Promise with table list
 */
export const getTableList = (dbId?: string) => {
  return getRequest<Table[]>(`${TABLE_PATH}/list`, { dbId });
};

/**
 * Get table details
 * @param tableId Table ID
 * @returns Promise with table details
 */
export const getTableDetail = (tableId: string) => {
  return getRequest<Table>(`${TABLE_PATH}/detail/${tableId}`);
};

/**
 * Get table columns
 * @param tableId Table ID
 * @returns Promise with column list
 */
export const getTableColumns = (tableId: string) => {
  return getRequest<Column[]>(`${TABLE_PATH}/columns/${tableId}`);
};

/**
 * Get table data preview
 * @param tableId Table ID
 * @param limit Number of rows to preview
 * @returns Promise with preview data
 */
export const getTablePreview = (tableId: string, limit: number = 100) => {
  return getRequest<{
    columns: Column[];
    rows: any[];
  }>(`${TABLE_PATH}/preview/${tableId}`, { limit });
};

/**
 * Get paginated table data with filters and sorting
 * @param tableId Table ID
 * @param page Page number
 * @param pageSize Number of rows per page
 * @param filters Optional filter conditions
 * @param sorts Optional sort conditions
 * @returns Promise with paginated data
 */
export const getTableData = (
  tableId: string,
  page: number = 1,
  pageSize: number = 20,
  filters?: any,
  sorts?: any
) => {
  return postRequest<{
    columns: Column[];
    rows: any[];
    total: number;
    page: number;
    pageSize: number;
  }>(`${TABLE_PATH}/data/${tableId}`, { page, pageSize, filters, sorts });
};

/**
 * SQL Query APIs
 */

/**
 * Execute SQL query
 * @param dbId Database ID
 * @param sql SQL query string
 * @returns Promise with query results
 */
export const executeQuery = (dbId: string, sql: string) => {
  return postRequest<{
    columns: Column[];
    rows: any[];
    rowsAffected?: number;
    executionTime: number;
  }>(`${QUERY_PATH}/execute`, { dbId, sql });
};

/**
 * Get query execution history
 * @returns Promise with query history
 */
export const getQueryHistory = () => {
  return getRequest<{
    id: string;
    dbId: string;
    sql: string;
    executionTime: number;
    timestamp: string;
    status: string;
  }[]>(`${QUERY_PATH}/history`);
};

/**
 * Save a query for future use
 * @param name Query name
 * @param dbId Database ID
 * @param sql SQL query string
 * @returns Promise with saved query details
 */
export const saveQuery = (name: string, dbId: string, sql: string) => {
  return postRequest<{
    id: string;
    name: string;
    dbId: string;
    sql: string;
  }>(`${QUERY_PATH}/save`, { name, dbId, sql });
};

/**
 * Get saved queries
 * @returns Promise with list of saved queries
 */
export const getSavedQueries = () => {
  return getRequest<{
    id: string;
    name: string;
    dbId: string;
    sql: string;
    createTime: string;
    updateTime: string;
  }[]>(`${QUERY_PATH}/saved`);
}; 