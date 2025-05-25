import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { CheckIcon, CopyIcon, Cross2Icon } from '@radix-ui/react-icons';
import axios from 'axios';

import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '../ui/card';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '../ui/form';
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from '../ui/alert';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { toast } from '../ui/toast';
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '../ui/tabs';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import {
  Step,
  StepDescription,
  StepIndicator,
  StepNumber,
  StepSeparator,
  StepStatus,
  StepTitle,
  Stepper,
  useSteps,
} from '../ui/stepper';

// 定义双因素认证设置类型
interface TwoFactorSetup {
  secretKey: string;
  totpUri: string;
  recoveryCodes: string[];
}

// 定义表单验证模式
const verificationFormSchema = z.object({
  code: z.string().min(6, {
    message: '验证码必须至少有 6 个字符',
  }).max(8),
});

// 步骤定义
const steps = [
  { title: '扫描二维码', description: '使用认证器应用扫描二维码' },
  { title: '验证设置', description: '输入验证码以确认设置' },
  { title: '保存恢复码', description: '保存恢复码以备不时之需' },
];

export function TwoFactorSetup() {
  const [setupData, setSetupData] = useState<TwoFactorSetup | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [enabled, setEnabled] = useState(false);
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedRecoveryCodes, setCopiedRecoveryCodes] = useState(false);
  const [showDisableDialog, setShowDisableDialog] = useState(false);
  const [disableCode, setDisableCode] = useState('');
  const [disableLoading, setDisableLoading] = useState(false);

  // 步骤控制
  const { activeStep, setActiveStep } = useSteps({
    index: 0,
    count: steps.length,
  });

  // 初始化表单
  const form = useForm<z.infer<typeof verificationFormSchema>>({
    resolver: zodResolver(verificationFormSchema),
    defaultValues: {
      code: '',
    },
  });

  // 检查双因素认证状态
  useEffect(() => {
    checkTwoFactorStatus();
  }, []);

  // 检查双因素认证状态
  const checkTwoFactorStatus = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/v1/auth/2fa/status');
      if (response.data.code === 0) {
        setEnabled(response.data.data.enabled);
      }
    } catch (error) {
      console.error('检查双因素认证状态失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 初始化双因素认证设置
  const initializeSetup = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.post('/api/v1/auth/2fa/setup');
      if (response.data.code === 0) {
        setSetupData(response.data.data);
        setActiveStep(0);
      } else {
        setError(response.data.message || '初始化双因素认证设置失败');
      }
    } catch (error: any) {
      console.error('初始化双因素认证设置失败:', error);
      setError(error.response?.data?.message || '初始化双因素认证设置失败，请检查网络连接');
    } finally {
      setLoading(false);
    }
  };

  // 验证并启用双因素认证
  const onSubmit = async (values: z.infer<typeof verificationFormSchema>) => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.post('/api/v1/auth/2fa/verify', {
        code: values.code,
        secretKey: setupData?.secretKey
      });

      if (response.data.code === 0) {
        setSuccess(true);
        setEnabled(true);
        setActiveStep(2);
        toast({
          title: '双因素认证已启用',
          description: '您的账户现在受到双因素认证的保护',
        });
      } else {
        setError(response.data.message || '验证失败');
      }
    } catch (error: any) {
      console.error('验证失败:', error);
      setError(error.response?.data?.message || '验证失败，请检查验证码是否正确');
    } finally {
      setLoading(false);
    }
  };

  // 禁用双因素认证
  const disableTwoFactor = async () => {
    setDisableLoading(true);
    try {
      const response = await axios.post('/api/v1/auth/2fa/disable', {
        code: disableCode
      });

      if (response.data.code === 0) {
        setEnabled(false);
        setShowDisableDialog(false);
        setDisableCode('');
        toast({
          title: '双因素认证已禁用',
          description: '您的账户不再受到双因素认证的保护',
        });
      } else {
        toast({
          title: '禁用双因素认证失败',
          description: response.data.message || '请检查验证码是否正确',
          variant: 'destructive',
        });
      }
    } catch (error: any) {
      console.error('禁用双因素认证失败:', error);
      toast({
        title: '禁用双因素认证失败',
        description: error.response?.data?.message || '请检查验证码是否正确',
        variant: 'destructive',
      });
    } finally {
      setDisableLoading(false);
    }
  };

  // 复制文本到剪贴板
  const copyToClipboard = (text: string, type: 'code' | 'recoveryCodes') => {
    navigator.clipboard.writeText(text).then(
      () => {
        if (type === 'code') {
          setCopiedCode(true);
          setTimeout(() => setCopiedCode(false), 2000);
        } else {
          setCopiedRecoveryCodes(true);
          setTimeout(() => setCopiedRecoveryCodes(false), 2000);
        }
        
        toast({
          title: '已复制',
          description: type === 'code' ? '密钥已复制到剪贴板' : '恢复码已复制到剪贴板',
        });
      },
      () => {
        toast({
          title: '复制失败',
          description: '无法复制到剪贴板',
          variant: 'destructive',
        });
      }
    );
  };

  // 下一步
  const nextStep = () => {
    setActiveStep((prev) => Math.min(prev + 1, steps.length - 1));
  };

  // 上一步
  const prevStep = () => {
    setActiveStep((prev) => Math.max(prev - 1, 0));
  };

  // 重置设置
  const resetSetup = () => {
    setSetupData(null);
    setSuccess(false);
    setError(null);
    form.reset();
  };

  // 渲染步骤内容
  const renderStepContent = () => {
    switch (activeStep) {
      case 0:
        return (
          <div className="space-y-4">
            <div className="flex justify-center">
              {setupData?.totpUri && (
                <div className="p-4 bg-white rounded-md">
                  <img
                    src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(
                      setupData.totpUri
                    )}`}
                    alt="TOTP QR Code"
                    width={200}
                    height={200}
                  />
                </div>
              )}
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground text-center">
                使用 Google Authenticator、Authy 或其他认证器应用扫描上面的二维码。
              </p>
              <p className="text-sm text-muted-foreground text-center">
                或者，您可以手动输入以下密钥：
              </p>
              <div className="flex items-center space-x-2">
                <Input
                  value={setupData?.secretKey}
                  readOnly
                  className="font-mono text-center"
                />
                <Button
                  size="icon"
                  variant="outline"
                  onClick={() => setupData?.secretKey && copyToClipboard(setupData.secretKey, 'code')}
                >
                  {copiedCode ? (
                    <CheckIcon className="h-4 w-4" />
                  ) : (
                    <CopyIcon className="h-4 w-4" />
                  )}
                </Button>
              </div>
            </div>
            <div className="flex justify-end">
              <Button onClick={nextStep}>下一步</Button>
            </div>
          </div>
        );
      
      case 1:
        return (
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              请输入认证器应用中显示的 6 位验证码，以验证设置是否正确。
            </p>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="code"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>验证码</FormLabel>
                      <FormControl>
                        <Input
                          placeholder="输入 6 位验证码"
                          {...field}
                          maxLength={8}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                {error && (
                  <Alert variant="destructive">
                    <AlertTitle>验证失败</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                  </Alert>
                )}
                <div className="flex justify-between">
                  <Button type="button" variant="outline" onClick={prevStep}>
                    上一步
                  </Button>
                  <Button type="submit" disabled={loading}>
                    {loading ? '验证中...' : '验证并启用'}
                  </Button>
                </div>
              </form>
            </Form>
          </div>
        );
      
      case 2:
        return (
          <div className="space-y-4">
            <Alert>
              <AlertTitle>设置成功</AlertTitle>
              <AlertDescription>
                双因素认证已成功启用。请保存以下恢复码，以便在无法访问认证器应用时使用。
              </AlertDescription>
            </Alert>
            <div className="p-4 bg-muted rounded-md">
              <div className="grid grid-cols-2 gap-2">
                {setupData?.recoveryCodes.map((code, index) => (
                  <div key={index} className="font-mono text-sm p-2 bg-background rounded border">
                    {code}
                  </div>
                ))}
              </div>
              <div className="mt-4 flex justify-end">
                <Button
                  variant="outline"
                  onClick={() => setupData?.recoveryCodes && copyToClipboard(setupData.recoveryCodes.join('\n'), 'recoveryCodes')}
                >
                  {copiedRecoveryCodes ? (
                    <>
                      <CheckIcon className="mr-2 h-4 w-4" />
                      已复制
                    </>
                  ) : (
                    <>
                      <CopyIcon className="mr-2 h-4 w-4" />
                      复制所有恢复码
                    </>
                  )}
                </Button>
              </div>
            </div>
            <div className="flex justify-end">
              <Button onClick={() => window.location.reload()}>完成</Button>
            </div>
          </div>
        );
      
      default:
        return null;
    }
  };

  return (
    <div className="container mx-auto py-6">
      <Card>
        <CardHeader>
          <CardTitle>双因素认证</CardTitle>
          <CardDescription>
            设置双因素认证以增强账户安全性。每次登录时，您都需要输入认证器应用生成的验证码。
          </CardDescription>
        </CardHeader>
        <CardContent>
          {enabled ? (
            <div className="space-y-4">
              <Alert>
                <CheckIcon className="h-4 w-4" />
                <AlertTitle>双因素认证已启用</AlertTitle>
                <AlertDescription>
                  您的账户受到双因素认证的保护。每次登录时，您都需要输入认证器应用生成的验证码。
                </AlertDescription>
              </Alert>
              <div className="flex justify-end">
                <Button
                  variant="destructive"
                  onClick={() => setShowDisableDialog(true)}
                >
                  禁用双因素认证
                </Button>
              </div>
            </div>
          ) : setupData ? (
            <div className="space-y-6">
              <Stepper index={activeStep}>
                {steps.map((step, index) => (
                  <Step key={index}>
                    <StepIndicator>
                      <StepStatus
                        complete={<CheckIcon />}
                        incomplete={<StepNumber>{index + 1}</StepNumber>}
                        active={<StepNumber>{index + 1}</StepNumber>}
                      />
                    </StepIndicator>
                    <div className="flex flex-col">
                      <StepTitle>{step.title}</StepTitle>
                      <StepDescription>{step.description}</StepDescription>
                    </div>
                    <StepSeparator />
                  </Step>
                ))}
              </Stepper>
              <div className="mt-6">
                {renderStepContent()}
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              <p>
                双因素认证为您的账户添加了额外的安全层。启用后，除了密码外，您还需要输入认证器应用生成的验证码才能登录。
              </p>
              <div className="flex justify-end">
                <Button
                  onClick={initializeSetup}
                  disabled={loading}
                >
                  {loading ? '加载中...' : '设置双因素认证'}
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 禁用双因素认证对话框 */}
      <Dialog open={showDisableDialog} onOpenChange={setShowDisableDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>禁用双因素认证</DialogTitle>
            <DialogDescription>
              禁用双因素认证将降低您账户的安全性。请输入认证器应用中的验证码以确认。
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="disable-code" className="text-sm font-medium">
                验证码
              </label>
              <Input
                id="disable-code"
                value={disableCode}
                onChange={(e) => setDisableCode(e.target.value)}
                placeholder="输入 6 位验证码"
                maxLength={8}
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setShowDisableDialog(false)}
            >
              取消
            </Button>
            <Button
              variant="destructive"
              onClick={disableTwoFactor}
              disabled={disableLoading || !disableCode}
            >
              {disableLoading ? '禁用中...' : '禁用双因素认证'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default TwoFactorSetup;
