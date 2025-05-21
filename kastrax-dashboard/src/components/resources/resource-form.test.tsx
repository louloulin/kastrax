import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import ResourceForm from './resource-form';
import { getResourceTypes } from '../../lib/api/resources';

// Mock the API
jest.mock('../../lib/api/resources', () => ({
  getResourceTypes: jest.fn().mockResolvedValue({
    data: {
      data: ['mysql', 'postgresql', 'mongodb', 'redis', 'http']
    }
  })
}));

describe('ResourceForm', () => {
  it('renders the form with basic fields', () => {
    render(<ResourceForm />);
    
    // Basic fields should be present
    expect(screen.getByLabelText('资源名称')).toBeInTheDocument();
    expect(screen.getByRole('combobox')).toBeInTheDocument();
  });
  
  it('loads resource types from API', async () => {
    render(<ResourceForm />);
    
    await waitFor(() => {
      // 确保下拉菜单已加载
      const selectElement = screen.getByRole('combobox');
      expect(selectElement).toBeInTheDocument();
    });
    
    expect(getResourceTypes).toHaveBeenCalled();
  });
  
  it('renders MySQL specific form fields', async () => {
    render(
      <ResourceForm 
        initialData={{
          resourceName: 'Test MySQL',
          resourceType: 'mysql',
          properties: {
            host: 'localhost',
            port: '3306',
            database: 'testdb',
            username: 'root',
            password: 'password'
          }
        }}
      />
    );
    
    // MySQL form fields should be visible
    await waitFor(() => {
      expect(screen.getByLabelText('主机地址')).toBeInTheDocument();
      expect(screen.getByLabelText('端口')).toBeInTheDocument();
      expect(screen.getByLabelText('数据库名')).toBeInTheDocument();
      expect(screen.getByLabelText('用户名')).toBeInTheDocument();
      expect(screen.getByLabelText('密码')).toBeInTheDocument();
    });
  });
  
  it('calls onChange handler when form values change', async () => {
    const onChange = jest.fn();
    
    render(
      <ResourceForm 
        onChange={onChange}
        initialData={{
          resourceName: 'Test Resource',
          resourceType: 'mysql',
          properties: {
            host: 'localhost',
            port: '3306',
            database: 'testdb',
            username: 'root',
            password: 'password'
          }
        }}
      />
    );
    
    // Wait for form to load
    await waitFor(() => {
      expect(screen.getByLabelText('资源名称')).toBeInTheDocument();
    });
    
    // Change resource name
    const nameInput = screen.getByLabelText('资源名称');
    await act(async () => {
      fireEvent.change(nameInput, { target: { value: 'Test DB' } });
    });
    
    // onChange should be called with updated value
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      resourceName: 'Test DB'
    }));
  });
  
  it('renders form in readonly mode', async () => {
    render(
      <ResourceForm 
        readOnly={true}
        initialData={{
          resourceName: 'Test DB',
          resourceType: 'mysql',
          properties: {
            host: 'localhost',
            port: '3306',
            database: 'testdb',
            username: 'root',
            password: 'password'
          }
        }}
      />
    );
    
    // All inputs should be disabled
    await waitFor(() => {
      const inputs = screen.getAllByRole('textbox');
      inputs.forEach(input => {
        expect(input).toBeDisabled();
      });
      
      // Password field uses type="password"
      const passwordInput = screen.getByLabelText('密码');
      expect(passwordInput).toBeDisabled();
    });
  });
}); 