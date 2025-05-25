import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ResourcesPage from './page';

// Mock API calls
jest.mock('../../lib/api/resources', () => ({
  getResourceList: jest.fn().mockResolvedValue({
    data: {
      data: [
        {
          resourceId: 'resource-1',
          resourceName: 'MySQL Database',
          resourceType: 'mysql',
          status: 1,
          createTime: '2023-01-01T00:00:00Z',
          updateTime: '2023-01-01T00:00:00Z'
        },
        {
          resourceId: 'resource-2',
          resourceName: 'PostgreSQL Database',
          resourceType: 'postgresql',
          status: 1,
          createTime: '2023-01-02T00:00:00Z',
          updateTime: '2023-01-02T00:00:00Z'
        },
        {
          resourceId: 'resource-3',
          resourceName: 'Redis Cache',
          resourceType: 'redis',
          status: 0,
          createTime: '2023-01-03T00:00:00Z',
          updateTime: '2023-01-03T00:00:00Z'
        }
      ]
    }
  }),
  deleteResource: jest.fn().mockResolvedValue({
    data: { success: true }
  })
}));

// Mock components
jest.mock('../../components/supabase-sidebar', () => {
  return function SupabaseSidebar() {
    return <div data-testid="supabase-sidebar">Sidebar</div>;
  };
});

jest.mock('../../components/top-navigation', () => {
  return function TopNavigation({ children }: { children?: React.ReactNode }) {
    return (
      <div data-testid="top-navigation">
        Top Navigation
        {children}
      </div>
    );
  };
});

