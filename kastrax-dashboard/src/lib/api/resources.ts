import client, { ApiResponse } from './client';
import { AxiosResponse } from 'axios';

export interface Resource {
  resourceId: string;
  resourceName: string;
  resourceType: string;
  status: number;
  properties: Record<string, any>;
  createTime: string;
  updateTime: string;
}

export interface ResourceQuery {
  resourceName?: string;
  resourceType?: string;
  page?: number;
  pageSize?: number;
}

export interface ResourceCreateParams {
  resourceName: string;
  resourceType: string;
  properties: Record<string, any>;
}

export interface ResourceUpdateParams {
  resourceId: string;
  resourceName?: string;
  properties?: Record<string, any>;
}

export interface ResourceTestParams {
  resourceType: string;
  properties: Record<string, any>;
}

// List resources with pagination
export function getResourceList(params: ResourceQuery = {}): Promise<AxiosResponse<ApiResponse<Resource[]>>> {
  return client.post('/api/resource/list', params);
}

// Get resource by ID
export function getResourceById(resourceId: string): Promise<AxiosResponse<ApiResponse<Resource>>> {
  return client.get(`/api/resource/info?resourceId=${resourceId}`);
}

// Create a new resource
export function createResource(params: ResourceCreateParams): Promise<AxiosResponse<ApiResponse<{resourceId: string}>>> {
  return client.post('/api/resource/add', params);
}

// Update an existing resource
export function updateResource(params: ResourceUpdateParams): Promise<AxiosResponse<ApiResponse<boolean>>> {
  return client.post('/api/resource/update', params);
}

// Delete a resource
export function deleteResource(resourceId: string): Promise<AxiosResponse<ApiResponse<boolean>>> {
  return client.post('/api/resource/remove', { resourceId });
}

// Test resource connection
export function testResource(params: ResourceTestParams): Promise<AxiosResponse<ApiResponse<boolean>>> {
  return client.post('/api/resource/test', params);
}

// Test a resource connection
export function testResourceConnection(resourceId: string): Promise<AxiosResponse<ApiResponse<{success: boolean; message: string}>>> {
  return client.post(`/api/resource/test`, { resourceId });
}

// Test a resource configuration before saving
export function testResourceConfig(params: ResourceCreateParams): Promise<AxiosResponse<ApiResponse<{success: boolean; message: string}>>> {
  return client.post('/api/resource/test', params);
}

// Get resource types
export function getResourceTypes(): Promise<AxiosResponse<ApiResponse<string[]>>> {
  return client.get('/api/resource/types');
}