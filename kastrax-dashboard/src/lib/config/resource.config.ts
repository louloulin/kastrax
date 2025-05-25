import { timeUnitMap, TimeUnit } from './time.config';

// 资源分组映射
export const resourceGroupMap = {
  CHANNEL: '消息通道',
  PROTOCOL: '通讯协议',
  DATABASE: '数据存储'
};

// 资源类型定义
export type ResourceType = 'source' | 'sink' | 'all';
export type ResourceGroup = keyof typeof resourceGroupMap;

// 资源详情项接口
export interface ResourceDetailItem {
  name: string | ((resource: any) => string | undefined);
  value: string | ((resource: any) => string | undefined);
}

// 资源属性接口
export interface ResourceProperties {
  [key: string]: any;
  initialDelayUnit?: TimeUnit;
  initialDelay?: number;
  cronExpression?: string;
}

// 资源接口
export interface ResourceData {
  resourceType: string;
  properties: ResourceProperties;
}

// 安全的获取时间单位
function getTimeUnitString(unit?: string): string {
  if (!unit) return '';
  return unit in timeUnitMap ? timeUnitMap[unit as TimeUnit] : '';
}

// 资源配置接口
export interface ResourceConfig {
  name: string;
  type: ResourceType;
  group: ResourceGroup;
  component: string;
  details: {
    resource: ResourceDetailItem;
    rule: ResourceDetailItem[];
  };
}

