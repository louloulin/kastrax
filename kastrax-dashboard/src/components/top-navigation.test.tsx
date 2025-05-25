import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import TopNavigation from './top-navigation';

// Mock the ThemeSwitcher component to simplify testing
jest.mock('./theme-switcher', () => ({
  ThemeSwitcher: () => <div data-testid="theme-switcher">ThemeSwitcher</div>
}));

// Mock Lucide icons
jest.mock('lucide-react', () => ({
  Bell: () => <div data-testid="bell-icon">Bell</div>,
  Search: () => <div data-testid="search-icon">Search</div>,
  User: () => <div data-testid="user-icon">User</div>,
  ChevronDown: () => <div data-testid="chevron-down-icon">ChevronDown</div>
}));

describe('TopNavigation', () => {
  it('renders correctly with default props', () => {
    render(<TopNavigation />);
    
    // Check if the component renders basic elements
    expect(screen.getByText('仪表盘')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('搜索...')).toBeInTheDocument();
    
    // Check if icons are rendered
    expect(screen.getByTestId('search-icon')).toBeInTheDocument();
    expect(screen.getByTestId('bell-icon')).toBeInTheDocument();
    expect(screen.getByTestId('theme-switcher')).toBeInTheDocument();
    expect(screen.getByTestId('user-icon')).toBeInTheDocument();
    expect(screen.getByTestId('chevron-down-icon')).toBeInTheDocument();
    
    // Check if user info is rendered
    expect(screen.getByText('管理员')).toBeInTheDocument();
    expect(screen.getByText('admin@example.com')).toBeInTheDocument();
  });

  it('renders with children', () => {
    render(
      <TopNavigation>
        <div data-testid="child-element">Child Element</div>
      </TopNavigation>
    );
    
    // Check if the child element is rendered
    expect(screen.getByTestId('child-element')).toBeInTheDocument();
    expect(screen.getByText('Child Element')).toBeInTheDocument();
  });

  it('changes search input background when focused', () => {
    render(<TopNavigation />);
    
    // Get the search container div
    const searchContainer = screen.getByPlaceholderText('搜索...').parentElement;
    
    // Before focus, it should have bg-secondary/50 class 
    // (though we can't test the actual styling in JSDOM, we can check className contains the right classes)
    expect(searchContainer?.className).toContain('bg-secondary/50');
    expect(searchContainer?.className).not.toContain('bg-background');
    
    // Focus the input
    fireEvent.focus(screen.getByPlaceholderText('搜索...'));
    
    // After focus, it should have bg-background class
    expect(searchContainer?.className).toContain('bg-background');
    expect(searchContainer?.className).not.toContain('bg-secondary/50');
    
    // Blur the input
    fireEvent.blur(screen.getByPlaceholderText('搜索...'));
    
    // After blur, it should go back to bg-secondary/50
    expect(searchContainer?.className).toContain('bg-secondary/50');
    expect(searchContainer?.className).not.toContain('bg-background');
  });

  it('has notification indicator', () => {
    render(<TopNavigation />);
    
    // Check if the notification button has a dot indicator
    // The indicator is a span with bg-primary class
    const bellButton = screen.getByTestId('bell-icon').closest('button');
    const indicator = bellButton?.querySelector('span');
    
    expect(indicator).toBeInTheDocument();
    expect(indicator?.className).toContain('bg-primary');
  });

  it('renders responsive user section', () => {
    // Mock window.matchMedia for testing responsive behavior
    window.matchMedia = jest.fn().mockImplementation(query => ({
      matches: query.includes('max-width: 768px'), // This will simulate mobile view when true
      media: query,
      onchange: null,
      addListener: jest.fn(),
      removeListener: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      dispatchEvent: jest.fn(),
    }));
    
    render(<TopNavigation />);
    
    // On desktop, user info should be visible
    const userInfo = screen.getByText('管理员').parentElement;
    
    // Check that the parent div has the right classes for responsive behavior
    expect(userInfo?.className).toContain('hidden');
    expect(userInfo?.className).toContain('md:block');
  });
}); 