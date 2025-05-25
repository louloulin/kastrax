import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SettingsPage from './page';
import { getSettingsByGroup, updateSetting, resetSettings } from '../../lib/api/settings';

// Mock the API functions
jest.mock('../../lib/api/settings', () => ({
  getSettingsByGroup: jest.fn(),
  updateSetting: jest.fn(),
  resetSettings: jest.fn(),
  SETTING_GROUPS: [
    { id: 'general', name: '通用设置', icon: 'Settings' },
    { id: 'security', name: '安全设置', icon: 'Shield' },
  ],
}));

describe('SettingsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock API responses
    (getSettingsByGroup as jest.Mock).mockResolvedValue({
      data: [
        {
          id: '1',
          key: 'system.name',
          value: 'Dataflare',
          description: '系统名称',
          group: 'general',
          type: 'string',
          updateTime: '2023-05-01T00:00:00Z',
        },
        {
          id: '2',
          key: 'system.debug',
          value: 'false',
          description: '调试模式',
          group: 'general',
          type: 'boolean',
          updateTime: '2023-05-01T00:00:00Z',
        },
      ],
    });
    
    (updateSetting as jest.Mock).mockResolvedValue({ data: { success: true } });
    (resetSettings as jest.Mock).mockResolvedValue({ data: { success: true } });
  });

  test('renders settings page with tab groups', async () => {
    render(<SettingsPage />);
    
    // Check page title
    expect(screen.getByText('系统设置')).toBeInTheDocument();
    
    // Check tabs
    expect(screen.getByText('通用设置')).toBeInTheDocument();
    expect(screen.getByText('安全设置')).toBeInTheDocument();
    
    // Check that settings are loaded
    await waitFor(() => {
      expect(getSettingsByGroup).toHaveBeenCalledWith('general');
    });
    
    // Check if settings are displayed
    await waitFor(() => {
      expect(screen.getByText('system.name')).toBeInTheDocument();
      expect(screen.getByText('system.debug')).toBeInTheDocument();
    });
  });

  test('handles setting changes correctly', async () => {
    render(<SettingsPage />);
    
    // Wait for settings to load
    await waitFor(() => {
      expect(screen.getByText('system.name')).toBeInTheDocument();
    });
    
    // Change a setting value
    const input = screen.getByDisplayValue('Dataflare');
    await userEvent.clear(input);
    await userEvent.type(input, 'NewName');
    
    // Save changes
    const saveButton = screen.getByText('保存更改');
    expect(saveButton).toBeEnabled();
    await userEvent.click(saveButton);
    
    // Check that updateSetting was called
    await waitFor(() => {
      expect(updateSetting).toHaveBeenCalledWith({
        key: 'system.name',
        value: 'NewName',
      });
    });
    
    // Check that settings are reloaded
    await waitFor(() => {
      expect(getSettingsByGroup).toHaveBeenCalledTimes(2);
    });
  });

  test('handles reset settings correctly', async () => {
    render(<SettingsPage />);
    
    // Wait for settings to load
    await waitFor(() => {
      expect(screen.getByText('系统名称')).toBeInTheDocument();
    });
    
    // Click reset button
    const resetButton = screen.getByText('重置默认');
    await userEvent.click(resetButton);
    
    // Confirm reset
    const confirmButton = screen.getByText('确认重置');
    await userEvent.click(confirmButton);
    
    // Check that resetSettings was called
    await waitFor(() => {
      expect(resetSettings).toHaveBeenCalled();
    });
    
    // Check that settings are reloaded
    await waitFor(() => {
      expect(getSettingsByGroup).toHaveBeenCalledTimes(2);
    });
  });
}); 