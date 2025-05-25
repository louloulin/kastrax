import axios from 'axios';
import client from './client';

// 脚本语言类型
export enum ScriptLanguage {
  TYPESCRIPT = 'typescript',
  JAVASCRIPT = 'javascript',
  PYTHON = 'python',
  GROOVY = 'groovy'
}

// 脚本接口定义
export interface Script {
  scriptId: string;
  scriptName: string;
  scriptContent: string;
  language: ScriptLanguage;
  description?: string;
  dependencies?: string[]; // 依赖列表
  createTime: string;
  updateTime: string;
  testData?: string; // 测试数据（JSON格式字符串）
  testEnabled?: boolean; // 是否启用自动测试
}

// 脚本查询参数
export interface ScriptQuery {
  scriptName?: string;
  language?: ScriptLanguage;
  page?: number;
  size?: number;
}

// 脚本创建参数
export interface ScriptCreateParams {
  scriptName: string;
  scriptContent: string;
  language: ScriptLanguage;
  description?: string;
  dependencies?: string[];
  testData?: string; // 测试数据（JSON格式字符串）
  testEnabled?: boolean; // 是否启用自动测试
}

// 脚本更新参数
export interface ScriptUpdateParams {
  scriptId: string;
  scriptName?: string;
  scriptContent?: string;
  language?: ScriptLanguage;
  description?: string;
  dependencies?: string[];
  testData?: string; // 测试数据（JSON格式字符串）
  testEnabled?: boolean; // 是否启用自动测试
}

// 脚本执行参数
export interface ScriptExecuteParams {
  scriptId: string;
  params?: Record<string, any>; // 执行时传入的参数
  timeout?: number; // 执行超时时间(毫秒)
  breakpoints?: number[]; // 断点行号，仅用于调试
  isTest?: boolean; // 是否为测试执行
}

// 脚本执行结果
export interface ScriptExecuteResult {
  success: boolean;
  executionTime: number; // 执行耗时(毫秒)
  output: string; // 输出内容
  error?: string; // 错误信息
  returnValue?: any; // 返回值
}

// 脚本调试结果
export interface ScriptDebugResult extends ScriptExecuteResult {
  debugSteps: ScriptDebugStep[]; // 调试步骤信息
  memoryUsage?: string; // 内存使用
  cpuTime?: string; // CPU时间
}

// 脚本调试步骤
export interface ScriptDebugStep {
  line: number; // 行号
  time: number; // 执行到该步骤的时间（ms）
  state: Record<string, any>; // 变量状态
}

// 脚本依赖管理参数
export interface ScriptDependencyParams {
  scriptId: string;
  dependencies: string[]; // 依赖列表
}

// 获取脚本列表
export const getScriptList = (params?: ScriptQuery) => {
  return client.get('/scripts', { params });
};

// 获取脚本详情
export const getScriptById = (scriptId: string) => {
  return client.get(`/scripts/${scriptId}`);
};

// 创建脚本
export const createScript = (params: ScriptCreateParams) => {
  return client.post('/scripts', params);
};

// 更新脚本
export const updateScript = (params: ScriptUpdateParams) => {
  return client.put(`/scripts/${params.scriptId}`, params);
};

// 删除脚本
export const deleteScript = (scriptId: string) => {
  return client.delete(`/scripts/${scriptId}`);
};

// 执行脚本
export const executeScript = (params: ScriptExecuteParams) => {
  return client.post(`/scripts/${params.scriptId}/execute`, params);
};

// 调试脚本
export const debugScript = (params: ScriptExecuteParams) => {
  return client.post(`/scripts/${params.scriptId}/debug`, params);
};

// 更新脚本依赖
export const updateScriptDependencies = (params: ScriptDependencyParams) => {
  return client.put(`/scripts/${params.scriptId}/dependencies`, params);
};

// 安装/更新依赖
export const installDependencies = (scriptId: string) => {
  return client.post(`/scripts/${scriptId}/dependencies/install`);
};

// 测试脚本
export const testScript = (params: ScriptExecuteParams) => {
  return client.post(`/scripts/${params.scriptId}/test`, params);
};

// 获取支持的脚本语言
export const getScriptLanguages = () => {
  return client.get('/scripts/languages');
};

// Mock 数据支持
// Use a function to determine if mock is enabled instead of directly using import.meta.env
function isMockEnabled() {
  try {
    // Check if we're in a browser environment
    return typeof window !== 'undefined' && 
           process.env.VITE_USE_MOCK_DATA === 'true';
  } catch (e) {
    // In test environment, return false by default
    return false;
  }
}

