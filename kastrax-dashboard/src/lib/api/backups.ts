import client, { ApiResponse } from './client';
import { AxiosResponse } from 'axios';

export interface Backup {
  backupId: string;
  backupName: string;
  backupSize: number; // in bytes
  backupType: string;
  createTime: string;
  status: number; // 1: 完成, 0: 进行中, -1: 失败
  description?: string;
}

export interface BackupQuery {
  backupName?: string;
  backupType?: string;
  page?: number;
  size?: number;
}

export interface BackupCreateParams {
  backupName: string;
  backupType: string;
  description?: string;
  modules?: string[]; // Optional: specify modules to back up
}

// Mock 数据支持
// Use a function to determine if mock is enabled instead of directly using import.meta.env
function isMockEnabled() {
  try {
    // Check if we're in a browser environment
    return typeof window !== 'undefined' && 
           process.env.VITE_USE_MOCK_DATA === 'true';
  } catch (e) {
    // In test environment, return false by default
    return false;
  }
}

// Mock数据
const mockBackups: Backup[] = [
  {
    backupId: 'backup-001',
    backupName: 'Daily-Backup-20240320',
    backupSize: 15728640, // 15MB
    backupType: 'full',
    createTime: '2024-03-20T00:00:00Z',
    status: 1,
    description: '每日自动备份'
  },
  {
    backupId: 'backup-002',
    backupName: 'Manual-Backup-20240321',
    backupSize: 20971520, // 20MB
    backupType: 'full',
    createTime: '2024-03-21T15:30:00Z',
    status: 1,
    description: '手动创建的完整备份'
  },
  {
    backupId: 'backup-003',
    backupName: 'Config-Backup-20240322',
    backupSize: 5242880, // 5MB
    backupType: 'config',
    createTime: '2024-03-22T10:15:00Z',
    status: 1,
    description: '仅配置备份'
  },
  {
    backupId: 'backup-004',
    backupName: 'Data-Backup-20240322',
    backupSize: 52428800, // 50MB
    backupType: 'data',
    createTime: '2024-03-22T12:45:00Z',
    status: 1,
    description: '仅数据备份'
  },
  {
    backupId: 'backup-005',
    backupName: 'Backup-In-Progress',
    backupSize: 0,
    backupType: 'full',
    createTime: '2024-03-23T08:30:00Z',
    status: 0,
    description: '正在进行中的备份'
  }
];

// 创建模拟响应
const createMockResponse = <T>(data: T): AxiosResponse<ApiResponse<T>> => {
  return {
    data: {
      code: 200,
      success: true,
      msg: 'success',
      data
    },
    status: 200,
    statusText: 'OK',
    headers: {},
    config: { headers: {} } as any
  };
};

