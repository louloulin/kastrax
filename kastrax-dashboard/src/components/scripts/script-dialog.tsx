import React, { useEffect, useState } from 'react';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import Editor from '@monaco-editor/react';
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle,
  DialogFooter
} from '@/components/ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from '@/components/ui/form';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { PlusCircle, X, Loader2, CheckCircle, XCircle, Clock } from 'lucide-react';
import { useToast } from '@/components/ui/use-toast';
import { Script, ScriptLanguage, createScript, updateScript } from '@/lib/api/scripts';
import { Switch } from '@/components/ui/switch';

// 扩展Script类型以包含测试相关属性
interface ExtendedScript extends Script {
  testData?: string;
  testEnabled?: boolean;
}

// 表单验证模式
const scriptFormSchema = z.object({
  scriptName: z.string()
    .min(2, { message: '脚本名称至少需要2个字符' })
    .max(100, { message: '脚本名称不能超过100个字符' }),
  language: z.nativeEnum(ScriptLanguage, {
    errorMap: () => ({ message: '请选择有效的脚本语言' })
  }),
  description: z.string().optional(),
  scriptContent: z.string()
    .min(1, { message: '脚本内容不能为空' }),
  dependencies: z.array(z.string()).optional(),
  testData: z.string().optional().default('{}'),
  testEnabled: z.boolean().optional().default(false)
});

type ScriptFormValues = z.infer<typeof scriptFormSchema>;

interface ScriptDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  script?: Script;
  onSuccess?: (data: any) => void;
}

// 脚本语言对应的Monaco Editor语言
const languageToEditorMap = {
  [ScriptLanguage.TYPESCRIPT]: 'typescript',
  [ScriptLanguage.JAVASCRIPT]: 'javascript',
  [ScriptLanguage.PYTHON]: 'python',
  [ScriptLanguage.GROOVY]: 'java', // Monaco Editor没有Groovy支持，使用Java作为替代
};

// 各语言的示例代码
const languageTemplates = {
  [ScriptLanguage.TYPESCRIPT]: `// TypeScript示例脚本
const sayHello = (name: string): string => {
  return \`Hello, \${name}!\`;
};

// 输出结果
console.log(sayHello("World"));
return sayHello("World");`,

  [ScriptLanguage.JAVASCRIPT]: `// JavaScript示例脚本
function transformData(data) {
  return data.map(item => ({
    ...item,
    fullName: \`\${item.firstName} \${item.lastName}\`,
    isActive: Boolean(item.status === 'active')
  }));
}

// 测试数据
const testData = [
  { id: 1, firstName: 'John', lastName: 'Doe', status: 'active' },
  { id: 2, firstName: 'Jane', lastName: 'Smith', status: 'inactive' }
];

// 输出结果
const result = transformData(testData);
console.log(JSON.stringify(result, null, 2));
return result;`,

  [ScriptLanguage.PYTHON]: `# Python示例脚本
def analyze_data(data):
    """分析数据并返回统计结果"""
    if not data:
        return {"error": "No data provided"}
    
    total = sum(data)
    count = len(data)
    avg = total / count if count > 0 else 0
    
    return {
        "total": total,
        "count": count,
        "average": avg,
        "min": min(data) if data else None,
        "max": max(data) if data else None
    }

# 测试数据
test_data = [12, 45, 67, 89, 23, 56]

# 分析并输出结果
result = analyze_data(test_data)
print(f"Analysis result: {result}")

# 返回结果
return result`,

  [ScriptLanguage.GROOVY]: `// Groovy示例脚本
def processText(text) {
    def words = text.split(/\\s+/)
    def wordCount = words.size()
    def charCount = text.replaceAll(/\\s+/, '').size()
    
    def result = [
        originalText: text,
        wordCount: wordCount,
        charCount: charCount,
        upperCase: text.toUpperCase(),
        lowerCase: text.toLowerCase(),
        reversed: new StringBuilder(text).reverse().toString()
    ]
    
    println "处理结果: \${result}"
    return result
}

// 测试数据
def sampleText = "The quick brown fox jumps over the lazy dog"

// 输出结果
def processResult = processText(sampleText)
return processResult`
};

