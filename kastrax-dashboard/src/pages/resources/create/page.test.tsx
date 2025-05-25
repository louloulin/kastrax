import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import '@testing-library/jest-dom';

// Mock navigate function
const mockNavigate = jest.fn();

// Mock React Router
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

// Mock API
jest.mock('@/lib/api/resources', () => ({
  createResource: jest.fn(),
}));

// Mock components
jest.mock('@/components/resources/resource-form', () => ({
  __esModule: true,
  default: function MockResourceForm({ onChange, onSubmit, resourceData = {}, readOnly }) {
    return (
      <div data-testid="resource-form">
        <button 
          data-testid="fill-form" 
          onClick={() => onChange && onChange({ resourceName: 'Test Resource', resourceType: 'mysql' })}
        >
          Fill Form
        </button>
        <button 
          data-testid="submit-form" 
          onClick={() => onSubmit && onSubmit()}
          disabled={readOnly}
        >
          Submit
        </button>
      </div>
    );
  }
}));

jest.mock('@/components/layout/top-navigation', () => ({
  __esModule: true,
  default: function MockTopNavigation() {
    return <div data-testid="top-navigation">Top Navigation</div>;
  }
}));

jest.mock('@/components/layout/supabase-sidebar', () => ({
  __esModule: true,
  default: function MockSupabaseSidebar() {
    return <div data-testid="supabase-sidebar">Supabase Sidebar</div>;
  }
}));

// Import the actual component
import CreateResourcePage from './page';
import { createResource } from '@/lib/api/resources';

describe('CreateResourcePage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the page with form and buttons', () => {
    render(
      <BrowserRouter>
        <CreateResourcePage />
      </BrowserRouter>
    );

    expect(screen.getByTestId('resource-form')).toBeInTheDocument();
    expect(screen.getByTestId('fill-form')).toBeInTheDocument();
    expect(screen.getByTestId('submit-form')).toBeInTheDocument();
  });

  it('updates form data on change', () => {
    render(
      <BrowserRouter>
        <CreateResourcePage />
      </BrowserRouter>
    );

    fireEvent.click(screen.getByTestId('fill-form'));
    expect(screen.getByRole('button', { name: /创建资源/i })).toBeInTheDocument();
  });

  it('navigates to resource detail after successful creation', async () => {
    (createResource as jest.Mock).mockResolvedValueOnce({ 
      data: { 
        data: { 
          resourceId: '123',
          name: 'Test Resource' 
        } 
      } 
    });

    render(
      <BrowserRouter>
        <CreateResourcePage />
      </BrowserRouter>
    );

    // Fill form first
    fireEvent.click(screen.getByTestId('fill-form'));
    
    // Then click create button
    fireEvent.click(screen.getByRole('button', { name: /创建资源/i }));

    await waitFor(() => {
      expect(createResource).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/resources/123');
    });
  });

  it('shows error message when API call fails', async () => {
    const errorMessage = '资源名称已存在';
    (createResource as jest.Mock).mockRejectedValueOnce({
      response: { data: { message: errorMessage } }
    });

    render(
      <BrowserRouter>
        <CreateResourcePage />
      </BrowserRouter>
    );

    // Fill form
    fireEvent.click(screen.getByTestId('fill-form'));
    
    // Click create button
    fireEvent.click(screen.getByRole('button', { name: /创建资源/i }));

    await waitFor(() => {
      expect(createResource).toHaveBeenCalled();
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
    });
  });

  it('navigates back when cancel is clicked', () => {
    render(
      <BrowserRouter>
        <CreateResourcePage />
      </BrowserRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: /返回/i }));
    
    expect(mockNavigate).toHaveBeenCalledWith('/resources');
  });
}); 