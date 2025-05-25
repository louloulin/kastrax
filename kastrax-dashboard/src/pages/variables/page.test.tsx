import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import VariablesPage from './page';
import { getVariableList, createVariable, updateVariable, deleteVariable } from '@/lib/api/variables';

// Mock API函数
jest.mock('@/lib/api/variables', () => ({
  getVariableList: jest.fn(),
  createVariable: jest.fn(),
  updateVariable: jest.fn(),
  deleteVariable: jest.fn(),
}));

// Mock组件
jest.mock('@/components/supabase-sidebar', () => ({
  __esModule: true,
  default: ({ collapsed, setCollapsed }: { collapsed: boolean; setCollapsed: (collapsed: boolean) => void }) => <div data-testid="sidebar">Sidebar</div>
}));

jest.mock('@/components/top-navigation', () => ({
  __esModule: true,
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="top-navigation">
      {children}
    </div>
  )
}));

jest.mock('@/components/variables/variable-dialog', () => ({
  __esModule: true,
  default: ({ open, onOpenChange, onSuccess, variable }: any) => {
    if (open) {
      return (
        <div data-testid="variable-dialog">
          {variable ? '编辑变量' : '创建变量'}
          <button onClick={() => onOpenChange(false)}>关闭</button>
          <button 
            data-testid="save-variable"
            onClick={() => {
              if (onSuccess) onSuccess();
              onOpenChange(false);
            }}
          >
            保存
          </button>
        </div>
      );
    }
    return null;
  }
}));

jest.mock('@/components/variables/variable-delete-dialog', () => ({
  __esModule: true,
  default: ({ open, onOpenChange, onSuccess, variable }: any) => {
    if (open) {
      return (
        <div data-testid="delete-dialog">
          确认删除 {variable?.variableName}
          <button onClick={() => onOpenChange(false)}>取消</button>
          <button 
            data-testid="confirm-delete"
            onClick={() => {
              if (onSuccess) onSuccess();
              onOpenChange(false);
            }}
          >
            确认
          </button>
        </div>
      );
    }
    return null;
  }
}));

