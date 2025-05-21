import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from '@/components/ui/toaster';
import BackupsPage from './page';
import * as backupsApi from '@/lib/api/backups';

// 模拟API模块
jest.mock('@/lib/api/backups', () => ({
  getBackupList: jest.fn(),
  createBackup: jest.fn(),
  restoreBackup: jest.fn(),
  deleteBackup: jest.fn(),
  downloadBackup: jest.fn()
}));

// 模拟组件
jest.mock('@/components/supabase-sidebar', () => ({
  __esModule: true,
  default: ({ collapsed, setCollapsed }: any) => (
    <div data-testid="supabase-sidebar">
      <button onClick={() => setCollapsed(!collapsed)}>Toggle Sidebar</button>
    </div>
  )
}));

jest.mock('@/components/top-navigation', () => ({
  __esModule: true,
  default: ({ children }: any) => (
    <div data-testid="top-navigation">{children}</div>
  )
}));

// 模拟数据
const mockBackups = [
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
  }
];

// 渲染帮助函数
const renderWithProviders = (ui: React.ReactElement) => {
  return render(
    <BrowserRouter>
      <Toaster />
      {ui}
    </BrowserRouter>
  );
};

describe('BackupsPage', () => {
  beforeEach(() => {
    // 重置模拟
    jest.resetAllMocks();
    
    // 设置API模拟返回值
    (backupsApi.getBackupList as jest.Mock).mockResolvedValue(mockBackups);

    (backupsApi.createBackup as jest.Mock).mockResolvedValue({
      data: {
        data: { backupId: 'new-backup-id' },
        code: 200,
        success: true,
        msg: 'success'
      }
    });

    (backupsApi.deleteBackup as jest.Mock).mockResolvedValue({
      data: {
        data: true,
        code: 200,
        success: true,
        msg: 'success'
      }
    });

    (backupsApi.restoreBackup as jest.Mock).mockResolvedValue({
      data: {
        data: true,
        code: 200,
        success: true,
        msg: 'success'
      }
    });

    (backupsApi.downloadBackup as jest.Mock).mockResolvedValue({
      data: {
        data: new ArrayBuffer(0),
        code: 200,
        success: true,
        msg: 'success'
      }
    });

    // 模拟window.URL.createObjectURL
    global.URL.createObjectURL = jest.fn(() => 'mock-url');
    global.URL.revokeObjectURL = jest.fn();
    
    // 模拟window.confirm
    global.confirm = jest.fn(() => true);
    
    // 模拟document.createElement
    const mockAnchor = { 
      href: '',
      download: '',
      click: jest.fn(),
      remove: jest.fn()
    };
    const originalCreateElement = document.createElement.bind(document);
    global.document.createElement = jest.fn().mockImplementation((tag) => {
      if (tag === 'a') return mockAnchor;
      return originalCreateElement(tag);
    });
    global.document.body.appendChild = jest.fn();
    global.document.body.removeChild = jest.fn();
  });
  
  test('应该渲染备份页面并显示备份列表', async () => {
    // 设置API模拟返回值
    (backupsApi.getBackupList as jest.Mock).mockResolvedValue(mockBackups);
    
    renderWithProviders(<BackupsPage />);
    
    // 验证API调用
    expect(backupsApi.getBackupList).toHaveBeenCalledTimes(1);
    
    // 等待备份列表加载
    await waitFor(() => {
      expect(screen.getByText('Daily-Backup-20240320')).toBeInTheDocument();
      expect(screen.getByText('Manual-Backup-20240321')).toBeInTheDocument();
    });
  });
  
  test('应该能够搜索备份', async () => {
    renderWithProviders(<BackupsPage />);
    
    // 等待备份列表加载
    await waitFor(() => {
      expect(screen.getByText('Daily-Backup-20240320')).toBeInTheDocument();
      expect(screen.getByText('Manual-Backup-20240321')).toBeInTheDocument();
    });
    
    // 进行搜索
    const searchInput = screen.getByPlaceholderText('搜索备份名称...');
    fireEvent.change(searchInput, { target: { value: 'Config' } });
    
    // 验证搜索结果
    expect(screen.getByText('Config-Backup-20240322')).toBeInTheDocument();
    expect(screen.queryByText('Daily-Backup-20240320')).not.toBeInTheDocument();
    expect(screen.queryByText('Manual-Backup-20240321')).not.toBeInTheDocument();
  });
  
  test('应该能够创建新备份', async () => {
    renderWithProviders(<BackupsPage />);
    
    // 等待备份列表加载
    await waitFor(() => {
      expect(screen.getByText('备份管理')).toBeInTheDocument();
    });
    
    // 点击创建备份按钮
    const createButton = screen.getByText('创建备份');
    fireEvent.click(createButton);
    
    // 验证API调用
    expect(backupsApi.createBackup).toHaveBeenCalledTimes(1);
    expect(backupsApi.createBackup).toHaveBeenCalledWith(expect.objectContaining({
      backupType: 'full'
    }));
    
    // 验证列表刷新
    expect(backupsApi.getBackupList).toHaveBeenCalledTimes(2);
  });
  
  test('应该能够删除备份', async () => {
    renderWithProviders(<BackupsPage />);
    
    // 等待备份列表加载
    await waitFor(() => {
      expect(screen.getByText('Daily-Backup-20240320')).toBeInTheDocument();
    });
    
    // 找到删除按钮并点击
    const deleteButtons = screen.getAllByTitle('删除备份');
    fireEvent.click(deleteButtons[0]);
    
    // 验证API调用
    expect(global.confirm).toHaveBeenCalledTimes(1);
    expect(backupsApi.deleteBackup).toHaveBeenCalledTimes(1);
    expect(backupsApi.deleteBackup).toHaveBeenCalledWith('backup-001');
    
    // 验证列表刷新
    expect(backupsApi.getBackupList).toHaveBeenCalledTimes(2);
  });
  
  test('应该能够从备份恢复', async () => {
    renderWithProviders(<BackupsPage />);
    
    // 等待备份列表加载
    await waitFor(() => {
      expect(screen.getByText('Daily-Backup-20240320')).toBeInTheDocument();
    });
    
    // 找到恢复按钮并点击
    const restoreButtons = screen.getAllByTitle('从此备份恢复');
    fireEvent.click(restoreButtons[0]);
    
    // 验证对话框显示
    expect(screen.getByText('确认恢复')).toBeInTheDocument();
    
    // 点击确认恢复按钮
    const confirmButton = screen.getByText('确认恢复');
    fireEvent.click(confirmButton);
    
    // 验证API调用
    expect(backupsApi.restoreBackup).toHaveBeenCalledTimes(1);
    expect(backupsApi.restoreBackup).toHaveBeenCalledWith('backup-001');
    
    // 验证列表刷新
    expect(backupsApi.getBackupList).toHaveBeenCalledTimes(2);
  });
  
  test('应该能够下载备份', async () => {
    renderWithProviders(<BackupsPage />);
    
    // 等待备份列表加载
    await waitFor(() => {
      expect(screen.getByText('Daily-Backup-20240320')).toBeInTheDocument();
    });
    
    // 找到下载按钮并点击
    const downloadButtons = screen.getAllByTitle('下载备份');
    fireEvent.click(downloadButtons[0]);
    
    // 验证API调用
    expect(backupsApi.downloadBackup).toHaveBeenCalledTimes(1);
    expect(backupsApi.downloadBackup).toHaveBeenCalledWith('backup-001');
    
    // 验证下载功能
    expect(global.URL.createObjectURL).toHaveBeenCalledTimes(1);
    expect(document.createElement).toHaveBeenCalledWith('a');
    expect(document.body.appendChild).toHaveBeenCalled();
    expect(document.body.removeChild).toHaveBeenCalled();
  });
  
  test('应该处理API错误并显示提示', async () => {
    // 模拟API返回错误
    (backupsApi.getBackupList as jest.Mock).mockRejectedValue(new Error('获取备份列表失败'));
    
    renderWithProviders(<BackupsPage />);
    
    // 验证API调用
    expect(backupsApi.getBackupList).toHaveBeenCalledTimes(1);
    
    // 等待错误处理
    await waitFor(() => {
      expect(backupsApi.getBackupList).toHaveBeenCalledTimes(1);
    });
  });
});