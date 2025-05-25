import React, { useState, useEffect } from 'react';
import { Drawer, Spin, Tabs, Typography, Button, Space } from 'antd';
import { nodeComponentMap } from '../../lib/config/node.config';
import { createResourceForm } from '../ResourceForm';

const { Title } = Typography;
const { TabPane } = Tabs;

export interface NodePropertiesPanelProps {
  visible: boolean;
  nodeData: any; // 节点数据
  onClose: () => void;
  onSave: (nodeId: string, properties: any) => void;
  loading?: boolean;
}

const NodePropertiesPanel: React.FC<NodePropertiesPanelProps> = ({
  visible,
  nodeData,
  onClose,
  onSave,
  loading = false
}) => {
  const [properties, setProperties] = useState<Record<string, any>>({});
  const [activeTab, setActiveTab] = useState('basic');

  useEffect(() => {
    if (nodeData && nodeData.properties) {
      setProperties(nodeData.properties);
    } else {
      setProperties({});
    }
  }, [nodeData]);

  if (!nodeData) {
    return null;
  }

  const handleSave = () => {
    onSave(nodeData.id, properties);
  };

  const handlePropertiesChange = (values: Record<string, any>) => {
    setProperties((prev: Record<string, any>) => ({ ...prev, ...values }));
  };

  // 获取节点组件配置
  const componentConfig = nodeData.code ? nodeComponentMap[nodeData.code] : null;
  const resourceType = componentConfig?.param?.resourceType;
  const driverMode = componentConfig?.param?.driverMode;

  return (
    <Drawer
      title={
        <div>
          <Title level={4}>{nodeData.name || '节点属性'}</Title>
          <div>节点ID: {nodeData.id}</div>
        </div>
      }
      placement="right"
      width={600}
      onClose={onClose}
      visible={visible}
      footer={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleSave} loading={loading}>
            保存
          </Button>
        </Space>
      }
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin tip="加载中..." />
        </div>
      ) : (
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="基础属性" key="basic">
            <div>
              <div>节点类型: {nodeData.code}</div>
              <div>节点组别: {nodeData.group}</div>
              {resourceType && (
                createResourceForm(resourceType, {
                  driverMode,
                  formData: properties,
                  onChange: handlePropertiesChange,
                  loading,
                })
              )}
            </div>
          </TabPane>
          <TabPane tab="高级设置" key="advanced">
            <div>高级配置内容</div>
          </TabPane>
        </Tabs>
      )}
    </Drawer>
  );
};

export default NodePropertiesPanel; 