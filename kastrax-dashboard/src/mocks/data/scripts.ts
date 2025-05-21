/**
 * 脚本语言类型
 */
export enum ScriptLanguage {
  TYPESCRIPT = 'TYPESCRIPT',
  JAVASCRIPT = 'JAVASCRIPT',
  PYTHON = 'PYTHON',
  GROOVY = 'GROOVY',
  JAVA = 'JAVA',
  SQL = 'SQL'
}

/**
 * 脚本接口
 */
export interface Script {
  scriptId: string;
  scriptName: string;
  scriptContent: string;
  language: ScriptLanguage;
  description?: string;
  dependencies?: string[];
  createTime: string;
  updateTime: string;
}

/**
 * 脚本执行结果接口
 */
export interface ScriptExecuteResult {
  success: boolean;
  executionTime: number;
  output: string;
  returnValue?: any;
  error?: string;
}

/**
 * 模拟脚本数据
 */
export const mockScripts: Script[] = [
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
    scriptName: 'SQL Query (SQL)',
    scriptContent: `-- SQL示例脚本
SELECT 
  c.customer_id,
  c.customer_name,
  COUNT(o.order_id) AS total_orders,
  SUM(o.order_amount) AS total_amount,
  AVG(o.order_amount) AS avg_order_value,
  MAX(o.order_date) AS last_order_date
FROM 
  customers c
LEFT JOIN 
  orders o ON c.customer_id = o.customer_id
WHERE 
  o.order_date >= DATE_SUB(CURRENT_DATE(), INTERVAL 1 YEAR)
GROUP BY 
  c.customer_id, c.customer_name
HAVING 
  COUNT(o.order_id) > 5
ORDER BY 
  total_amount DESC
LIMIT 10;`,
    language: ScriptLanguage.SQL,
    description: 'SQL查询示例',
    dependencies: [],
    createTime: '2023-01-04T00:00:00Z',
    updateTime: '2023-01-04T00:00:00Z'
  }
]; 