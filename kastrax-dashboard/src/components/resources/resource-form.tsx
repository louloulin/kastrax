import React, { useState, useEffect } from 'react';
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
import { Textarea } from '@/components/ui/textarea';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { getResourceTypes } from '@/lib/api/resources';

interface ResourceFormProps {
  initialData?: {
    resourceName: string;
    resourceType?: string;
    properties: Record<string, any>;
  };
  resourceType?: string;
  onChange?: (data: any) => void;
  onSubmit?: (data: any) => void;
  readOnly?: boolean;
}

// Define form schema based on resource type
const getFormSchema = (resourceType: string) => {
  const baseSchema = {
    resourceName: z.string().min(1, '资源名称不能为空').max(50, '资源名称不能超过50个字符'),
  };

  // 为不同的资源类型定义不同的属性验证
  switch (resourceType) {
    case 'mysql':
      return z.object({
        ...baseSchema,
        resourceType: z.literal('mysql'),
        properties: z.object({
          host: z.string().min(1, '主机地址不能为空'),
          port: z.string().regex(/^\d+$/, '端口号必须是数字').transform(Number),
          database: z.string().min(1, '数据库名不能为空'),
          username: z.string().min(1, '用户名不能为空'),
          password: z.string().optional(),
          ssl: z.boolean().optional().default(false)
        })
      });
    case 'postgresql':
      return z.object({
        ...baseSchema,
        resourceType: z.literal('postgresql'),
        properties: z.object({
          host: z.string().min(1, '主机地址不能为空'),
          port: z.string().regex(/^\d+$/, '端口号必须是数字').transform(Number),
          database: z.string().min(1, '数据库名不能为空'),
          username: z.string().min(1, '用户名不能为空'),
          password: z.string().optional(),
          ssl: z.boolean().optional().default(false)
        })
      });
    case 'redis':
      return z.object({
        ...baseSchema,
        resourceType: z.literal('redis'),
        properties: z.object({
          host: z.string().min(1, '主机地址不能为空'),
          port: z.string().regex(/^\d+$/, '端口号必须是数字').transform(Number),
          password: z.string().optional(),
          database: z.string().regex(/^\d+$/, '数据库索引必须是数字').transform(Number).optional()
        })
      });
    case 'mongodb':
      return z.object({
        ...baseSchema,
        resourceType: z.literal('mongodb'),
        properties: z.object({
          connectionString: z.string().min(1, '连接字符串不能为空'),
          database: z.string().min(1, '数据库名不能为空')
        })
      });
    case 'http':
      return z.object({
        ...baseSchema,
        resourceType: z.literal('http'),
        properties: z.object({
          baseUrl: z.string().url('请输入有效的URL'),
          headers: z.record(z.string()).optional(),
          auth: z.object({
            type: z.enum(['none', 'basic', 'bearer']),
            username: z.string().optional(),
            password: z.string().optional(),
            token: z.string().optional()
          }).optional()
        })
      });
    default:
      return z.object({
        ...baseSchema,
        resourceType: z.string(),
        properties: z.record(z.any())
      });
  }
};

