import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import DataImportExportPage from './page';
import { BrowserRouter } from 'react-router-dom';

// Mock the API functions
jest.mock('../../components/supabase-sidebar', () => {
  return {
    __esModule: true,
    default: () => <div data-testid="mock-sidebar">Sidebar</div>
  };
});

jest.mock('../../components/top-navigation', () => {
  return {
    __esModule: true,
    default: ({ children }: { children: React.ReactNode }) => (
      <div data-testid="mock-navigation">
        Navigation
        {children}
      </div>
    )
  };
});

describe('DataImportExportPage', () => {
  beforeEach(() => {
    // Reset any mocks before each test
    jest.clearAllMocks();
  });

  const renderWithRouter = (ui: React.ReactElement) => {
    return render(
      <BrowserRouter>
        {ui}
      </BrowserRouter>
    );
  };

  it('renders the page with import and export tabs', () => {
    renderWithRouter(<DataImportExportPage />);
    
    // Check if the page title is rendered
    expect(screen.getByText('数据导入导出')).toBeInTheDocument();
    
    // Check if both tabs are rendered
    expect(screen.getByText('导入数据')).toBeInTheDocument();
    expect(screen.getByText('导出数据')).toBeInTheDocument();
  });

  it('switches between import and export tabs', () => {
    renderWithRouter(<DataImportExportPage />);
    
    // By default, it should show the import tab
    expect(screen.getByText('将CSV或其他格式的数据导入到数据库表中')).toBeInTheDocument();
    
    // Click on the export tab
    fireEvent.click(screen.getByText('导出数据'));
    
    // Now it should show the export tab content
    expect(screen.getByText('将数据库表导出为CSV或其他格式')).toBeInTheDocument();
  });

  it('validates import form before submission', async () => {
    renderWithRouter(<DataImportExportPage />);
    
    // Try to import without selecting a file
    const importButton = screen.getByText('开始导入');
    fireEvent.click(importButton);
    
    // Should show an error message
    await waitFor(() => {
      expect(screen.getByText('请选择要导入的文件')).toBeInTheDocument();
    });
  });

  it('validates export form before submission', async () => {
    renderWithRouter(<DataImportExportPage />);
    
    // Switch to export tab
    fireEvent.click(screen.getByText('导出数据'));
    
    // Try to export without selecting database and table
    const exportButton = screen.getByText('开始导出');
    fireEvent.click(exportButton);
    
    // Should show an error message
    await waitFor(() => {
      expect(screen.getByText('请选择源数据库')).toBeInTheDocument();
    });
  });
}); 