export default function ScriptDialog({ open, onOpenChange, script, onSuccess }: ScriptDialogProps) {
  const { toast } = useToast();
  const [activeTab, setActiveTab] = useState('basic');
  const [dependencies, setDependencies] = useState<string[]>([]);
  const [newDependency, setNewDependency] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isTestRunning, setIsTestRunning] = useState(false);
  const [testResult, setTestResult] = useState<any>(null);
  
  // 初始化表单
  const form = useForm<ScriptFormValues>({
    resolver: zodResolver(scriptFormSchema),
    defaultValues: {
      scriptName: '',
      language: ScriptLanguage.JAVASCRIPT,
      description: '',
      scriptContent: languageTemplates[ScriptLanguage.JAVASCRIPT],
      dependencies: [],
      testData: '{}',
      testEnabled: false
    }
  });
  
  // 当脚本对象变化时更新表单
  useEffect(() => {
    if (script) {
      // 编辑模式
      const extendedScript = script as ExtendedScript;
      form.reset({
        scriptName: script.scriptName,
        language: script.language,
        description: script.description || '',
        scriptContent: script.scriptContent,
        dependencies: script.dependencies || [],
        testData: extendedScript.testData || '{}',
        testEnabled: extendedScript.testEnabled || false
      });
      setDependencies(script.dependencies || []);
    } else {
      // 创建模式
      const defaultLanguage = ScriptLanguage.JAVASCRIPT;
      form.reset({
        scriptName: '',
        language: defaultLanguage,
        description: '',
        scriptContent: languageTemplates[defaultLanguage],
        dependencies: [],
        testData: '{}',
        testEnabled: false
      });
      setDependencies([]);
    }
  }, [script, form]);
  
  // 语言变化时更新编辑器模板（仅针对新建脚本）
  const handleLanguageChange = (value: ScriptLanguage) => {
    form.setValue('language', value);
    
    // 如果是新建脚本或内容为空，设置示例模板
    if (!script || !form.getValues('scriptContent')) {
      form.setValue('scriptContent', languageTemplates[value]);
    }
  };
  
  // 添加依赖
  const addDependency = () => {
    if (!newDependency.trim()) return;
    
    // 不允许重复的依赖
    if (dependencies.includes(newDependency)) {
      toast({
        variant: "destructive",
        title: "依赖已存在",
        description: `${newDependency} 已在依赖列表中`
      });
      return;
    }
    
    const updatedDependencies = [...dependencies, newDependency];
    setDependencies(updatedDependencies);
    form.setValue('dependencies', updatedDependencies);
    setNewDependency('');
  };
  
  // 移除依赖
  const removeDependency = (dependency: string) => {
    const updatedDependencies = dependencies.filter(d => d !== dependency);
    setDependencies(updatedDependencies);
    form.setValue('dependencies', updatedDependencies);
  };
  
  // 按下回车添加依赖
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addDependency();
    }
  };
  
  // 处理表单提交
  const onSubmit = async (values: ScriptFormValues) => {
    setIsSubmitting(true);
    try {
      if (script) {
        // 更新脚本
        const response = await updateScript({
          scriptId: script.scriptId,
          scriptName: values.scriptName,
          language: values.language,
          description: values.description,
          scriptContent: values.scriptContent,
          dependencies: values.dependencies,
          testData: values.testData, // 添加测试数据
          testEnabled: values.testEnabled // 添加测试启用状态
        });
        
        toast({
          title: "更新成功",
          description: `脚本 "${values.scriptName}" 已成功更新`,
        });
        
        onSuccess && onSuccess(response.data.data);
      } else {
        // 创建脚本
        const response = await createScript({
          scriptName: values.scriptName,
          language: values.language,
          description: values.description,
          scriptContent: values.scriptContent,
          dependencies: values.dependencies,
          testData: values.testData, // 添加测试数据
          testEnabled: values.testEnabled // 添加测试启用状态
        });
        
        toast({
          title: "创建成功",
          description: `脚本 "${values.scriptName}" 已成功创建`,
        });
        
        onSuccess && onSuccess(response.data.data);
      }
      
      onOpenChange(false);
    } catch (error) {
      console.error('保存脚本失败:', error);
      
      toast({
        variant: "destructive",
        title: "保存失败",
        description: "无法保存脚本，请稍后重试",
      });
    } finally {
      setIsSubmitting(false);
    }
  };
  
  // 测试脚本
  const handleTest = async () => {
    try {
      const values = form.getValues();
      setIsTestRunning(true);
      
      // 验证脚本内容
      if (!values.scriptContent?.trim()) {
        toast({
          variant: "destructive",
          title: "测试失败",
          description: "脚本内容不能为空",
        });
        setIsTestRunning(false);
        return;
      }
      
      // 解析测试数据
      let testParams = {};
      try {
        testParams = JSON.parse(values.testData || '{}');
      } catch (error) {
        toast({
          variant: "destructive",
          title: "测试数据格式错误",
          description: "测试数据必须是有效的JSON格式",
        });
        setIsTestRunning(false);
        return;
      }
      
      // 创建临时脚本对象用于测试
      const tempScript = {
        scriptId: script?.scriptId || 'temp-' + Date.now(),
        scriptName: values.scriptName || '临时测试脚本',
        language: values.language,
        scriptContent: values.scriptContent,
        description: values.description,
        dependencies: values.dependencies,
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString()
      };
      
      // 模拟测试结果（实际环境可以调用API）
      const mockTestResult = {
        success: true,
        executionTime: Math.floor(Math.random() * 200) + 50,
        output: `[测试] 执行脚本: ${tempScript.scriptName}\n> 正在执行...\n> 执行完成。`,
        returnValue: await simulateExecution(tempScript.scriptContent, testParams)
      };
      
      setTestResult(mockTestResult);
      
      toast({
        title: "测试完成",
        description: `脚本测试已完成，${mockTestResult.success ? '执行成功' : '执行失败'}`,
        variant: mockTestResult.success ? "default" : "destructive",
      });
    } catch (error: any) {
      console.error('测试脚本失败:', error);
      setTestResult({
        success: false,
        executionTime: 0,
        output: `[错误] 测试脚本时出现异常: ${error?.message || '未知错误'}`,
        error: error?.message || '未知错误'
      });
      
      toast({
        variant: "destructive",
        title: "测试失败",
        description: "无法测试脚本，请检查代码和参数",
      });
    } finally {
      setIsTestRunning(false);
    }
  };
  
  // 简单模拟脚本执行（仅用于前端测试）
  const simulateExecution = async (code: string, params: any) => {
    // 注意：这只是一个简单的模拟，实际环境应通过API调用后端执行
    try {
      // 创建一个函数，将参数传递给它
      const func = new Function('params', `
        try {
          ${code}
        } catch (error) {
          return { error: error.message };
        }
      `);
      
      // 执行函数并返回结果
      return func(params) || { result: '执行成功，但没有返回值' };
    } catch (e: any) {
      console.error('模拟执行失败:', e);
      return { error: e?.message || '执行时发生错误' };
    }
  };
  
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[900px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {script ? `编辑脚本: ${script.scriptName}` : '创建新脚本'}
          </DialogTitle>
        </DialogHeader>
        
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
            <Tabs value={activeTab} onValueChange={setActiveTab}>
              <TabsList className="grid w-full grid-cols-3">
                <TabsTrigger value="basic">基本信息</TabsTrigger>
                <TabsTrigger value="code">脚本代码</TabsTrigger>
                <TabsTrigger value="test">测试</TabsTrigger>
              </TabsList>
              
              <TabsContent value="basic" className="space-y-4">
                {/* 脚本名称 */}
                <FormField
                  control={form.control}
                  name="scriptName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>脚本名称</FormLabel>
                      <FormControl>
                        <Input placeholder="输入脚本名称" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                {/* 脚本语言 */}
                <FormField
                  control={form.control}
                  name="language"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>脚本语言</FormLabel>
                      <Select 
                        onValueChange={(value) => handleLanguageChange(value as ScriptLanguage)} 
                        defaultValue={field.value}
                        value={field.value}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="选择脚本语言" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value={ScriptLanguage.TYPESCRIPT}>TypeScript</SelectItem>
                          <SelectItem value={ScriptLanguage.JAVASCRIPT}>JavaScript</SelectItem>
                          <SelectItem value={ScriptLanguage.PYTHON}>Python</SelectItem>
                          <SelectItem value={ScriptLanguage.GROOVY}>Groovy</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                {/* 脚本描述 */}
                <FormField
                  control={form.control}
                  name="description"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>脚本描述</FormLabel>
                      <FormControl>
                        <Textarea 
                          placeholder="描述脚本的功能和用途" 
                          className="resize-none h-20"
                          {...field} 
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                {/* 依赖管理 */}
                <div className="space-y-2">
                  <FormLabel>依赖管理</FormLabel>
                  <div className="flex gap-2">
                    <Input
                      value={newDependency}
                      onChange={(e) => setNewDependency(e.target.value)}
                      onKeyDown={handleKeyDown}
                      placeholder="添加依赖 (例如: lodash@4.17.21)"
                      className="flex-1"
                    />
                    <Button 
                      type="button" 
                      onClick={addDependency} 
                      variant="outline"
                      size="sm"
                    >
                      <PlusCircle className="mr-2 h-4 w-4" />
                      添加
                    </Button>
                  </div>
                  
                  <div className="flex flex-wrap gap-2 mt-2">
                    {dependencies.length === 0 && (
                      <p className="text-sm text-muted-foreground">无依赖</p>
                    )}
                    
                    {dependencies.map((dep, index) => (
                      <Badge 
                        key={index} 
                        variant="secondary"
                        className="flex items-center gap-1 px-2 py-1"
                      >
                        {dep}
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-4 w-4 p-0 text-muted-foreground hover:text-foreground"
                          onClick={() => removeDependency(dep)}
                        >
                          <X className="h-3 w-3" />
                        </Button>
                      </Badge>
                    ))}
                  </div>
                </div>
              </TabsContent>
              
              <TabsContent value="code">
                {/* 脚本内容 */}
                <FormField
                  control={form.control}
                  name="scriptContent"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>脚本代码</FormLabel>
                      <FormControl>
                        <div className="h-[500px] w-full border rounded-md overflow-hidden">
                          <Editor
                            height="100%"
                            language={languageToEditorMap[form.getValues('language')] || 'javascript'}
                            value={field.value}
                            onChange={(value) => field.onChange(value || '')}
                            theme="vs-dark"
                            options={{
                              minimap: { enabled: true },
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
              </TabsContent>
              
              <TabsContent value="test" className="space-y-4">
                {/* 测试数据 */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <FormLabel className="text-base">测试参数</FormLabel>
                    <FormField
                      control={form.control}
                      name="testEnabled"
                      render={({ field }) => (
                        <FormItem className="flex items-center space-x-2 space-y-0">
                          <FormLabel htmlFor="testEnabled">启用自动测试</FormLabel>
                          <FormControl>
                            <Switch
                              id="testEnabled"
                              checked={field.value}
                              onCheckedChange={field.onChange}
                            />
                          </FormControl>
                        </FormItem>
                      )}
                    />
                  </div>
                  
                  <FormField
                    control={form.control}
                    name="testData"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>参数 (JSON格式)</FormLabel>
                        <FormControl>
                          <div className="h-[150px] w-full border rounded-md overflow-hidden">
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
                  
                  <Button 
                    type="button" 
                    onClick={handleTest}
                    disabled={isTestRunning}
                    className="w-full"
                  >
                    {isTestRunning ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        测试中...
                      </>
                    ) : (
                      <>测试脚本</>
                    )}
                  </Button>
                  
                  {/* 测试结果 */}
                  {testResult && (
                    <div className="space-y-4 border rounded-md p-4">
                      <div className="flex justify-between items-center">
                        <div className="flex items-center">
                          {testResult.success ? (
                            <CheckCircle className="mr-2 h-5 w-5 text-green-500" />
                          ) : (
                            <XCircle className="mr-2 h-5 w-5 text-red-500" />
                          )}
                          <span className={testResult.success ? 'text-green-500' : 'text-red-500'}>
                            {testResult.success ? '测试成功' : '测试失败'}
                          </span>
                        </div>
                        <div className="flex items-center text-muted-foreground">
                          <Clock className="mr-1 h-4 w-4" />
                          <span>耗时: {testResult.executionTime} ms</span>
                        </div>
                      </div>
                      
                      {/* 控制台输出 */}
                      <div>
                        <h4 className="text-sm font-medium mb-2">控制台输出</h4>
                        <div className="bg-secondary/50 rounded-md p-3 font-mono text-sm whitespace-pre-wrap overflow-auto max-h-[150px]">
                          {testResult.output || '(无输出)'}
                        </div>
                      </div>
                      
                      {/* 返回值 */}
                      <div>
                        <h4 className="text-sm font-medium mb-2">返回值</h4>
                        <div className="bg-secondary/50 rounded-md p-3 font-mono text-sm whitespace-pre-wrap overflow-auto max-h-[150px]">
                          {testResult.returnValue !== undefined 
                            ? JSON.stringify(testResult.returnValue, null, 2) 
                            : '(无返回值)'}
                        </div>
                      </div>
                      
                      {/* 错误信息 */}
                      {testResult.error && (
                        <div>
                          <h4 className="text-sm font-medium mb-2 text-red-500">错误信息</h4>
                          <div className="bg-red-500/10 rounded-md p-3 font-mono text-sm text-red-600 dark:text-red-400 whitespace-pre-wrap overflow-auto max-h-[150px]">
                            {testResult.error}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </TabsContent>
            </Tabs>
            
            <DialogFooter>
              <Button 
                type="button" 
                variant="outline" 
                onClick={() => onOpenChange(false)}
                disabled={isSubmitting}
              >
                取消
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {script ? '更新' : '创建'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
} 