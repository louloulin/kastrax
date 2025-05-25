import React, { useEffect } from 'react';
import { Form, Input, Switch, InputNumber, Select, Button } from 'antd';
import { ResourceFormProps } from './index';

const { TextArea } = Input;

interface MqttFormProps extends Omit<ResourceFormProps, 'resourceType'> {}

const MqttForm: React.FC<MqttFormProps> = ({
  driverMode = 'SOURCE',
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
        label="MQTT服务器地址"
        name="serverUrl"
        rules={[{ required: true, message: '请输入MQTT服务器地址' }]}
      >
        <Input placeholder="如: tcp://example.com:1883" />
      </Form.Item>

      <Form.Item
        label="客户端ID"
        name="clientId"
        rules={[{ required: true, message: '请输入客户端ID' }]}
      >
        <Input placeholder="请输入唯一的客户端ID" />
      </Form.Item>

      <Form.Item label="用户名" name="username">
        <Input placeholder="请输入用户名" />
      </Form.Item>

      <Form.Item label="密码" name="password">
        <Input.Password placeholder="请输入密码" />
      </Form.Item>

      {driverMode === 'SOURCE' && (
        <>
          <Form.Item
            label="订阅主题"
            name="topic"
            rules={[{ required: true, message: '请输入订阅主题' }]}
          >
            <Input placeholder="如: sensors/temperature" />
          </Form.Item>

          <Form.Item label="QoS级别" name="qos" initialValue={0}>
            <Select>
              <Select.Option value={0}>0 - 最多一次</Select.Option>
              <Select.Option value={1}>1 - 至少一次</Select.Option>
              <Select.Option value={2}>2 - 确保只有一次</Select.Option>
            </Select>
          </Form.Item>
        </>
      )}

      {driverMode === 'TARGET' && (
        <>
          <Form.Item
            label="发布主题"
            name="topic"
            rules={[{ required: true, message: '请输入发布主题' }]}
          >
            <Input placeholder="如: sensors/temperature" />
          </Form.Item>

          <Form.Item label="QoS级别" name="qos" initialValue={0}>
            <Select>
              <Select.Option value={0}>0 - 最多一次</Select.Option>
              <Select.Option value={1}>1 - 至少一次</Select.Option>
              <Select.Option value={2}>2 - 确保只有一次</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item label="保留消息" name="retained" valuePropName="checked" initialValue={false}>
            <Switch />
          </Form.Item>
        </>
      )}

      <Form.Item label="连接超时(秒)" name="connectionTimeout" initialValue={60}>
        <InputNumber min={1} max={300} />
      </Form.Item>

      <Form.Item label="保持连接时间(秒)" name="keepAliveInterval" initialValue={60}>
        <InputNumber min={1} max={300} />
      </Form.Item>

      <Form.Item label="清理会话" name="cleanSession" valuePropName="checked" initialValue={true}>
        <Switch />
      </Form.Item>

      <Form.Item label="SSL/TLS" name="useSsl" valuePropName="checked" initialValue={false}>
        <Switch />
      </Form.Item>

      <Form.Item label="高级配置" name="advanced">
        <TextArea rows={4} placeholder="请输入JSON格式的高级配置" />
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

export default MqttForm; 