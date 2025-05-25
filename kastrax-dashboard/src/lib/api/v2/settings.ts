import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';
import type { ApiResponse } from '../types';

export type Category = 'general' | 'security' | 'backup' | 'notification' | 'display' | 'integration';

export interface SystemSetting {
  settingId: string;
  category: Category;
  key: string;
  value: string;
  description?: string;
  createTime: string;
  updateTime: string;
}

export interface SettingUpdatePayload {
  value: string;
  description?: string;
}

export interface SettingCreatePayload {
  category: Category;
  key: string;
  value: string;
  description?: string;
}

export interface SettingsResponse {
  settings: SystemSetting[];
}

export interface SingleSettingResponse {
  setting: SystemSetting;
}

// Get all system settings
export async function getSystemSettings() {
  return getRequest<SettingsResponse>('/v2/settings');
}

// Get settings by category
export async function getSettingsByCategory(category: Category) {
  return getRequest<SettingsResponse>(`/v2/settings/category/${category}`);
}

// Get single setting
export async function getSetting(settingId: string) {
  return getRequest<SingleSettingResponse>(`/v2/settings/${settingId}`);
}

// Create setting
export async function createSetting(data: SettingCreatePayload) {
  return postRequest<SingleSettingResponse>('/v2/settings', data);
}

// Update setting
export async function updateSetting(settingId: string, data: SettingUpdatePayload) {
  return putRequest<SingleSettingResponse>(`/v2/settings/${settingId}`, data);
}

// Delete setting
export async function deleteSetting(settingId: string) {
  return deleteRequest<void>(`/v2/settings/${settingId}`);
}

// Batch update settings
export interface BatchUpdatePayload {
  settingId: string;
  value: string;
}

export async function batchUpdateSettings(settings: BatchUpdatePayload[]) {
  return putRequest<SettingsResponse>('/v2/settings/batch', { settings });
}

// Available setting groups
export const SETTING_GROUPS = [
  { id: 'general', name: '通用设置', icon: 'Settings' },
  { id: 'security', name: '安全设置', icon: 'Shield' },
  { id: 'backup', name: '备份设置', icon: 'Database' },
  { id: 'notification', name: '通知设置', icon: 'Bell' },
  { id: 'display', name: '显示设置', icon: 'Palette' },
  { id: 'integration', name: '集成设置', icon: 'Link' }
]; 