// 示例脚本数据
const mockScripts: Script[] = [
  {
    scriptId: 'script-001',
    scriptName: 'Hello World (TypeScript)',
    scriptContent: `// TypeScript示例脚本
const sayHello = (name: string): string => {
  return \`Hello, \${name}!\`;
};

// 输出结果
console.log(sayHello("World"));
return sayHello("World");`,
    language: ScriptLanguage.TYPESCRIPT,
    description: 'TypeScript示例脚本',
    dependencies: ['typescript@4.9.5'],
    createTime: '2023-01-01T00:00:00Z',
    updateTime: '2023-01-01T00:00:00Z'
  },
  {
    scriptId: 'script-002',
    scriptName: 'Data Transformation (JavaScript)',
    scriptContent: `// JavaScript示例脚本
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
    language: ScriptLanguage.JAVASCRIPT,
    description: 'JavaScript数据转换示例',
    dependencies: ['lodash@4.17.21'],
    createTime: '2023-01-02T00:00:00Z',
    updateTime: '2023-01-02T00:00:00Z'
  },
  {
    scriptId: 'script-003',
    scriptName: 'Number Analysis (Python)',
    scriptContent: `# Python示例脚本
import numpy as np
import pandas as pd

def analyze_numbers(numbers):
    arr = np.array(numbers)
    return {
        'mean': np.mean(arr),
        'median': np.median(arr),
        'std': np.std(arr),
        'min': np.min(arr),
        'max': np.max(arr)
    }

# 测试数据
test_numbers = [12, 34, 56, 78, 90, 23, 45, 67]

# 输出结果
result = analyze_numbers(test_numbers)
print(result)

# 返回结果
return result`,
    language: ScriptLanguage.PYTHON,
    description: 'Python数据分析示例',
    dependencies: ['numpy==1.24.3', 'pandas==2.0.1'],
    createTime: '2023-01-03T00:00:00Z',
    updateTime: '2023-01-03T00:00:00Z'
  },
  {
    scriptId: 'script-004',
    scriptName: 'String Manipulation (Groovy)',
    scriptContent: `// Groovy示例脚本
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
return processResult`,
    language: ScriptLanguage.GROOVY,
    description: 'Groovy字符串处理示例',
    dependencies: [],
    createTime: '2023-01-04T00:00:00Z',
    updateTime: '2023-01-04T00:00:00Z'
  }
];

// 创建一个模拟响应
const createMockResponse = <T>(data: T) => {
  return {
    data: {
      code: 200,
      success: true,
      msg: 'success',
      data
    }
  };
};

// 模拟执行结果
const generateMockExecutionResult = (script: Script): ScriptExecuteResult => {
  // 根据不同语言生成不同的模拟执行结果
  switch (script.language) {
    case ScriptLanguage.TYPESCRIPT:
    case ScriptLanguage.JAVASCRIPT:
      return {
        success: true,
        executionTime: Math.floor(Math.random() * 500) + 50,
        output: '> Hello, World!',
        returnValue: 'Hello, World!'
      };
    case ScriptLanguage.PYTHON:
      return {
        success: true,
        executionTime: Math.floor(Math.random() * 800) + 100,
        output: "> {'mean': 50.625, 'median': 50.5, 'std': 25.21, 'min': 12, 'max': 90}",
        returnValue: {
          mean: 50.625,
          median: 50.5,
          std: 25.21,
          min: 12,
          max: 90
        }
      };
    case ScriptLanguage.GROOVY:
      return {
        success: true,
        executionTime: Math.floor(Math.random() * 600) + 80,
        output: '> 处理结果: [originalText:The quick brown fox jumps over the lazy dog, wordCount:9, charCount:35, upperCase:THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG, lowerCase:the quick brown fox jumps over the lazy dog, reversed:god yzal eht revo spmuj xof nworb kciuq ehT]',
        returnValue: {
          originalText: 'The quick brown fox jumps over the lazy dog',
          wordCount: 9,
          charCount: 35,
          upperCase: 'THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG',
          lowerCase: 'the quick brown fox jumps over the lazy dog',
          reversed: 'god yzal eht revo spmuj xof nworb kciuq ehT'
        }
      };
    default:
      return {
        success: false,
        executionTime: 0,
        output: '',
        error: '不支持的脚本语言'
      };
  }
};

// Mock调试结果函数
const generateMockDebugSteps = (script: Script, breakpoints?: number[]): ScriptDebugStep[] => {
  const lines = script.scriptContent.split('\n');
  const steps: ScriptDebugStep[] = [];
  let cumulativeTime = 0;
  let variables: Record<string, any> = {};
  
  // 遍历每一行代码生成调试步骤
  for (let i = 0; i < lines.length; i++) {
    const line = i + 1;
    const code = lines[i].trim();
    
    // 跳过空行和注释
    if (code === '' || code.startsWith('//') || code.startsWith('/*') || code.startsWith('*')) {
      continue;
    }
    
    // 如果设置了断点且当前行不是断点，则跳过
    if (breakpoints && breakpoints.length > 0 && !breakpoints.includes(line)) {
      continue;
    }
    
    // 模拟执行时间
    cumulativeTime += Math.floor(Math.random() * 10) + 5;
    
    // 根据代码行生成变量状态
    if (code.includes('=')) {
      const parts = code.split('=');
      if (parts.length >= 2) {
        const varName = parts[0].trim().replace('const ', '').replace('let ', '').replace('var ', '');
        if (varName && !varName.includes('.') && !varName.includes('[')) {
          try {
            // 尝试解析值
            const valueStr = parts[1].trim().replace(/;$/, '');
            let value: any = valueStr;
            
            // 简单处理字符串、数字和布尔值
            if (valueStr.startsWith('"') || valueStr.startsWith("'") || valueStr.startsWith('`')) {
              value = valueStr.substring(1, valueStr.length - 1);
            } else if (valueStr === 'true') {
              value = true;
            } else if (valueStr === 'false') {
              value = false;
            } else if (!isNaN(Number(valueStr))) {
              value = Number(valueStr);
            } else if (valueStr.startsWith('{') && valueStr.endsWith('}')) {
              try {
                // 尝试解析对象
                value = { mockObject: true };
              } catch (e) {
                // 忽略解析错误
              }
            } else if (valueStr.startsWith('[') && valueStr.endsWith(']')) {
              try {
                // 尝试解析数组
                value = ['mockItem'];
              } catch (e) {
                // 忽略解析错误
              }
            }
            
            variables[varName] = value;
          } catch (e) {
            // 忽略解析错误
          }
        }
      }
    }
    
    // 添加调试步骤
    steps.push({
      line,
      time: cumulativeTime,
      state: { ...variables }
    });
  }
  
  return steps;
};

