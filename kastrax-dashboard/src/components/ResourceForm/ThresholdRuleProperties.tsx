import React, { useEffect } from 'react';
import { Form, Input, Switch, Button, InputNumber, Select } from 'antd';
import { ResourceFormProps } from './index';

const { TextArea } = Input;

interface ThresholdRulePropertiesProps extends Omit<ResourceFormProps, 'resourceType'> {}

const ThresholdRuleProperties: React.FC<ThresholdRulePropertiesProps> = ({
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
        label="监控属性路径"
        name="propertyPath"
        tooltip="要监控的数据属性路径，如 temperature"
        rules={[{ required: true, message: '请输入监控属性路径' }]}
      >
        <Input placeholder="例如: temperature" />
      </Form.Item>

      <Form.Item
        label="阈值类型"
        name="thresholdType"
        initialValue="static"
        rules={[{ required: true, message: '请选择阈值类型' }]}
      >
        <Select
          options={[
            { value: 'static', label: '固定阈值' },
            { value: 'dynamic', label: '动态阈值' },
            { value: 'reference', label: '参考值阈值' }
          ]}
        />
      </Form.Item>

      <Form.Item
        label="上限阈值"
        name="upperThreshold"
        tooltip="当值超过此阈值时触发"
      >
        <InputNumber />
      </Form.Item>

      <Form.Item
        label="下限阈值"
        name="lowerThreshold"
        tooltip="当值低于此阈值时触发"
      >
        <InputNumber />
      </Form.Item>

      <Form.Item
        label="百分比模式"
        name="percentageMode"
        valuePropName="checked"
        initialValue={false}
        tooltip="选中则将阈值视为百分比变化而非绝对值"
      >
        <Switch />
      </Form.Item>

      <Form.Item
        label="参考属性路径"
        name="referencePropertyPath"
        tooltip="当使用参考值阈值时的参考数据属性路径"
      >
        <Input placeholder="例如: referenceTemperature" />
      </Form.Item>

      <Form.Item
        label="允许偏差范围(%)"
        name="allowedDeviation"
        initialValue={10}
        tooltip="当使用动态阈值或参考值阈值时的允许偏差百分比"
      >
        <InputNumber min={0} max={100} />
      </Form.Item>

      <Form.Item
        label="连续触发次数"
        name="consecutiveCount"
        initialValue={1}
        tooltip="需要连续触发多少次才算一次有效触发"
      >
        <InputNumber min={1} />
      </Form.Item>

      <Form.Item
        label="触发后冷却时间(秒)"
        name="cooldownPeriod"
        initialValue={0}
        tooltip="触发后多少秒内不再重复触发"
      >
        <InputNumber min={0} />
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
        label="高级阈值表达式"
        name="advancedExpression"
        tooltip="使用JS表达式定义的高级阈值判断逻辑，会覆盖上面的标准设置"
      >
        <TextArea rows={4} placeholder="例如: Math.abs(value - baseline) > threshold" />
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

export default ThresholdRuleProperties; 