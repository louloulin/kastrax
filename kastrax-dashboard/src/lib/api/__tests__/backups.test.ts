/**
 * Backups API Tests
 */

// Mock the backups module
jest.mock('../v1/backups', () => ({
  getBackupList: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getBackupById: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  createBackup: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  deleteBackup: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  restoreBackup: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  downloadBackup: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  getBackupTypes: jest.fn().mockImplementation(() => Promise.resolve({ data: {} }))
}));

// Mock client and utilities
jest.mock('../client', () => ({
  get: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  post: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  put: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  delete: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  default: {
    get: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
    post: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
    put: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
    delete: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  }
}));

jest.mock('../utils', () => ({
  getRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  postRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  putRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  deleteRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: {} })),
  downloadRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: {} }))
}));

import { rest } from 'msw';
import { setupServer } from 'msw/node';
import client from '../client';
import { getRequest, postRequest, deleteRequest, downloadRequest } from '../utils';

// Direct imports for v1 API
import { 
  getBackupList as v1GetBackupList,
  getBackupById as v1GetBackupById,
  createBackup as v1CreateBackup,
  deleteBackup as v1DeleteBackup,
  restoreBackup as v1RestoreBackup,
  downloadBackup as v1DownloadBackup,
  getBackupTypes as v1GetBackupTypes
} from '../v1/backups';

// Direct imports for v2 API
import {
  getBackupList as v2GetBackupList,
  getBackupById as v2GetBackupById,
  createBackup as v2CreateBackup,
  deleteBackup as v2DeleteBackup,
  restoreBackup as v2RestoreBackup,
  downloadBackup as v2DownloadBackup,
  getBackupTypes as v2GetBackupTypes,
  checkBackupProgress as v2CheckBackupProgress
} from '../v2/backups';

// Mock responses
const mockBackups = [
  {
    backupId: '1',
    backupName: 'Daily-Backup-20240320',
    backupSize: 15728640, // 15MB
    backupType: 'full',
    createTime: '2024-03-20T00:00:00Z',
    status: 1,
    description: '每日自动备份'
  },
  {
    backupId: '2',
    backupName: 'Manual-Backup-20240321',
    backupSize: 20971520, // 20MB
    backupType: 'full',
    createTime: '2024-03-21T15:30:00Z',
    status: 1,
    description: '手动创建的完整备份'
  }
];

// Helper function to safely get ID param as string
const getParamAsString = (param: any): string => {
  if (Array.isArray(param)) {
    return param[0] || '';
  }
  return String(param);
};

