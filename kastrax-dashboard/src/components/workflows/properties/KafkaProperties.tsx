import React, { useEffect, useState } from 'react';
import { Form, Input, Select, Switch, Tabs, Button, Tooltip, Typography } from 'antd';
import { QuestionCircleOutlined } from '@ant-design/icons';

const { TabPane } = Tabs;
const { Option } = Select;
const { TextArea } = Input;
const { Text } = Typography;

interface KafkaPropertiesProps {
  data?: any;
  driverMode?: 'SOURCE' | 'TARGET';
  onChange?: (values: any) => void;
  onTest?: (values: any) => Promise<boolean>;
}

const KafkaProperties: React.FC<KafkaPropertiesProps> = ({
  data = {},
  driverMode = 'SOURCE',
  onChange,
  onTest
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [activeKey, setActiveKey] = useState('basic');
  
  useEffect(() => {
    form.setFieldsValue(data);
  }, [data, form]);
  
  const handleValuesChange = (changedValues: any, allValues: any) => {
    if (onChange) {
      onChange(allValues);
    }
  };
  
  const handleTest = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();
      
      if (onTest) {
        setLoading(true);
        const success = await onTest(values);
        setLoading(false);
        
        if (success) {
          alert('连接测试成功');
        } else {
          alert('连接测试失败');
        }
      }
    } catch (error) {
      console.error('表单验证失败', error);
    }
  };
  
  return (
    <Form 
      form={form}
      layout="vertical"
      onValuesChange={handleValuesChange}
      initialValues={{
        authType: 'none',
        autoCommit: true,
        offsetReset: 'latest',
        compressionType: 'none',
        acks: '1',
        ...data
      }}
    >
      <Tabs activeKey={activeKey} onChange={setActiveKey}>
        <TabPane tab="基本配置" key="basic">
          <Form.Item 
            label="Broker列表" 
            name="brokers"
            rules={[{ required: true, message: '请输入Kafka Broker列表' }]}
          >
            <Input placeholder="例如: localhost:9092,localhost:9093" />
          </Form.Item>
          
          <Form.Item 
            label="主题" 
            name="topic"
            rules={[{ required: true, message: '请输入主题名称' }]}
          >
            <Input placeholder="例如: my-topic" />
          </Form.Item>
          
          {driverMode === 'SOURCE' ? (
            <>
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
              >
                <Switch />
              </Form.Item>
              
              <Form.Item 
                label="偏移量重置策略" 
                name="offsetReset"
              >
                <Select>
                  <Option value="latest">最新偏移量</Option>
                  <Option value="earliest">最早偏移量</Option>
                </Select>
              </Form.Item>
            </>
          ) : (
            <>
              <Form.Item 
                label={
                  <span>
                    键名
                    <Tooltip title="消息键名，用于分区路由">
                      <QuestionCircleOutlined style={{ marginLeft: 4 }} />
                    </Tooltip>
                  </span>
                } 
                name="keyName"
              >
                <Input placeholder="例如: deviceId" />
              </Form.Item>
              
              <Form.Item 
                label="压缩类型" 
                name="compressionType"
              >
                <Select>
                  <Option value="none">无压缩</Option>
                  <Option value="gzip">GZIP</Option>
                  <Option value="snappy">Snappy</Option>
                  <Option value="lz4">LZ4</Option>
                </Select>
              </Form.Item>
              
              <Form.Item 
                label="确认机制" 
                name="acks"
              >
                <Select>
                  <Option value="0">不等待确认</Option>
                  <Option value="1">等待leader确认</Option>
                  <Option value="all">等待所有副本确认</Option>
                </Select>
              </Form.Item>
            </>
          )}
        </TabPane>
        
        <TabPane tab="认证配置" key="auth">
          <Form.Item 
            label="认证类型" 
            name="authType"
          >
            <Select>
              <Option value="none">无认证</Option>
              <Option value="sasl_plain">SASL/PLAIN</Option>
              <Option value="sasl_scram">SASL/SCRAM</Option>
              <Option value="ssl">SSL</Option>
            </Select>
          </Form.Item>
          
          <Form.Item noStyle shouldUpdate={(prevValues, currentValues) => 
            prevValues.authType !== currentValues.authType
          }>
            {({ getFieldValue }) => {
              const authType = getFieldValue('authType');
              
              if (!authType || authType === 'none') {
                return null;
              }
              
              if (authType === 'sasl_plain' || authType === 'sasl_scram') {
                return (
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
                );
              }
              
              if (authType === 'ssl') {
                return (
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
                );
              }
              
              return null;
            }}
          </Form.Item>
        </TabPane>
        
        <TabPane tab="高级配置" key="advanced">
          <Form.Item 
            label={
              <span>
                高级配置选项
                <Tooltip title="JSON格式的Kafka高级配置选项">
                  <QuestionCircleOutlined style={{ marginLeft: 4 }} />
                </Tooltip>
              </span>
            } 
            name="advancedConfig"
          >
            <TextArea 
              rows={10} 
              placeholder='例如: {"max.poll.records": 500, "session.timeout.ms": 30000}' 
            />
          </Form.Item>
          
          <Text type="secondary">
            高级配置项将直接透传给Kafka客户端。详细配置请参考Kafka官方文档。
          </Text>
        </TabPane>
      </Tabs>
      
      {onTest && (
        <div style={{ marginTop: 16 }}>
          <Button type="primary" onClick={handleTest} loading={loading}>
            测试连接
          </Button>
        </div>
      )}
    </Form>
  );
};

export default KafkaProperties; 