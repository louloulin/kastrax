import React, { useState } from 'react';
import { 
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger
} from '@/components/ui/accordion';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Input } from '@/components/ui/input';
import { Search } from 'lucide-react';
import { 
  Tooltip, 
  TooltipContent, 
  TooltipProvider, 
  TooltipTrigger 
} from '@/components/ui/tooltip';

// 导入节点组和节点列表
import { nodeListWithGroup } from '@/lib/config/node.config';

// 节点图标映射
const nodeIconMap: Record<string, React.ReactNode> = {
  // 消息队列
  MQTT_SUBSCRIBE: <div className="text-green-500 text-xl">MQ</div>,
  MQTT_PUBLISH: <div className="text-blue-500 text-xl">MQ</div>,
  KAFKA_SUBSCRIBE: <div className="text-green-500 text-xl">KF</div>,
  KAFKA_PUBLISH: <div className="text-blue-500 text-xl">KF</div>,
  ROCKETMQ_SUBSCRIBE: <div className="text-green-500 text-xl">RQ</div>,
  ROCKETMQ_PUBLISH: <div className="text-blue-500 text-xl">RQ</div>,
  ACTIVEMQ_SUBSCRIBE: <div className="text-green-500 text-xl">AQ</div>,
  ACTIVEMQ_PUBLISH: <div className="text-blue-500 text-xl">AQ</div>,
  RABBITMQ_SUBSCRIBE: <div className="text-green-500 text-xl">RM</div>,
  RABBITMQ_PUBLISH: <div className="text-blue-500 text-xl">RM</div>,
  
  // 网络
  TCP_LISTEN: <div className="text-orange-500 text-xl">TCP</div>,
  UDP_LISTEN: <div className="text-orange-500 text-xl">UDP</div>,
  HTTP_CLIENT: <div className="text-purple-500 text-xl">HC</div>,
  HTTP_SERVER: <div className="text-purple-500 text-xl">HS</div>,
  HTTP_RESPONSE: <div className="text-purple-500 text-xl">HR</div>,
  COAP_CLIENT: <div className="text-purple-500 text-xl">CC</div>,
  COAP_SERVER: <div className="text-purple-500 text-xl">CS</div>,
  OPCUA_READ: <div className="text-orange-500 text-xl">OPC</div>,
  OPCUA_WRITE: <div className="text-orange-500 text-xl">OPC</div>,
  MODBUSTCP_READ: <div className="text-orange-500 text-xl">MB</div>,
  MODBUSTCP_WRITE: <div className="text-orange-500 text-xl">MB</div>,
  SNMP_READ: <div className="text-orange-500 text-xl">SN</div>,
  
  // 数据库
  MYSQL_READ: <div className="text-cyan-500 text-xl">My</div>,
  MYSQL_WRITE: <div className="text-cyan-500 text-xl">My</div>,
  POSTGRESQL_READ: <div className="text-cyan-500 text-xl">PG</div>,
  POSTGRESQL_WRITE: <div className="text-cyan-500 text-xl">PG</div>,
  TDENGINE_READ: <div className="text-cyan-500 text-xl">TD</div>,
  TDENGINE_WRITE: <div className="text-cyan-500 text-xl">TD</div>,
  SQLSERVER_READ: <div className="text-cyan-500 text-xl">SS</div>,
  SQLSERVER_WRITE: <div className="text-cyan-500 text-xl">SS</div>,
  REDIS_READ: <div className="text-red-500 text-xl">RD</div>,
  REDIS_WRITE: <div className="text-red-500 text-xl">RD</div>,
  
  // 文件
  FILE_WATCH: <div className="text-amber-500 text-xl">FW</div>,
  FILE_WRITE: <div className="text-amber-500 text-xl">FW</div>,
  
  // 脚本和流程控制
  JAVASCRIPT: <div className="text-yellow-500 text-xl">JS</div>,
  GROOVY: <div className="text-yellow-500 text-xl">GV</div>,
  SCHEDULE: <div className="text-indigo-500 text-xl">SC</div>,
  TEMPLATE: <div className="text-indigo-500 text-xl">TM</div>,
  SWITCH: <div className="text-indigo-500 text-xl">SW</div>,
  FILTER: <div className="text-indigo-500 text-xl">FL</div>,
  DELAY: <div className="text-indigo-500 text-xl">DL</div>,
  SPEED: <div className="text-indigo-500 text-xl">SP</div>,
  PACK: <div className="text-indigo-500 text-xl">PK</div>,
  COMMAND: <div className="text-indigo-500 text-xl">CM</div>,
  
  // 规则节点
  RULE_ENGINE: <div className="text-pink-500 text-xl">RE</div>,
  CONDITION_RULE: <div className="text-pink-500 text-xl">CR</div>,
  THRESHOLD_RULE: <div className="text-pink-500 text-xl">TR</div>,
  TIMING_RULE: <div className="text-pink-500 text-xl">TR</div>,
  EXPRESSION_RULE: <div className="text-pink-500 text-xl">ER</div>,
};