// 资源配置映射
export const resourceConfigMap: Record<string, ResourceConfig> = {
  MQTT: {
    name: 'MQTT',
    type: 'all',
    group: 'CHANNEL',
    component: 'MqttProperties',
    details: {
      resource: { name: '地址', value: (resource) => resource.properties.url },
      rule: [
        { name: '地址', value: (resource) => resource.properties.url },
        { name: 'Topic', value: (resource) => resource.properties.topic }
      ]
    }
  },
  SNMP: {
    name: 'SNMP',
    type: 'source',
    group: 'PROTOCOL',
    component: 'SnmpProperties',
    details: {
      resource: { name: '地址', value: (resource) => `udp:${resource.properties.ip}/${resource.properties.port}` },
      rule: [
        { name: '地址', value: (resource) => `udp:${resource.properties.ip}/${resource.properties.port}` },
        {
          name: '读取点位',
          value: (resource) => resource.properties.points ? resource.properties.points.length : undefined
        },
        {
          name: '启动延迟',
          value: (resource) => resource.properties.initialDelay
            ? `${resource.properties.initialDelay}${getTimeUnitString(resource.properties.initialDelayUnit)}`
            : undefined
        },
        {
          name: 'Cron表达式', value: (resource) => resource.properties.cronExpression
        }
      ]
    }
  },
  KAFKA: {
    name: 'Kafka',
    type: 'all',
    group: 'CHANNEL',
    component: 'KafkaProperties',
    details: {
      resource: { name: '地址', value: (resource) => `${resource.properties.url}` },
      rule: [
        { name: '地址', value: (resource) => `${resource.properties.url}` },
        { name: 'Topic', value: (resource) => resource.properties.topic }
      ]
    }
  },
  RABBITMQ: {
    name: 'RabbitMQ',
    type: 'all',
    group: 'CHANNEL',
    component: 'RabbitMQProperties',
    details: {
      resource: { name: '地址', value: (resource) => `${resource.properties.ip}:${resource.properties.port}` },
      rule: [
        { name: '地址', value: (resource) => `${resource.properties.ip}:${resource.properties.port}` },
        { name: '虚拟主机', value: (resource) => resource.properties.virtualHost },
        { name: '交换机', value: (resource) => resource.properties.exchange },
        { name: '队列', value: (resource) => resource.properties.queue }
      ]
    }
  },
  ROCKETMQ: {
    name: 'RocketMQ',
    type: 'all',
    group: 'CHANNEL',
    component: 'RocketMQProperties',
    details: {
      resource: { name: '地址', value: (resource) => `${resource.properties.url}` },
      rule: [
        { name: '地址', value: (resource) => `${resource.properties.url}` },
        { name: '分组', value: (resource) => resource.properties.group },
        { name: 'Topic', value: (resource) => resource.properties.topic }
      ]
    }
  },
  ACTIVEMQ: {
    name: 'ActiveMQ',
    type: 'all',
    group: 'CHANNEL',
    component: 'ActiveMQProperties',
    details: {
      resource: { name: '地址', value: (resource) => `${resource.properties.url}` },
      rule: [
        { name: '地址', value: (resource) => `${resource.properties.url}` },
        {
          name: (resource) => resource.properties.model === 'queue' ? 'Queue' : 'Topic',
          value: (resource) => resource.properties.model === 'queue' ? resource.properties.queue : resource.properties.topic
        }
      ]
    }
  },
  MYSQL: {
    name: 'MySQL',
    type: 'all',
    group: 'DATABASE',
    component: 'JdbcProperties',
    details: {
      resource: {
        name: '地址',
        value: (resource) => `${resource.properties.ip}:${resource.properties.port}/${resource.properties.databaseName}`
      },
      rule: [
        {
          name: '地址',
          value: (resource) => `${resource.properties.ip}:${resource.properties.port}/${resource.properties.databaseName}`
        },
        { name: 'SQL模板', value: (resource) => resource.properties.sql },
        {
          name: '启动延迟',
          value: (resource) => resource.properties.initialDelay ? `${resource.properties.initialDelay}${getTimeUnitString(resource.properties.initialDelayUnit)}` : undefined
        },
        {
          name: 'Cron表达式', value: (resource) => resource.properties.cronExpression
        }
      ]
    }
  },
  POSTGRESQL: {
    name: 'PostgreSQL',
    type: 'all',
    group: 'DATABASE',
    component: 'JdbcProperties',
    details: {
      resource: {
        name: '地址',
        value: (resource) => `${resource.properties.ip}:${resource.properties.port}/${resource.properties.databaseName}`
      },
      rule: [
        {
          name: '地址',
          value: (resource) => `${resource.properties.ip}:${resource.properties.port}/${resource.properties.databaseName}`
        },
        { name: 'SQL模板', value: (resource) => resource.properties.sql },
        {
          name: '启动延迟',
          value: (resource) => resource.properties.initialDelay ? `${resource.properties.initialDelay}${getTimeUnitString(resource.properties.initialDelayUnit)}` : undefined
        },
        {
          name: 'Cron表达式', value: (resource) => resource.properties.cronExpression
        }
      ]
    }
  },
  SQLSERVER: {
    name: 'SQL Server',
    type: 'all',
    group: 'DATABASE',
    component: 'JdbcProperties',
    details: {
      resource: {
        name: '地址',
        value: (resource) => `${resource.properties.ip}:${resource.properties.port}/${resource.properties.databaseName}`
      },
      rule: [
        {
          name: '地址',
          value: (resource) => `${resource.properties.ip}:${resource.properties.port}/${resource.properties.databaseName}`
        },
        { name: 'SQL模板', value: (resource) => resource.properties.sql },
        {
          name: '启动延迟',
          value: (resource) => resource.properties.initialDelay ? `${resource.properties.initialDelay}${getTimeUnitString(resource.properties.initialDelayUnit)}` : undefined
        },
        {
          name: 'Cron表达式', value: (resource) => resource.properties.cronExpression
        }
      ]
    }
  },
  TDENGINE: {
    name: 'TDengine',
    type: 'all',
    group: 'DATABASE',
    component: 'JdbcProperties',
    details: {
      resource: {
        name: '地址',
        value: (resource) => `${resource.properties.ip}:${resource.properties.port}/${resource.properties.databaseName}`
      },
      rule: [
        {
          name: '地址',
          value: (resource) => `${resource.properties.ip}:${resource.properties.port}/${resource.properties.databaseName}`
        },
        { name: 'SQL模板', value: (resource) => resource.properties.sql },
        {
          name: '启动延迟',
          value: (resource) => resource.properties.initialDelay ? `${resource.properties.initialDelay}${getTimeUnitString(resource.properties.initialDelayUnit)}` : undefined
        },
        {
          name: 'Cron表达式', value: (resource) => resource.properties.cronExpression
        }
      ]
    }
  },
  OPCUA: {
    name: 'OPC UA',
    type: 'all',
    group: 'PROTOCOL',
    component: 'OpcUAProperties',
    details: {
      resource: { name: '地址', value: (resource) => `${resource.properties.ip}:${resource.properties.port}` },
      rule: [
        { name: '地址', value: (resource) => `opc.tcp://${resource.properties.ip}:${resource.properties.port}` },
        {
          name: '读取点位',
          value: (resource) => resource.properties.points ? resource.properties.points.length : undefined
        },
        {
          name: '启动延迟',
          value: (resource) => resource.properties.initialDelay ? `${resource.properties.initialDelay}${getTimeUnitString(resource.properties.initialDelayUnit)}` : undefined
        },
        {
          name: 'Cron表达式', value: (resource) => resource.properties.cronExpression
        },
        {
          name: '点位地址', value: (resource) => resource.properties.address
        },
        {
          name: '数据值', value: (resource) => resource.properties.dataValue
        }
      ]
    }
  },
  MODBUSTCP: {
    name: 'Modbus TCP',
    type: 'all',
    group: 'PROTOCOL',
    component: 'ModbusTcpProperties',
    details: {
      resource: { name: '地址', value: (resource) => `${resource.properties.ip}:${resource.properties.port}` },
      rule: [
        { name: '地址', value: (resource) => `${resource.properties.ip}:${resource.properties.port}` },
        {
          name: '读取点位',
          value: (resource) => resource.properties.points ? resource.properties.points.length : undefined
        },
        {
          name: '启动延迟',
          value: (resource) => resource.properties.initialDelay ? `${resource.properties.initialDelay}${getTimeUnitString(resource.properties.initialDelayUnit)}` : undefined
        },
        {
          name: 'Cron表达式', value: (resource) => resource.properties.cronExpression
        },
        {
          name: '数据地址', value: (resource) => resource.properties.address
        },
        {
          name: '数据值', value: (resource) => resource.properties.dataValue
        }
      ]
    }
  },
  REDIS: {
    name: 'Redis',
    type: 'all',
    group: 'DATABASE',
    component: 'RedisProperties',
    details: {
      resource: {
        name: '地址',
        value: (resource) => resource.properties.mode === 'STANDALONE' ? `${resource.properties.ip}:${resource.properties.port}` : `${resource.properties.nodes}`
      },
      rule: [
        {
          name: '地址',
          value: (resource) => resource.properties.mode === 'STANDALONE' ? `${resource.properties.ip}:${resource.properties.port}` : `${resource.properties.nodes}`
        },
        { name: '执行命令', value: (resource) => `${resource.properties.command}` },
        {
          name: '启动延迟',
          value: (resource) => resource.properties.initialDelay ? `${resource.properties.initialDelay}${getTimeUnitString(resource.properties.initialDelayUnit)}` : undefined
        },
        {
          name: 'Cron表达式', value: (resource) => resource.properties.cronExpression
        }
      ]
    }
  },
};

