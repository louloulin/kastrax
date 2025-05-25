import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import SettingsDialog from './settings-dialog';
import { SystemSetting } from '@/lib/api/settings';
import * as api from '@/lib/api/settings';

// Mock API functions
jest.mock('@/lib/api/settings', () => ({
  updateSetting: jest.fn(),
}));

// Mock toast
jest.mock('@/components/ui/use-toast', () => ({
  useToast: () => ({
    toast: jest.fn(),
  }),
}));

describe('设置对话框组件测试', () => {
  const mockSetting: SystemSetting = {
    settingId: '1',
    category: 'general',
    key: 'site_name',
    value: 'Dataflare',
    description: '站点名称',
    createTime: '2023-01-01T00:00:00Z',
    updateTime: '2023-01-01T00:00:00Z',
  };

  const mockOnOpenChange = jest.fn();
  const mockOnSuccess = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (api.updateSetting as jest.Mock).mockResolvedValue({
      status: 'success',
      data: mockSetting,
    });
  });

  test('当打开状态为false时不渲染对话框', () => {
    const { container } = render(
      <SettingsDialog
        open={false}
        onOpenChange={mockOnOpenChange}
        setting={mockSetting}
        onSuccess={mockOnSuccess}
      />
    );
    
    // 检查对话框内容是否不存在
    expect(container.innerHTML).toBe('');
  });

  test('当打开状态为true时渲染对话框内容', () => {
    render(
      <SettingsDialog
        open={true}
        onOpenChange={mockOnOpenChange}
        setting={mockSetting}
        onSuccess={mockOnSuccess}
      />
    );
    
    // 检查对话框标题和内容
    expect(screen.getByText('编辑设置: site_name')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Dataflare')).toBeInTheDocument();
    expect(screen.getByLabelText(/描述/i)).toBeInTheDocument();
  });

  test('成功提交表单时关闭对话框并调用onSuccess', async () => {
    render(
      <SettingsDialog
        open={true}
        onOpenChange={mockOnOpenChange}
        setting={mockSetting}
        onSuccess={mockOnSuccess}
      />
    );
    
    // 修改值
    fireEvent.change(screen.getByLabelText('site_name'), { target: { value: 'New Site Name' } });
    
    // 提交表单
    fireEvent.click(screen.getByRole('button', { name: /保存/i }));
    
    await waitFor(() => {
      // 验证API调用
      expect(api.updateSetting).toHaveBeenCalledWith({
        settingId: '1',
        value: 'New Site Name',
        description: '站点名称',
      });
      
      // 验证成功回调和对话框关闭
      expect(mockOnSuccess).toHaveBeenCalledTimes(1);
      expect(mockOnOpenChange).toHaveBeenCalledWith(false);
    });
  });

  test('API调用失败时显示错误信息', async () => {
    // 模拟API调用失败
    (api.updateSetting as jest.Mock).mockRejectedValue(new Error('Update failed'));
    
    render(
      <SettingsDialog
        open={true}
        onOpenChange={mockOnOpenChange}
        setting={mockSetting}
        onSuccess={mockOnSuccess}
      />
    );
    
    // 提交表单
    fireEvent.click(screen.getByRole('button', { name: /保存/i }));
    
    await waitFor(() => {
      // 验证错误提示显示
      expect(screen.getByText('更新设置失败：Update failed')).toBeInTheDocument();
      
      // 验证对话框未关闭，成功回调未调用
      expect(mockOnSuccess).not.toHaveBeenCalled();
      expect(mockOnOpenChange).not.toHaveBeenCalledWith(false);
    });
  });

  test('点击取消按钮关闭对话框', () => {
    render(
      <SettingsDialog
        open={true}
        onOpenChange={mockOnOpenChange}
        setting={mockSetting}
        onSuccess={mockOnSuccess}
      />
    );
    
    // 点击取消按钮
    fireEvent.click(screen.getByRole('button', { name: /取消/i }));
    
    // 验证对话框关闭
    expect(mockOnOpenChange).toHaveBeenCalledWith(false);
    // 验证API未被调用
    expect(api.updateSetting).not.toHaveBeenCalled();
  });
});
