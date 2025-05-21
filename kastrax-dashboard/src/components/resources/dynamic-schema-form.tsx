import React, { useEffect, useState } from 'react';
import {
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormDescription,
  FormMessage
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Switch } from '@/components/ui/switch';
import { Checkbox } from '@/components/ui/checkbox';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from '@/components/ui/select';
import { Slider } from '@/components/ui/slider';
import { cn } from '@/lib/utils';

// 支持的数据类型
type SchemaType = 'string' | 'number' | 'integer' | 'boolean' | 'array' | 'object';

// JSON Schema属性接口
interface SchemaProperty {
  type: SchemaType;
  title?: string;
  description?: string;
  enum?: any[];
  default?: any;
  format?: string;
  minimum?: number;
  maximum?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  items?: SchemaProperty;
  properties?: Record<string, SchemaProperty>;
  required?: string[];
}

// JSON Schema接口
interface JsonSchema {
  type: SchemaType;
  title?: string;
  description?: string;
  properties?: Record<string, SchemaProperty>;
  required?: string[];
}

interface DynamicSchemaFormProps {
  schema: string | JsonSchema;
  schemaType?: 'json' | 'yaml';
  formContext: any;
  onChange?: (value: any) => void;
  disabled?: boolean;
}

export default function DynamicSchemaForm({
  schema,
  schemaType = 'json',
  formContext,
  onChange,
  disabled = false
}: DynamicSchemaFormProps) {
  const [parsedSchema, setParsedSchema] = useState<JsonSchema | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 解析schema
  useEffect(() => {
    try {
      if (typeof schema === 'string') {
        if (schemaType === 'json') {
          setParsedSchema(JSON.parse(schema));
        } else {
          // 这里需要使用YAML解析库
          // 由于目前没有引入YAML库，暂时不实现YAML解析
          setError('YAML parsing not implemented yet');
        }
      } else {
        setParsedSchema(schema);
      }
      setError(null);
    } catch (e: any) {
      setError(`Schema parsing error: ${e.message}`);
      setParsedSchema(null);
    }
  }, [schema, schemaType]);

  if (error) {
    return <div className="text-red-500">{error}</div>;
  }

  if (!parsedSchema || parsedSchema.type !== 'object') {
    return <div>Invalid schema or non-object schema</div>;
  }

  // 根据schema生成表单字段
  return (
    <div className="space-y-4">
      {parsedSchema.properties && Object.entries(parsedSchema.properties).map(([propertyName, propertySchema]) => (
        <SchemaFormField
          key={propertyName}
          name={propertyName}
          schema={propertySchema}
          formContext={formContext}
          required={parsedSchema.required?.includes(propertyName)}
          disabled={disabled}
        />
      ))}
    </div>
  );
}

interface SchemaFormFieldProps {
  name: string;
  schema: SchemaProperty;
  formContext: any;
  required?: boolean;
  disabled?: boolean;
  path?: string;
}

function SchemaFormField({
  name,
  schema,
  formContext,
  required = false,
  disabled = false,
  path = ''
}: SchemaFormFieldProps) {
  const fullPath = path ? `${path}.${name}` : name;
  const label = schema.title || name;
  const description = schema.description;

  // 根据类型渲染不同的表单控件
  const renderControl = () => {
    switch (schema.type) {
      case 'string':
        if (schema.enum) {
          return (
            <Select
              disabled={disabled}
              onValueChange={(value) => formContext.setValue(fullPath, value)}
              defaultValue={formContext.watch(fullPath) || schema.default}
            >
              <SelectTrigger>
                <SelectValue placeholder={`请选择${label}`} />
              </SelectTrigger>
              <SelectContent>
                {schema.enum.map((option) => (
                  <SelectItem key={option} value={option}>
                    {option}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          );
        } else if (schema.format === 'textarea') {
          return (
            <Textarea
              disabled={disabled}
              placeholder={`请输入${label}`}
              {...formContext.register(fullPath)}
            />
          );
        } else {
          return (
            <Input
              disabled={disabled}
              placeholder={`请输入${label}`}
              type={schema.format === 'password' ? 'password' : 'text'}
              {...formContext.register(fullPath)}
            />
          );
        }
      
      case 'number':
      case 'integer':
        if (schema.minimum !== undefined && schema.maximum !== undefined) {
          return (
            <div className="space-y-2">
              <Slider
                disabled={disabled}
                defaultValue={[formContext.watch(fullPath) || schema.default || schema.minimum]}
                min={schema.minimum}
                max={schema.maximum}
                step={schema.type === 'integer' ? 1 : 0.1}
                onValueChange={(value) => formContext.setValue(fullPath, value[0])}
              />
              <div className="text-sm text-right">
                {formContext.watch(fullPath) || schema.default || schema.minimum}
              </div>
            </div>
          );
        } else {
          return (
            <Input
              disabled={disabled}
              type="number"
              placeholder={`请输入${label}`}
              {...formContext.register(fullPath, {
                valueAsNumber: true
              })}
              min={schema.minimum}
              max={schema.maximum}
              step={schema.type === 'integer' ? 1 : 0.1}
            />
          );
        }
      
      case 'boolean':
        return (
          <div className="flex items-center space-x-2">
            <Switch
              disabled={disabled}
              checked={formContext.watch(fullPath) || false}
              onCheckedChange={(checked) => formContext.setValue(fullPath, checked)}
            />
            <span className="text-sm text-muted-foreground">
              {formContext.watch(fullPath) ? '开启' : '关闭'}
            </span>
          </div>
        );
      
      case 'array':
        if (schema.items && schema.items.enum) {
          // 复选框选项
          return (
            <div className="space-y-2">
              {schema.items.enum.map((option) => {
                const values = formContext.watch(fullPath) || [];
                return (
                  <div key={option} className="flex items-center space-x-2">
                    <Checkbox
                      disabled={disabled}
                      id={`${fullPath}-${option}`}
                      checked={values.includes(option)}
                      onCheckedChange={(checked) => {
                        const currentValues = [...(formContext.watch(fullPath) || [])];
                        if (checked) {
                          formContext.setValue(fullPath, [...currentValues, option]);
                        } else {
                          formContext.setValue(
                            fullPath,
                            currentValues.filter((value: any) => value !== option)
                          );
                        }
                      }}
                    />
                    <label
                      htmlFor={`${fullPath}-${option}`}
                      className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                    >
                      {option}
                    </label>
                  </div>
                );
              })}
            </div>
          );
        }
        return null;
      
      case 'object':
        // 对象类型，递归渲染子属性
        if (schema.properties) {
          return (
            <div className="space-y-4 border rounded p-4">
              {Object.entries(schema.properties).map(([subName, subSchema]) => (
                <SchemaFormField
                  key={subName}
                  name={subName}
                  schema={subSchema as SchemaProperty}
                  formContext={formContext}
                  required={schema.required?.includes(subName)}
                  disabled={disabled}
                  path={fullPath}
                />
              ))}
            </div>
          );
        }
        return null;
      
      default:
        return <div>Unsupported type: {schema.type}</div>;
    }
  };

  return (
    <FormField
      control={formContext.control}
      name={fullPath}
      render={({ field }) => (
        <FormItem>
          <FormLabel className={cn(required && 'required')}>{label}</FormLabel>
          <FormControl>{renderControl()}</FormControl>
          {description && <FormDescription>{description}</FormDescription>}
          <FormMessage />
        </FormItem>
      )}
    />
  );
} 