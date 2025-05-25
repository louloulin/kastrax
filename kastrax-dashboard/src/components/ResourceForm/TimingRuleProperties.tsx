import React, { useEffect } from 'react';
import { Form, Input, Switch, Button, Select, InputNumber } from 'antd';
import { ResourceFormProps } from './index';

const { TextArea } = Input;

interface TimingRulePropertiesProps extends Omit<ResourceFormProps, 'resourceType'> {}

const TimingRuleProperties: React.FC<TimingRulePropertiesProps> = ({
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
        label="定时类型"
        name="timingType"
        initialValue="cron"
        rules={[{ required: true, message: '请选择定时类型' }]}
      >
        <Select
          options={[
            { value: 'cron', label: 'Cron表达式' },
            { value: 'fixed', label: '固定间隔' },
            { value: 'datetime', label: '特定时间' }
          ]}
        />
      </Form.Item>

      <Form.Item
        label="Cron表达式"
        name="cronExpression"
        tooltip="标准的Cron表达式，如 0 0/5 * * * ? 表示每5分钟执行一次"
        rules={[
          ({ getFieldValue }) => ({
            required: getFieldValue('timingType') === 'cron',
            message: '请输入Cron表达式'
          })
        ]}
      >
        <Input placeholder="例如: 0 0/5 * * * ?" />
      </Form.Item>

      <Form.Item
        label="执行间隔(秒)"
        name="fixedInterval"
        tooltip="固定时间间隔，单位为秒"
        rules={[
          ({ getFieldValue }) => ({
            required: getFieldValue('timingType') === 'fixed',
            message: '请输入执行间隔'
          })
        ]}
      >
        <InputNumber min={1} />
      </Form.Item>

      <Form.Item
        label="执行时间"
        name="executeTime"
        tooltip="特定执行时间，格式为 YYYY-MM-DD HH:mm:ss"
        rules={[
          ({ getFieldValue }) => ({
            required: getFieldValue('timingType') === 'datetime',
            message: '请输入执行时间'
          })
        ]}
      >
        <Input placeholder="例如: 2023-12-31 23:59:59" />
      </Form.Item>

      <Form.Item
        label="时区"
        name="timezone"
        initialValue="Asia/Shanghai"
        tooltip="执行的时区设置"
      >
        <Select
          options={[
            { value: 'Asia/Shanghai', label: '中国标准时间 (UTC+8)' },
            { value: 'UTC', label: '协调世界时 (UTC)' },
            { value: 'America/New_York', label: '美国东部时间' },
            { value: 'Europe/London', label: '英国标准时间' }
          ]}
        />
      </Form.Item>

      <Form.Item
        label="最大执行次数"
        name="maxExecutions"
        tooltip="最大执行次数，0表示无限次"
        initialValue={0}
      >
        <InputNumber min={0} />
      </Form.Item>

      <Form.Item
        label="错过执行策略"
        name="missedFirePolicy"
        initialValue="ignore"
        tooltip="当调度错过时如何处理"
      >
        <Select
          options={[
            { value: 'ignore', label: '忽略错过的执行' },
            { value: 'fire_once', label: '错过后执行一次' },
            { value: 'fire_all', label: '执行所有错过的次数' }
          ]}
        />
      </Form.Item>

      <Form.Item
        label="启用超时控制"
        name="enableTimeout"
        valuePropName="checked"
        initialValue={false}
        tooltip="是否启用执行超时控制"
      >
        <Switch />
      </Form.Item>

      <Form.Item
        label="超时时间(秒)"
        name="timeoutSeconds"
        initialValue={60}
        tooltip="执行超时时间，单位秒"
      >
        <InputNumber min={1} max={3600} />
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

export default TimingRuleProperties; 