jest.mock('../../components/data-table', () => {
  return function DataTable({ 
    columns, 
    data, 
    actions,
    onRowClick
  }: any) {
    return (
      <div data-testid="data-table">
        <div>Mock Data Table</div>
        <div>Rows: {data.length}</div>
        <table>
          <thead>
            <tr>
              {columns.map((col: any) => (
                <th key={col.id}>{col.header}</th>
              ))}
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {data.map((row: any) => (
              <tr key={row.resourceId} onClick={() => onRowClick && onRowClick(row)}>
                <td>{row.resourceName}</td>
                <td>{row.resourceType}</td>
                <td>{row.status === 1 ? '正常' : '异常'}</td>
                <td>{row.createTime}</td>
                <td>
                  {actions && actions(
                    { original: row }
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };
});

jest.mock('../../components/ui/use-toast', () => ({
  toast: jest.fn()
}));

// Mock window.confirm
const originalConfirm = window.confirm;
beforeAll(() => {
  window.confirm = jest.fn().mockReturnValue(true);
});
afterAll(() => {
  window.confirm = originalConfirm;
});

describe('ResourcesPage', () => {
  const mockedNavigate = jest.fn();
  
  beforeEach(() => {
    jest.clearAllMocks();
    // Mock useNavigate
    jest.mock('react-router-dom', () => ({
      ...jest.requireActual('react-router-dom'),
      useNavigate: () => mockedNavigate
    }));
  });
  
  it('renders the page with resource list', async () => {
    render(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>
    );
    
    // Page title should be visible
    expect(screen.getByText('资源管理')).toBeInTheDocument();
    
    // Sidebar and navigation should be rendered
    expect(screen.getByTestId('supabase-sidebar')).toBeInTheDocument();
    expect(screen.getByTestId('top-navigation')).toBeInTheDocument();
    
    // Data table should be rendered
    expect(screen.getByTestId('data-table')).toBeInTheDocument();
    
    // Resources should be loaded and displayed
    await waitFor(() => {
      expect(screen.getByText('Rows: 3')).toBeInTheDocument();
      expect(screen.getByText('MySQL Database')).toBeInTheDocument();
      expect(screen.getByText('PostgreSQL Database')).toBeInTheDocument();
      expect(screen.getByText('Redis Cache')).toBeInTheDocument();
    });
  });
  
  it('filters resources by search query', async () => {
    render(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>
    );
    
    // Wait for resources to load
    await waitFor(() => {
      expect(screen.getByText('MySQL Database')).toBeInTheDocument();
    });
    
    // Enter search query
    const searchInput = screen.getByPlaceholderText('搜索资源名称或类型...');
    fireEvent.change(searchInput, { target: { value: 'MySQL' } });
    
    // Only MySQL resource should be shown
    expect(screen.getByText('MySQL Database')).toBeInTheDocument();
    expect(screen.queryByText('PostgreSQL Database')).not.toBeInTheDocument();
    expect(screen.queryByText('Redis Cache')).not.toBeInTheDocument();
    
    // Search by resource type
    fireEvent.change(searchInput, { target: { value: 'redis' } });
    
    // Only Redis resource should be shown
    expect(screen.queryByText('MySQL Database')).not.toBeInTheDocument();
    expect(screen.queryByText('PostgreSQL Database')).not.toBeInTheDocument();
    expect(screen.getByText('Redis Cache')).toBeInTheDocument();
    
    // Clear search
    fireEvent.change(searchInput, { target: { value: '' } });
    
    // All resources should be shown again
    expect(screen.getByText('MySQL Database')).toBeInTheDocument();
    expect(screen.getByText('PostgreSQL Database')).toBeInTheDocument();
    expect(screen.getByText('Redis Cache')).toBeInTheDocument();
  });
  
  it('deletes a resource when delete button is clicked', async () => {
    const { deleteResource } = require('../../lib/api/resources');
    const { toast } = require('../../components/ui/use-toast');
    
    render(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>
    );
    
    // Wait for resources to load
    await waitFor(() => {
      expect(screen.getByText('MySQL Database')).toBeInTheDocument();
    });
    
    // Find and click delete button for the first resource
    const deleteButtons = screen.getAllByTitle('删除资源');
    fireEvent.click(deleteButtons[0]);
    
    // Confirmation dialog should be shown
    expect(window.confirm).toHaveBeenCalled();
    
    // After confirmation, the delete API should be called
    await waitFor(() => {
      expect(deleteResource).toHaveBeenCalledWith('resource-1');
      expect(toast).toHaveBeenCalledWith(expect.objectContaining({
        title: '资源已删除'
      }));
    });
  });
  
  it('navigates to resource detail page when row is clicked', async () => {
    const mockNavigate = jest.fn();
    jest.mock('react-router-dom', () => ({
      ...jest.requireActual('react-router-dom'),
      useNavigate: () => mockNavigate
    }));
    
    render(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>
    );
    
    // Wait for resources to load
    await waitFor(() => {
      expect(screen.getByText('MySQL Database')).toBeInTheDocument();
    });
    
    // Click on the first resource row
    fireEvent.click(screen.getByText('MySQL Database'));
    
    // Should navigate to the resource detail page
    expect(mockedNavigate).toHaveBeenCalledWith('/resources/resource-1');
  });
  
  it('refreshes the resource list when refresh button is clicked', async () => {
    const { getResourceList } = require('../../lib/api/resources');
    
    render(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>
    );
    
    // Wait for initial resource load
    await waitFor(() => {
      expect(screen.getByText('MySQL Database')).toBeInTheDocument();
    });
    
    // getResourceList should have been called once for initial load
    expect(getResourceList).toHaveBeenCalledTimes(1);
    
    // Click refresh button
    fireEvent.click(screen.getByText('刷新'));
    
    // getResourceList should have been called again
    expect(getResourceList).toHaveBeenCalledTimes(2);
  });
  
  it('navigates to create resource page when new button is clicked', () => {
    const mockNavigate = jest.fn();
    jest.mock('react-router-dom', () => ({
      ...jest.requireActual('react-router-dom'),
      useNavigate: () => mockNavigate
    }));
    
    render(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>
    );
    
    // Click new resource button
    fireEvent.click(screen.getByText('新建资源'));
    
    // Should navigate to the create resource page
    expect(mockedNavigate).toHaveBeenCalledWith('/resources/create');
  });
  
  it('handles errors during resource list loading', async () => {
    const { getResourceList } = require('../../lib/api/resources');
    const { toast } = require('../../components/ui/use-toast');
    
    // Mock API error
    getResourceList.mockRejectedValueOnce(new Error('Failed to load'));
    
    render(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>
    );
    
    // Error toast should be shown
    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith(expect.objectContaining({
        title: '获取资源列表失败',
        variant: 'destructive'
      }));
    });
  });
}); 