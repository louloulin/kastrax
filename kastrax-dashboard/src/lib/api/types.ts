/**
 * 标准API响应格式
 */
export interface ApiResponse<T = any> {
  code: number;
  success: boolean;
  msg: string;
  data: T;
}

/**
 * 分页响应格式
 */
export interface PaginatedResponse<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

/**
 * API错误格式
 */
export interface ApiError {
  code: number;
  message: string;
  details?: any;
}

/**
 * 通用查询参数
 */
export interface BaseQueryParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

/**
 * API响应状态码
 */
export enum ApiStatusCode {
  SUCCESS = 200,
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
  NOT_FOUND = 404,
  INTERNAL_ERROR = 500,
}

/**
 * 任务执行状态
 */
export enum TaskStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

/**
 * API端点基础路径
 */
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/api/v1/auth/login',
    LOGOUT: '/api/v1/auth/logout',
    REGISTER: '/api/v1/auth/register',
    REFRESH_TOKEN: '/api/v1/auth/refresh-token',
    USER_INFO: '/api/v1/auth/info',
  },
  RULES: {
    BASE: '/api/v1/rules',
    EXECUTE: (id: string) => `/api/v1/rules/${id}/execute`,
    HISTORY: (id: string) => `/api/v1/rules/${id}/history`,
    RESTORE: (id: string, version: number) => `/api/v1/rules/${id}/restore/${version}`,
  },
  SCRIPTS: {
    BASE: '/api/v1/scripts',
    EXECUTE: (id: string) => `/api/v1/scripts/${id}/execute`,
    DEBUG: (id: string) => `/api/v1/scripts/${id}/debug`,
    DEPENDENCIES: (id: string) => `/api/v1/scripts/${id}/dependencies`,
    INSTALL_DEPS: (id: string) => `/api/v1/scripts/${id}/dependencies/install`,
    TEST: (id: string) => `/api/v1/scripts/${id}/test`,
  },
  WORKFLOWS: {
    BASE: '/api/v1/workflows',
    EXECUTE: (id: string) => `/api/v1/workflows/${id}/execute`,
    HISTORY: (id: string) => `/api/v1/workflows/${id}/history`,
    LOGS: (id: string) => `/api/v1/workflows/${id}/logs`,
  },
}; 