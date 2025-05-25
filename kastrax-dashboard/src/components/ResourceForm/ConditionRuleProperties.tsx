import React, { useEffect } from 'react';
import { Form, Input, Switch, Button, Select, InputNumber } from 'antd';
import { ResourceFormProps } from './index';

const { TextArea } = Input;

interface ConditionRulePropertiesProps extends Omit<ResourceFormProps, 'resourceType'> {}

const ConditionRuleProperties: React.FC<ConditionRulePropertiesProps> = ({
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

  // 定义比较操作符选项
  const operatorOptions = [
    { value: 'eq', label: '等于 (=)' },
    { value: 'neq', label: '不等于 (!=)' },
    { value: 'gt', label: '大于 (>)' },
    { value: 'gte', label: '大于等于 (>=)' },
    { value: 'lt', label: '小于 (<)' },
    { value: 'lte', label: '小于等于 (<=)' },
    { value: 'contains', label: '包含' },
    { value: 'notContains', label: '不包含' },
    { value: 'startsWith', label: '开头是' },
    { value: 'endsWith', label: '结尾是' },
    { value: 'isNull', label: '为空' },
    { value: 'isNotNull', label: '不为空' }
  ];

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
        label="属性路径"
        name="propertyPath"
        tooltip="要检查的数据属性路径，如 temperature"
        rules={[{ required: true, message: '请输入属性路径' }]}
      >
        <Input placeholder="例如: temperature" />
      </Form.Item>

      <Form.Item
        label="比较操作符"
        name="operator"
        initialValue="eq"
        rules={[{ required: true, message: '请选择比较操作符' }]}
      >
        <Select options={operatorOptions} />
      </Form.Item>

      <Form.Item
        label="比较值"
        name="value"
        tooltip="用于比较的值"
      >
        <Input placeholder="请输入比较值" />
      </Form.Item>

      <Form.Item
        label="逻辑反转"
        name="negate"
        valuePropName="checked"
        initialValue={false}
        tooltip="选中则反转条件判断结果"
      >
        <Switch />
      </Form.Item>

      <Form.Item
        label="传递模式"
        name="passMode"
        initialValue="all"
        tooltip="当条件满足时的数据传递行为"
      >
        <Select
          options={[
            { value: 'all', label: '传递所有数据' },
            { value: 'matched', label: '仅传递匹配数据' },
            { value: 'filtered', label: '仅传递过滤结果' }
          ]}
        />
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
        label="自定义条件表达式"
        name="customExpression"
        tooltip="使用JS表达式定义的自定义条件，会覆盖上面的标准设置"
      >
        <TextArea rows={4} placeholder="例如: value.temperature > 30 && value.humidity < 80" />
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

export default ConditionRuleProperties; 