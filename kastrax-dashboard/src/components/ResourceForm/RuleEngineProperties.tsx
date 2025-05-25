import React, { useEffect } from 'react';
import { Form, Input, Switch, Select, Button, InputNumber } from 'antd';
import { ResourceFormProps } from './index';

const { TextArea } = Input;

interface RuleEnginePropertiesProps extends Omit<ResourceFormProps, 'resourceType'> {}

const RuleEngineProperties: React.FC<RuleEnginePropertiesProps> = ({
  formData = {},
  onChange,
  onSubmit,
  loading = false,
  readOnly = false
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (formData) {
      form.setFieldsValue(formData);
    }
  }, [formData, form]);

  const handleFormChange = () => {
    if (onChange) {
      const values = form.getFieldsValue();
      onChange(values);
    }
  };

  const handleSubmit = () => {
    form.validateFields().then((values: Record<string, any>) => {
      if (onSubmit) {
        onSubmit(values);
      }
    });
  };

  return (
    <Form
      form={form}
      layout="vertical"
      onValuesChange={handleFormChange}
      disabled={loading || readOnly}
    >
      <Form.Item
        label="规则引擎名称"
        name="engineName"
        rules={[{ required: true, message: '请输入规则引擎名称' }]}
      >
        <Input placeholder="请输入规则引擎名称" />
      </Form.Item>

      <Form.Item
        label="规则执行模式"
        name="executionMode"
        initialValue="sequential"
        rules={[{ required: true, message: '请选择规则执行模式' }]}
      >
        <Select
          options={[
            { value: 'sequential', label: '顺序执行' },
            { value: 'parallel', label: '并行执行' }
          ]}
        />
      </Form.Item>

      <Form.Item
        label="上下文键"
        name="contextKey"
        tooltip="规则引擎处理的JSON数据路径，如 payload.data"
      >
        <Input placeholder="例如: payload.data" />
      </Form.Item>

      <Form.Item
        label="最大执行时间 (毫秒)"
        name="maxExecutionTime"
        initialValue={5000}
      >
        <InputNumber min={100} max={60000} />
      </Form.Item>

      <Form.Item
        label="启用调试日志"
        name="enableDebugLog"
        valuePropName="checked"
        initialValue={false}
      >
        <Switch />
      </Form.Item>

      <Form.Item
        label="规则引擎脚本"
        name="script"
        tooltip="使用JavaScript或其他规则语言编写的规则逻辑"
      >
        <TextArea rows={8} placeholder="请输入规则引擎脚本" />
      </Form.Item>

      {!readOnly && onSubmit && (
        <Form.Item>
          <Button type="primary" onClick={handleSubmit} loading={loading}>
            保存
          </Button>
        </Form.Item>
      )}
    </Form>
  );
};

export default RuleEngineProperties; 