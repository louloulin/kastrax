import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import PreferencesTab from './preferences-tab';

// Mock the theme provider
jest.mock('../../components/theme-provider', () => ({
  useTheme: () => ({
    theme: 'light',
    setTheme: jest.fn(),
  }),
}));

// Mock Lucide icons
jest.mock('lucide-react', () => ({
  Laptop: () => <div data-testid="laptop-icon">Laptop</div>,
  Moon: () => <div data-testid="moon-icon">Moon</div>,
  Sun: () => <div data-testid="sun-icon">Sun</div>,
}));

describe('PreferencesTab', () => {
  it('renders the preferences tab with theme options', () => {
    render(<PreferencesTab />);
    
    // Check if the component renders basic elements
    expect(screen.getByText('用户偏好设置')).toBeInTheDocument();
    expect(screen.getByText('自定义应用程序的外观和行为')).toBeInTheDocument();
    
    // Check if theme settings section is rendered
    expect(screen.getByText('主题设置')).toBeInTheDocument();
    expect(screen.getByText('选择应用程序的主题模式')).toBeInTheDocument();
    
    // Check if theme options are rendered
    expect(screen.getByText('浅色模式')).toBeInTheDocument();
    expect(screen.getByText('深色模式')).toBeInTheDocument();
    expect(screen.getByText('跟随系统')).toBeInTheDocument();
    
    // Check if icons are rendered
    expect(screen.getByTestId('sun-icon')).toBeInTheDocument();
    expect(screen.getByTestId('moon-icon')).toBeInTheDocument();
    expect(screen.getByTestId('laptop-icon')).toBeInTheDocument();
  });
  
  it('renders layout settings section', () => {
    render(<PreferencesTab />);
    
    // Check if layout settings section is rendered
    expect(screen.getByText('布局设置')).toBeInTheDocument();
    expect(screen.getByText('自定义应用程序的布局')).toBeInTheDocument();
    
    // Check if layout options are rendered
    expect(screen.getByText('紧凑模式')).toBeInTheDocument();
    expect(screen.getByText('减小边距和内边距，显示更多内容')).toBeInTheDocument();
    expect(screen.getByText('固定标题栏')).toBeInTheDocument();
    expect(screen.getByText('滚动时保持标题栏可见')).toBeInTheDocument();
    
    // Check if switches are rendered
    const switches = screen.getAllByRole('switch');
    expect(switches).toHaveLength(2);
    
    // Second switch (sticky header) should be checked by default
    expect(switches[1]).toHaveAttribute('aria-checked', 'true');
  });
  
  it('renders table settings section', () => {
    render(<PreferencesTab />);
    
    // Check if table settings section is rendered
    expect(screen.getByText('表格设置')).toBeInTheDocument();
    expect(screen.getByText('自定义表格显示方式')).toBeInTheDocument();
    
    // Check if page size options are rendered
    expect(screen.getByText('默认分页大小')).toBeInTheDocument();
    expect(screen.getByText('每页显示的行数')).toBeInTheDocument();
    
    // Check if select is rendered with default value
    expect(screen.getByRole('combobox')).toHaveTextContent('10');
  });
  
  it('calls setTheme when clicking theme options', () => {
    const mockSetTheme = jest.fn();
    
    // Override the mock implementation for this test
    (require('../../components/theme-provider') as any).useTheme = () => ({
      theme: 'light',
      setTheme: mockSetTheme,
    });
    
    render(<PreferencesTab />);
    
    // Click on dark mode option
    fireEvent.click(screen.getByText('深色模式').closest('div')!);
    expect(mockSetTheme).toHaveBeenCalledWith('dark');
    
    // Click on system mode option
    fireEvent.click(screen.getByText('跟随系统').closest('div')!);
    expect(mockSetTheme).toHaveBeenCalledWith('system');
    
    // Click on light mode option
    fireEvent.click(screen.getByText('浅色模式').closest('div')!);
    expect(mockSetTheme).toHaveBeenCalledWith('light');
  });
  
  it('applies correct border based on current theme', () => {
    // Test with light theme
    (require('../../components/theme-provider') as any).useTheme = () => ({
      theme: 'light',
      setTheme: jest.fn(),
    });
    
    const { rerender } = render(<PreferencesTab />);
    
    // Light theme option should have primary border
    const lightOption = screen.getByText('浅色模式').closest('div');
    const darkOption = screen.getByText('深色模式').closest('div');
    const systemOption = screen.getByText('跟随系统').closest('div');
    
    expect(lightOption).toHaveClass('border-primary');
    expect(darkOption).toHaveClass('border-border');
    expect(systemOption).toHaveClass('border-border');
    
    // Test with dark theme
    (require('../../components/theme-provider') as any).useTheme = () => ({
      theme: 'dark',
      setTheme: jest.fn(),
    });
    
    rerender(<PreferencesTab />);
    
    // After rerender with dark theme, dark option should have primary border
    expect(lightOption).toHaveClass('border-border');
    expect(darkOption).toHaveClass('border-primary');
    expect(systemOption).toHaveClass('border-border');
  });
  
  it('disables controls when loading is true', () => {
    render(<PreferencesTab loading={true} />);
    
    // Check if switches are disabled
    const switches = screen.getAllByRole('switch');
    switches.forEach(switchEl => {
      expect(switchEl).toBeDisabled();
    });
    
    // Check if select is disabled
    expect(screen.getByRole('combobox')).toBeDisabled();
  });
}); 