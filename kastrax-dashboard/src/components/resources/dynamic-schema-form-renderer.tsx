import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Form } from '@/components/ui/form';
import { SaveIcon, RotateCcw } from 'lucide-react';
import DynamicSchemaForm from './dynamic-schema-form';

interface DynamicSchemaFormRendererProps {
  schema: string;
  schemaType: 'json' | 'yaml';
  initialData?: Record<string, any>;
  onSubmit?: (data: Record<string, any>) => void;
  onReset?: () => void;
  title?: string;
  submitLabel?: string;
  readOnly?: boolean;
}

export default function DynamicSchemaFormRenderer({
  schema,
  schemaType,
  initialData = {},
  onSubmit,
  onReset,
  title = '属性配置',
  submitLabel = '保存',
  readOnly = false
}: DynamicSchemaFormRendererProps) {
  const [validationSchema, setValidationSchema] = useState<z.ZodType<any>>();
  const [parsedSchema, setParsedSchema] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  // 解析schema
  useEffect(() => {
    try {
      if (schemaType === 'json') {
        const parsed = typeof schema === 'string' ? JSON.parse(schema) : schema;
        setParsedSchema(parsed);
        
        // 构建Zod验证Schema
        const zodSchema = buildZodSchema(parsed);
        setValidationSchema(zodSchema);
      } else {
        // YAML解析需要额外库
        setError('YAML parsing not implemented yet');
      }
    } catch (e: any) {
      setError(`Schema parsing error: ${e.message}`);
    }
  }, [schema, schemaType]);

  // 根据JSON Schema构建Zod验证Schema
  const buildZodSchema = (jsonSchema: any): z.ZodType<any> => {
    if (!jsonSchema || jsonSchema.type !== 'object' || !jsonSchema.properties) {
      return z.any();
    }

    const shape: Record<string, z.ZodType<any>> = {};
    
    Object.entries(jsonSchema.properties).forEach(([key, propSchema]: [string, any]) => {
      let fieldSchema: any; // 使用any类型以允许链式调用
      
      switch (propSchema.type) {
        case 'string':
          fieldSchema = z.string();
          if (propSchema.minLength) fieldSchema = fieldSchema.min(propSchema.minLength);
          if (propSchema.maxLength) fieldSchema = fieldSchema.max(propSchema.maxLength);
          if (propSchema.pattern) fieldSchema = fieldSchema.regex(new RegExp(propSchema.pattern));
          break;
        
        case 'number':
        case 'integer':
          fieldSchema = propSchema.type === 'integer' ? z.number().int() : z.number();
          if (propSchema.minimum !== undefined) fieldSchema = fieldSchema.min(propSchema.minimum);
          if (propSchema.maximum !== undefined) fieldSchema = fieldSchema.max(propSchema.maximum);
          break;
        
        case 'boolean':
          fieldSchema = z.boolean();
          break;
        
        case 'array':
          fieldSchema = z.array(z.any());
          break;
        
        case 'object':
          fieldSchema = buildZodSchema(propSchema);
          break;
        
        default:
          fieldSchema = z.any();
      }
      
      // 处理必填字段
      if (jsonSchema.required && jsonSchema.required.includes(key)) {
        shape[key] = fieldSchema;
      } else {
        shape[key] = fieldSchema.optional();
      }
    });
    
    return z.object(shape);
  };

  // 创建表单
  const form = useForm({
    resolver: validationSchema ? zodResolver(validationSchema) : undefined,
    defaultValues: initialData,
  });

  // 处理表单提交
  const handleSubmit = form.handleSubmit((data) => {
    if (onSubmit) {
      onSubmit(data);
    }
  });

  // 重置表单
  const handleReset = () => {
    form.reset(initialData);
    if (onReset) {
      onReset();
    }
  };

  if (error) {
    return <div className="text-red-500">{error}</div>;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={handleSubmit} className="space-y-6">
            {parsedSchema && (
              <DynamicSchemaForm
                schema={parsedSchema}
                schemaType={schemaType}
                formContext={form}
                disabled={readOnly}
              />
            )}
            
            {!readOnly && (
              <div className="flex justify-end space-x-2">
                <Button 
                  type="button" 
                  variant="outline" 
                  onClick={handleReset}
                >
                  <RotateCcw className="mr-2 h-4 w-4" />
                  重置
                </Button>
                <Button type="submit">
                  <SaveIcon className="mr-2 h-4 w-4" />
                  {submitLabel}
                </Button>
              </div>
            )}
          </form>
        </Form>
      </CardContent>
    </Card>
  );
} 