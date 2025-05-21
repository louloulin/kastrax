import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import VariableForm, { VariableFormValues } from './variable-form';

// Mock响应函数
const mockOnSubmit = jest.fn();
const mockOnCancel = jest.fn();

describe('变量表单组件测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('渲染空表单', () => {
    render(
      <VariableForm 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    // 验证表单元素存在
    expect(screen.getByLabelText(/变量名称/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/变量值/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/描述/i)).toBeInTheDocument();
    
    // 验证按钮存在
    expect(screen.getByRole('button', { name: /保存/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /取消/i })).toBeInTheDocument();
  });

  test('渲染带默认值的表单', () => {
    const defaultValues: Partial<VariableFormValues> = {
      variableName: 'TEST_VAR',
      variableValue: 'test123',
      description: '测试变量',
    };

    render(
      <VariableForm 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel}
        defaultValues={defaultValues}
      />
    );

    // 验证默认值已填充
    expect(screen.getByDisplayValue('TEST_VAR')).toBeInTheDocument();
    expect(screen.getByDisplayValue('test123')).toBeInTheDocument();
    expect(screen.getByDisplayValue('测试变量')).toBeInTheDocument();
    
    // 变量名称字段应被禁用（编辑模式）
    expect(screen.getByDisplayValue('TEST_VAR')).toBeDisabled();
  });

  test('表单提交验证', async () => {
    render(
      <VariableForm 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    // 不填写必填字段直接提交
    fireEvent.click(screen.getByRole('button', { name: /保存/i }));
    
    // 验证错误消息
    await waitFor(() => {
      expect(screen.getByText(/变量名称不能为空/i)).toBeInTheDocument();
      expect(screen.getByText(/变量值不能为空/i)).toBeInTheDocument();
    });
    
    // 验证未调用提交函数
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  test('填写有效数据并提交', async () => {
    render(
      <VariableForm 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    // 填写表单
    fireEvent.change(screen.getByLabelText(/变量名称/i), { target: { value: 'VALID_NAME' } });
    fireEvent.change(screen.getByLabelText(/变量值/i), { target: { value: 'valid-value-123' } });
    fireEvent.change(screen.getByLabelText(/描述/i), { target: { value: '这是一个有效的描述' } });
    
    // 提交表单
    fireEvent.click(screen.getByRole('button', { name: /保存/i }));
    
    // 验证提交函数被调用，且参数正确
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith({
        variableName: 'VALID_NAME',
        variableValue: 'valid-value-123',
        description: '这是一个有效的描述',
      }, expect.anything());
    });
  });

  test('点击取消按钮', () => {
    render(
      <VariableForm 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    // 点击取消按钮
    fireEvent.click(screen.getByRole('button', { name: /取消/i }));
    
    // 验证取消函数被调用
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  test('变量名称格式验证', async () => {
    render(
      <VariableForm 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel} 
      />
    );

    // 填写无效的变量名（包含特殊字符）
    fireEvent.change(screen.getByLabelText(/变量名称/i), { target: { value: 'INVALID-NAME!' } });
    fireEvent.change(screen.getByLabelText(/变量值/i), { target: { value: 'some-value' } });
    
    // 提交表单
    fireEvent.click(screen.getByRole('button', { name: /保存/i }));
    
    // 验证显示错误消息
    await waitFor(() => {
      expect(screen.getByText(/变量名称只能包含字母、数字和下划线/i)).toBeInTheDocument();
    });
    
    // 验证未调用提交函数
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  test('切换变量值显示/隐藏', () => {
    render(
      <VariableForm 
        onSubmit={mockOnSubmit} 
        onCancel={mockOnCancel}
        defaultValues={{ variableValue: 'secret-value' }}
      />
    );

    // 初始状态为密码字段（隐藏）
    const valueInput = screen.getByDisplayValue('secret-value');
    expect(valueInput).toHaveAttribute('type', 'password');
    
    // 点击显示按钮
    fireEvent.click(screen.getByRole('button', { name: '' })); // 眼睛图标按钮
    
    // 验证切换为文本字段（显示）
    expect(valueInput).toHaveAttribute('type', 'text');
    
    // 再次点击隐藏按钮
    fireEvent.click(screen.getByRole('button', { name: '' })); // 眼睛图标按钮
    
    // 验证切换回密码字段（隐藏）
    expect(valueInput).toHaveAttribute('type', 'password');
  });
}); 