// 如果启用了模拟数据，覆盖API函数
if (isMockEnabled()) {
  console.log('[MOCK] 启用脚本Mock数据模式');
  
  // 创建一个mock实现包装器
  const mock = {
    // 获取脚本列表
    getScriptList: (params?: ScriptQuery) => {
      console.log('[MOCK] 获取脚本列表，参数:', params);
      
      let filteredScripts = [...mockScripts];
      
      // 筛选
      if (params?.scriptName) {
        filteredScripts = filteredScripts.filter(s => 
          s.scriptName.toLowerCase().includes(params.scriptName!.toLowerCase())
        );
      }
      
      if (params?.language) {
        filteredScripts = filteredScripts.filter(s => s.language === params.language);
      }
      
      // 分页
      const page = params?.page || 1;
      const size = params?.size || 10;
      const start = (page - 1) * size;
      const end = start + size;
      const pagedScripts = filteredScripts.slice(start, end);
      
      return Promise.resolve(createMockResponse({
        list: pagedScripts,
        total: filteredScripts.length,
        page,
        size
      }));
    },
    
    // 获取脚本详情
    getScriptById: (scriptId: string) => {
      console.log('[MOCK] 获取脚本详情，ID:', scriptId);
      const script = mockScripts.find(s => s.scriptId === scriptId);
      
      if (script) {
        return Promise.resolve(createMockResponse(script));
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '脚本不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 创建脚本
    createScript: (params: ScriptCreateParams) => {
      console.log('[MOCK] 创建脚本，参数:', params);
      const newScript: Script = {
        scriptId: `script-${Date.now()}`,
        ...params,
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString()
      };
      
      mockScripts.push(newScript);
      return Promise.resolve(createMockResponse({ scriptId: newScript.scriptId }));
    },
    
    // 更新脚本
    updateScript: (params: ScriptUpdateParams) => {
      console.log('[MOCK] 更新脚本，参数:', params);
      const index = mockScripts.findIndex(s => s.scriptId === params.scriptId);
      
      if (index !== -1) {
        mockScripts[index] = {
          ...mockScripts[index],
          ...params,
          updateTime: new Date().toISOString()
        };
        return Promise.resolve(createMockResponse(true));
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '脚本不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 删除脚本
    deleteScript: (scriptId: string) => {
      console.log('[MOCK] 删除脚本，ID:', scriptId);
      const index = mockScripts.findIndex(s => s.scriptId === scriptId);
      
      if (index !== -1) {
        mockScripts.splice(index, 1);
        return Promise.resolve(createMockResponse(true));
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '脚本不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 执行脚本
    executeScript: (params: ScriptExecuteParams) => {
      console.log('[MOCK] 执行脚本，参数:', params);
      const script = mockScripts.find(s => s.scriptId === params.scriptId);
      
      if (script) {
        // 模拟随机执行时间
        return new Promise(resolve => {
          setTimeout(() => {
            resolve(createMockResponse(generateMockExecutionResult(script)));
          }, Math.random() * 1000 + 500);
        });
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '脚本不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 调试脚本
    debugScript: (params: ScriptExecuteParams) => {
      console.log('[MOCK] 调试脚本，ID:', params.scriptId, '参数:', params);
      
      const script = mockScripts.find(s => s.scriptId === params.scriptId);
      
      if (script) {
        // 模拟调试过程
        return new Promise(resolve => {
          setTimeout(() => {
            const debugResult: ScriptDebugResult = {
              success: true,
              executionTime: Math.floor(Math.random() * 200) + 100,
              output: `[输出] 调试脚本: ${script.scriptName}\n> 正在执行...\n> 执行完成。`,
              returnValue: script.language.includes('script') ? { result: '模拟返回值', timestamp: new Date().toISOString() } : '模拟返回值',
              debugSteps: generateMockDebugSteps(script, params.breakpoints),
              memoryUsage: `${Math.floor(Math.random() * 50) + 10} MB`,
              cpuTime: `${Math.floor(Math.random() * 100) + 50} ms`
            };
            
            resolve(createMockResponse(debugResult));
          }, Math.random() * 1000 + 500);
        });
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '脚本不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 更新脚本依赖
    updateScriptDependencies: (params: ScriptDependencyParams) => {
      console.log('[MOCK] 更新脚本依赖，参数:', params);
      const index = mockScripts.findIndex(s => s.scriptId === params.scriptId);
      
      if (index !== -1) {
        mockScripts[index] = {
          ...mockScripts[index],
          dependencies: params.dependencies,
          updateTime: new Date().toISOString()
        };
        return Promise.resolve(createMockResponse(true));
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '脚本不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 安装依赖
    installDependencies: (scriptId: string) => {
      console.log('[MOCK] 安装脚本依赖，ID:', scriptId);
      const script = mockScripts.find(s => s.scriptId === scriptId);
      
      if (script) {
        // 模拟安装过程
        return new Promise(resolve => {
          setTimeout(() => {
            resolve(createMockResponse({
              success: true,
              installedDependencies: script.dependencies,
              log: script.dependencies?.map(dep => `安装依赖: ${dep}`) || []
            }));
          }, Math.random() * 2000 + 1000);
        });
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '脚本不存在',
              data: null
            }
          }
        });
      }
    },
    
    // 测试脚本
    testScript: (params: ScriptExecuteParams) => {
      console.log('[MOCK] 测试脚本，ID:', params.scriptId, '参数:', params);
      
      const script = mockScripts.find(s => s.scriptId === params.scriptId);
      
      if (script) {
        // 模拟测试过程
        return new Promise(resolve => {
          setTimeout(() => {
            // 创建测试结果（与执行类似但添加测试特定信息）
            const testResult: ScriptExecuteResult = {
              success: true,
              executionTime: Math.floor(Math.random() * 150) + 30, // 测试通常更快
              output: `[测试] 执行脚本: ${script.scriptName}\n> 使用测试参数: ${JSON.stringify(params.params || {})}\n> 测试执行完成。`,
              returnValue: script.language.includes('script') 
                ? { result: '测试返回值', tested: true, timestamp: new Date().toISOString() } 
                : '测试返回值',
            };
            
            // 有10%的几率模拟测试失败
            if (Math.random() < 0.1) {
              testResult.success = false;
              testResult.error = '测试断言失败: 期望值与实际值不符';
              testResult.output += '\n> 测试断言失败';
            }
            
            resolve(createMockResponse(testResult));
          }, Math.random() * 800 + 200); // 测试通常比正常执行更快
        });
      } else {
        return Promise.reject({
          response: {
            data: {
              code: 404,
              success: false,
              msg: '脚本不存在',
              data: null
            }
          }
        });
      }
    }
  };
  
  // 使用模块模式覆盖原始导出函数
  Object.assign(module.exports, mock);
}