// 生成一个新的ID
const generateId = () => {
  return `backup-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
};

// List backups with pagination
export function getBackupList(params: BackupQuery = {}): Promise<AxiosResponse<ApiResponse<Backup[]>>> {
  if (isMockEnabled()) {
    console.log('[MOCK] 获取备份列表，参数:', params);
    
    let filteredBackups = [...mockBackups];
    
    // 过滤
    if (params.backupName) {
      filteredBackups = filteredBackups.filter(backup => 
        backup.backupName.toLowerCase().includes(params.backupName!.toLowerCase())
      );
    }
    
    if (params.backupType) {
      filteredBackups = filteredBackups.filter(backup => 
        backup.backupType === params.backupType
      );
    }
    
    // 分页
    const page = params.page || 1;
    const size = params.size || 10;
    const start = (page - 1) * size;
    const end = start + size;
    const pagedBackups = filteredBackups.slice(start, end);
    
    return Promise.resolve(createMockResponse(pagedBackups));
  }
  
  return client.get('/backup/list', { params });
}

// Get backup by ID
export function getBackupById(backupId: string): Promise<AxiosResponse<ApiResponse<Backup>>> {
  if (isMockEnabled()) {
    console.log('[MOCK] 获取备份详情，ID:', backupId);
    
    const backup = mockBackups.find(b => b.backupId === backupId);
    
    if (backup) {
      return Promise.resolve(createMockResponse(backup));
    } else {
      return Promise.reject({
        response: {
          data: {
            code: 404,
            success: false,
            msg: '备份不存在',
            data: null
          }
        }
      });
    }
  }
  
  return client.get(`/backup/${backupId}`);
}

// Create a new backup
export function createBackup(params: BackupCreateParams): Promise<AxiosResponse<ApiResponse<{backupId: string}>>> {
  if (isMockEnabled()) {
    console.log('[MOCK] 创建备份，参数:', params);
    
    // 生成一个新的备份
    const newBackupId = generateId();
    const newBackup: Backup = {
      backupId: newBackupId,
      backupName: params.backupName,
      backupSize: 0, // 初始大小为0
      backupType: params.backupType,
      createTime: new Date().toISOString(),
      status: 0, // 刚创建的备份状态为"进行中"
      description: params.description
    };
    
    // 将新备份添加到mock数据中
    mockBackups.push(newBackup);
    
    // 模拟备份过程
    setTimeout(() => {
      const backupIndex = mockBackups.findIndex(b => b.backupId === newBackupId);
      if (backupIndex !== -1) {
        // 更新备份状态为"完成"和生成随机大小
        mockBackups[backupIndex].status = 1;
        mockBackups[backupIndex].backupSize = Math.floor(Math.random() * 50 * 1024 * 1024) + 5 * 1024 * 1024; // 5-55MB
      }
    }, 3000);
    
    return Promise.resolve(createMockResponse({ backupId: newBackupId }));
  }
  
  return client.post('/backup/create', params);
}

// Delete a backup
export function deleteBackup(backupId: string): Promise<AxiosResponse<ApiResponse<boolean>>> {
  if (isMockEnabled()) {
    console.log('[MOCK] 删除备份，ID:', backupId);
    
    const backupIndex = mockBackups.findIndex(b => b.backupId === backupId);
    
    if (backupIndex !== -1) {
      mockBackups.splice(backupIndex, 1);
      return Promise.resolve(createMockResponse(true));
    } else {
      return Promise.reject({
        response: {
          data: {
            code: 404,
            success: false,
            msg: '备份不存在',
            data: null
          }
        }
      });
    }
  }
  
  return client.post('/backup/delete', { backupId });
}

// Restore a backup
export function restoreBackup(backupId: string): Promise<AxiosResponse<ApiResponse<boolean>>> {
  if (isMockEnabled()) {
    console.log('[MOCK] 恢复备份，ID:', backupId);
    
    const backup = mockBackups.find(b => b.backupId === backupId);
    
    if (backup) {
      if (backup.status !== 1) {
        return Promise.reject({
          response: {
            data: {
              code: 400,
              success: false,
              msg: '只能从完成状态的备份中恢复',
              data: null
            }
          }
        });
      }
      
      // 模拟恢复成功
      return new Promise(resolve => {
        setTimeout(() => {
          resolve(createMockResponse(true));
        }, 2000);
      });
    } else {
      return Promise.reject({
        response: {
          data: {
            code: 404,
            success: false,
            msg: '备份不存在',
            data: null
          }
        }
      });
    }
  }
  
  return client.post('/backup/restore', { backupId });
}

// Download a backup
export function downloadBackup(backupId: string): Promise<AxiosResponse<ApiResponse<ArrayBuffer>>> {
  if (isMockEnabled()) {
    console.log('[MOCK] 下载备份，ID:', backupId);
    
    const backup = mockBackups.find(b => b.backupId === backupId);
    
    if (backup) {
      if (backup.status !== 1) {
        return Promise.reject({
          response: {
            data: {
              code: 400,
              success: false,
              msg: '只能下载完成状态的备份',
              data: null
            }
          }
        });
      }
      
      // 模拟下载成功 - 返回一个空的ArrayBuffer
      // 在实际实现中，这里会返回备份文件的二进制数据
      return Promise.resolve(createMockResponse(new ArrayBuffer(0)));
    } else {
      return Promise.reject({
        response: {
          data: {
            code: 404,
            success: false,
            msg: '备份不存在',
            data: null
          }
        }
      });
    }
  }
  
  return client.get(`/backup/download/${backupId}`, { responseType: 'arraybuffer' });
}

// Get supported backup types
export function getBackupTypes(): Promise<AxiosResponse<ApiResponse<string[]>>> {
  if (isMockEnabled()) {
    console.log('[MOCK] 获取支持的备份类型');
    
    return Promise.resolve(createMockResponse(['full', 'data', 'config']));
  }
  
  return client.get('/backup/types');
}