// Mock DataTable组件
jest.mock('@/components/data-table', () => ({
  __esModule: true,
  default: ({ columns, data, isLoading, actions }: any) => {
    return (
      <div data-testid="data-table">
        <table>
          <thead>
            <tr>
              {columns.map((col: any) => (
                <th key={col.id}>{col.header}</th>
              ))}
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {data.map((row: any, index: number) => (
              <tr key={index} data-testid={`variable-row-${index}`}>
                {columns.map((col: any) => {
                  if (col.cell && typeof col.cell === 'function') {
                    return (
                      <td key={col.id}>
                        {col.cell({ row: { original: row } })}
                      </td>
                    );
                  }
                  return <td key={col.id}>{row[col.accessorKey]}</td>;
                })}
                <td>{actions(row)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {isLoading && <div data-testid="loading-indicator">加载中...</div>}
        {data.length === 0 && !isLoading && (
          <div data-testid="empty-state">无数据</div>
        )}
      </div>
    );
  }
}));

// 模拟数据
const mockVariables = [
  {
    variableId: '1',
    variableName: 'TEST_API_KEY',
    variableValue: 'abc123',
    description: '测试API密钥',
    createTime: '2023-01-01T00:00:00Z',
    updateTime: '2023-01-01T00:00:00Z',
  },
  {
    variableId: '2',
    variableName: 'DATABASE_URL',
    variableValue: 'mysql://user:pass@localhost:3306/db',
    description: '数据库连接URL',
    createTime: '2023-01-02T00:00:00Z',
    updateTime: '2023-01-02T00:00:00Z',
  },
];

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: jest.fn(),
  },
});

describe('变量管理页面测试', () => {
  beforeEach(() => {
    (getVariableList as jest.Mock).mockResolvedValue({
      data: {
        data: mockVariables,
        success: true,
        code: 200,
        msg: 'success',
      },
    });
    
    (createVariable as jest.Mock).mockResolvedValue({
      data: {
        data: { variableId: '3' },
        success: true,
        code: 200,
        msg: 'success',
      },
    });
    
    (updateVariable as jest.Mock).mockResolvedValue({
      data: {
        data: true,
        success: true,
        code: 200,
        msg: 'success',
      },
    });
    
    (deleteVariable as jest.Mock).mockResolvedValue({
      data: {
        data: true,
        success: true,
        code: 200,
        msg: 'success',
      },
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  test('渲染页面并加载变量列表', async () => {
    render(<VariablesPage />);

    // 检查页面标题和组件
    expect(screen.getByText('变量管理')).toBeInTheDocument();
    expect(screen.getByTestId('sidebar')).toBeInTheDocument();
    expect(screen.getByTestId('top-navigation')).toBeInTheDocument();
    expect(screen.getByTestId('data-table')).toBeInTheDocument();

    // 验证API调用
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(1);
    });

    // 检查表格内容
    expect(screen.getByText('TEST_API_KEY')).toBeInTheDocument();
    expect(screen.getByText('DATABASE_URL')).toBeInTheDocument();
    expect(screen.getByText('测试API密钥')).toBeInTheDocument();
    expect(screen.getByText('数据库连接URL')).toBeInTheDocument();
  });

  test.skip('搜索变量', async () => {
    render(<VariablesPage />);

    // 等待数据加载
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(1);
    });

    // 输入搜索内容
    const searchInput = screen.getByPlaceholderText('搜索变量名称或描述...');
    fireEvent.change(searchInput, { target: { value: 'API' } });

    // 检查筛选后的表格内容
    expect(screen.getByText('TEST_API_KEY')).toBeInTheDocument();
    expect(screen.queryByText('DATABASE_URL')).not.toBeInTheDocument();
  });

  test('打开新建变量对话框', async () => {
    render(<VariablesPage />);

    // 等待数据加载
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(1);
    });

    // 点击新建按钮
    const createButton = screen.getByText('新建变量');
    fireEvent.click(createButton);

    // 检查对话框是否打开
    expect(screen.getByTestId('variable-dialog')).toBeInTheDocument();
    expect(screen.getByText('创建变量')).toBeInTheDocument();
  });

  test('编辑变量', async () => {
    render(<VariablesPage />);

    // 等待数据加载
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(1);
    });

    // 找到编辑按钮并点击
    const editButtons = screen.getAllByTitle('编辑变量');
    fireEvent.click(editButtons[0]);

    // 检查编辑对话框是否打开
    expect(screen.getByTestId('variable-dialog')).toBeInTheDocument();
    expect(screen.getByText('编辑变量')).toBeInTheDocument();

    // 点击保存按钮
    fireEvent.click(screen.getByTestId('save-variable'));

    // 验证API调用和对话框关闭
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(2);
    });
  });

  test('删除变量', async () => {
    render(<VariablesPage />);

    // 等待数据加载
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(1);
    });

    // 找到删除按钮并点击
    const deleteButtons = screen.getAllByTitle('删除变量');
    fireEvent.click(deleteButtons[0]);

    // 检查删除确认对话框是否打开
    expect(screen.getByTestId('delete-dialog')).toBeInTheDocument();
    expect(screen.getByText(/确认删除/)).toBeInTheDocument();

    // 点击确认按钮
    fireEvent.click(screen.getByTestId('confirm-delete'));

    // 验证API调用和对话框关闭
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(2);
    });
  });

  test('复制变量值', async () => {
    render(<VariablesPage />);

    // 等待数据加载
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(1);
    });

    // 找到复制按钮并点击
    const copyButtons = screen.getAllByTitle('复制变量值');
    fireEvent.click(copyButtons[0]);

    // 验证剪贴板API调用
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('abc123');
  });

  test('点击刷新按钮重新加载变量列表', async () => {
    render(<VariablesPage />);

    // 等待初始数据加载
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(1);
    });

    // 找到刷新按钮并点击
    const refreshButton = screen.getByText('刷新');
    fireEvent.click(refreshButton);

    // 验证API再次调用
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(2);
    });
  });

  test('切换变量值显示', async () => {
    render(<VariablesPage />);

    // 等待数据加载
    await waitFor(() => {
      expect(getVariableList).toHaveBeenCalledTimes(1);
    });

    // 初始状态应该是隐藏的
    expect(screen.getAllByText('••••••••').length).toBe(2);

    // 找到第一行的显示按钮并点击
    const showButtons = screen.getAllByTitle('显示值');
    fireEvent.click(showButtons[0]);

    // 验证第一个值显示了，第二个还是隐藏的
    expect(screen.getByText('abc123')).toBeInTheDocument();
    expect(screen.getByText('••••••••')).toBeInTheDocument();
  });
}); 