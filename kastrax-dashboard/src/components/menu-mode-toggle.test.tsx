import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MenuModeToggle } from './menu-mode-toggle';

// Mock the theme provider
jest.mock('./theme-provider', () => ({
  useTheme: () => ({
    theme: 'light',
    setTheme: jest.fn(),
  }),
}));

// Mock Lucide icons
jest.mock('lucide-react', () => ({
  Moon: () => <div data-testid="moon-icon">Moon</div>,
  Sun: () => <div data-testid="sun-icon">Sun</div>,
}));

describe('MenuModeToggle', () => {
  it('renders with light theme by default', () => {
    render(<MenuModeToggle />);
    
    // Should show Sun icon when theme is light
    expect(screen.getByTestId('sun-icon')).toBeInTheDocument();
    expect(screen.queryByTestId('moon-icon')).not.toBeInTheDocument();
  });
  
  it('renders with dark theme when theme is dark', () => {
    // Override theme mock to return 'dark'
    (require('./theme-provider') as any).useTheme = () => ({
      theme: 'dark',
      setTheme: jest.fn(),
    });
    
    render(<MenuModeToggle />);
    
    // Should show Moon icon when theme is dark
    expect(screen.getByTestId('moon-icon')).toBeInTheDocument();
    expect(screen.queryByTestId('sun-icon')).not.toBeInTheDocument();
  });
  
  it('toggles theme when clicked', () => {
    const mockSetTheme = jest.fn();
    
    // First test light to dark transition
    (require('./theme-provider') as any).useTheme = () => ({
      theme: 'light',
      setTheme: mockSetTheme,
    });
    
    const { rerender } = render(<MenuModeToggle />);
    
    // Click the button
    fireEvent.click(screen.getByRole('button'));
    
    // setTheme should be called with 'dark'
    expect(mockSetTheme).toHaveBeenCalledWith('dark');
    
    // Now test dark to light transition
    (require('./theme-provider') as any).useTheme = () => ({
      theme: 'dark',
      setTheme: mockSetTheme,
    });
    
    rerender(<MenuModeToggle />);
    
    // Click the button again
    fireEvent.click(screen.getByRole('button'));
    
    // setTheme should now be called with 'light'
    expect(mockSetTheme).toHaveBeenCalledWith('light');
  });
}); 