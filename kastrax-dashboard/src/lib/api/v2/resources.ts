/**
 * Resources API v2
 * 
 * 资源管理相关的API
 */
import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';
import { Resource, ResourceCreateParams, ResourceQuery, ResourceUpdateParams, ResourceTestParams } from '../resources';

/**
 * API Endpoints
 */
const BASE_PATH = '/api/v2';
const RESOURCES_PATH = `${BASE_PATH}/resources`;

/**
 * List resources with pagination
 * @param params Query parameters
 * @returns Promise with paginated resources list
 */
export const getResourceList = (params: ResourceQuery = {}) => {
  return getRequest<{
    list: Resource[];
    total: number;
    page: number;
    size: number;
  }>(RESOURCES_PATH, params);
};

/**
 * Get resource by ID
 * @param resourceId Resource ID
 * @returns Promise with resource details
 */
export const getResourceById = (resourceId: string) => {
  return getRequest<Resource>(`${RESOURCES_PATH}/${resourceId}`);
};

/**
 * Create a new resource
 * @param data Resource creation parameters
 * @returns Promise with created resource ID
 */
export const createResource = (data: ResourceCreateParams) => {
  return postRequest<{ resourceId: string }>(RESOURCES_PATH, data);
};

/**
 * Update an existing resource
 * @param resourceId Resource ID
 * @param data Resource update parameters
 * @returns Promise with update status
 */
export const updateResource = (resourceId: string, data: Omit<ResourceUpdateParams, 'resourceId'>) => {
  return putRequest<{ success: boolean }>(`${RESOURCES_PATH}/${resourceId}`, data);
};

/**
 * Delete a resource
 * @param resourceId Resource ID
 * @returns Promise with deletion status
 */
export const deleteResource = (resourceId: string) => {
  return deleteRequest<{ success: boolean }>(`${RESOURCES_PATH}/${resourceId}`);
};

/**
 * Test resource connection
 * @param resourceId Resource ID
 * @returns Promise with test result
 */
export const testResourceConnection = (resourceId: string) => {
  return postRequest<{ success: boolean; message: string }>(
    `${RESOURCES_PATH}/${resourceId}/test`
  );
};

/**
 * Test a resource configuration before saving
 * @param data Resource configuration to test
 * @returns Promise with test result
 */
export const testResourceConfig = (data: ResourceCreateParams) => {
  return postRequest<{ success: boolean; message: string }>(
    `${RESOURCES_PATH}/test-config`,
    data
  );
};

/**
 * Get available resource types
 * @returns Promise with resource types list
 */
export const getResourceTypes = () => {
  return getRequest<string[]>(`${RESOURCES_PATH}/types`);
}; 