// 创建组件映射
export const resourceComponentMap = Object.fromEntries(
  Object.entries(resourceConfigMap).map(([key, config]) => [key, config.component])
);

// 创建类型映射
export const resourceTypeMap = Object.fromEntries(
  Object.entries(resourceConfigMap).map(([key, config]) => [key, config.name])
);

// 空详情项
export const emptyDetail = { name: '', value: '' };

/**
 * 根据类型获取资源列表
 * @param type 资源类型 (source, sink, all)
 */
export function getResourceListByType(type?: ResourceType): Array<{group: string, list: Array<{name: string, type: ResourceType, code: string}>}> {
  const result = [];
  
  for (const groupKey in resourceGroupMap) {
    const resourceTypeList = getResourceListByTypeAndGroup(type, groupKey as ResourceGroup);
    if (!resourceTypeList || resourceTypeList.length === 0) {
      continue;
    }
    result.push({
      group: resourceGroupMap[groupKey as ResourceGroup],
      list: resourceTypeList
    });
  }
  
  return result;
}

/**
 * 根据类型和分组获取资源列表
 * @param type 资源类型
 * @param group 资源分组
 */
export function getResourceListByTypeAndGroup(type?: ResourceType, group?: ResourceGroup): Array<{name: string, type: ResourceType, code: string}> {
  const result = [];
  
  for (const resourceKey in resourceConfigMap) {
    const resourceConfig = resourceConfigMap[resourceKey];
    if (type && resourceConfig.type !== type && resourceConfig.type !== 'all') {
      continue;
    }
    if (group && resourceConfig.group !== group) {
      continue;
    }
    const item = {
      name: resourceConfig.name,
      type: resourceConfig.type,
      code: resourceKey
    };
    result.push(item);
  }
  
  return result;
}

