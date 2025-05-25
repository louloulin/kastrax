import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../lib/context/auth-context';
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
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '../ui/tabs';
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from '../ui/alert';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Checkbox } from '../ui/checkbox';
import { Separator } from '../ui/separator';
import { toast } from '../ui/toast';

// 定义登录表单验证模式
const loginFormSchema = z.object({
  username: z.string().min(2, {
    message: '用户名至少需要 2 个字符',
  }),
  password: z.string().min(6, {
    message: '密码至少需要 6 个字符',
  }),
  rememberMe: z.boolean().default(false),
});

// 定义 API Key 表单验证模式
const apiKeyFormSchema = z.object({
  apiKey: z.string().min(10, {
    message: 'API Key 至少需要 10 个字符',
  }),
});

// 定义双因素认证表单验证模式
const twoFactorFormSchema = z.object({
  code: z.string().min(6, {
    message: '验证码至少需要 6 个字符',
  }).max(8),
});

// 定义 OAuth 提供商
interface OAuthProvider {
  id: string;
  name: string;
  icon: string;
}

// OAuth 提供商列表
const oauthProviders: OAuthProvider[] = [
  {
    id: 'google',
    name: 'Google',
    icon: '/icons/google.svg',
  },
  {
    id: 'github',
    name: 'GitHub',
    icon: '/icons/github.svg',
  },
];