// 节点样式类
const nodeStyleClass: Record<string, string> = {
  source: 'border-l-4 border-green-500',
  handle: 'border-l-4 border-blue-500',
  target: 'border-l-4 border-red-500'
};

interface NodePanelProps {
  onNodeSelect: (nodeType: string) => void;
}

export default function NodePanel({ onNodeSelect }: NodePanelProps) {
  const [searchTerm, setSearchTerm] = useState('');
  
  // 过滤节点
  const filteredGroups = nodeListWithGroup.map(group => {
    return {
      ...group,
      nodeList: group.nodeList.filter(node => 
        node.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        node.code.toLowerCase().includes(searchTerm.toLowerCase())
      )
    };
  }).filter(group => group.nodeList.length > 0);
  
  // 处理节点拖动
  const handleDragStart = (event: React.DragEvent, nodeType: string) => {
    event.dataTransfer.setData('application/reactflow', nodeType);
    event.dataTransfer.effectAllowed = 'move';
  };
  
  // 添加 Kafka 相关节点到面板
  const addKafkaNodes = () => {
    // 查找消息队列组
    const mqGroupIndex = nodeListWithGroup.findIndex(group => group.groupCode === 'MQ');
    
    if (mqGroupIndex >= 0) {
      // 确保 Kafka 节点已经存在于列表中
      const hasKafkaSubscribe = nodeListWithGroup[mqGroupIndex].nodeList.some(node => node.code === 'KAFKA_SUBSCRIBE');
      const hasKafkaPublish = nodeListWithGroup[mqGroupIndex].nodeList.some(node => node.code === 'KAFKA_PUBLISH');
      
      // 如果不存在，添加它们
      if (!hasKafkaSubscribe) {
        nodeListWithGroup[mqGroupIndex].nodeList.push({
          name: 'Kafka 订阅',
          code: 'KAFKA_SUBSCRIBE',
          group: 'SUBSCRIBE',
          style: 'source',
          ports: { in: 0, out: 1 },
          component: { 
            name: 'KafkaProperties', 
            param: { driverMode: 'SOURCE', resourceType: 'KAFKA' } 
          }
        });
      }
      
      if (!hasKafkaPublish) {
        nodeListWithGroup[mqGroupIndex].nodeList.push({
          name: 'Kafka 发布',
          code: 'KAFKA_PUBLISH',
          group: 'PUBLISH',
          style: 'target',
          ports: { in: 1, out: 0 },
          component: { 
            name: 'KafkaProperties', 
            param: { driverMode: 'TARGET', resourceType: 'KAFKA' } 
          }
        });
      }
    }
  };

  // 确保 Kafka 节点添加到面板
  addKafkaNodes();

  return (
    <div className="h-full flex flex-col border-r">
      <div className="p-4">
        <div className="relative">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="搜索节点..."
            className="pl-8"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>
      
      <ScrollArea className="flex-1">
        <Accordion type="multiple" defaultValue={nodeListWithGroup.map(g => g.groupCode)} className="px-4">
          {filteredGroups.map((group) => (
            <AccordionItem key={group.groupCode} value={group.groupCode}>
              <AccordionTrigger className="text-sm font-medium">
                {group.groupName}
              </AccordionTrigger>
              <AccordionContent>
                <div className="grid grid-cols-2 gap-2">
                  {group.nodeList.map((node) => (
                    <TooltipProvider key={node.code}>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <div
                            className={`p-2 rounded-md border cursor-grab bg-background flex items-center gap-2 hover:bg-accent ${nodeStyleClass[node.style]}`}
                            draggable
                            onDragStart={(e) => handleDragStart(e, node.code)}
                            onClick={() => onNodeSelect(node.code)}
                          >
                            {nodeIconMap[node.code] || <div className="w-6 h-6 bg-muted rounded-full" />}
                            <span className="text-xs truncate">{node.name}</span>
                          </div>
                        </TooltipTrigger>
                        <TooltipContent side="right">
                          <div className="text-xs">
                            <p><strong>名称:</strong> {node.name}</p>
                            <p><strong>编码:</strong> {node.code}</p>
                            <p><strong>类型:</strong> {
                              node.style === 'source' ? '源节点' : 
                              node.style === 'target' ? '目标节点' : '处理节点'
                            }</p>
                            <p><strong>输入:</strong> {node.ports.in}, <strong>输出:</strong> {node.ports.out}</p>
                          </div>
                        </TooltipContent>
                      </Tooltip>
                    </TooltipProvider>
                  ))}
                </div>
              </AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </ScrollArea>
    </div>
  );
} 