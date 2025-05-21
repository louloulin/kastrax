import React, { useState, useRef, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Loader2, Play, SkipForward, Pause, RotateCcw, ListChecks, CheckCircle, XCircle, Save, Copy, Bug, Clock, Maximize, Minimize } from 'lucide-react';
import { useToast } from '@/components/ui/use-toast';
import { executeScript, debugScript, Script, ScriptLanguage, ScriptExecuteResult, ScriptDebugResult } from '@/lib/api/scripts';
import Editor, { Monaco } from '@monaco-editor/react';
import * as monacoEditor from 'monaco-editor';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Badge } from '@/components/ui/badge';

// 脚本执行表单验证模式
const scriptRunFormSchema = z.object({
  params: z.string().optional().default('{}'),
  timeout: z.number().optional().default(30000),
});

type ScriptRunFormValues = z.infer<typeof scriptRunFormSchema>;

interface ScriptRunDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  script: Script | null;
}

// 语言对应的Monaco Editor语言
const languageToEditorMap = {
  [ScriptLanguage.TYPESCRIPT]: 'typescript',
  [ScriptLanguage.JAVASCRIPT]: 'javascript',
  [ScriptLanguage.PYTHON]: 'python',
  [ScriptLanguage.GROOVY]: 'java', // Monaco Editor没有Groovy支持，使用Java作为替代
};

