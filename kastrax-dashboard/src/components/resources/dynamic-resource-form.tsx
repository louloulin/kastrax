import React, { useState, useEffect, lazy, Suspense } from 'react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle
} from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from '@/components/ui/select';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { resourcePropertyComponentMap, resourceTypeList, resourceTypeGroupedList } from '@/lib/config/resource.config';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Loader2 } from 'lucide-react';

// 资源分组类型定义
interface ResourceGroupItem {
  group: string;
  name: string;
  list: Array<{code: string, name: string}>;
}

// 动态加载资源属性组件
const ResourcePropertyComponents: Record<string, React.LazyExoticComponent<any>> = {
  JdbcProperties: lazy(() => import('./forms/jdbc-properties')),
  MqttProperties: lazy(() => import('./forms/mqtt-properties')),
  KafkaProperties: lazy(() => import('./forms/kafka-properties')),
  RedisProperties: lazy(() => import('./forms/redis-properties')),
  OpcuaProperties: lazy(() => import('./forms/opcua-properties')),
  ModbustcpProperties: lazy(() => import('./forms/modbustcp-properties')),
  // 其他组件按需加载，如果对应组件不存在，将会在运行时捕获错误
};

interface DynamicResourceFormProps {
  initialData?: {
    resourceName: string;
    resourceType?: string;
    properties: Record<string, any>;
  };
  onChange?: (data: any) => void;
  onSubmit?: (data: any) => void;
  readOnly?: boolean;
}

// 基础表单验证模式
const formSchema = z.object({
  resourceName: z.string().min(1, '资源名称不能为空').max(50, '资源名称不能超过50个字符'),
  resourceType: z.string().min(1, '请选择资源类型'),
  properties: z.record(z.any()).optional()
});

export default function DynamicResourceForm({
  initialData,
  onChange,
  onSubmit,
  readOnly = false
}: DynamicResourceFormProps) {
  const [resourceTypes, setResourceTypes] = useState<ResourceGroupItem[]>([]);
  const [selectedTab, setSelectedTab] = useState('basic');
  const [loading, setLoading] = useState(false);
  const [propertyComponent, setPropertyComponent] = useState<React.LazyExoticComponent<any> | null>(null);

  // 初始化表单
  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      resourceName: initialData?.resourceName || '',
      resourceType: initialData?.resourceType || '',
      properties: initialData?.properties || {}
    }
  });

  // 加载资源类型列表
  useEffect(() => {
    try {
      setResourceTypes(resourceTypeGroupedList as ResourceGroupItem[]);
    } catch (error) {
      console.error('Failed to load resource types:', error);
      // 如果无法从配置加载，使用备用列表
      setResourceTypes([
        {
          group: 'DATABASE',
          name: '数据库',
          list: [
            { code: 'MYSQL', name: 'MySQL数据库' },
            { code: 'POSTGRESQL', name: 'PostgreSQL数据库' },
            { code: 'REDIS', name: 'Redis数据库' }
          ]
        },
        {
          group: 'CHANNEL',
          name: '消息通道',
          list: [
            { code: 'MQTT', name: 'MQTT消息通道' },
            { code: 'KAFKA', name: 'KAFKA消息通道' }
          ]
        }
      ]);
    }
  }, []);

  // 更新属性组件
  useEffect(() => {
    const resourceType = form.watch('resourceType');
    if (resourceType && resourcePropertyComponentMap[resourceType]) {
      const componentName = resourcePropertyComponentMap[resourceType];
      setPropertyComponent(ResourcePropertyComponents[componentName] || null);
    } else {
      setPropertyComponent(null);
    }
  }, [form.watch('resourceType')]);

  // 表单值变化时触发onChange回调
  useEffect(() => {
    const subscription = form.watch((data) => {
      if (onChange) {
        onChange(data);
      }
    });
    return () => subscription.unsubscribe();
  }, [form, onChange]);

  const handleSubmit = form.handleSubmit((data) => {
    if (onSubmit) {
      onSubmit(data);
    }
  });

  // 渲染属性组件
  const renderPropertyComponent = () => {
    if (!propertyComponent) {
      return (
        <div className="text-muted-foreground p-4 text-center">
          请先选择资源类型
        </div>
      );
    }

    return (
      <Suspense fallback={
        <div className="flex items-center justify-center p-8">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <span className="ml-2">加载组件中...</span>
        </div>
      }>
        {React.createElement(propertyComponent)}
      </Suspense>
    );
  };

  return (
    <Form {...form}>
      <form onSubmit={handleSubmit} className="space-y-8">
        <Tabs value={selectedTab} onValueChange={setSelectedTab}>
          <TabsList className="mb-4">
            <TabsTrigger value="basic">基本信息</TabsTrigger>
            <TabsTrigger value="properties" disabled={!form.watch('resourceType')}>属性配置</TabsTrigger>
          </TabsList>

          <TabsContent value="basic">
            <Card>
              <CardHeader>
                <CardTitle>基本信息</CardTitle>
                <CardDescription>
                  设置资源的基本信息
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <FormField
                  control={form.control}
                  name="resourceName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="required">资源名称</FormLabel>
                      <FormControl>
                        <Input disabled={readOnly} {...field} />
                      </FormControl>
                      <FormDescription>
                        一个用于标识此资源的唯一名称
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="resourceType"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="required">资源类型</FormLabel>
                      <Select
                        disabled={readOnly}
                        onValueChange={(value) => {
                          field.onChange(value);
                          // 当选择资源类型后，可以切换到属性配置选项卡
                          setSelectedTab('properties');
                        }}
                        value={field.value}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="选择资源类型" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {resourceTypes.map(group => (
                            <React.Fragment key={group.group}>
                              <SelectItem value={group.group} disabled>
                                {group.name}
                              </SelectItem>
                              {group.list.map(item => (
                                <SelectItem key={item.code} value={item.code} className="pl-6">
                                  {item.name}
                                </SelectItem>
                              ))}
                            </React.Fragment>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="properties">
            <Card>
              <CardHeader>
                <CardTitle>属性配置</CardTitle>
                <CardDescription>
                  配置资源的连接参数
                </CardDescription>
              </CardHeader>
              <CardContent>
                {renderPropertyComponent()}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </form>
    </Form>
  );
}