export default function ResourceForm({ 
  initialData, 
  resourceType = '', 
  onChange, 
  onSubmit,
  readOnly = false 
}: ResourceFormProps) {
  const [resourceTypes, setResourceTypes] = useState<string[]>([]);
  const [selectedType, setSelectedType] = useState(resourceType || initialData?.resourceType || '');
  const [loading, setLoading] = useState(false);

  // 初始化表单
  const form = useForm({
    resolver: zodResolver(getFormSchema(selectedType)),
    defaultValues: {
      resourceName: initialData?.resourceName || '',
      resourceType: selectedType,
      properties: initialData?.properties || {}
    }
  });

  // 加载资源类型列表
  useEffect(() => {
    const fetchResourceTypes = async () => {
      setLoading(true);
      try {
        const response = await getResourceTypes();
        setResourceTypes(response.data.data || []);
      } catch (error) {
        console.error('Failed to fetch resource types:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchResourceTypes();
  }, []);

  // 当选择的资源类型变化时重置表单架构
  useEffect(() => {
    if (selectedType) {
      form.reset({
        resourceName: form.getValues('resourceName'),
        resourceType: selectedType,
        properties: initialData?.properties || {}
      });
    }
  }, [selectedType, form]);

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

  // 根据不同的资源类型渲染不同的属性表单
  const renderPropertiesFields = () => {
    switch (selectedType) {
      case 'mysql':
      case 'postgresql':
        return (
          <>
            <FormField
              control={form.control}
              name="properties.host"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>主机地址</FormLabel>
                  <FormControl>
                    <Input placeholder="localhost" disabled={readOnly} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="properties.port"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>端口</FormLabel>
                  <FormControl>
                    <Input 
                      placeholder={selectedType === 'mysql' ? "3306" : "5432"} 
                      disabled={readOnly} 
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="properties.database"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>数据库名</FormLabel>
                  <FormControl>
                    <Input placeholder="mydatabase" disabled={readOnly} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="properties.username"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>用户名</FormLabel>
                  <FormControl>
                    <Input placeholder="root" disabled={readOnly} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="properties.password"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>密码</FormLabel>
                  <FormControl>
                    <Input 
                      type="password" 
                      placeholder="输入密码" 
                      disabled={readOnly} 
                      {...field} 
                    />
                  </FormControl>
                  <FormDescription>
                    {initialData?.properties?.password && !field.value 
                      ? "密码已保存，留空则不修改" 
                      : ""}
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />
          </>
        );
      
      case 'redis':
        return (
          <>
            <FormField
              control={form.control}
              name="properties.host"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>主机地址</FormLabel>
                  <FormControl>
                    <Input placeholder="localhost" disabled={readOnly} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="properties.port"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>端口</FormLabel>
                  <FormControl>
                    <Input placeholder="6379" disabled={readOnly} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="properties.password"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>密码</FormLabel>
                  <FormControl>
                    <Input 
                      type="password" 
                      placeholder="输入密码（如果需要）" 
                      disabled={readOnly} 
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="properties.database"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>数据库索引</FormLabel>
                  <FormControl>
                    <Input placeholder="0" disabled={readOnly} {...field} />
                  </FormControl>
                  <FormDescription>Redis数据库索引，默认为0</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />
          </>
        );
      
      case 'mongodb':
        return (
          <>
            <FormField
              control={form.control}
              name="properties.connectionString"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>连接字符串</FormLabel>
                  <FormControl>
                    <Input 
                      placeholder="mongodb://localhost:27017" 
                      disabled={readOnly} 
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="properties.database"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>数据库名</FormLabel>
                  <FormControl>
                    <Input placeholder="mydatabase" disabled={readOnly} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </>
        );
      
      case 'http':
        return (
          <>
            <FormField
              control={form.control}
              name="properties.baseUrl"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>基础URL</FormLabel>
                  <FormControl>
                    <Input 
                      placeholder="https://api.example.com" 
                      disabled={readOnly} 
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="properties.auth.type"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>认证类型</FormLabel>
                  <Select 
                    disabled={readOnly}
                    onValueChange={field.onChange}
                    defaultValue={field.value}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="选择认证类型" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="none">无认证</SelectItem>
                      <SelectItem value="basic">Basic认证</SelectItem>
                      <SelectItem value="bearer">Bearer Token</SelectItem>
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            
            {form.watch('properties.auth.type') === 'basic' && (
              <>
                <FormField
                  control={form.control}
                  name="properties.auth.username"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>用户名</FormLabel>
                      <FormControl>
                        <Input disabled={readOnly} {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="properties.auth.password"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>密码</FormLabel>
                      <FormControl>
                        <Input type="password" disabled={readOnly} {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </>
            )}
            
            {form.watch('properties.auth.type') === 'bearer' && (
              <FormField
                control={form.control}
                name="properties.auth.token"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Token</FormLabel>
                    <FormControl>
                      <Input disabled={readOnly} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}
            
            <FormField
              control={form.control}
              name="properties.headers"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>请求头</FormLabel>
                  <FormControl>
                    <Textarea 
                      placeholder='{"Content-Type": "application/json"}' 
                      disabled={readOnly} 
                      {...field}
                      value={field.value ? JSON.stringify(field.value, null, 2) : ''}
                      onChange={e => {
                        try {
                          const parsed = JSON.parse(e.target.value);
                          field.onChange(parsed);
                        } catch {
                          field.onChange(e.target.value);
                        }
                      }}
                    />
                  </FormControl>
                  <FormDescription>
                    请输入JSON格式的请求头配置
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />
          </>
        );
      
      default:
        return (
          <div className="text-muted-foreground p-4 text-center">
            请先选择资源类型
          </div>
        );
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={handleSubmit} className="space-y-8">
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
                  <FormLabel>资源名称</FormLabel>
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
                  <FormLabel>资源类型</FormLabel>
                  <Select 
                    disabled={readOnly || !!resourceType}
                    onValueChange={(value) => {
                      field.onChange(value);
                      setSelectedType(value);
                    }}
                    defaultValue={field.value}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="选择资源类型" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {resourceTypes.map(type => (
                        <SelectItem key={type} value={type}>
                          {type.charAt(0).toUpperCase() + type.slice(1)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        {selectedType && (
          <Card>
            <CardHeader>
              <CardTitle>连接配置</CardTitle>
              <CardDescription>
                配置资源的连接参数
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {renderPropertiesFields()}
            </CardContent>
          </Card>
        )}
      </form>
    </Form>
  );
} 