export default function ScriptRunDialog({
  open,
  onOpenChange,
  script,
}: ScriptRunDialogProps) {
  const { toast } = useToast();
  const [activeTab, setActiveTab] = useState('params');
  const [isExecuting, setIsExecuting] = useState(false);
  const [isDebugging, setIsDebugging] = useState(false);
  const [debugMode, setDebugMode] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [executeResult, setExecuteResult] = useState<ScriptExecuteResult | null>(null);
  const [debugResult, setDebugResult] = useState<ScriptDebugResult | null>(null);
  const [currentStep, setCurrentStep] = useState(0);
  const [breakpoints, setBreakpoints] = useState<number[]>([]);
  const editorRef = useRef<monacoEditor.editor.IStandaloneCodeEditor | null>(null);
  const debugDecorations = useRef<string[]>([]);
  
  // 断点的装饰器选项
  const breakpointDecoration = {
    isWholeLine: true,
    className: 'breakpoint-decoration',
    glyphMarginClassName: 'breakpoint-glyph',
  };
  
  // 当前执行步骤的装饰器选项
  const currentStepDecoration = {
    isWholeLine: true,
    className: 'current-step-decoration',
    glyphMarginClassName: 'current-step-glyph',
  };
  
  // 处理编辑器实例初始化
  const handleEditorDidMount = (editor: monacoEditor.editor.IStandaloneCodeEditor, monaco: typeof monacoEditor) => {
    editorRef.current = editor;
    
    // 添加断点点击监听
    editor.onMouseDown((e) => {
      if (e.target.type === monaco.editor.MouseTargetType.GUTTER_GLYPH_MARGIN) {
        const lineNumber = e.target.position?.lineNumber;
        if (lineNumber) {
          toggleBreakpoint(lineNumber);
        }
      }
    });
    
    // 添加全局样式
    const styleElement = document.createElement('style');
    styleElement.textContent = `
      .breakpoint-glyph {
        background: #e51400;
        border-radius: 50%;
        margin-left: 2px;
        width: 8px !important;
        height: 8px !important;
      }
      
      .breakpoint-decoration {
        background: rgba(229, 20, 0, 0.1);
      }
      
      .current-step-glyph {
        background: #007acc;
        width: 0 !important;
        height: 0 !important;
        border-style: solid;
        border-width: 6px 0 6px 8px;
        border-color: transparent transparent transparent #007acc;
        margin-left: 2px;
      }
      
      .current-step-decoration {
        background: rgba(0, 122, 204, 0.1);
      }
    `;
    document.head.appendChild(styleElement);
    
    return () => {
      styleElement.remove();
    };
  };
  
  // 切换断点
  const toggleBreakpoint = (lineNumber: number) => {
    if (breakpoints.includes(lineNumber)) {
      setBreakpoints(breakpoints.filter(bp => bp !== lineNumber));
    } else {
      setBreakpoints([...breakpoints, lineNumber]);
    }
  };
  
  // 更新编辑器装饰
  useEffect(() => {
    if (editorRef.current && debugResult) {
      const monaco = (window as any).monaco;
      
      // 更新断点装饰
      const breakpointDecorations = breakpoints.map(lineNumber => ({
        range: new monaco.Range(lineNumber, 1, lineNumber, 1),
        options: breakpointDecoration
      }));
      
      // 更新当前步骤装饰
      const currentLineDecorations = debugResult && currentStep < debugResult.debugSteps.length 
        ? [{
            range: new monaco.Range(debugResult.debugSteps[currentStep].line, 1, debugResult.debugSteps[currentStep].line, 1),
            options: currentStepDecoration
          }]
        : [];
      
      // 应用装饰
      debugDecorations.current = editorRef.current.deltaDecorations(
        debugDecorations.current,
        [...breakpointDecorations, ...currentLineDecorations]
      );
    }
  }, [breakpoints, debugResult, currentStep]);
  
  // 初始化表单
  const form = useForm<ScriptRunFormValues>({
    resolver: zodResolver(scriptRunFormSchema),
    defaultValues: {
      params: '{}',
      timeout: 30000,
    },
  });
  
  // 当脚本有测试数据时自动填充
  useEffect(() => {
    if (script?.testData) {
      form.setValue('params', script.testData);
    }
  }, [script, form]);
  
  // 执行脚本
  const handleExecute = async (values: ScriptRunFormValues) => {
    if (!script) return;
    
    setIsExecuting(true);
    try {
      // 尝试解析JSON参数
      let params = {};
      try {
        params = JSON.parse(values.params);
      } catch (error) {
        toast({
          variant: "destructive",
          title: "参数格式错误",
          description: "参数必须是有效的JSON格式",
        });
        setIsExecuting(false);
        return;
      }
      
      // 是否为测试执行
      const isTest = Boolean(script.testEnabled);
      
      // 执行脚本
      const response = await executeScript({
        scriptId: script.scriptId,
        params,
        timeout: values.timeout,
        isTest // 指明是否为测试执行
      });
      
      // 设置执行结果
      setExecuteResult(response.data.data);
      
      // 切换到结果标签
      setActiveTab('result');
      
      toast({
        title: response.data.data.success ? "执行成功" : "执行失败",
        description: `脚本 "${script.scriptName}" ${response.data.data.success ? '已成功执行' : '执行失败'}`,
        variant: response.data.data.success ? "default" : "destructive",
      });
    } catch (error) {
      console.error('执行脚本失败:', error);
      toast({
        variant: "destructive",
        title: "执行失败",
        description: "无法执行脚本，请稍后重试",
      });
    } finally {
      setIsExecuting(false);
    }
  };
  
  // 调试脚本
  const handleDebug = async (values: ScriptRunFormValues) => {
    if (!script) return;
    
    setIsDebugging(true);
    setDebugMode(true); // 自动开启调试模式
    try {
      // 尝试解析JSON参数
      let params = {};
      try {
        params = JSON.parse(values.params);
      } catch (error) {
        toast({
          variant: "destructive",
          title: "参数格式错误",
          description: "参数必须是有效的JSON格式",
        });
        setIsDebugging(false);
        return;
      }
      
      // 发送断点信息
      const breakpointLines = breakpoints.length > 0 ? breakpoints : undefined;
      
      // 调试脚本
      const response = await debugScript({
        scriptId: script.scriptId,
        params,
        timeout: values.timeout,
        breakpoints: breakpointLines, // 传递断点信息
      });
      
      // 设置调试结果
      setDebugResult(response.data.data);
      setCurrentStep(0); // 重置步骤
      
      // 切换到调试标签
      setActiveTab('debug');
      
      toast({
        title: response.data.data.success ? "调试成功" : "调试失败",
        description: `脚本 "${script.scriptName}" ${response.data.data.success ? '已成功调试' : '调试失败'}`,
        variant: response.data.data.success ? "default" : "destructive",
      });
    } catch (error) {
      console.error('调试脚本失败:', error);
      toast({
        variant: "destructive",
        title: "调试失败",
        description: "无法调试脚本，请稍后重试",
      });
    } finally {
      setIsDebugging(false);
    }
  };
  
  // 复制结果到剪贴板
  const copyResultToClipboard = () => {
    if (!executeResult) return;
    
    const resultStr = typeof executeResult.returnValue === 'object' 
      ? JSON.stringify(executeResult.returnValue, null, 2) 
      : String(executeResult.returnValue);
    
    navigator.clipboard.writeText(resultStr);
    
    toast({
      title: "已复制",
      description: "执行结果已复制到剪贴板",
    });
  };
  
  // 格式化返回值显示
  const formatReturnValue = (value: any): string => {
    if (value === undefined) return 'undefined';
    if (value === null) return 'null';
    
    try {
      return typeof value === 'object' 
        ? JSON.stringify(value, null, 2) 
        : String(value);
    } catch (e) {
      return String(value);
    }
  };
  
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className={`${isFullscreen ? 'fixed inset-0 max-w-none rounded-none' : 'sm:max-w-[900px]'} max-h-[90vh] overflow-y-auto`}>
        <DialogHeader>
          <div className="flex items-center justify-between">
            <DialogTitle className="flex items-center">
              <span>执行脚本: {script?.scriptName}</span>
              {script && (
                <Badge 
                  className="ml-2"
                  variant="outline"
                >
                  {script.language}
                </Badge>
              )}
            </DialogTitle>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setIsFullscreen(!isFullscreen)}
            >
              {isFullscreen ? (
                <Minimize className="h-4 w-4" />
              ) : (
                <Maximize className="h-4 w-4" />
              )}
            </Button>
          </div>
          <DialogDescription>
            配置执行参数并运行脚本
          </DialogDescription>
        </DialogHeader>
        
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="params">执行参数</TabsTrigger>
            <TabsTrigger value="result">执行结果</TabsTrigger>
            <TabsTrigger value="debug">调试信息</TabsTrigger>
          </TabsList>
          
          <TabsContent value="params">
            <Form {...form}>
              <form onSubmit={form.handleSubmit(handleExecute)} className="space-y-4">
                {/* 参数设置 */}
                <FormField
                  control={form.control}
                  name="params"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>参数 (JSON格式)</FormLabel>
                      <FormControl>
                        <div className="h-[200px] w-full border rounded-md overflow-hidden">
                          <Editor
                            height="100%"
                            language="json"
                            value={field.value}
                            onChange={(value) => field.onChange(value || '{}')}
                            theme="vs-dark"
                            options={{
                              minimap: { enabled: false },
                              fontSize: 14,
                              scrollBeyondLastLine: false,
                              automaticLayout: true,
                              tabSize: 2,
                              lineNumbers: "on",
                              wordWrap: "on",
                            }}
                          />
                        </div>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                {/* 超时设置 */}
                <FormField
                  control={form.control}
                  name="timeout"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>超时时间 (毫秒)</FormLabel>
                      <FormControl>
                        <Input 
                          type="number" 
                          {...field} 
                          onChange={(e) => field.onChange(parseInt(e.target.value))}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                <div className="flex justify-between pt-4">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => onOpenChange(false)}
                    disabled={isExecuting || isDebugging}
                  >
                    取消
                  </Button>
                  
                  <div className="flex gap-2">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={form.handleSubmit(handleDebug)}
                      disabled={isExecuting || isDebugging}
                    >
                      {isDebugging ? (
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      ) : (
                        <Bug className="mr-2 h-4 w-4" />
                      )}
                      调试
                    </Button>
                    <Button
                      type="submit"
                      disabled={isExecuting || isDebugging}
                    >
                      {isExecuting ? (
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      ) : (
                        <Play className="mr-2 h-4 w-4" />
                      )}
                      执行
                    </Button>
                  </div>
                </div>
              </form>
            </Form>
          </TabsContent>
          
          <TabsContent value="result">
            {!executeResult ? (
              <div className="py-8 text-center text-muted-foreground">
                <p>尚未执行脚本，请在参数标签中配置并执行</p>
              </div>
            ) : (
              <div className="space-y-4">
                {/* 状态和时间信息 */}
                <div className="flex justify-between items-center">
                  <div className="flex items-center">
                    {executeResult.success ? (
                      <CheckCircle className="mr-2 h-5 w-5 text-green-500" />
                    ) : (
                      <XCircle className="mr-2 h-5 w-5 text-red-500" />
                    )}
                    <span className={executeResult.success ? 'text-green-500' : 'text-red-500'}>
                      {executeResult.success ? '执行成功' : '执行失败'}
                    </span>
                  </div>
                  <div className="flex items-center text-muted-foreground">
                    <Clock className="mr-1 h-4 w-4" />
                    <span>执行耗时: {executeResult.executionTime} ms</span>
                  </div>
                </div>
                
                {/* 控制台输出 */}
                <div>
                  <h4 className="text-sm font-medium mb-2">控制台输出</h4>
                  <div className="bg-secondary/50 rounded-md p-3 font-mono text-sm whitespace-pre-wrap overflow-auto max-h-[200px]">
                    {executeResult.output || '(无输出)'}
                  </div>
                </div>
                
                {/* 返回值 */}
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <h4 className="text-sm font-medium">返回值</h4>
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      className="h-8 px-2"
                      onClick={copyResultToClipboard}
                    >
                      <Copy className="h-4 w-4 mr-1" /> 复制
                    </Button>
                  </div>
                  <div className="bg-secondary/50 rounded-md p-3 font-mono text-sm whitespace-pre-wrap overflow-auto max-h-[200px]">
                    {executeResult.returnValue !== undefined 
                      ? formatReturnValue(executeResult.returnValue) 
                      : '(无返回值)'}
                  </div>
                </div>
                
                {/* 错误信息 */}
                {executeResult.error && (
                  <div>
                    <h4 className="text-sm font-medium mb-2 text-red-500">错误信息</h4>
                    <div className="bg-red-500/10 rounded-md p-3 font-mono text-sm text-red-600 dark:text-red-400 whitespace-pre-wrap overflow-auto max-h-[200px]">
                      {executeResult.error}
                    </div>
                  </div>
                )}
              </div>
            )}
          </TabsContent>
          
          <TabsContent value="debug">
            {!debugResult ? (
              <div className="py-8 text-center text-muted-foreground">
                <p>尚未调试脚本，请在参数标签中配置并调试</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
                {/* 左侧代码区域 */}
                <div className="md:col-span-3 space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex gap-2">
                      <Label htmlFor="debug-mode" className="cursor-pointer">调试模式</Label>
                      <Switch 
                        id="debug-mode" 
                        checked={debugMode}
                        onCheckedChange={setDebugMode}
                      />
                    </div>
                    {debugMode && (
                      <div className="flex gap-1">
                        <Button 
                          variant="outline" 
                          size="icon" 
                          title="下一步"
                          onClick={() => {
                            if (debugResult && currentStep < debugResult.debugSteps.length - 1) {
                              setCurrentStep(currentStep + 1);
                            }
                          }}
                          disabled={!debugResult || currentStep >= debugResult.debugSteps.length - 1}
                        >
                          <SkipForward className="h-4 w-4" />
                        </Button>
                        <Button 
                          variant="outline" 
                          size="icon" 
                          title="重置"
                          onClick={() => setCurrentStep(0)}
                          disabled={!debugResult || currentStep === 0}
                        >
                          <RotateCcw className="h-4 w-4" />
                        </Button>
                        <Button 
                          variant="outline" 
                          size="icon" 
                          title="查看变量"
                          onClick={() => {
                            // 打开变量查看器
                          }}
                          disabled={!debugResult}
                        >
                          <ListChecks className="h-4 w-4" />
                        </Button>
                      </div>
                    )}
                  </div>

                  {/* 代码编辑器 */}
                  <div className="h-[350px] w-full border rounded-md overflow-hidden">
                    <Editor
                      height="100%"
                      language={script?.language?.toLowerCase() || 'javascript'}
                      value={script?.scriptContent || ''}
                      theme="vs-dark"
                      options={{
                        readOnly: true,
                        minimap: { enabled: false },
                        fontSize: 14,
                        scrollBeyondLastLine: false,
                        automaticLayout: true,
                        tabSize: 2,
                        lineNumbers: "on",
                        glyphMargin: true, // 显示断点区域
                        wordWrap: "on",
                      }}
                      onMount={handleEditorDidMount}
                    />
                  </div>
                  
                  {/* 控制台输出 */}
                  <div>
                    <h4 className="text-sm font-medium mb-2">控制台输出</h4>
                    <div className="bg-secondary/50 rounded-md p-3 font-mono text-sm whitespace-pre-wrap overflow-auto max-h-[150px]">
                      {debugResult.output || '(无输出)'}
                    </div>
                  </div>
                </div>
                
                {/* 右侧变量和状态区域 */}
                <div className="md:col-span-2 space-y-4">
                  {/* 执行状态 */}
                  <Card>
                    <CardHeader className="pb-2">
                      <CardTitle className="text-sm">执行状态</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2">
                      <div className="flex justify-between flex-wrap gap-1">
                        <div className="flex items-center">
                          {debugResult.success ? (
                            <CheckCircle className="mr-2 h-4 w-4 text-green-500" />
                          ) : (
                            <XCircle className="mr-2 h-4 w-4 text-red-500" />
                          )}
                          <span className={debugResult.success ? 'text-green-500' : 'text-red-500'}>
                            {debugResult.success ? '调试成功' : '调试失败'}
                          </span>
                        </div>
                        
                        <div className="flex items-center text-muted-foreground">
                          <Clock className="mr-1 h-4 w-4" />
                          <span>耗时: {debugResult.executionTime} ms</span>
                        </div>
                      </div>
                      
                      <div className="grid grid-cols-2 gap-2 text-sm">
                        {debugResult.memoryUsage && (
                          <div className="text-muted-foreground">
                            内存: {debugResult.memoryUsage}
                          </div>
                        )}
                        
                        {debugResult.cpuTime && (
                          <div className="text-muted-foreground">
                            CPU: {debugResult.cpuTime}
                          </div>
                        )}
                        
                        {debugMode && (
                          <div className="text-muted-foreground col-span-2">
                            当前步骤: {currentStep + 1}/{debugResult.debugSteps?.length || 0}
                          </div>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                  
                  {/* 变量监视器 */}
                  {debugMode && debugResult?.debugSteps && currentStep < debugResult.debugSteps.length && (
                    <Card>
                      <CardHeader className="pb-2">
                        <CardTitle className="text-sm">变量监视器</CardTitle>
                        <CardDescription>步骤 {currentStep + 1} 的变量状态</CardDescription>
                      </CardHeader>
                      <CardContent>
                        <ScrollArea className="h-[200px]">
                          <div className="space-y-1">
                            {Object.entries(debugResult.debugSteps[currentStep].state || {}).map(([key, value]) => (
                              <div key={key} className="flex justify-between py-1 border-b border-border/40 last:border-0">
                                <span className="font-mono text-xs">{key}</span>
                                <span className="font-mono text-xs max-w-[150px] truncate" title={JSON.stringify(value)}>
                                  {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                                </span>
                              </div>
                            ))}
                            
                            {Object.keys(debugResult.debugSteps[currentStep].state || {}).length === 0 && (
                              <div className="text-center text-muted-foreground py-2">
                                无变量数据
                              </div>
                            )}
                          </div>
                        </ScrollArea>
                      </CardContent>
                    </Card>
                  )}
                  
                  {/* 返回值 */}
                  <Card>
                    <CardHeader className="pb-2">
                      <CardTitle className="text-sm">返回值</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="bg-secondary/50 rounded-md p-2 font-mono text-sm whitespace-pre-wrap overflow-auto max-h-[100px]">
                        {debugResult.returnValue !== undefined 
                          ? formatReturnValue(debugResult.returnValue) 
                          : '(无返回值)'}
                      </div>
                    </CardContent>
                  </Card>
                  
                  {/* 错误信息 */}
                  {debugResult.error && (
                    <Card className="border-red-200 dark:border-red-900">
                      <CardHeader className="pb-2">
                        <CardTitle className="text-sm text-red-500">错误信息</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="bg-red-500/10 rounded-md p-2 font-mono text-sm text-red-600 dark:text-red-400 whitespace-pre-wrap overflow-auto max-h-[100px]">
                          {debugResult.error}
                        </div>
                      </CardContent>
                    </Card>
                  )}
                </div>
              </div>
            )}
          </TabsContent>
        </Tabs>
        
        <DialogFooter className="mt-4">
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
          >
            关闭
          </Button>
          
          {executeResult && executeResult.success && (
            <Button 
              variant="secondary"
              onClick={copyResultToClipboard}
            >
              <Copy className="mr-2 h-4 w-4" /> 
              复制结果
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
} 