import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import DataTable, { Column } from './data-table';

// Mock the Lucide icons
jest.mock('lucide-react', () => ({
  Plus: () => <div data-testid="plus-icon">Plus</div>,
  Filter: () => <div data-testid="filter-icon">Filter</div>,
  MoreHorizontal: () => <div data-testid="more-icon">More</div>,
  Edit: () => <div data-testid="edit-icon">Edit</div>,
  Trash: () => <div data-testid="trash-icon">Trash</div>,
  ExternalLink: () => <div data-testid="external-link-icon">ExternalLink</div>,
  Loader2: () => <div data-testid="loader-icon">Loader</div>,
}));

describe('DataTable', () => {
  // Sample data for tests
  const columns: Column[] = [
    { id: 'name', header: '名称', accessorKey: 'name' },
    { id: 'type', header: '类型', accessorKey: 'type' },
    { id: 'status', header: '状态', accessorKey: 'status' },
    { 
      id: 'custom', 
      header: '自定义', 
      accessorKey: 'custom',
      cell: ({ row }) => <span data-testid="custom-cell">{row.custom.toUpperCase()}</span>
    },
  ];

  const data = [
    { id: '1', name: '测试1', type: '类型A', status: '活跃', custom: 'custom1' },
    { id: '2', name: '测试2', type: '类型B', status: '非活跃', custom: 'custom2' },
  ];

  it('renders the table with title and description', () => {
    render(
      <DataTable 
        title="测试表格" 
        description="这是一个测试表格描述" 
        columns={columns} 
        data={data} 
      />
    );

    // Check title and description
    expect(screen.getByText('测试表格')).toBeInTheDocument();
    expect(screen.getByText('这是一个测试表格描述')).toBeInTheDocument();
    
    // Check column headers
    expect(screen.getByText('名称')).toBeInTheDocument();
    expect(screen.getByText('类型')).toBeInTheDocument();
    expect(screen.getByText('状态')).toBeInTheDocument();
    expect(screen.getByText('自定义')).toBeInTheDocument();
    
    // Check data rows
    expect(screen.getByText('测试1')).toBeInTheDocument();
    expect(screen.getByText('类型A')).toBeInTheDocument();
    expect(screen.getByText('活跃')).toBeInTheDocument();
    
    expect(screen.getByText('测试2')).toBeInTheDocument();
    expect(screen.getByText('类型B')).toBeInTheDocument();
    expect(screen.getByText('非活跃')).toBeInTheDocument();

    // Check custom cell renderer
    const customCells = screen.getAllByTestId('custom-cell');
    expect(customCells).toHaveLength(2);
    expect(customCells[0]).toHaveTextContent('CUSTOM1');
    expect(customCells[1]).toHaveTextContent('CUSTOM2');
  });

  it('displays loading state correctly', () => {
    render(
      <DataTable 
        columns={columns} 
        data={data} 
        isLoading={true}
      />
    );

    // Check if loading indicator is displayed
    expect(screen.getByTestId('loader-icon')).toBeInTheDocument();
    expect(screen.getByText('加载中...')).toBeInTheDocument();
    
    // Data should not be rendered when loading
    expect(screen.queryByText('测试1')).not.toBeInTheDocument();
  });

  it('displays empty state when no data is provided', () => {
    render(
      <DataTable 
        columns={columns} 
        data={[]} 
      />
    );

    // Check for empty state message
    expect(screen.getByText('没有找到数据')).toBeInTheDocument();
  });

  it('handles row selection correctly', () => {
    render(
      <DataTable 
        columns={columns} 
        data={data} 
      />
    );

    // Get all checkboxes
    const checkboxes = screen.getAllByRole('checkbox');
    // First checkbox is the "select all" checkbox, followed by row checkboxes
    const selectAllCheckbox = checkboxes[0];
    const firstRowCheckbox = checkboxes[1];
    const secondRowCheckbox = checkboxes[2];

    // Initially, no checkboxes should be checked
    expect(selectAllCheckbox).not.toBeChecked();
    expect(firstRowCheckbox).not.toBeChecked();
    expect(secondRowCheckbox).not.toBeChecked();

    // Select the first row
    fireEvent.click(firstRowCheckbox);
    expect(firstRowCheckbox).toBeChecked();
    expect(secondRowCheckbox).not.toBeChecked();
    expect(selectAllCheckbox).not.toBeChecked(); // Not all rows are selected

    // Select the second row
    fireEvent.click(secondRowCheckbox);
    expect(firstRowCheckbox).toBeChecked();
    expect(secondRowCheckbox).toBeChecked();
    expect(selectAllCheckbox).toBeChecked(); // All rows are now selected

    // Deselect the first row
    fireEvent.click(firstRowCheckbox);
    expect(firstRowCheckbox).not.toBeChecked();
    expect(secondRowCheckbox).toBeChecked();
    expect(selectAllCheckbox).not.toBeChecked(); // Not all rows are selected again

    // Use "select all" checkbox to select all rows
    fireEvent.click(selectAllCheckbox);
    expect(selectAllCheckbox).toBeChecked();
    expect(firstRowCheckbox).toBeChecked();
    expect(secondRowCheckbox).toBeChecked();

    // Use "select all" checkbox to deselect all rows
    fireEvent.click(selectAllCheckbox);
    expect(selectAllCheckbox).not.toBeChecked();
    expect(firstRowCheckbox).not.toBeChecked();
    expect(secondRowCheckbox).not.toBeChecked();
  });

  it('renders custom action buttons when provided', () => {
    const customActions = (row: any) => (
      <button data-testid={`custom-action-${row.id}`}>Custom {row.name}</button>
    );

    render(
      <DataTable 
        columns={columns} 
        data={data} 
        actions={customActions}
      />
    );

    // Check if custom action buttons are rendered
    expect(screen.getByTestId('custom-action-1')).toBeInTheDocument();
    expect(screen.getByTestId('custom-action-2')).toBeInTheDocument();
    expect(screen.getByText('Custom 测试1')).toBeInTheDocument();
    expect(screen.getByText('Custom 测试2')).toBeInTheDocument();

    // Default action buttons should not be present
    expect(screen.queryByTestId('edit-icon')).not.toBeInTheDocument();
    expect(screen.queryByTestId('trash-icon')).not.toBeInTheDocument();
  });

  it('renders default action buttons when actions prop is not provided', () => {
    render(
      <DataTable 
        columns={columns} 
        data={data} 
      />
    );

    // Check if default action buttons are rendered
    const editButtons = screen.getAllByTestId('edit-icon');
    const trashButtons = screen.getAllByTestId('trash-icon');
    const moreButtons = screen.getAllByTestId('more-icon');
    
    // Should have one set of buttons for each row
    expect(editButtons).toHaveLength(2);
    expect(trashButtons).toHaveLength(2);
    expect(moreButtons).toHaveLength(2);
  });

  it('displays pagination info correctly', () => {
    render(
      <DataTable 
        columns={columns} 
        data={data} 
      />
    );

    // Check pagination info
    expect(screen.getByText('显示 2 条中的 1-2 条')).toBeInTheDocument();
    expect(screen.getByText('第 1 页')).toBeInTheDocument();
    
    // Pagination buttons should be disabled for this simple test
    const prevButton = screen.getByText('上一页');
    const nextButton = screen.getByText('下一页');
    expect(prevButton).toBeDisabled();
    expect(nextButton).toBeDisabled();
  });
}); 