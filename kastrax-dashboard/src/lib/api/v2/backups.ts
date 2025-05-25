/**
 * Backups API v2
 * 
 * 备份管理相关的API
 */
import { getRequest, postRequest, putRequest, deleteRequest, downloadRequest } from '../utils';
import { 
  Backup, 
  BackupQuery, 
  BackupCreateParams 
} from '../backups';

/**
 * API Endpoints
 */
const BASE_PATH = '/api/v2';
const BACKUPS_PATH = `${BASE_PATH}/backups`;

/**
 * Get backup list with pagination and filters
 * @param params Query parameters
 * @returns Promise with paginated backup list
 */
export const getBackupList = (params: BackupQuery = {}) => {
  return getRequest<{
    list: Backup[];
    total: number;
    page: number;
    size: number;
  }>(BACKUPS_PATH, params);
};

/**
 * Get backup by ID
 * @param backupId Backup ID
 * @returns Promise with backup details
 */
export const getBackupById = (backupId: string) => {
  return getRequest<Backup>(`${BACKUPS_PATH}/${backupId}`);
};

/**
 * Create a new backup
 * @param data Backup creation parameters
 * @returns Promise with created backup ID
 */
export const createBackup = (data: BackupCreateParams) => {
  return postRequest<{ backupId: string }>(BACKUPS_PATH, data);
};

/**
 * Delete a backup
 * @param backupId Backup ID
 * @returns Promise with deletion status
 */
export const deleteBackup = (backupId: string) => {
  return deleteRequest<{ success: boolean }>(`${BACKUPS_PATH}/${backupId}`);
};

/**
 * Restore a backup
 * @param backupId Backup ID
 * @returns Promise with restoration status
 */
export const restoreBackup = (backupId: string) => {
  return postRequest<{ success: boolean }>(
    `${BACKUPS_PATH}/${backupId}/restore`, 
    {}
  );
};

/**
 * Download a backup
 * @param backupId Backup ID
 * @returns Promise with backup file data
 */
export const downloadBackup = (backupId: string) => {
  return downloadRequest(`${BACKUPS_PATH}/${backupId}/download`);
};

/**
 * Get available backup types
 * @returns Promise with list of backup types
 */
export const getBackupTypes = () => {
  return getRequest<string[]>(`${BACKUPS_PATH}/types`);
};

/**
 * Check backup progress
 * @param backupId Backup ID
 * @returns Promise with backup status and progress
 */
export const checkBackupProgress = (backupId: string) => {
  return getRequest<{ 
    status: number; 
    progress: number;
    message: string;
  }>(`${BACKUPS_PATH}/${backupId}/progress`);
}; 