export function Login() {
  const { login, isAuthenticated, error: authError, clearError } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  
  const [activeTab, setActiveTab] = useState('password');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [requireTwoFactor, setRequireTwoFactor] = useState(false);
  const [username, setUsername] = useState('');
  const [availableProviders, setAvailableProviders] = useState<OAuthProvider[]>([]);
  
  // 获取重定向 URL
  const searchParams = new URLSearchParams(location.search);
  const returnUrl = searchParams.get('returnUrl') || '/';
  
  // 初始化表单
  const loginForm = useForm<z.infer<typeof loginFormSchema>>({
    resolver: zodResolver(loginFormSchema),
    defaultValues: {
      username: '',
      password: '',
      rememberMe: false,
    },
  });
  
  const apiKeyForm = useForm<z.infer<typeof apiKeyFormSchema>>({
    resolver: zodResolver(apiKeyFormSchema),
    defaultValues: {
      apiKey: '',
    },
  });
  
  const twoFactorForm = useForm<z.infer<typeof twoFactorFormSchema>>({
    resolver: zodResolver(twoFactorFormSchema),
    defaultValues: {
      code: '',
    },
  });
  
  // 如果已经认证，重定向到首页或返回 URL
  useEffect(() => {
    if (isAuthenticated) {
      navigate(returnUrl);
    }
  }, [isAuthenticated, navigate, returnUrl]);
  
  // 获取可用的 OAuth 提供商
  useEffect(() => {
    const fetchOAuthProviders = async () => {
      try {
        const response = await axios.get('/api/v1/auth/oauth2/providers');
        if (response.data.code === 0) {
          const providers = Object.keys(response.data.data).map(id => {
            const provider = oauthProviders.find(p => p.id === id);
            return provider || {
              id,
              name: response.data.data[id],
              icon: '/icons/oauth.svg',
            };
          });
          setAvailableProviders(providers);
        }
      } catch (error) {
        console.error('获取 OAuth 提供商失败:', error);
        // 使用默认提供商
        setAvailableProviders(oauthProviders);
      }
    };
    
    fetchOAuthProviders();
  }, []);
  
  // 处理表单错误
  useEffect(() => {
    if (authError) {
      setError(authError);
    }
  }, [authError]);
  
  // 处理密码登录
  const handlePasswordLogin = async (values: z.infer<typeof loginFormSchema>) => {
    setLoading(true);
    setError(null);
    clearError();
    
    try {
      // 尝试登录
      const success = await login(values.username, values.password);
      
      if (!success) {
        // 检查是否需要双因素认证
        try {
          const response = await axios.get(`/api/v1/auth/2fa/status?username=${values.username}`);
          if (response.data.code === 0 && response.data.data.enabled) {
            setUsername(values.username);
            setRequireTwoFactor(true);
          }
        } catch (error) {
          console.error('检查双因素认证状态失败:', error);
        }
      }
    } catch (error: any) {
      setError(error.message || '登录失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };
  
  // 处理 API Key 登录
  const handleApiKeyLogin = async (values: z.infer<typeof apiKeyFormSchema>) => {
    setLoading(true);
    setError(null);
    
    try {
      // 使用 API Key 验证
      const response = await axios.get('/api/v1/auth/verify', {
        headers: {
          'X-API-Key': values.apiKey,
        },
      });
      
      if (response.data.code === 0) {
        // API Key 验证成功，存储 API Key
        localStorage.setItem('apiKey', values.apiKey);
        
        // 设置 API Key 请求头
        axios.defaults.headers.common['X-API-Key'] = values.apiKey;
        
        // 刷新页面以应用新的认证状态
        window.location.href = returnUrl;
      } else {
        setError(response.data.message || 'API Key 验证失败');
      }
    } catch (error: any) {
      console.error('API Key 登录失败:', error);
      setError(error.response?.data?.message || 'API Key 验证失败，请检查 API Key 是否正确');
    } finally {
      setLoading(false);
    }
  };
  
  // 处理双因素认证验证
  const handleTwoFactorVerify = async (values: z.infer<typeof twoFactorFormSchema>) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await axios.post('/api/v1/auth/2fa/login/verify', {
        username,
        code: values.code,
      });
      
      if (response.data.code === 0) {
        // 双因素认证验证成功，存储 token
        localStorage.setItem('token', response.data.data.token);
        
        // 设置认证请求头
        axios.defaults.headers.common['Authorization'] = `Bearer ${response.data.data.token}`;
        
        // 刷新页面以应用新的认证状态
        window.location.href = returnUrl;
      } else {
        setError(response.data.message || '验证失败');
      }
    } catch (error: any) {
      console.error('双因素认证验证失败:', error);
      setError(error.response?.data?.message || '验证失败，请检查验证码是否正确');
    } finally {
      setLoading(false);
    }
  };
  
  // 处理 OAuth 登录
  const handleOAuthLogin = (providerId: string) => {
    // 重定向到 OAuth 授权页面
    window.location.href = `/api/v1/auth/oauth2/authorize/${providerId}?returnUrl=${encodeURIComponent(returnUrl)}`;
  };
  
  // 渲染双因素认证表单
  if (requireTwoFactor) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-background">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>双因素认证</CardTitle>
            <CardDescription>
              请输入认证器应用中显示的验证码。
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Form {...twoFactorForm}>
              <form onSubmit={twoFactorForm.handleSubmit(handleTwoFactorVerify)} className="space-y-4">
                <FormField
                  control={twoFactorForm.control}
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
                <Button
                  type="submit"
                  className="w-full"
                  disabled={loading}
                >
                  {loading ? '验证中...' : '验证'}
                </Button>
              </form>
            </Form>
          </CardContent>
          <CardFooter className="flex justify-between">
            <Button
              variant="ghost"
              onClick={() => setRequireTwoFactor(false)}
            >
              返回登录
            </Button>
            <Button
              variant="link"
              onClick={() => {
                // 跳转到恢复码页面
                navigate('/recovery-code');
              }}
            >
              使用恢复码
            </Button>
          </CardFooter>
        </Card>
      </div>
    );
  }
  
  // 渲染主登录表单
  return (
    <div className="flex items-center justify-center min-h-screen bg-background">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>登录</CardTitle>
          <CardDescription>
            选择登录方式访问您的账户。
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="password" value={activeTab} onValueChange={setActiveTab}>
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="password">密码</TabsTrigger>
              <TabsTrigger value="apikey">API Key</TabsTrigger>
              <TabsTrigger value="oauth">OAuth</TabsTrigger>
            </TabsList>
            <TabsContent value="password">
              <Form {...loginForm}>
                <form onSubmit={loginForm.handleSubmit(handlePasswordLogin)} className="space-y-4">
                  <FormField
                    control={loginForm.control}
                    name="username"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>用户名</FormLabel>
                        <FormControl>
                          <Input placeholder="输入用户名" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={loginForm.control}
                    name="password"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>密码</FormLabel>
                        <FormControl>
                          <Input
                            type="password"
                            placeholder="输入密码"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <div className="flex items-center justify-between">
                    <FormField
                      control={loginForm.control}
                      name="rememberMe"
                      render={({ field }) => (
                        <FormItem className="flex flex-row items-center space-x-2 space-y-0">
                          <FormControl>
                            <Checkbox
                              checked={field.value}
                              onCheckedChange={field.onChange}
                            />
                          </FormControl>
                          <FormLabel className="text-sm font-normal">
                            记住我
                          </FormLabel>
                        </FormItem>
                      )}
                    />
                    <Button
                      variant="link"
                      className="p-0 h-auto"
                      onClick={() => {
                        // 跳转到忘记密码页面
                        navigate('/forgot-password');
                      }}
                    >
                      忘记密码？
                    </Button>
                  </div>
                  {error && (
                    <Alert variant="destructive">
                      <AlertTitle>登录失败</AlertTitle>
                      <AlertDescription>{error}</AlertDescription>
                    </Alert>
                  )}
                  <Button
                    type="submit"
                    className="w-full"
                    disabled={loading}
                  >
                    {loading ? '登录中...' : '登录'}
                  </Button>
                </form>
              </Form>
            </TabsContent>
            <TabsContent value="apikey">
              <Form {...apiKeyForm}>
                <form onSubmit={apiKeyForm.handleSubmit(handleApiKeyLogin)} className="space-y-4">
                  <FormField
                    control={apiKeyForm.control}
                    name="apiKey"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>API Key</FormLabel>
                        <FormControl>
                          <Input
                            type="password"
                            placeholder="输入 API Key"
                            {...field}
                          />
                        </FormControl>
                        <FormDescription>
                          使用 API Key 进行认证，适用于程序化访问。
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {error && (
                    <Alert variant="destructive">
                      <AlertTitle>认证失败</AlertTitle>
                      <AlertDescription>{error}</AlertDescription>
                    </Alert>
                  )}
                  <Button
                    type="submit"
                    className="w-full"
                    disabled={loading}
                  >
                    {loading ? '认证中...' : '认证'}
                  </Button>
                </form>
              </Form>
            </TabsContent>
            <TabsContent value="oauth">
              <div className="space-y-4">
                <p className="text-sm text-muted-foreground">
                  使用第三方账户登录，无需记住额外的密码。
                </p>
                {availableProviders.length > 0 ? (
                  <div className="space-y-2">
                    {availableProviders.map((provider) => (
                      <Button
                        key={provider.id}
                        variant="outline"
                        className="w-full justify-start"
                        onClick={() => handleOAuthLogin(provider.id)}
                        disabled={loading}
                      >
                        <img
                          src={provider.icon}
                          alt={provider.name}
                          className="mr-2 h-4 w-4"
                        />
                        使用 {provider.name} 登录
                      </Button>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    暂无可用的 OAuth 提供商。
                  </p>
                )}
              </div>
            </TabsContent>
          </Tabs>
        </CardContent>
        <CardFooter className="flex flex-col">
          <Separator className="mb-4" />
          <p className="text-sm text-muted-foreground text-center">
            没有账户？请联系系统管理员创建账户。
          </p>
        </CardFooter>
      </Card>
    </div>
  );
}

export default Login;