// 所有资源类型列表
export const resourceTypeAllList = getResourceListByType();

/**
 * 获取资源详情
 * @param resource 资源对象
 * @param mode 模式 ('resource' 或 'rule')
 */
export function getResourceDetails(resource: ResourceData, mode: 'resource' | 'rule'): any {
  if (!resource || !resource.resourceType) {
    return mode === 'resource' ? emptyDetail : [emptyDetail];
  }
  
  const resourceConfig = resourceConfigMap[resource.resourceType];
  if (!resourceConfig) {
    return mode === 'resource' ? emptyDetail : [emptyDetail];
  }
  
  const detail = resourceConfig.details[mode];
  
  if (mode === 'resource') {
    return toResultItem(detail as ResourceDetailItem, resource);
  } else {
    return (detail as ResourceDetailItem[])
      .map((item) => toResultItem(item, resource))
      .filter((item) => item.name && item.value);
  }
}

/**
 * 转换为结果项
 * @param detailItem 详情项
 * @param resource 资源对象
 */
function toResultItem(detailItem: ResourceDetailItem, resource: ResourceData): {name: string, value: string} {
  const name = typeof detailItem.name === 'string' ? detailItem.name : detailItem.name(resource);
  if (!name) return emptyDetail;
  
  const value = typeof detailItem.value === 'string' ? detailItem.value : detailItem.value(resource);
  if (!value) return emptyDetail;
  
  return { name, value };
}

// 资源类型分组
export enum ResourceGroupType {
  CHANNEL = "CHANNEL",
  PROTOCOL = "PROTOCOL",
  DATABASE = "DATABASE",
  STORAGE = "STORAGE",
  API = "API",
}

// 资源类型显示名称映射
export const resourceDisplayNameMap: Record<string, string> = {
  // 消息通道类
  MQTT: "MQTT消息通道",
  KAFKA: "KAFKA消息通道",
  RABBITMQ: "RABBITMQ消息通道",
  ROCKETMQ: "ROCKETMQ消息通道",
  ACTIVEMQ: "ACTIVEMQ消息通道",
  
  // 协议类
  SNMP: "SNMP协议",
  OPCUA: "OPC UA协议",
  MODBUSTCP: "Modbus TCP协议",
  
  // 数据库类
  MYSQL: "MySQL数据库",
  POSTGRESQL: "PostgreSQL数据库",
  SQLSERVER: "SQL Server数据库",
  TDENGINE: "TDengine时序库",
  REDIS: "Redis数据库",
  
  // 存储类
  MINIO: "MinIO对象存储",
  S3: "Amazon S3存储",
  OSS: "阿里云OSS存储",
  
  // API类
  RESTAPI: "REST API",
  GRAPHQL: "GraphQL API",
};

// 资源类型列表（分组）
export const resourceTypeList = [
  {
    group: ResourceGroupType.CHANNEL,
    name: "消息通道",
    list: [
      { code: "MQTT", name: "MQTT消息通道" },
      { code: "KAFKA", name: "KAFKA消息通道" },
      { code: "RABBITMQ", name: "RABBITMQ消息通道" },
      { code: "ROCKETMQ", name: "ROCKETMQ消息通道" },
      { code: "ACTIVEMQ", name: "ACTIVEMQ消息通道" },
    ],
  },
  {
    group: ResourceGroupType.PROTOCOL,
    name: "协议",
    list: [
      { code: "SNMP", name: "SNMP协议" },
      { code: "OPCUA", name: "OPC UA协议" },
      { code: "MODBUSTCP", name: "Modbus TCP协议" },
    ],
  },
  {
    group: ResourceGroupType.DATABASE,
    name: "数据库",
    list: [
      { code: "MYSQL", name: "MySQL数据库" },
      { code: "POSTGRESQL", name: "PostgreSQL数据库" },
      { code: "SQLSERVER", name: "SQL Server数据库" },
      { code: "TDENGINE", name: "TDengine时序库" },
      { code: "REDIS", name: "Redis数据库" },
    ],
  },
  {
    group: ResourceGroupType.STORAGE,
    name: "存储",
    list: [
      { code: "MINIO", name: "MinIO对象存储" },
      { code: "S3", name: "Amazon S3存储" },
      { code: "OSS", name: "阿里云OSS存储" },
    ],
  },
  {
    group: ResourceGroupType.API,
    name: "API",
    list: [
      { code: "RESTAPI", name: "REST API" },
      { code: "GRAPHQL", name: "GraphQL API" },
    ],
  },
];

