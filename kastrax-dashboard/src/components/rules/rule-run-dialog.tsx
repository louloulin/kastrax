import React, { useState } from 'react';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Textarea } from '@/components/ui/textarea';
import { Input } from '@/components/ui/input';
import { executeRule, Rule, RuleExecuteResult } from '@/lib/api/rules';
import { useToast } from '@/components/ui/use-toast';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { ReloadIcon } from '@radix-ui/react-icons';
import { cn } from '@/lib/utils';

interface RuleRunDialogProps {
  rule: Rule;
  open: boolean;
  onClose: () => void;
}

const formSchema = z.object({
  params: z.string().optional(),
  timeout: z.coerce.number().min(1).max(60000).optional(),
});

const RuleRunDialog: React.FC<RuleRunDialogProps> = ({ rule, open, onClose }) => {
  const [executing, setExecuting] = useState(false);
  const [result, setResult] = useState<RuleExecuteResult | null>(null);
  const { toast } = useToast();

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      params: '',
      timeout: 30000,
    },
  });

  const handleExecute = async (data: z.infer<typeof formSchema>) => {
    setExecuting(true);
    setResult(null);

    try {
      let parsedParams = {};
      if (data.params && data.params.trim() !== '') {
        try {
          parsedParams = JSON.parse(data.params);
        } catch (e) {
          toast({
            title: 'JSON格式错误',
            description: '请输入有效的JSON格式参数',
            variant: 'destructive',
          });
          setExecuting(false);
          return;
        }
      }

      const response = await executeRule({
        ruleId: rule.ruleId,
        params: parsedParams,
        timeout: data.timeout,
      });

      const executionResult = response.data.data;
      setResult(executionResult);
      
      if (executionResult.success) {
        toast({
          title: '规则执行成功',
          description: `规则 "${rule.ruleName}" 已成功执行`,
        });
      } else {
        toast({
          title: '规则执行失败',
          description: executionResult.error || '执行过程中发生错误',
          variant: 'destructive',
        });
      }
    } catch (error: any) {
      console.error('Failed to execute rule:', error);
      toast({
        title: '执行失败',
        description: error.response?.data?.msg || '请求执行规则失败',
        variant: 'destructive',
      });
    } finally {
      setExecuting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>执行规则</DialogTitle>
          <DialogDescription>
            {rule.ruleName} ({rule.type})
            {rule.description && <p className="mt-1">{rule.description}</p>}
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleExecute)} className="space-y-4">
            <FormField
              control={form.control}
              name="params"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>参数 (JSON格式)</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder='{"key": "value"}'
                      className="h-32 font-mono"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="timeout"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>超时时间 (毫秒)</FormLabel>
                  <FormControl>
                    <Input type="number" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {result && (
              <>
                <Separator className="my-4" />
                <Card>
                  <CardHeader className={cn(
                    "py-2",
                    result.success ? "bg-green-50" : "bg-red-50"
                  )}>
                    <CardTitle className={cn(
                      "text-sm font-medium",
                      result.success ? "text-green-700" : "text-red-700"
                    )}>
                      执行结果: {result.success ? '成功' : '失败'}
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="p-4 space-y-2">
                    <div>
                      <span className="font-medium">执行时间:</span> {result.executionTime}ms
                    </div>
                    {result.error && (
                      <div>
                        <span className="font-medium text-red-600">错误:</span> {result.error}
                      </div>
                    )}
                    {result.output && (
                      <div>
                        <span className="font-medium">输出:</span>
                        <pre className="mt-2 bg-slate-100 p-2 rounded text-sm overflow-auto">
                          {result.output}
                        </pre>
                      </div>
                    )}
                    {result.result && (
                      <div>
                        <span className="font-medium">返回值:</span>
                        <pre className="mt-2 bg-slate-100 p-2 rounded text-sm overflow-auto">
                          {JSON.stringify(result.result, null, 2)}
                        </pre>
                      </div>
                    )}
                    {result.path && result.path.length > 0 && (
                      <div>
                        <span className="font-medium">执行路径:</span>
                        <div className="mt-2 text-sm">
                          {result.path.join(' → ')}
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </>
            )}

            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                关闭
              </Button>
              <Button type="submit" disabled={executing}>
                {executing && <ReloadIcon className="mr-2 h-4 w-4 animate-spin" />}
                {executing ? '执行中...' : '执行'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
};

export default RuleRunDialog; 