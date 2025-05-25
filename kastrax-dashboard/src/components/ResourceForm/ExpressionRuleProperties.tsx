import React, { useEffect } from 'react';
import { Form, Input, Switch, Button } from 'antd';
import { ResourceFormProps } from './index';

const { TextArea } = Input;

interface ExpressionRulePropertiesProps extends Omit<ResourceFormProps, 'resourceType'> {}

const ExpressionRuleProperties: React.FC<ExpressionRulePropertiesProps> = ({
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
        label="规则名称"
        name="ruleName"
        rules={[{ required: true, message: '请输入规则名称' }]}
      >
        <Input placeholder="请输入规则名称" />
      </Form.Item>

      <Form.Item
        label="表达式"
        name="expression"
        tooltip="规则表达式，使用JavaScript语法"
        rules={[{ required: true, message: '请输入表达式' }]}
      >
        <TextArea rows={6} placeholder="例如: message.temperature > 30 && message.humidity < 80" />
      </Form.Item>

      <Form.Item
        label="上下文变量"
        name="contextVariables"
        tooltip="表达式中可用的上下文变量名称，用逗号分隔"
      >
        <Input placeholder="例如: message,device,timestamp" />
      </Form.Item>

      <Form.Item
        label="错误处理表达式"
        name="errorHandlingExpression"
        tooltip="当主表达式出错时的处理逻辑"
      >
        <TextArea rows={3} placeholder="例如: console.error(error); return false;" />
      </Form.Item>

      <Form.Item
        label="启用自定义结果处理"
        name="enableCustomResult"
        valuePropName="checked"
        initialValue={false}
        tooltip="启用后表达式结果将通过结果处理器函数处理"
      >
        <Switch />
      </Form.Item>

      <Form.Item
        label="结果处理器"
        name="resultHandler"
        tooltip="处理表达式执行结果的函数"
      >
        <TextArea rows={4} placeholder="例如: function(result, context) { return { passed: result, timestamp: Date.now() }; }" />
      </Form.Item>

      <Form.Item
        label="启用调试日志"
        name="enableDebugLog"
        valuePropName="checked"
        initialValue={false}
      >
        <Switch />
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

export default ExpressionRuleProperties; 