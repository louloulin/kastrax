import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import SettingsPage from './page';
import * as api from '@/lib/api/settings';

// Mock API functions
jest.mock('@/lib/api/settings', () => ({
  getSystemSettings: jest.fn(),
  batchUpdateSettings: jest.fn(),
}));

// Mock toast
jest.mock('@/components/ui/use-toast', () => ({
  useToast: () => ({
    toast: jest.fn(),
  }),
}));

// Mock SettingsCategory component
jest.mock('@/components/settings/settings-category', () => {
  return function MockSettingsCategory({ 
    category, 
    title, 
    description 
  }: { 
    category: string; 
    title: string; 
    description: string 
  }) {
    return (
      <div data-testid={`settings-category-${category}`}>
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    );
  };
});

// Mock sidebar component
jest.mock('@/components/supabase-sidebar', () => {
  return function MockSupabaseSidebar() {
    return <div data-testid="sidebar" />;
  };
});

// Mock TopNavigation component
jest.mock('@/components/top-navigation', () => {
  return function MockTopNavigation() {
    return <div data-testid="top-navigation" />;
  };
});

describe('设置页面测试', () => {
  const mockSettings = [
    {
      settingId: '1',
      category: 'general',
      key: 'site_name',
      value: 'Dataflare',
      description: '站点名称',
      createTime: '2023-01-01T00:00:00Z',
      updateTime: '2023-01-01T00:00:00Z',
    },
    {
      settingId: '2',
      category: 'security',
      key: 'enable_2fa',
      value: 'true',
      description: '启用两步验证',
      createTime: '2023-01-01T00:00:00Z',
      updateTime: '2023-01-01T00:00:00Z',
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (api.getSystemSettings as jest.Mock).mockResolvedValue({
      status: 'success',
      data: mockSettings,
    });
    (api.batchUpdateSettings as jest.Mock).mockResolvedValue({
      status: 'success',
      data: mockSettings,
    });
    // Mock window.URL.createObjectURL
    global.URL.createObjectURL = jest.fn(() => 'blob:test');
    // Mock document.createElement
    document.createElement = jest.fn().mockImplementation((tag) => {
      if (tag === 'a') {
        return {
          setAttribute: jest.fn(),
          click: jest.fn(),
          remove: jest.fn(),
        };
      }
      return document.createElement(tag);
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('渲染设置页面并加载所有设置', async () => {
    render(<SettingsPage />);

    // 首先显示加载状态
    expect(screen.getByTestId('settings-loading')).toBeInTheDocument();

    // 等待加载完成
    await waitFor(() => {
      expect(api.getSystemSettings).toHaveBeenCalled();
      expect(screen.queryByTestId('settings-loading')).not.toBeInTheDocument();
    });

    // 检查各设置类别是否渲染
    expect(screen.getByTestId('settings-category-general')).toBeInTheDocument();
    expect(screen.getByTestId('settings-category-security')).toBeInTheDocument();
    expect(screen.getByTestId('settings-category-backup')).toBeInTheDocument();
    expect(screen.getByTestId('settings-category-notification')).toBeInTheDocument();
    expect(screen.getByTestId('settings-category-display')).toBeInTheDocument();
    expect(screen.getByTestId('settings-category-integration')).toBeInTheDocument();
  });

  test('API调用失败时显示错误信息', async () => {
    (api.getSystemSettings as jest.Mock).mockRejectedValue(new Error('Failed to load settings'));

    render(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByText('加载设置失败：Failed to load settings')).toBeInTheDocument();
    });
  });

  test('点击选项卡切换设置类别', async () => {
    render(<SettingsPage />);

    // 等待加载完成
    await waitFor(() => {
      expect(screen.queryByTestId('settings-loading')).not.toBeInTheDocument();
    });

    // 模拟点击安全设置选项卡
    fireEvent.click(screen.getByRole('tab', { name: /安全设置/i }));

    // 验证安全设置选项卡处于活动状态
    expect(screen.getByRole('tab', { name: /安全设置/i })).toHaveAttribute('data-state', 'active');
  });

  test('导出设置功能', async () => {
    render(<SettingsPage />);

    // 等待加载完成
    await waitFor(() => {
      expect(screen.queryByTestId('settings-loading')).not.toBeInTheDocument();
    });

    // 点击导出按钮
    fireEvent.click(screen.getByRole('button', { name: /导出设置/i }));

    // 验证createObjectURL被调用并且下载链接被创建
    expect(global.URL.createObjectURL).toHaveBeenCalled();
    expect(document.createElement).toHaveBeenCalledWith('a');
  });

  test('导入设置功能', async () => {
    const file = new File([JSON.stringify(mockSettings)], 'settings.json', { type: 'application/json' });
    
    render(<SettingsPage />);

    // 等待加载完成
    await waitFor(() => {
      expect(screen.queryByTestId('settings-loading')).not.toBeInTheDocument();
    });

    // 点击导入按钮
    fireEvent.click(screen.getByRole('button', { name: /导入设置/i }));
    
    // 模拟文件输入
    const fileInput = screen.getByTestId('settings-file-input');
    fireEvent.change(fileInput, { target: { files: [file] } });
    
    // 验证batch update API被调用
    await waitFor(() => {
      expect(api.batchUpdateSettings).toHaveBeenCalledWith(mockSettings);
    });
  });
}); 