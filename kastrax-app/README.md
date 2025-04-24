# KastraX App

这是一个基于 KastraX 框架的应用程序脚手架，提供了构建 AI 应用程序的基础结构。

## 功能特点

- 使用 KastraX 原生 DSL 方式构建应用程序
- 预配置的代理、工具和工作流
- 灵活的配置系统
- 日志记录
- 示例实现

## 快速开始

### 先决条件

- Java 17 或更高版本
- Gradle 7.0 或更高版本
- KastraX CLI（可选，用于开发）

### 安装

1. 克隆此仓库：

```bash
git clone https://github.com/yourusername/kastrax-app.git
cd kastrax-app
```

2. 配置 API 密钥：

```bash
cp .env.example .env
# 编辑 .env 文件，添加你的 API 密钥
```

3. 构建项目：

```bash
./gradlew build
```

### 运行

```bash
./gradlew run
```

或者使用 KastraX CLI 启动开发服务器：

```bash
kastrax dev
```

## 项目结构

```
kastrax-app/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── ai/
│   │   │       └── kastrax/
│   │   │           └── app/
│   │   │               ├── Main.kt                 # 应用程序入口点
│   │   │               ├── agents/                 # 代理定义
│   │   │               ├── tools/                  # 工具定义
│   │   │               ├── workflows/              # 工作流定义
│   │   │               ├── config/                 # 配置
│   │   │               └── examples/               # 示例应用程序
│   │   └── resources/                              # 资源文件
│   └── test/                                       # 测试
├── build.gradle.kts                                # Gradle 构建配置
├── settings.gradle.kts                             # Gradle 设置
├── config.json.example                             # 配置文件示例
├── .env.example                                    # 环境变量示例
└── README.md                                       # 文档
```

## 使用方法

### 基本用法

```kotlin
// 在 Main.kt 中，kastraxInstance 已经创建并配置好了
// 你可以直接使用它

// 使用代理
val assistantAgent = kastraxInstance.getAgent("assistant")
val response = assistantAgent?.generate(
    "北京今天的天气怎么样？",
    AgentGenerateOptions(temperature = 0.7, maxTokens = 1000)
)
println(response?.text)
```

### 自定义

#### 添加新代理

1. 在 `src/main/kotlin/ai/kastrax/app/agents/` 目录中创建新的代理文件
2. 在 `Main.kt` 中更新 `kastraxInstance`，注册新代理

```kotlin
val kastraxInstance = kastrax {
    // 注册现有代理
    agent(assistantAgent.id, assistantAgent)
    agent(expertAgent.id, expertAgent)
    
    // 注册新代理
    agent(myNewAgent.id, myNewAgent)
}
```

#### 添加新工具

1. 在 `src/main/kotlin/ai/kastrax/app/tools/` 目录中创建新的工具文件
2. 在代理定义中使用新工具

```kotlin
val myAgent = agent("my_agent") {
    // ...
    tools(
        calculatorTool,
        weatherTool,
        myNewTool
    )
    // ...
}
```

## 配置

应用程序配置可以通过以下方式提供：

1. `config.json` 文件
2. 环境变量
3. `.env` 文件

配置优先级：环境变量 > `.env` 文件 > `config.json` > 默认值

## 开发

### 使用 KastraX CLI

KastraX CLI 提供了便捷的开发工具：

```bash
# 启动开发服务器
kastrax dev

# 创建新组件
kastrax create agent MyNewAgent
kastrax create tool MyNewTool
kastrax create workflow MyNewWorkflow
```

### 测试

```bash
./gradlew test
```

## 许可证

[MIT](LICENSE)
