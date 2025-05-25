import React, { useState, useEffect, lazy, Suspense } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Form } from '@/components/ui/form';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Loader2 } from 'lucide-react';
import DynamicResourceForm from './dynamic-resource-form';
import DynamicSchemaFormRenderer from './dynamic-schema-form-renderer';
import { resourcePropertyComponentMap } from '@/lib/config/resource.config';

// 基础表单验证模式
const formSchema = z.object({
  resourceName: z.string().min(1, '资源名称不能为空'),
  resourceType: z.string().min(1, '请选择资源类型'),
  properties: z.record(z.any()).optional()
});

interface ResourceFormWrapperProps {
  initialData?: {
    resourceName: string;
    resourceType?: string;
    properties: Record<string, any>;
  };
  resourceTypeDefinitions?: Array<{
    code: string;
    name: string;
    schema: {
      type: 'json' | 'yaml';
      content: string;
    };
  }>;
  onSubmit?: (data: any) => void;
  readOnly?: boolean;
}

export default function ResourceFormWrapper({
  initialData,
  resourceTypeDefinitions = [],
  onSubmit,
  readOnly = false
}: ResourceFormWrapperProps) {
  const [loading, setLoading] = useState(false);
  const [selectedTab, setSelectedTab] = useState('dynamic');
  const [schemaData, setSchemaData] = useState<{
    schema: string;
    schemaType: 'json' | 'yaml';
  } | null>(null);

  // 创建表单
  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: initialData || {
      resourceName: '',
      resourceType: '',
      properties: {}
    }
  });

  // 当资源类型变化时，获取对应的schema
  useEffect(() => {
    const resourceType = form.watch('resourceType');
    if (!resourceType) {
      setSchemaData(null);
      return;
    }

    // 查找自定义资源类型定义
    const typeDef = resourceTypeDefinitions.find(def => def.code === resourceType);
    
    if (typeDef) {
      // 使用自定义schema
      setSchemaData({
        schema: typeDef.schema.content,
        schemaType: typeDef.schema.type
      });
      setSelectedTab('schema');
    } else if (resourcePropertyComponentMap[resourceType]) {
      // 使用预定义组件
      setSelectedTab('dynamic');
    } else {
      // 无法找到对应的资源类型定义
      setSchemaData(null);
    }
  }, [form.watch('resourceType'), resourceTypeDefinitions]);

  // 处理表单提交
  const handleSubmit = async (data: any) => {
    if (onSubmit) {
      setLoading(true);
      try {
        await onSubmit(data);
      } finally {
        setLoading(false);
      }
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)}>
        <Tabs value={selectedTab} onValueChange={setSelectedTab}>
          <TabsList className="mb-4">
            <TabsTrigger value="dynamic">通用资源配置</TabsTrigger>
            <TabsTrigger 
              value="schema" 
              disabled={!schemaData}
            >
              自定义资源配置
            </TabsTrigger>
            <TabsTrigger value="yaml">YAML配置</TabsTrigger>
            <TabsTrigger value="json">JSON配置</TabsTrigger>
          </TabsList>
          
          <TabsContent value="dynamic">
            <DynamicResourceForm
              initialData={initialData}
              onChange={(data) => {
                // 在这里可以同步其他标签页的表单数据
                form.setValue('resourceName', data.resourceName);
                form.setValue('resourceType', data.resourceType);
                form.setValue('properties', data.properties);
              }}
              readOnly={readOnly}
            />
          </TabsContent>
          
          <TabsContent value="schema">
            {schemaData ? (
              <div className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>基本信息</CardTitle>
                    <CardDescription>
                      设置资源的基本信息
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <DynamicSchemaFormRenderer
                      schema={schemaData.schema}
                      schemaType={schemaData.schemaType}
                      initialData={initialData?.properties}
                      onSubmit={(data) => {
                        form.setValue('properties', data);
                      }}
                      readOnly={readOnly}
                      title=""
                    />
                  </CardContent>
                </Card>
              </div>
            ) : (
              <div className="text-center p-8 text-muted-foreground">
                请选择支持Schema配置的资源类型
              </div>
            )}
          </TabsContent>
          
          <TabsContent value="yaml">
            <Card>
              <CardHeader>
                <CardTitle>YAML配置</CardTitle>
                <CardDescription>
                  使用YAML格式配置资源
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="text-center p-8 text-muted-foreground">
                  YAML配置编辑器即将推出...
                </div>
              </CardContent>
            </Card>
          </TabsContent>
          
          <TabsContent value="json">
            <Card>
              <CardHeader>
                <CardTitle>JSON配置</CardTitle>
                <CardDescription>
                  使用JSON格式配置资源
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="text-center p-8 text-muted-foreground">
                  JSON配置编辑器即将推出...
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
        
        {!readOnly && (
          <div className="flex justify-end space-x-2 mt-6">
            <Button type="submit" disabled={loading}>
              {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              保存
            </Button>
          </div>
        )}
      </form>
    </Form>
  );
} 