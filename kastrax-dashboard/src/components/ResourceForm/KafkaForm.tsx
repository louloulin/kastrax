import React, { useEffect } from 'react';
import { Form, Input, Switch, Button, InputNumber, Select } from 'antd';
import { ResourceFormProps } from './index';

const { TextArea } = Input;

interface KafkaFormProps extends ResourceFormProps {}

const KafkaForm: React.FC<KafkaFormProps> = ({
  resourceType,
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
        label="Broker列表"
        name="brokers"
        rules={[{ required: true, message: '请输入Kafka Broker列表' }]}
      >
        <Input placeholder="例如: localhost:9092,localhost:9093" />
      </Form.Item>

      {driverMode === 'SOURCE' ? (
        // 订阅相关配置
        <>
          <Form.Item
            label="主题"
            name="topic"
            rules={[{ required: true, message: '请输入要订阅的主题' }]}
          >
            <Input placeholder="例如: my-topic" />
          </Form.Item>
          
          <Form.Item
            label="消费组ID"
            name="groupId"
            rules={[{ required: true, message: '请输入消费组ID' }]}
          >
            <Input placeholder="例如: my-group" />
          </Form.Item>
          
          <Form.Item
            label="自动提交"
            name="autoCommit"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>
          
          <Form.Item
            label="偏移量重置策略"
            name="offsetReset"
            initialValue="latest"
          >
            <Select
              options={[
                { value: 'latest', label: '最新偏移量' },
                { value: 'earliest', label: '最早偏移量' }
              ]}
            />
          </Form.Item>
        </>
      ) : (
        // 发布相关配置
        <>
          <Form.Item
            label="主题"
            name="topic"
            rules={[{ required: true, message: '请输入要发布的主题' }]}
          >
            <Input placeholder="例如: my-topic" />
          </Form.Item>
          
          <Form.Item
            label="键名"
            name="keyName"
            tooltip="消息键名，用于分区路由"
          >
            <Input placeholder="例如: deviceId" />
          </Form.Item>
          
          <Form.Item
            label="压缩类型"
            name="compressionType"
            initialValue="none"
          >
            <Select
              options={[
                { value: 'none', label: '无压缩' },
                { value: 'gzip', label: 'GZIP' },
                { value: 'snappy', label: 'Snappy' },
                { value: 'lz4', label: 'LZ4' }
              ]}
            />
          </Form.Item>
          
          <Form.Item
            label="确认机制"
            name="acks"
            initialValue="1"
          >
            <Select
              options={[
                { value: '0', label: '不等待确认' },
                { value: '1', label: '等待leader确认' },
                { value: 'all', label: '等待所有副本确认' }
              ]}
            />
          </Form.Item>
        </>
      )}

      <Form.Item
        label="认证类型"
        name="authType"
        initialValue="none"
      >
        <Select
          options={[
            { value: 'none', label: '无认证' },
            { value: 'sasl_plain', label: 'SASL/PLAIN' },
            { value: 'sasl_scram', label: 'SASL/SCRAM' },
            { value: 'ssl', label: 'SSL' }
          ]}
        />
      </Form.Item>

      <Form.Item
        noStyle
        shouldUpdate={(prevValues, currentValues) => prevValues.authType !== currentValues.authType}
      >
        {({ getFieldValue }: { getFieldValue: (name: string) => any }) => {
          const authType = getFieldValue('authType');
          return authType && authType !== 'none' ? (
            <>
              {(authType === 'sasl_plain' || authType === 'sasl_scram') && (
                <>
                  <Form.Item
                    label="用户名"
                    name="username"
                    rules={[{ required: true, message: '请输入用户名' }]}
                  >
                    <Input placeholder="请输入用户名" />
                  </Form.Item>
                  
                  <Form.Item
                    label="密码"
                    name="password"
                    rules={[{ required: true, message: '请输入密码' }]}
                  >
                    <Input.Password placeholder="请输入密码" />
                  </Form.Item>
                </>
              )}
              
              {authType === 'ssl' && (
                <>
                  <Form.Item
                    label="证书路径"
                    name="certPath"
                    rules={[{ required: true, message: '请输入证书路径' }]}
                  >
                    <Input placeholder="例如: /path/to/client.crt" />
                  </Form.Item>
                  
                  <Form.Item
                    label="私钥路径"
                    name="keyPath"
                    rules={[{ required: true, message: '请输入私钥路径' }]}
                  >
                    <Input placeholder="例如: /path/to/client.key" />
                  </Form.Item>
                  
                  <Form.Item
                    label="CA证书路径"
                    name="caPath"
                    rules={[{ required: true, message: '请输入CA证书路径' }]}
                  >
                    <Input placeholder="例如: /path/to/ca.crt" />
                  </Form.Item>
                </>
              )}
            </>
          ) : null;
        }}
      </Form.Item>

      <Form.Item
        label="高级配置"
        name="advancedConfig"
        tooltip="JSON格式的Kafka高级配置选项"
      >
        <TextArea rows={4} placeholder='例如: {"max.poll.records": 500, "session.timeout.ms": 30000}' />
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

export default KafkaForm; 