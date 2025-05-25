import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import SettingsForm from './settings-form';
import { SystemSetting } from '@/lib/api/settings';

// Mock响应函数
const mockOnSubmit = jest.fn();
const mockOnCancel = jest.fn();

describe('设置表单组件测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('渲染字符串类型的设置', () => {
    const stringSetting: SystemSetting = {
      settingId: '1',
      category: 'general',
      key: 'site_name',
      value: 'Dataflare',
      description: '站点名称',
      createTime: '2023-01-01T00:00:00Z',
      updateTime: '2023-01-01T00:00:00Z',
    };

    render(
      <SettingsForm 
        setting={stringSetting} 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    expect(screen.getByLabelText('site_name')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Dataflare')).toBeInTheDocument();
    expect(screen.getByLabelText(/描述/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue('站点名称')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /保存/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /取消/i })).toBeInTheDocument();
  });

  test('渲染布尔类型的设置', () => {
    const booleanSetting: SystemSetting = {
      settingId: '2',
      category: 'security',
      key: 'enable_2fa',
      value: 'true',
      description: '启用两步验证',
      createTime: '2023-01-01T00:00:00Z',
      updateTime: '2023-01-01T00:00:00Z',
    };

    render(
      <SettingsForm 
        setting={booleanSetting} 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    expect(screen.getByText('enable_2fa')).toBeInTheDocument();
    expect(screen.getByText('启用两步验证')).toBeInTheDocument();
    expect(screen.getByRole('switch')).toBeInTheDocument();
    expect(screen.getByRole('switch')).toBeChecked();
  });

  test('渲染数字类型的设置', () => {
    const numberSetting: SystemSetting = {
      settingId: '3',
      category: 'general',
      key: 'session_timeout',
      value: '30',
      description: '会话超时时间（分钟）',
      createTime: '2023-01-01T00:00:00Z',
      updateTime: '2023-01-01T00:00:00Z',
    };

    render(
      <SettingsForm 
        setting={numberSetting} 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    expect(screen.getByLabelText('session_timeout')).toBeInTheDocument();
    expect(screen.getByDisplayValue('30')).toHaveAttribute('type', 'number');
  });

  test('提交表单', async () => {
    const setting: SystemSetting = {
      settingId: '1',
      category: 'general',
      key: 'site_name',
      value: 'Dataflare',
      description: '站点名称',
      createTime: '2023-01-01T00:00:00Z',
      updateTime: '2023-01-01T00:00:00Z',
    };

    render(
      <SettingsForm 
        setting={setting} 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    // 修改值
    fireEvent.change(screen.getByLabelText('site_name'), { target: { value: 'New Name' } });
    
    // 提交表单
    fireEvent.click(screen.getByRole('button', { name: /保存/i }));
    
    // 验证提交函数被调用且参数正确
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith({
        settingId: '1',
        value: 'New Name',
        description: '站点名称',
      });
    });
  });

  test('取消表单', () => {
    const setting: SystemSetting = {
      settingId: '1',
      category: 'general',
      key: 'site_name',
      value: 'Dataflare',
      description: '站点名称',
      createTime: '2023-01-01T00:00:00Z',
      updateTime: '2023-01-01T00:00:00Z',
    };

    render(
      <SettingsForm 
        setting={setting} 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    // 修改值但不提交
    fireEvent.change(screen.getByLabelText('site_name'), { target: { value: 'Changed Value' } });
    
    // 点击取消按钮
    fireEvent.click(screen.getByRole('button', { name: /取消/i }));
    
    // 验证取消函数被调用，而提交函数没有被调用
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  test('切换布尔设置', async () => {
    const booleanSetting: SystemSetting = {
      settingId: '2',
      category: 'security',
      key: 'enable_2fa',
      value: 'true',
      description: '启用两步验证',
      createTime: '2023-01-01T00:00:00Z',
      updateTime: '2023-01-01T00:00:00Z',
    };

    render(
      <SettingsForm 
        setting={booleanSetting} 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    // 切换开关
    fireEvent.click(screen.getByRole('switch'));
    
    // 提交表单
    fireEvent.click(screen.getByRole('button', { name: /保存/i }));
    
    // 验证提交函数被调用且参数正确
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith({
        settingId: '2',
        value: 'false',
        description: '启用两步验证',
      });
    });
  });
}); 