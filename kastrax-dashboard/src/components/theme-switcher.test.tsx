import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { ThemeSwitcher } from './theme-switcher';
import { ThemeProvider } from './theme-provider';

// Mock the useTheme hook
jest.mock('./theme-provider', () => ({
  ThemeProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  useTheme: () => ({
    theme: 'light',
    setTheme: jest.fn(),
    themes: ['light', 'dark', 'system'],
    resolvedTheme: 'light',
    systemTheme: 'light',
  }),
}));

describe('ThemeSwitcher', () => {
  it('renders the theme button', () => {
    render(<ThemeSwitcher />);
    
    // Check if the button is in the document
    const themeButton = screen.getByRole('button');
    expect(themeButton).toBeInTheDocument();
  });

  it('opens the dropdown when clicked', () => {
    render(<ThemeSwitcher />);
    
    // Find and click the theme button
    const themeButton = screen.getByRole('button');
    fireEvent.click(themeButton);
    
    // Check if the dropdown items are shown
    expect(screen.getByText('浅色')).toBeInTheDocument();
    expect(screen.getByText('深色')).toBeInTheDocument();
    expect(screen.getByText('系统')).toBeInTheDocument();
  });

  it('changes theme when dropdown item is clicked', () => {
    const mockSetTheme = jest.fn();
    
    // Override the mock implementation for this test
    (require('./theme-provider') as any).useTheme = () => ({
      theme: 'light',
      setTheme: mockSetTheme,
      themes: ['light', 'dark', 'system'],
      resolvedTheme: 'light',
      systemTheme: 'light',
    });
    
    render(<ThemeSwitcher />);
    
    // Open the dropdown
    const themeButton = screen.getByRole('button');
    fireEvent.click(themeButton);
    
    // Click on "深色" to switch to dark mode
    const darkModeOption = screen.getByText('深色');
    fireEvent.click(darkModeOption);
    
    // Check if setTheme was called with 'dark'
    expect(mockSetTheme).toHaveBeenCalledWith('dark');
  });
}); 