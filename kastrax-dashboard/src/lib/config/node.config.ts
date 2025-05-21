// 节点分组列表
export interface NodeGroup {
  code: string;
  name: string;
}

// 节点配置接口
export interface NodeConfig {
  name: string;
  code: string;
  group: string;
  style: 'source' | 'target' | 'handle';
  ports: {
    in: number;
    out: number;
  };
  component: {
    name: string;
    param?: Record<string, any>;
  };
}

// 分组后的节点列表接口
export interface GroupedNodeList {
  groupName: string;
  groupCode: string;
  nodeList: NodeConfig[];
}

export const nodeGroupList: NodeGroup[] = [
  { code: 'SUBSCRIBE', name: '订阅' },
  { code: 'READ', name: '读取' },
  { code: 'NET', name: '网络' },
  { code: 'HANDLE', name: '处理' },
  { code: 'PUBLISH', name: '发布' },
  { code: 'STORAGE', name: '存储' },
  { code: 'RULE', name: '规则' }
];

export const nodeConfigList: NodeConfig[] = [
  {
    name: 'MQTT订阅',
    code: 'MQTT_SUBSCRIBE',
    group: 'SUBSCRIBE',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'MqttProperties', param: { driverMode: 'SOURCE', resourceType: 'MQTT' } }
  },
  {
    name: 'MQTT发布',
    code: 'MQTT_PUBLISH',
    group: 'PUBLISH',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'MqttProperties', param: { driverMode: 'TARGET', resourceType: 'MQTT' } }
  },
  {
    name: 'Kafka订阅',
    code: 'KAFKA_SUBSCRIBE',
    group: 'SUBSCRIBE',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'KafkaProperties', param: { driverMode: 'SOURCE', resourceType: 'KAFKA' } }
  },
  {
    name: 'Kafka发布',
    code: 'KAFKA_PUBLISH',
    group: 'PUBLISH',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'KafkaProperties', param: { driverMode: 'TARGET', resourceType: 'KAFKA' } }
  },
  {
    name: 'RocketMQ订阅',
    code: 'ROCKETMQ_SUBSCRIBE',
    group: 'SUBSCRIBE',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'RocketMQProperties', param: { driverMode: 'SOURCE', resourceType: 'ROCKETMQ' } }
  },
  {
    name: 'RocketMQ发布',
    code: 'ROCKETMQ_PUBLISH',
    group: 'PUBLISH',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'RocketMQProperties', param: { driverMode: 'TARGET', resourceType: 'ROCKETMQ' } }
  },
  {
    name: 'ActiveMQ订阅',
    code: 'ACTIVEMQ_SUBSCRIBE',
    group: 'SUBSCRIBE',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'ActiveMQProperties', param: { driverMode: 'SOURCE', resourceType: 'ACTIVEMQ' } }
  },
  {
    name: 'ActiveMQ发布',
    code: 'ACTIVEMQ_PUBLISH',
    group: 'PUBLISH',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'ActiveMQProperties', param: { driverMode: 'TARGET', resourceType: 'ACTIVEMQ' } }
  },
  {
    name: 'RabbitMQ订阅',
    code: 'RABBITMQ_SUBSCRIBE',
    group: 'SUBSCRIBE',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'RabbitMQProperties', param: { driverMode: 'SOURCE', resourceType: 'RABBITMQ' } }
  },
  {
    name: 'RabbitMQ发布',
    code: 'RABBITMQ_PUBLISH',
    group: 'PUBLISH',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'RabbitMQProperties', param: { driverMode: 'TARGET', resourceType: 'RABBITMQ' } }
  },
  {
    name: 'TCP监听',
    code: 'TCP_LISTEN',
    group: 'NET',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'TcpProperties' }
  },
  {
    name: 'UDP监听',
    code: 'UDP_LISTEN',
    group: 'NET',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'UdpProperties' }
  },
  {
    name: 'HTTP请求',
    code: 'HTTP_CLIENT',
    group: 'NET',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'HttpClientProperties' }
  },
  {
    name: 'HTTP服务',
    code: 'HTTP_SERVER',
    group: 'NET',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'HttpServerProperties' }
  },
  {
    name: 'HTTP响应',
    code: 'HTTP_RESPONSE',
    group: 'NET',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'HttpResponseProperties' }
  },
  {
    name: 'CoAP请求',
    code: 'COAP_CLIENT',
    group: 'NET',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'CoapClientProperties' }
  },
  {
    name: 'CoAP服务',
    code: 'COAP_SERVER',
    group: 'NET',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'CoapServerProperties' }
  },
  {
    name: 'OPCUA读取',
    code: 'OPCUA_READ',
    group: 'NET',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'OpcUAProperties', param: { driverMode: 'SOURCE', resourceType: 'OPCUA' } }
  },
  {
    name: 'OPCUA写入',
    code: 'OPCUA_WRITE',
    group: 'NET',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'OpcUAProperties', param: { driverMode: 'TARGET', resourceType: 'OPCUA' } }
  },
  {
    name: 'ModbusTCP读取',
    code: 'MODBUSTCP_READ',
    group: 'NET',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'ModbusTcpProperties', param: { driverMode: 'SOURCE', resourceType: 'MODBUSTCP' } }
  },
  {
    name: 'ModbusTCP写入',
    code: 'MODBUSTCP_WRITE',
    group: 'NET',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'ModbusTcpProperties', param: { driverMode: 'TARGET', resourceType: 'MODBUSTCP' } }
  },
  {
    name: 'SNMP读取',
    code: 'SNMP_READ',
    group: 'NET',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'SnmpProperties', param: { driverMode: 'SOURCE', resourceType: 'SNMP' } }
  },
  {
    name: 'Mysql读取',
    code: 'MYSQL_READ',
    group: 'READ',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'JdbcProperties', param: { driverMode: 'SOURCE', resourceType: 'MYSQL' } }
  },
  {
    name: 'Mysql写入',
    code: 'MYSQL_WRITE',
    group: 'STORAGE',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'JdbcProperties', param: { driverMode: 'TARGET', resourceType: 'MYSQL' } }
  },
  {
    name: 'Postgresql读取',
    code: 'POSTGRESQL_READ',
    group: 'READ',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'JdbcProperties', param: { driverMode: 'SOURCE', resourceType: 'POSTGRESQL' } }
  },
  {
    name: 'Postgresql写入',
    code: 'POSTGRESQL_WRITE',
    group: 'STORAGE',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'JdbcProperties', param: { driverMode: 'TARGET', resourceType: 'POSTGRESQL' } }
  },
  {
    name: 'TDengine读取',
    code: 'TDENGINE_READ',
    group: 'READ',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'JdbcProperties', param: { driverMode: 'SOURCE', resourceType: 'TDENGINE' } }
  },
  {
    name: 'TDengine写入',
    code: 'TDENGINE_WRITE',
    group: 'STORAGE',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'JdbcProperties', param: { driverMode: 'TARGET', resourceType: 'TDENGINE' } }
  },
  {
    name: 'SQLServer读取',
    code: 'SQLSERVER_READ',
    group: 'READ',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'JdbcProperties', param: { driverMode: 'SOURCE', resourceType: 'SQLSERVER' } }
  },
  {
    name: 'SQLServer写入',
    code: 'SQLSERVER_WRITE',
    group: 'STORAGE',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'JdbcProperties', param: { driverMode: 'TARGET', resourceType: 'SQLSERVER' } }
  },
  {
    name: 'Redis读取',
    code: 'REDIS_READ',
    group: 'READ',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'RedisProperties', param: { driverMode: 'SOURCE', resourceType: 'REDIS' } }
  },
  {
    name: 'Redis写入',
    code: 'REDIS_WRITE',
    group: 'STORAGE',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'RedisProperties', param: { driverMode: 'TARGET', resourceType: 'REDIS' } }
  },
  {
    name: '文件监听',
    code: 'FILE_WATCH',
    group: 'READ',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'FileWatchProperties' }
  },
  {
    name: '文件写入',
    code: 'FILE_WRITE',
    group: 'STORAGE',
    style: 'target',
    ports: { in: 1, out: 0 },
    component: { name: 'FileWriteProperties' }
  },
  {
    name: 'JavaScript脚本',
    code: 'JAVASCRIPT',
    group: 'HANDLE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'ScriptProperties', param: { language: 'javascript' } }
  },
  {
    name: 'Groovy脚本',
    code: 'GROOVY',
    group: 'HANDLE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'ScriptProperties', param: { language: 'groovy' } }
  },
  {
    name: '定时任务',
    code: 'SCHEDULE',
    group: 'HANDLE',
    style: 'source',
    ports: { in: 0, out: 1 },
    component: { name: 'ScheduleProperties' }
  },
  {
    name: '模板',
    code: 'TEMPLATE',
    group: 'HANDLE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'TemplateProperties' }
  },
  {
    name: '路由',
    code: 'SWITCH',
    group: 'HANDLE',
    style: 'handle',
    ports: { in: 1, out: 2 },
    component: { name: 'SwitchProperties' }
  },
  {
    name: '过滤',
    code: 'FILTER',
    group: 'HANDLE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'FilterProperties' }
  },
  {
    name: '延迟',
    code: 'DELAY',
    group: 'HANDLE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'DelayProperties' }
  },
  {
    name: '限流',
    code: 'SPEED',
    group: 'HANDLE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'SpeedProperties' }
  },
  {
    name: '打包',
    code: 'PACK',
    group: 'HANDLE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'PackProperties' }
  },
  {
    name: '命令',
    code: 'COMMAND',
    group: 'HANDLE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'CommandProperties' }
  },
  {
    name: '规则引擎',
    code: 'RULE_ENGINE',
    group: 'RULE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'RuleEngineProperties' }
  },
  {
    name: '条件规则',
    code: 'CONDITION_RULE',
    group: 'RULE',
    style: 'handle',
    ports: { in: 1, out: 2 },
    component: { name: 'ConditionRuleProperties' }
  },
  {
    name: '阈值规则',
    code: 'THRESHOLD_RULE',
    group: 'RULE',
    style: 'handle',
    ports: { in: 1, out: 2 },
    component: { name: 'ThresholdRuleProperties' }
  },
  {
    name: '时间规则',
    code: 'TIMING_RULE',
    group: 'RULE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'TimingRuleProperties' }
  },
  {
    name: '表达式规则',
    code: 'EXPRESSION_RULE',
    group: 'RULE',
    style: 'handle',
    ports: { in: 1, out: 1 },
    component: { name: 'ExpressionRuleProperties' }
  }
];

// 创建组件映射
function createComponentMap(): Record<string, any> {
  const result: Record<string, any> = {};
  for (const nodeConfig of nodeConfigList) {
    result[nodeConfig.code] = nodeConfig.component;
  }
  return result;
}

export const nodeComponentMap = createComponentMap();

// 获取分组后的节点列表
function getNodeListWithGroup(): GroupedNodeList[] {
  return nodeGroupList.map(nodeGroup => {
    return {
      groupName: nodeGroup.name,
      groupCode: nodeGroup.code,
      nodeList: nodeConfigList.filter(item => nodeGroup.code === item.group)
    };
  });
}

export const nodeListWithGroup = getNodeListWithGroup(); 