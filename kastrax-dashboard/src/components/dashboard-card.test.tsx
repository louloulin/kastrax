import React from 'react';
import { render, screen } from '@testing-library/react';
import { DashboardCard, StatCard } from './dashboard-card';

// Mock Lucide icons
jest.mock('lucide-react', () => ({
  MoreHorizontal: () => <div data-testid="more-icon">More</div>,
}));

describe('DashboardCard', () => {
  it('renders children content', () => {
    render(
      <DashboardCard>
        <div data-testid="test-content">Test Content</div>
      </DashboardCard>
    );
    
    expect(screen.getByTestId('test-content')).toBeInTheDocument();
    expect(screen.getByText('Test Content')).toBeInTheDocument();
  });
  
  it('renders with title', () => {
    render(
      <DashboardCard title="Test Title">
        <div>Content</div>
      </DashboardCard>
    );
    
    expect(screen.getByText('Test Title')).toBeInTheDocument();
    expect(screen.getByTestId('more-icon')).toBeInTheDocument();
  });
  
  it('renders with custom action', () => {
    const customAction = <button data-testid="custom-action">Custom</button>;
    
    render(
      <DashboardCard title="Test Title" action={customAction}>
        <div>Content</div>
      </DashboardCard>
    );
    
    expect(screen.getByTestId('custom-action')).toBeInTheDocument();
    expect(screen.queryByTestId('more-icon')).not.toBeInTheDocument();
  });
  
  it('applies additional class names', () => {
    const { container } = render(
      <DashboardCard className="custom-class">
        <div>Content</div>
      </DashboardCard>
    );
    
    expect(container.firstChild).toHaveClass('custom-class');
  });
  
  it('applies full height class when specified', () => {
    const { container } = render(
      <DashboardCard fullHeight>
        <div>Content</div>
      </DashboardCard>
    );
    
    expect(container.firstChild).toHaveClass('h-full');
  });
});

describe('StatCard', () => {
  it('renders with title and value', () => {
    render(<StatCard title="Users" value={1250} />);
    
    expect(screen.getByText('Users')).toBeInTheDocument();
    expect(screen.getByText('1250')).toBeInTheDocument();
  });
  
  it('renders with change indicator', () => {
    render(<StatCard title="Revenue" value="$12,500" change="+12.5%" />);
    
    expect(screen.getByText('Revenue')).toBeInTheDocument();
    expect(screen.getByText('$12,500')).toBeInTheDocument();
    expect(screen.getByText('+12.5%')).toBeInTheDocument();
    
    // By default, change should have primary color (for increases)
    expect(screen.getByText('+12.5%')).toHaveClass('text-primary');
  });
  
  it('shows decreasing change with negative styling', () => {
    render(
      <StatCard 
        title="Visitors" 
        value={5823} 
        change="-4.2%" 
        decreasing 
      />
    );
    
    expect(screen.getByText('Visitors')).toBeInTheDocument();
    expect(screen.getByText('5823')).toBeInTheDocument();
    expect(screen.getByText('-4.2%')).toBeInTheDocument();
    
    // For decreasing values, should have destructive color
    expect(screen.getByText('-4.2%')).toHaveClass('text-destructive');
  });
  
  it('renders with icon', () => {
    const icon = <div data-testid="custom-icon">Icon</div>;
    
    render(
      <StatCard 
        title="Orders" 
        value={340} 
        icon={icon}
      />
    );
    
    expect(screen.getByText('Orders')).toBeInTheDocument();
    expect(screen.getByText('340')).toBeInTheDocument();
    expect(screen.getByTestId('custom-icon')).toBeInTheDocument();
  });
}); 