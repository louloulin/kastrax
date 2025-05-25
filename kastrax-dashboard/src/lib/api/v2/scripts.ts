/**
 * Scripts API v2
 * 
 * 脚本管理相关的API
 */
import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';
import { 
  Script, 
  ScriptCreateParams, 
  ScriptUpdateParams, 
  ScriptQuery, 
  ScriptExecuteParams, 
  ScriptExecuteResult, 
  ScriptDebugResult,
  ScriptDependencyParams,
  ScriptLanguage 
} from '../scripts';

/**
 * API Endpoints
 */
const BASE_PATH = '/api/v2';
const SCRIPTS_PATH = `${BASE_PATH}/scripts`;

/**
 * Get script list with pagination and filters
 * @param params Query parameters
 * @returns Promise with paginated script list
 */
export const getScriptList = (params: ScriptQuery = {}) => {
  return getRequest<{
    list: Script[];
    total: number;
    page: number;
    size: number;
  }>(SCRIPTS_PATH, params);
};

/**
 * Get script by ID
 * @param scriptId Script ID
 * @returns Promise with script details
 */
export const getScriptById = (scriptId: string) => {
  return getRequest<Script>(`${SCRIPTS_PATH}/${scriptId}`);
};

/**
 * Create a new script
 * @param data Script creation parameters
 * @returns Promise with created script ID
 */
export const createScript = (data: ScriptCreateParams) => {
  return postRequest<{ scriptId: string }>(SCRIPTS_PATH, data);
};

/**
 * Update an existing script
 * @param scriptId Script ID
 * @param data Script update parameters
 * @returns Promise with update status
 */
export const updateScript = (scriptId: string, data: Omit<ScriptUpdateParams, 'scriptId'>) => {
  return putRequest<{ success: boolean }>(`${SCRIPTS_PATH}/${scriptId}`, data);
};

/**
 * Delete a script
 * @param scriptId Script ID
 * @returns Promise with deletion status
 */
export const deleteScript = (scriptId: string) => {
  return deleteRequest<{ success: boolean }>(`${SCRIPTS_PATH}/${scriptId}`);
};

/**
 * Execute a script
 * @param scriptId Script ID
 * @param params Execution parameters
 * @returns Promise with execution result
 */
export const executeScript = (scriptId: string, params: Omit<ScriptExecuteParams, 'scriptId'> = {}) => {
  return postRequest<ScriptExecuteResult>(
    `${SCRIPTS_PATH}/${scriptId}/execute`, 
    { ...params, scriptId }
  );
};

/**
 * Debug a script
 * @param scriptId Script ID
 * @param params Debug parameters
 * @returns Promise with debug result
 */
export const debugScript = (scriptId: string, params: Omit<ScriptExecuteParams, 'scriptId'> = {}) => {
  return postRequest<ScriptDebugResult>(
    `${SCRIPTS_PATH}/${scriptId}/debug`, 
    { ...params, scriptId }
  );
};

/**
 * Update script dependencies
 * @param scriptId Script ID
 * @param dependencies List of dependencies
 * @returns Promise with update status
 */
export const updateScriptDependencies = (scriptId: string, dependencies: string[]) => {
  return putRequest<{ success: boolean }>(
    `${SCRIPTS_PATH}/${scriptId}/dependencies`, 
    { dependencies }
  );
};

/**
 * Install script dependencies
 * @param scriptId Script ID
 * @returns Promise with installation status
 */
export const installDependencies = (scriptId: string) => {
  return postRequest<{ success: boolean; log: string }>(
    `${SCRIPTS_PATH}/${scriptId}/dependencies/install`
  );
};

/**
 * Run script tests
 * @param scriptId Script ID
 * @param params Test parameters
 * @returns Promise with test result
 */
export const testScript = (scriptId: string, params: Omit<ScriptExecuteParams, 'scriptId'> = {}) => {
  return postRequest<ScriptExecuteResult>(
    `${SCRIPTS_PATH}/${scriptId}/test`, 
    { ...params, scriptId, isTest: true }
  );
};

/**
 * Get available script languages
 * @returns Promise with list of available languages
 */
export const getScriptLanguages = () => {
  return getRequest<string[]>(`${SCRIPTS_PATH}/languages`);
}; 