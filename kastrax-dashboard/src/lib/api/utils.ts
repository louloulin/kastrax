import client from './client';
import type { ApiResponse } from './types';

/**
 * 标准化的 GET 请求
 * @param url API 路径
 * @param params 查询参数
 * @returns Promise with API response
 */
export async function getRequest<T = any>(url: string, params?: any): Promise<ApiResponse<T>> {
  const response = await client.get<ApiResponse<T>>(url, { params });
  return response.data;
}

/**
 * 标准化的 POST 请求
 * @param url API 路径
 * @param data 请求体数据
 * @returns Promise with API response
 */
export async function postRequest<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
  const response = await client.post<ApiResponse<T>>(url, data);
  return response.data;
}

/**
 * 标准化的 PUT 请求
 * @param url API 路径
 * @param data 请求体数据
 * @returns Promise with API response
 */
export async function putRequest<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
  const response = await client.put<ApiResponse<T>>(url, data);
  return response.data;
}

/**
 * 标准化的 DELETE 请求
 * @param url API 路径
 * @param params 查询参数
 * @returns Promise with API response
 */
export async function deleteRequest<T = any>(url: string, params?: any): Promise<ApiResponse<T>> {
  const response = await client.delete<ApiResponse<T>>(url, { params });
  return response.data;
}

/**
 * 下载文件请求
 * @param url API 路径
 * @param params 查询参数
 * @returns Promise with blob response
 */
export function downloadRequest(url: string, params?: any) {
  return client.get(url, { 
    params,
    responseType: 'blob'
  });
}

/**
 * 上传文件请求
 * @param url API 路径
 * @param formData 包含文件的 FormData 对象
 * @returns Promise with API response
 */
export function uploadRequest<T = any>(url: string, formData: FormData) {
  return client.post<T>(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
} 