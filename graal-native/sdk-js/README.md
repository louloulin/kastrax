# KastraX JavaScript SDK

KastraX的JavaScript语言SDK，允许在JavaScript应用程序中使用KastraX AI Agent功能。

## 安装

```bash
npm install kastrax
```

## 使用示例

### Node.js

```javascript
const { Agent, Tool } = require('kastrax');

// 创建Agent
const agent = new Agent({
  name: 'my-agent',
  model: 'deepseek-coder'
});

// 添加工具
agent.addTool(new Tool({
  name: 'calculator',
  function: (input) => {
    // 工具实现
    return '4';
  }
}));

// 运行Agent
agent.run('Calculate 2+2')
  .then(response => console.log(response))
  .catch(error => console.error(error));
```

### 浏览器

```html
<script src="https://unpkg.com/kastrax@0.1.0/dist/kastrax.min.js"></script>
<script>
  const agent = new kastrax.Agent({
    name: 'my-agent',
    model: 'deepseek-coder'
  });
  
  // 使用Agent...
</script>
```

## API参考

### Agent

创建和管理AI Agent的主要类。

```javascript
const agent = new Agent({
  name: 'my-agent',
  model: 'deepseek-coder',
  options: {
    temperature: 0.7,
    maxTokens: 1000
  }
});
```

#### 方法

- `run(input: string): Promise<string>` - 使用给定输入运行Agent
- `addTool(tool: Tool): void` - 向Agent添加工具
- `setMemory(memory: Memory): void` - 设置Agent的记忆
- `getState(): AgentState` - 获取Agent的当前状态

### Tool

定义Agent可以使用的工具的类。

```javascript
const tool = new Tool({
  name: 'calculator',
  description: '执行计算',
  function: (input) => {
    // 工具实现
    return '4';
  }
});
```

### Memory

管理Agent记忆的类。

```javascript
const memory = new Memory({
  type: 'inmemory',
  options: {
    maxMessages: 100
  }
});
```

## 示例

请参阅`examples`目录获取完整的使用示例。

## 构建说明

### 前提条件

- Node.js 16或更高版本
- Kotlin 1.9.22或更高版本

### 构建步骤

```bash
# 克隆仓库
git clone https://github.com/kastrax/kastrax.git
cd kastrax

# 构建JavaScript SDK
./gradlew :graal-native:sdk-js:build

# 生成NPM包
./gradlew :graal-native:sdk-js:generateNpmPackage
```

生成的NPM包将位于`graal-native/sdk-js/build/npm`目录下。

## 许可证

MIT
