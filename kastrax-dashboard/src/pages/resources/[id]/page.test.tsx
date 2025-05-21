import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import ResourceDetailPage from './page';
import { 
  getResourceById, 
  updateResource, 
  deleteResource, 
  testResourceConnection 
} from '../../../lib/api/resources';

// Mock the navigation hooks
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useParams: () => ({ id: 'test-resource-id' })
}));

// Mock the API
jest.mock('../../../lib/api/resources', () => ({
  getResourceById: jest.fn(),
  updateResource: jest.fn(),
  deleteResource: jest.fn(),
  testResourceConnection: jest.fn()
}));

// Mock the resource form component
jest.mock('../../../components/resources/resource-form', () => {
  return jest.fn(props => (
    <div data-testid="resource-form">
      <button 
        data-testid="update-form"
        onClick={() => props.onChange && props.onChange({ 
          resourceName: 'Updated Resource',
          resourceType: 'mysql' 
        })}
      >
        Update Form
      </button>
      {props.initialData && (
        <div data-testid="resource-data">
          Resource: {props.initialData.resourceName}
        </div>
      )}
    </div>
  ));
});

// Mock layout components - we already created these files
jest.mock('../../../components/layout/top-navigation', () => {
  return function TopNavigation() {
    return <div data-testid="top-navigation">Top Navigation</div>;
  };
});

jest.mock('../../../components/layout/supabase-sidebar', () => {
  return function SupabaseSidebar() {
    return <div data-testid="supabase-sidebar">Sidebar</div>;
  };
});

// Create a mock navigate function
const mockNavigate = jest.fn();

describe('ResourceDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows loading state initially', () => {
    // Setup API mock to delay response
    (getResourceById as jest.Mock).mockImplementationOnce(() => new Promise(resolve => setTimeout(resolve, 500)));
    
    render(
      <BrowserRouter>
        <ResourceDetailPage />
      </BrowserRouter>
    );
    
    expect(screen.getByText('加载中...')).toBeInTheDocument();
  });

  it('loads resource details', async () => {
    // Setup API mock to return resource data
    (getResourceById as jest.Mock).mockResolvedValueOnce({
      data: {
        data: {
          resourceId: 'test-resource-id',
          resourceName: 'Test MySQL DB',
          resourceType: 'mysql',
          properties: {
            host: 'localhost',
            port: '3306'
          }
        }
      }
    });
    
    render(
      <BrowserRouter>
        <ResourceDetailPage />
      </BrowserRouter>
    );
    
    // Wait for resource to load
    await waitFor(() => {
      expect(screen.getByTestId('resource-data')).toBeInTheDocument();
      expect(screen.getByText('Resource: Test MySQL DB')).toBeInTheDocument();
    });
  });

  it('tests resource connection', async () => {
    // Setup API mocks
    (getResourceById as jest.Mock).mockResolvedValueOnce({
      data: {
        data: {
          resourceId: 'test-resource-id',
          resourceName: 'Test MySQL DB',
          resourceType: 'mysql'
        }
      }
    });
    
    (testResourceConnection as jest.Mock).mockResolvedValueOnce({
      data: {
        data: {
          success: true,
          message: '连接成功'
        }
      }
    });
    
    render(
      <BrowserRouter>
        <ResourceDetailPage />
      </BrowserRouter>
    );
    
    // Wait for resource to load
    await waitFor(() => {
      expect(screen.getByTestId('resource-data')).toBeInTheDocument();
    });
    
    // Find and click the test connection button
    const testButton = screen.getByText('测试连接');
    fireEvent.click(testButton);
    
    // Wait for success message
    await waitFor(() => {
      expect(screen.getByText('连接成功')).toBeInTheDocument();
    });
  });

  it('saves resource changes', async () => {
    // Setup API mocks
    (getResourceById as jest.Mock).mockResolvedValueOnce({
      data: {
        data: {
          resourceId: 'test-resource-id',
          resourceName: 'Test MySQL DB',
          resourceType: 'mysql'
        }
      }
    });
    
    (updateResource as jest.Mock).mockResolvedValueOnce({
      data: {
        success: true
      }
    });
    
    render(
      <BrowserRouter>
        <ResourceDetailPage />
      </BrowserRouter>
    );
    
    // Wait for resource to load
    await waitFor(() => {
      expect(screen.getByTestId('resource-data')).toBeInTheDocument();
    });
    
    // Update the form
    fireEvent.click(screen.getByTestId('update-form'));
    
    // Save the changes
    fireEvent.click(screen.getByText('保存'));
    
    // Wait for API call to complete
    await waitFor(() => {
      expect(updateResource).toHaveBeenCalledWith('test-resource-id', {
        resourceName: 'Updated Resource',
        resourceType: 'mysql'
      });
      expect(screen.getByText('保存成功')).toBeInTheDocument();
    });
  });

  it('deletes resource and redirects', async () => {
    // Setup API mocks
    (getResourceById as jest.Mock).mockResolvedValueOnce({
      data: {
        data: {
          resourceId: 'test-resource-id',
          resourceName: 'Test MySQL DB',
          resourceType: 'mysql'
        }
      }
    });
    
    (deleteResource as jest.Mock).mockResolvedValueOnce({
      data: { success: true }
    });
    
    // Mock confirm to return true
    window.confirm = jest.fn().mockImplementation(() => true);
    
    render(
      <BrowserRouter>
        <ResourceDetailPage />
      </BrowserRouter>
    );
    
    // Wait for resource to load
    await waitFor(() => {
      expect(screen.getByTestId('resource-data')).toBeInTheDocument();
    });
    
    // Click delete button
    fireEvent.click(screen.getByText('删除'));
    
    // Expect confirm dialog to be shown
    expect(window.confirm).toHaveBeenCalled();
    
    // Wait for API call and navigation
    await waitFor(() => {
      expect(deleteResource).toHaveBeenCalledWith('test-resource-id');
      expect(mockNavigate).toHaveBeenCalledWith('/resources');
    });
  });

  it('changes active tab', async () => {
    // Setup API mock
    (getResourceById as jest.Mock).mockResolvedValueOnce({
      data: {
        data: {
          resourceId: 'test-resource-id',
          resourceName: 'Test MySQL DB',
          resourceType: 'mysql'
        }
      }
    });
    
    render(
      <BrowserRouter>
        <ResourceDetailPage />
      </BrowserRouter>
    );
    
    // Wait for resource to load
    await waitFor(() => {
      expect(screen.getByTestId('resource-data')).toBeInTheDocument();
    });
    
    // Detail tab should be active by default
    expect(screen.getByText('详情').closest('button')).toHaveClass('bg-gray-50');
    
    // Click monitoring tab
    fireEvent.click(screen.getByText('监控'));
    
    // Monitoring tab should now be active
    expect(screen.getByText('监控').closest('button')).toHaveClass('bg-gray-50');
    
    // Click back to detail tab
    fireEvent.click(screen.getByText('详情'));
    
    // Detail tab should be active again
    expect(screen.getByText('详情').closest('button')).toHaveClass('bg-gray-50');
  });
}); 