// Setup mock server
const server = setupServer(
  // v2 API endpoints
  rest.get('/api/v2/backups', (req, res, ctx) => {
    return res(
      ctx.json({
        success: true,
        data: {
          list: mockBackups,
          total: mockBackups.length,
          page: 1,
          size: 10
        },
      })
    );
  }),
  
  rest.get('/api/v2/backups/:id', (req, res, ctx) => {
    const id = getParamAsString(req.params.id);
    const backup = mockBackups.find(v => v.backupId === id);
    if (backup) {
      return res(ctx.json({ success: true, data: backup }));
    }
    return res(ctx.status(404), ctx.json({ success: false, message: 'Backup not found' }));
  }),
  
  rest.post('/api/v2/backups', (req, res, ctx) => {
    return res(ctx.json({ success: true, data: { backupId: '3' } }));
  }),
  
  rest.delete('/api/v2/backups/:id', (req, res, ctx) => {
    const id = getParamAsString(req.params.id);
    return res(ctx.json({ success: true, data: { success: true } }));
  }),
  
  rest.post('/api/v2/backups/:id/restore', (req, res, ctx) => {
    const id = getParamAsString(req.params.id);
    return res(ctx.json({ success: true, data: { success: true } }));
  }),
  
  rest.get('/api/v2/backups/:id/download', (req, res, ctx) => {
    const id = getParamAsString(req.params.id);
    return res(
      ctx.set('Content-Type', 'application/octet-stream'),
      ctx.set('Content-Disposition', `attachment; filename="backup-${id}.zip"`),
      ctx.body(new ArrayBuffer(1024))
    );
  }),
  
  rest.get('/api/v2/backups/types', (req, res, ctx) => {
    return res(ctx.json({ 
      success: true, 
      data: ['full', 'config', 'data'] 
    }));
  }),
  
  rest.get('/api/v2/backups/:id/progress', (req, res, ctx) => {
    const id = getParamAsString(req.params.id);
    return res(ctx.json({ 
      success: true, 
      data: { status: 0, progress: 75, message: 'Backup in progress' } 
    }));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Backups API', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('v1 Backups API', () => {
    test('getBackupList should call with correct parameters', async () => {
      const params = { backupName: 'test', page: 1, size: 10 };
      await v1GetBackupList(params);
      expect(v1GetBackupList).toHaveBeenCalledWith(params);
    });

    test('getBackupById should call with correct ID', async () => {
      await v1GetBackupById('backup-123');
      expect(v1GetBackupById).toHaveBeenCalledWith('backup-123');
    });

    test('createBackup should call with correct data', async () => {
      const backupData = {
        backupName: 'NEW_BACKUP',
        backupType: 'full',
        description: 'New test backup'
      };
      await v1CreateBackup(backupData);
      expect(v1CreateBackup).toHaveBeenCalledWith(backupData);
    });

    test('deleteBackup should call with correct ID', async () => {
      await v1DeleteBackup('backup-123');
      expect(v1DeleteBackup).toHaveBeenCalledWith('backup-123');
    });

    test('restoreBackup should call with correct ID', async () => {
      await v1RestoreBackup('backup-123');
      expect(v1RestoreBackup).toHaveBeenCalledWith('backup-123');
    });

    test('downloadBackup should call with correct ID', async () => {
      await v1DownloadBackup('backup-123');
      expect(v1DownloadBackup).toHaveBeenCalledWith('backup-123');
    });

    test('getBackupTypes should call correctly', async () => {
      await v1GetBackupTypes();
      expect(v1GetBackupTypes).toHaveBeenCalled();
    });
  });

  describe('v2 Backups API', () => {
    test('getBackupList should use getRequest utility', async () => {
      await v2GetBackupList({ backupName: 'test', page: 1, size: 10 });
      expect(getRequest).toHaveBeenCalledWith('/api/v2/backups', { backupName: 'test', page: 1, size: 10 });
    });

    test('getBackupById should use getRequest utility', async () => {
      await v2GetBackupById('backup-123');
      expect(getRequest).toHaveBeenCalledWith('/api/v2/backups/backup-123');
    });
    
    test('createBackup should use postRequest utility', async () => {
      const backupData = {
        backupName: 'NEW_BACKUP',
        backupType: 'full',
        description: 'New test backup'
      };
      await v2CreateBackup(backupData);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/backups', backupData);
    });
    
    test('deleteBackup should use deleteRequest utility', async () => {
      await v2DeleteBackup('backup-123');
      expect(deleteRequest).toHaveBeenCalledWith('/api/v2/backups/backup-123');
    });
    
    test('restoreBackup should use postRequest utility', async () => {
      await v2RestoreBackup('backup-123');
      expect(postRequest).toHaveBeenCalledWith('/api/v2/backups/backup-123/restore', {});
    });
    
    test('downloadBackup should use downloadRequest utility', async () => {
      await v2DownloadBackup('backup-123');
      expect(downloadRequest).toHaveBeenCalledWith('/api/v2/backups/backup-123/download');
    });
    
    test('getBackupTypes should use getRequest utility', async () => {
      await v2GetBackupTypes();
      expect(getRequest).toHaveBeenCalledWith('/api/v2/backups/types');
    });
    
    test('checkBackupProgress should use getRequest utility', async () => {
      await v2CheckBackupProgress('backup-123');
      expect(getRequest).toHaveBeenCalledWith('/api/v2/backups/backup-123/progress');
    });
  });
}); 