// 所有资源类型列表（扁平化）
export const resourceTypeGroupedList = resourceTypeList;

// 资源类型与组件映射扩展 - 用于加载属性编辑组件
export const resourcePropertyComponentMap: Record<string, string> = {
  // 数据库类
  MYSQL: "JdbcProperties",
  POSTGRESQL: "JdbcProperties",
  SQLSERVER: "JdbcProperties",
  TDENGINE: "JdbcProperties",
  REDIS: "RedisProperties",
  
  // 消息通道类
  MQTT: "MqttProperties",
  KAFKA: "KafkaProperties",
  RABBITMQ: "RabbitMQProperties",
  ROCKETMQ: "RocketMQProperties",
  ACTIVEMQ: "ActiveMQProperties",
  
  // 协议类
  SNMP: "SnmpProperties",
  OPCUA: "OpcuaProperties",
  MODBUSTCP: "ModbustcpProperties",
  
  // 存储类
  MINIO: "MinioProperties",
  S3: "S3Properties",
  OSS: "OssProperties",
  
  // API类
  RESTAPI: "RestApiProperties",
  GRAPHQL: "GraphqlProperties",
};

// 判断资源类型是否为数据库
export const isDatabaseType = (type: string): boolean => {
  return [
    "MYSQL", 
    "POSTGRESQL", 
    "SQLSERVER", 
    "TDENGINE", 
    "REDIS"
  ].includes(type);
};

// 判断资源类型是否为消息通道
export const isChannelType = (type: string): boolean => {
  return [
    "MQTT", 
    "KAFKA", 
    "RABBITMQ", 
    "ROCKETMQ", 
    "ACTIVEMQ"
  ].includes(type);
};

// 判断资源类型是否为协议
export const isProtocolType = (type: string): boolean => {
  return [
    "SNMP", 
    "OPCUA", 
    "MODBUSTCP"
  ].includes(type);
};

// 判断资源类型是否为存储
export const isStorageType = (type: string): boolean => {
  return [
    "MINIO", 
    "S3", 
    "OSS"
  ].includes(type);
};

// 判断资源类型是否为API
export const isApiType = (type: string): boolean => {
  return [
    "RESTAPI", 
    "GRAPHQL"
  ].includes(type);
};

// 获取资源类型对应的图标
export const getResourceTypeIcon = (type: string): string => {
  if (isDatabaseType(type)) return "database";
  if (isChannelType(type)) return "message-square";
  if (isProtocolType(type)) return "plug";
  if (isStorageType(type)) return "hard-drive";
  if (isApiType(type)) return "server";
  return "circle";
};

// 获取资源分组
export const getResourceGroup = (type: string): ResourceGroupType | undefined => {
  if (isDatabaseType(type)) return ResourceGroupType.DATABASE;
  if (isChannelType(type)) return ResourceGroupType.CHANNEL;
  if (isProtocolType(type)) return ResourceGroupType.PROTOCOL;
  if (isStorageType(type)) return ResourceGroupType.STORAGE;
  if (isApiType(type)) return ResourceGroupType.API;
  return undefined;
};

// 根据资源类型查找资源列表
export const filterResourcesByType = (type: string, resourceList: any[]): any[] => {
  return resourceList.filter(resource => resource.resourceType === type);
};

// 根据资源分组查找资源列表
export const filterResourcesByGroup = (group: ResourceGroupType, resourceList: any[]): any[] => {
  return resourceList.filter(resource => {
    const resourceGroup = getResourceGroup(resource.resourceType);
    return resourceGroup === group;
  });
}; 