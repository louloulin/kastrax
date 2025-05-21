import React, { useState } from 'react';
import { Collapse, List, Typography, Input } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { nodeListWithGroup, NodeConfig } from '../../lib/config/node.config';
import styles from './index.module.css';

const { Panel } = Collapse;
const { Title, Text } = Typography;

export interface NodePanelProps {
  onDragStart?: (event: React.DragEvent<HTMLDivElement>, nodeConfig: NodeConfig) => void;
}

const NodePanel: React.FC<NodePanelProps> = ({ onDragStart }) => {
  const [searchText, setSearchText] = useState('');

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchText(e.target.value);
  };

  const handleDragStart = (event: React.DragEvent<HTMLDivElement>, nodeConfig: NodeConfig) => {
    // 设置拖拽数据
    event.dataTransfer.setData('application/reactflow', JSON.stringify(nodeConfig));
    event.dataTransfer.effectAllowed = 'move';

    if (onDragStart) {
      onDragStart(event, nodeConfig);
    }
  };

  // 过滤节点
  const filteredNodeGroups = nodeListWithGroup.map(group => {
    return {
      ...group,
      nodeList: group.nodeList.filter(node => 
        node.name.toLowerCase().includes(searchText.toLowerCase()) ||
        node.code.toLowerCase().includes(searchText.toLowerCase())
      )
    };
  }).filter(group => group.nodeList.length > 0);

  return (
    <div className={styles.nodePanel}>
      <div className={styles.searchContainer}>
        <Input
          placeholder="搜索节点"
          prefix={<SearchOutlined />}
          value={searchText}
          onChange={handleSearch}
          allowClear
        />
      </div>

      <Collapse defaultActiveKey={nodeListWithGroup.map(g => g.groupCode)}>
        {filteredNodeGroups.map(group => (
          <Panel 
            header={<Title level={5}>{group.groupName}</Title>} 
            key={group.groupCode}
          >
            <List
              dataSource={group.nodeList}
              renderItem={node => (
                <div
                  className={styles.nodeItem}
                  draggable
                  onDragStart={(e) => handleDragStart(e, node)}
                  key={node.code}
                >
                  <Text>{node.name}</Text>
                </div>
              )}
            />
          </Panel>
        ))}
      </Collapse>
    </div>
  );
};

export default NodePanel; 