import client from './client';

export interface SystemSetting {
  settingId: string;
  category: 'general' | 'security' | 'backup' | 'notification' | 'display' | 'integration';
  key: string;
  value: string;
  description?: string;
  createTime: string;
  updateTime: string;
}

export interface SettingUpdatePayload {
  settingId: string;
  value: string;
  description?: string;
}

export interface SettingCreatePayload {
  category: 'general' | 'security' | 'backup' | 'notification' | 'display' | 'integration';
  key: string;
  value: string;
  description?: string;
}

// 获取所有系统设置
export async function getSystemSettings() {
  return client.get('/settings');
}

// 获取特定类别的系统设置
export async function getSettingsByCategory(category: string) {
  return client.get(`/settings/category/${category}`);
}

// 获取单个设置项
export async function getSetting(settingId: string) {
  return client.get(`/settings/${settingId}`);
}

// 创建设置项
export async function createSetting(data: SettingCreatePayload) {
  return client.post('/settings', data);
}

// 更新设置项
export async function updateSetting(data: SettingUpdatePayload) {
  return client.put(`/settings/${data.settingId}`, data);
}

// 删除设置项
export async function deleteSetting(settingId: string) {
  return client.delete(`/settings/${settingId}`);
}

// 批量更新设置
export async function batchUpdateSettings(data: SettingUpdatePayload[]) {
  return client.put('/settings/batch', { settings: data });
}

// Available setting groups
export const SETTING_GROUPS = [
  { id: 'general', name: '通用设置', icon: 'Settings' },
  { id: 'security', name: '安全设置', icon: 'Shield' },
  { id: 'backup', name: '备份设置', icon: 'Database' },
  { id: 'notification', name: '通知设置', icon: 'Bell' },
  { id: 'appearance', name: '显示设置', icon: 'Palette' },
  { id: 'integration', name: '集成设置', icon: 'Link' }
]; 