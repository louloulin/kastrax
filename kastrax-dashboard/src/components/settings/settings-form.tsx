import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { SystemSetting, SettingUpdatePayload } from '@/lib/api/settings';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Switch } from '@/components/ui/switch';
import { Loader2 } from 'lucide-react';

// 验证模式
const settingSchema = z.object({
  value: z.string().min(1, { message: '设置值不能为空' }),
  description: z.string().optional(),
});

type SettingFormValues = z.infer<typeof settingSchema>;

interface SettingsFormProps {
  setting: SystemSetting;
  onSubmit: (values: SettingUpdatePayload) => Promise<void>;
  onCancel?: () => void;
}

export default function SettingsForm({ setting, onSubmit, onCancel }: SettingsFormProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [renderType, setRenderType] = useState(() => {
    // 尝试检测值的类型以选择合适的输入控件
    if (setting.value === 'true' || setting.value === 'false') {
      return 'boolean';
    }
    if (!isNaN(Number(setting.value))) {
      return 'number';
    }
    try {
      JSON.parse(setting.value);
      return 'json';
    } catch (e) {
      return 'string';
    }
  });

  const form = useForm<SettingFormValues>({
    resolver: zodResolver(settingSchema),
    defaultValues: {
      value: setting.value,
      description: setting.description || '',
    },
  });

  const handleSubmit = async (values: SettingFormValues) => {
    setIsSubmitting(true);
    try {
      await onSubmit({
        settingId: setting.settingId,
        value: values.value,
        description: values.description,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderInputField = () => {
    switch (renderType) {
      case 'boolean':
        return (
          <FormField
            control={form.control}
            name="value"
            render={({ field }) => (
              <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                <div className="space-y-0.5">
                  <FormLabel className="text-base">{setting.key}</FormLabel>
                  {setting.description && (
                    <div className="text-sm text-muted-foreground">
                      {setting.description}
                    </div>
                  )}
                </div>
                <FormControl>
                  <Switch
                    checked={field.value === 'true'}
                    onCheckedChange={(checked) => {
                      field.onChange(checked ? 'true' : 'false');
                    }}
                  />
                </FormControl>
              </FormItem>
            )}
          />
        );
      case 'number':
        return (
          <FormField
            control={form.control}
            name="value"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{setting.key}</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    {...field}
                    onChange={(e) => field.onChange(e.target.value)}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        );
      case 'json':
        return (
          <FormField
            control={form.control}
            name="value"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{setting.key}</FormLabel>
                <FormControl>
                  <Textarea
                    className="font-mono min-h-[100px]"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        );
      default:
        return (
          <FormField
            control={form.control}
            name="value"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{setting.key}</FormLabel>
                <FormControl>
                  <Input {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        );
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
        {renderInputField()}

        {renderType !== 'boolean' && (
          <FormField
            control={form.control}
            name="description"
            render={({ field }) => (
              <FormItem>
                <FormLabel>描述</FormLabel>
                <FormControl>
                  <Textarea
                    placeholder="输入设置描述（可选）"
                    className="min-h-[80px]"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        )}

        <div className="flex justify-end gap-2 pt-4">
          {onCancel && (
            <Button type="button" variant="outline" onClick={onCancel}>
              取消
            </Button>
          )}
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                保存中...
              </>
            ) : (
              '保存'
            )}
          </Button>
        </div>
      </form>
    </Form>
  );
} 