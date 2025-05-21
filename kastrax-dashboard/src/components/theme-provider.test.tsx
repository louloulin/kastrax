import React from 'react';
import { render, screen, act } from '@testing-library/react';
import { ThemeProvider, useTheme } from './theme-provider';

// Create a mock of next-themes
jest.mock('next-themes', () => {
  const originalModule = jest.requireActual('next-themes');
  
  // Mock theme state
  let currentTheme = 'light';
  let currentResolvedTheme = 'light';
  const themes = ['light', 'dark', 'system'];
  
  // Create mock of useTheme that allows us to control theme state
  const useNextTheme = jest.fn(() => ({
    theme: currentTheme,
    setTheme: jest.fn((theme) => {
      currentTheme = theme;
      if (theme === 'system') {
        currentResolvedTheme = 'light'; // Simulate system theme being light
      } else {
        currentResolvedTheme = theme;
      }
    }),
    themes,
    resolvedTheme: currentResolvedTheme,
    systemTheme: 'light',
  }));
  
  return {
    ...originalModule,
    useTheme: useNextTheme,
  };
});

// Test component that uses the theme hook
const TestComponent = () => {
  const { theme, setTheme, resolvedTheme } = useTheme();
  
  return (
    <div>
      <span data-testid="current-theme">{theme}</span>
      <span data-testid="resolved-theme">{resolvedTheme}</span>
      <button data-testid="light-button" onClick={() => setTheme('light')}>Light</button>
      <button data-testid="dark-button" onClick={() => setTheme('dark')}>Dark</button>
      <button data-testid="system-button" onClick={() => setTheme('system')}>System</button>
    </div>
  );
};

describe('ThemeProvider', () => {
  it('renders its children', () => {
    render(
      <ThemeProvider>
        <div data-testid="child-element">Test Child</div>
      </ThemeProvider>
    );
    
    expect(screen.getByTestId('child-element')).toBeInTheDocument();
  });
  
  it('passes props to NextThemesProvider', () => {
    // This is more of an implementation test - we're testing that our ThemeProvider
    // correctly passes props to the underlying NextThemesProvider
    const mockNextThemesProvider = require('next-themes').ThemeProvider;
    
    render(
      <ThemeProvider
        defaultTheme="dark"
        forcedTheme="light"
        disableTransitionOnChange={true}
        enableColorScheme={false}
        enableSystem={false}
        storageKey="custom-key"
      >
        <div>Test</div>
      </ThemeProvider>
    );
    
    // Check if NextThemesProvider was called with correct props
    expect(mockNextThemesProvider).toHaveBeenCalledWith(
      expect.objectContaining({
        defaultTheme: "dark",
        forcedTheme: "light",
        disableTransitionOnChange: true,
        enableColorScheme: false,
        enableSystem: false,
        storageKey: "custom-key",
      }),
      expect.anything()
    );
  });
});

describe('useTheme hook', () => {
  it('provides theme state and methods', () => {
    render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );
    
    expect(screen.getByTestId('current-theme').textContent).toBe('light');
  });
  
  it('allows changing the theme', () => {
    render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );
    
    // Initial state
    expect(screen.getByTestId('current-theme').textContent).toBe('light');
    
    // Change to dark
    act(() => {
      screen.getByTestId('dark-button').click();
    });
    
    // Verify theme was updated
    expect(screen.getByTestId('current-theme').textContent).toBe('dark');
    expect(screen.getByTestId('resolved-theme').textContent).toBe('dark');
    
    // Change to system
    act(() => {
      screen.getByTestId('system-button').click();
    });
    
    // Verify theme was updated to system, with resolved theme as light
    expect(screen.getByTestId('current-theme').textContent).toBe('system');
    expect(screen.getByTestId('resolved-theme').textContent).toBe('light');
  });
}); 