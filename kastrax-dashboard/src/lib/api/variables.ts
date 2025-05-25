import client, { ApiResponse } from './client';
import { AxiosResponse } from 'axios';

export interface Variable {
  variableId: string;
  variableName: string;
  variableValue: string;
  description: string;
  createTime: string;
  updateTime: string;
}

export interface VariableQuery {
  variableName?: string;
  page?: number;
  pageSize?: number;
}

export interface VariableCreateParams {
  variableName: string;
  variableValue: string;
  description?: string;
}

export interface VariableUpdateParams {
  variableId: string;
  variableName?: string;
  variableValue?: string;
  description?: string;
}

// Generate a simple unique ID
function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substring(2);
}

// Mock数据支持
// Use a function to determine if mock is enabled instead of directly using import.meta.env
function isMockEnabled() {
  try {
    // Check if we're in a browser environment
    return typeof window !== 'undefined' && 
           (process.env.NODE_ENV === 'development' || !process.env.VITE_API_BASE_URL);
  } catch (e) {
    // In test environment, return false by default
    return false;
  }
}

// Mock data for development
let mockVariables: Variable[] = [
  {
    variableId: "1",
    variableName: "DB_HOST",
    variableValue: "localhost",
    description: "数据库主机地址",
    createTime: "2023-06-15T08:00:00Z",
    updateTime: "2023-06-15T08:00:00Z"
  },
  {
    variableId: "2",
    variableName: "DB_PORT",
    variableValue: "3306",
    description: "数据库端口",
    createTime: "2023-06-15T08:01:00Z",
    updateTime: "2023-06-15T08:01:00Z"
  },
  {
    variableId: "3",
    variableName: "API_KEY",
    variableValue: "sk_test_abcdefghijklmnopqrstuvwxyz",
    description: "API密钥",
    createTime: "2023-06-15T08:02:00Z",
    updateTime: "2023-06-15T08:02:00Z"
  },
  {
    variableId: "4",
    variableName: "DEBUG_MODE",
    variableValue: "true",
    description: "调试模式开关",
    createTime: "2023-06-15T08:03:00Z",
    updateTime: "2023-06-15T08:03:00Z"
  }
];

// Helper function to create mock response
function createMockResponse<T>(data: T): Promise<AxiosResponse<ApiResponse<T>>> {
  return Promise.resolve({
    data: {
      code: 200,
      msg: "success",
      data,
      success: true
    },
    status: 200,
    statusText: "OK",
    headers: {},
    config: {} as any
  });
}

// List variables with pagination
export function getVariableList(params: VariableQuery = {}): Promise<AxiosResponse<ApiResponse<Variable[]>>> {
  if (isMockEnabled()) {
    console.log("[MOCK] Getting variable list with params:", params);
    let result = [...mockVariables];
    
    // Filter by name if provided
    if (params.variableName) {
      result = result.filter(v => 
        v.variableName.toLowerCase().includes(params.variableName!.toLowerCase())
      );
    }
    
    // Simple pagination
    if (params.page && params.pageSize) {
      const start = (params.page - 1) * params.pageSize;
      const end = start + params.pageSize;
      result = result.slice(start, end);
    }
    
    return createMockResponse(result);
  }
  
  return client.post('/variable/list', params);
}

// Get variable by ID
export function getVariableById(variableId: string): Promise<AxiosResponse<ApiResponse<Variable>>> {
  if (isMockEnabled()) {
    console.log("[MOCK] Getting variable by ID:", variableId);
    const variable = mockVariables.find(v => v.variableId === variableId);
    if (variable) {
      return createMockResponse(variable);
    }
    return Promise.reject(new Error("Variable not found"));
  }
  
  return client.get(`/variable/${variableId}`);
}

// Create a new variable
export function createVariable(params: VariableCreateParams): Promise<AxiosResponse<ApiResponse<{variableId: string}>>> {
  if (isMockEnabled()) {
    console.log("[MOCK] Creating variable:", params);
    
    // Check if variable name already exists
    if (mockVariables.some(v => v.variableName === params.variableName)) {
      return Promise.reject(new Error("Variable name already exists"));
    }
    
    const now = new Date().toISOString();
    const newVariable: Variable = {
      variableId: generateId(),
      variableName: params.variableName,
      variableValue: params.variableValue,
      description: params.description || "",
      createTime: now,
      updateTime: now
    };
    
    mockVariables.push(newVariable);
    return createMockResponse({ variableId: newVariable.variableId });
  }
  
  return client.post('/variable/add', params);
}

// Update an existing variable
export function updateVariable(params: VariableUpdateParams): Promise<AxiosResponse<ApiResponse<boolean>>> {
  if (isMockEnabled()) {
    console.log("[MOCK] Updating variable:", params);
    const index = mockVariables.findIndex(v => v.variableId === params.variableId);
    
    if (index === -1) {
      return Promise.reject(new Error("Variable not found"));
    }
    
    // Update variable
    const now = new Date().toISOString();
    mockVariables[index] = {
      ...mockVariables[index],
      variableName: params.variableName || mockVariables[index].variableName,
      variableValue: params.variableValue || mockVariables[index].variableValue,
      description: params.description !== undefined ? params.description : mockVariables[index].description,
      updateTime: now
    };
    
    return createMockResponse(true);
  }
  
  return client.post('/variable/update', params);
}

// Delete a variable
export function deleteVariable(variableId: string): Promise<AxiosResponse<ApiResponse<boolean>>> {
  if (isMockEnabled()) {
    console.log("[MOCK] Deleting variable:", variableId);
    const index = mockVariables.findIndex(v => v.variableId === variableId);
    
    if (index === -1) {
      return Promise.reject(new Error("Variable not found"));
    }
    
    mockVariables.splice(index, 1);
    return createMockResponse(true);
  }
  
  return client.post('/variable/delete', { variableId });
} 