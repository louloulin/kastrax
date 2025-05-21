import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';

import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Button } from '@/components/ui/button';
import { Eye, EyeOff } from 'lucide-react';

// 验证模式
const variableSchema = z.object({
  variableName: z.string()
    .min(1, { message: '变量名称不能为空' })
    .max(50, { message: '变量名称不能超过50个字符' })
    .regex(/^[a-zA-Z0-9_]+$/, { message: '变量名称只能包含字母、数字和下划线' }),
  variableValue: z.string()
    .min(1, { message: '变量值不能为空' }),
  description: z.string().optional(),
});

export type VariableFormValues = z.infer<typeof variableSchema>;

interface VariableFormProps {
  defaultValues?: Partial<VariableFormValues>;
  onSubmit: (values: VariableFormValues) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export default function VariableForm({
  defaultValues,
  onSubmit,
  onCancel,
  isSubmitting = false,
}: VariableFormProps) {
  const [showValue, setShowValue] = React.useState(false);

  const form = useForm<VariableFormValues>({
    resolver: zodResolver(variableSchema),
    defaultValues: {
      variableName: defaultValues?.variableName || '',
      variableValue: defaultValues?.variableValue || '',
      description: defaultValues?.description || '',
    },
  });

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="variableName"
          render={({ field }) => (
            <FormItem>
              <FormLabel>变量名称</FormLabel>
              <FormControl>
                <Input 
                  placeholder="输入变量名称" 
                  {...field} 
                  disabled={!!defaultValues?.variableName} 
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="variableValue"
          render={({ field }) => (
            <FormItem>
              <FormLabel>变量值</FormLabel>
              <div className="flex gap-2">
                <FormControl className="flex-1">
                  <div className="relative">
                    <Input
                      placeholder="输入变量值"
                      type={showValue ? 'text' : 'password'}
                      {...field}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="absolute right-2 top-1/2 -translate-y-1/2 h-6 w-6 p-0"
                      onClick={() => setShowValue(!showValue)}
                    >
                      {showValue ? (
                        <EyeOff className="h-3.5 w-3.5" />
                      ) : (
                        <Eye className="h-3.5 w-3.5" />
                      )}
                    </Button>
                  </div>
                </FormControl>
              </div>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>描述</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="输入变量的描述信息（可选）"
                  className="min-h-[100px]"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="flex justify-end gap-2 pt-4">
          <Button type="button" variant="outline" onClick={onCancel}>
            取消
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? '保存中...' : '保存'}
          </Button>
        </div>
      </form>
    </Form>
  );
} 