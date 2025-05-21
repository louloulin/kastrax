import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import DataVisualizationPage from './page';
import { BrowserRouter } from 'react-router-dom';

// Mock the components and recharts
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

// Mock recharts components
jest.mock('recharts', () => {
  const OriginalModule = jest.requireActual('recharts');
  
  return {
    ...OriginalModule,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div data-testid="mock-responsive-container">{children}</div>
    ),
    BarChart: ({ children }: { children: React.ReactNode }) => (
      <div data-testid="mock-bar-chart">{children}</div>
    ),
    LineChart: ({ children }: { children: React.ReactNode }) => (
      <div data-testid="mock-line-chart">{children}</div>
    ),
    PieChart: ({ children }: { children: React.ReactNode }) => (
      <div data-testid="mock-pie-chart">{children}</div>
    ),
    Pie: (props: any) => <div data-testid="mock-pie">Pie Chart</div>,
    Bar: (props: any) => <div data-testid="mock-bar">Bar</div>,
    Line: (props: any) => <div data-testid="mock-line">Line</div>,
    Cell: (props: any) => <div data-testid="mock-cell">Cell</div>,
    Legend: () => <div data-testid="mock-legend">Legend</div>,
    Tooltip: () => <div data-testid="mock-tooltip">Tooltip</div>,
    CartesianGrid: () => <div data-testid="mock-grid">Grid</div>,
    XAxis: () => <div data-testid="mock-xaxis">XAxis</div>,
    YAxis: () => <div data-testid="mock-yaxis">YAxis</div>,
  };
});

describe('DataVisualizationPage', () => {
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

  it('renders the page with chart type tabs', () => {
    renderWithRouter(<DataVisualizationPage />);
    
    // Check if the page title is rendered
    expect(screen.getByText('数据可视化')).toBeInTheDocument();
    
    // Check if chart type tabs are rendered
    expect(screen.getByText('柱状图')).toBeInTheDocument();
    expect(screen.getByText('折线图')).toBeInTheDocument();
    expect(screen.getByText('饼图')).toBeInTheDocument();
  });

  it('validates form before generating chart', async () => {
    renderWithRouter(<DataVisualizationPage />);
    
    // Try to generate chart without filling the form
    const generateButton = screen.getByText('生成图表');
    fireEvent.click(generateButton);
    
    // Should show an error message
    await waitFor(() => {
      expect(screen.getByText('请选择数据库')).toBeInTheDocument();
    });
  });

  it('switches between chart types', () => {
    renderWithRouter(<DataVisualizationPage />);
    
    // Default should be bar chart
    expect(screen.getByText('数据柱状图')).toBeInTheDocument();
    
    // Switch to line chart
    fireEvent.click(screen.getByText('折线图'));
    expect(screen.getByText('数据折线图')).toBeInTheDocument();
    
    // Switch to pie chart
    fireEvent.click(screen.getByText('饼图'));
    expect(screen.getByText('数据饼图')).toBeInTheDocument();
  });

  it('fills the form and attempts to generate chart', async () => {
    renderWithRouter(<DataVisualizationPage />);
    
    // Fill out the form
    fireEvent.change(screen.getByPlaceholderText('例如: date, category'), {
      target: { value: 'date' }
    });
    
    fireEvent.change(screen.getByPlaceholderText('例如: count, amount'), {
      target: { value: 'count' }
    });
    
    // Select database
    fireEvent.click(screen.getByText('选择数据库'));
    fireEvent.click(screen.getByText('主数据库'));
    
    // Enter table name
    fireEvent.change(screen.getByPlaceholderText('输入表名'), {
      target: { value: 'users' }
    });
    
    // Try to generate chart
    const generateButton = screen.getByText('生成图表');
    fireEvent.click(generateButton);
    
    // Should show loading state
    expect(screen.getByText('生成中...')).toBeInTheDocument();
    
    // After loading, should show chart
    await waitFor(() => {
      expect(screen.queryByText('生成中...')).not.toBeInTheDocument();
    }, { timeout: 2000 });
  });
}); 