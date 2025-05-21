import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import SettingsCategory from './settings-category';
import { SystemSetting } from '@/lib/api/settings';
import * as api from '@/lib/api/settings';

// Mock API functions
jest.mock('@/lib/api/settings', () => ({
  getSettingsByCategory: jest.fn(),
  updateSetting: jest.fn(),
}));

// Mock toast
jest.mock('@/components/ui/use-toast', () => ({
  useToast: () => ({
    toast: jest.fn(),
  }),
}));

// Mock dialog component
jest.mock('./settings-dialog', () => {
  return function MockSettingsDialog({ 
    open, 
    onOpenChange, 
    setting, 
    onSuccess 
  }: { 
    open: boolean; 
    onOpenChange: (open: boolean) => void; 
    setting: SystemSetting; 
    onSuccess: () => void 
  }) {
    return open ? (
      <div data-testid="mock-settings-dialog">
        <button onClick={() => onSuccess()}>Success</button>
        <button onClick={() => onOpenChange(false)}>Cancel</button>
      </div>
    ) : null;
  };
});

describe('设置类别组件测试', () => {
  const mockSettings: SystemSetting[] = [
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
      category: 'general',
      key: 'enable_feature',
      value: 'true',
      description: '启用功能',
      createTime: '2023-01-01T00:00:00Z',
      updateTime: '2023-01-01T00:00:00Z',
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (api.getSettingsByCategory as jest.Mock).mockResolvedValue({
      status: 'success',
      data: mockSettings,
    });
  });

  test('渲染设置类别组件并加载设置', async () => {
    render(
      <SettingsCategory
        category="general"
        title="通用设置"
        description="系统的通用设置项"
      />
    );

    // 首先显示加载状态
    expect(screen.getByTestId('settings-loading')).toBeInTheDocument();

    // 等待加载完成
    await waitFor(() => {
      expect(api.getSettingsByCategory).toHaveBeenCalledWith('general');
      expect(screen.queryByTestId('settings-loading')).not.toBeInTheDocument();
    });

    // 检查设置项是否渲染
    expect(screen.getByText('site_name')).toBeInTheDocument();
    expect(screen.getByText('enable_feature')).toBeInTheDocument();
    expect(screen.getByText('站点名称')).toBeInTheDocument();
    expect(screen.getByText('启用功能')).toBeInTheDocument();
  });

  test('API调用失败时显示错误信息', async () => {
    (api.getSettingsByCategory as jest.Mock).mockRejectedValue(new Error('Failed to load settings'));

    render(
      <SettingsCategory
        category="general"
        title="通用设置"
        description="系统的通用设置项"
      />
    );

    await waitFor(() => {
      expect(screen.getByText('加载设置失败：Failed to load settings')).toBeInTheDocument();
    });
  });

  test('点击布尔类型设置的开关切换值', async () => {
    (api.updateSetting as jest.Mock).mockResolvedValue({
      status: 'success',
      data: {
        ...mockSettings[1],
        value: 'false',
      },
    });

    render(
      <SettingsCategory
        category="general"
        title="通用设置"
        description="系统的通用设置项"
      />
    );

    // 等待设置加载完成
    await waitFor(() => {
      expect(screen.queryByTestId('settings-loading')).not.toBeInTheDocument();
    });

    // 找到开关并点击
    const toggleSwitch = screen.getByRole('switch');
    fireEvent.click(toggleSwitch);

    // 验证API调用
    await waitFor(() => {
      expect(api.updateSetting).toHaveBeenCalledWith({
        settingId: '2',
        value: 'false',
        description: '启用功能',
      });
    });
  });

  test('点击非布尔类型设置打开编辑对话框', async () => {
    render(
      <SettingsCategory
        category="general"
        title="通用设置"
        description="系统的通用设置项"
      />
    );

    // 等待设置加载完成
    await waitFor(() => {
      expect(screen.queryByTestId('settings-loading')).not.toBeInTheDocument();
    });

    // 找到编辑按钮并点击
    const editButton = screen.getByText('Dataflare');
    fireEvent.click(editButton);

    // 验证对话框打开
    expect(screen.getByTestId('mock-settings-dialog')).toBeInTheDocument();
  });

  test('对话框成功回调后重新加载设置', async () => {
    render(
      <SettingsCategory
        category="general"
        title="通用设置"
        description="系统的通用设置项"
      />
    );

    // 等待设置加载完成
    await waitFor(() => {
      expect(screen.queryByTestId('settings-loading')).not.toBeInTheDocument();
    });

    // 找到编辑按钮并点击打开对话框
    const editButton = screen.getByText('Dataflare');
    fireEvent.click(editButton);

    // 清除之前的调用记录
    (api.getSettingsByCategory as jest.Mock).mockClear();

    // 点击对话框的成功按钮
    fireEvent.click(screen.getByText('Success'));

    // 验证设置重新加载
    await waitFor(() => {
      expect(api.getSettingsByCategory).toHaveBeenCalledTimes(1);
      expect(api.getSettingsByCategory).toHaveBeenCalledWith('general');
    });
  });
}); 