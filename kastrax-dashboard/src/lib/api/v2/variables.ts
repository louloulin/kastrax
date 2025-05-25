/**
 * Variables API v2
 * 
 * 变量管理相关的API
 */
import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';
import { 
  Variable, 
  VariableQuery, 
  VariableCreateParams, 
  VariableUpdateParams 
} from '../variables';

/**
 * API Endpoints
 */
const BASE_PATH = '/api/v2';
const VARIABLES_PATH = `${BASE_PATH}/variables`;

/**
 * Get variable list with pagination and filters
 * @param params Query parameters
 * @returns Promise with paginated variable list
 */
export const getVariableList = (params: VariableQuery = {}) => {
  return getRequest<{
    list: Variable[];
    total: number;
    page: number;
    size: number;
  }>(VARIABLES_PATH, params);
};

/**
 * Get variable by ID
 * @param variableId Variable ID
 * @returns Promise with variable details
 */
export const getVariableById = (variableId: string) => {
  return getRequest<Variable>(`${VARIABLES_PATH}/${variableId}`);
};

/**
 * Create a new variable
 * @param data Variable creation parameters
 * @returns Promise with created variable ID
 */
export const createVariable = (data: VariableCreateParams) => {
  return postRequest<{ variableId: string }>(VARIABLES_PATH, data);
};

/**
 * Update an existing variable
 * @param variableId Variable ID
 * @param data Variable update parameters
 * @returns Promise with update status
 */
export const updateVariable = (variableId: string, data: Omit<VariableUpdateParams, 'variableId'>) => {
  return putRequest<{ success: boolean }>(`${VARIABLES_PATH}/${variableId}`, data);
};

/**
 * Delete a variable
 * @param variableId Variable ID
 * @returns Promise with deletion status
 */
export const deleteVariable = (variableId: string) => {
  return deleteRequest<{ success: boolean }>(`${VARIABLES_PATH}/${variableId}`);
};

/**
 * Get variables by prefix
 * @param prefix Variable name prefix
 * @returns Promise with matching variables
 */
export const getVariablesByPrefix = (prefix: string) => {
  return getRequest<Variable[]>(`${VARIABLES_PATH}/prefix/${prefix}`);
};

/**
 * Bulk create or update variables
 * @param variables List of variables to create or update
 * @returns Promise with operation results
 */
export const bulkUpsertVariables = (variables: VariableCreateParams[]) => {
  return postRequest<{ success: boolean, inserted: number, updated: number }>(
    `${VARIABLES_PATH}/bulk`, 
    { variables }
  );
};

/**
 * Export variables as JSON
 * @param variableIds Optional list of variable IDs to export (all if not specified)
 * @returns Promise with variables JSON data
 */
export const exportVariables = (variableIds?: string[]) => {
  return postRequest<{ variables: Variable[] }>(
    `${VARIABLES_PATH}/export`,
    { variableIds }
  );
};

/**
 * Import variables from JSON
 * @param variables List of variables to import
 * @param overwrite Whether to overwrite existing variables
 * @returns Promise with import results
 */
export const importVariables = (variables: Variable[], overwrite: boolean = false) => {
  return postRequest<{ success: boolean, imported: number, skipped: number }>(
    `${VARIABLES_PATH}/import`,
    { variables, overwrite }
  );
}; 