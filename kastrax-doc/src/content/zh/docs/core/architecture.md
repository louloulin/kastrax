# Kastrax 架构

## 架构概述

Kastrax 是一个基于 Kotlin 的 AI Agent 框架，采用模块化设计，提供了构建智能代理应用程序所需的核心组件。本文档介绍 Kastrax 的整体架构设计、核心组件以及它们之间的交互方式。

## 核心组件

Kastrax 框架由以下核心组件组成：

### 1. KastraX 类

`KastraX` 是框架的主入口点，负责管理 Agent 和其他组件。它提供了创建和访问 Agent 的方法，以及框架的全局配置。

```kotlin
class KastraX(
    private val agents: Map<String, Agent> = emptyMap()
) : KastraXBase(component = "KASTRAX", name = "kastrax") {
    
    // 获取 Agent
    fun getAgent(agentId: String): Agent {
        return agents[agentId] ?: throw IllegalArgumentException("Agent not found: $agentId")
    }
    
    // 获取所有 Agent
    fun getAgents(): Map<String, Agent> {
        return agents
    }
}
```

使用 `KastraXBuilder` 可以方便地创建 KastraX 实例：

```kotlin
val kastrax = KastraXBuilder()
    .agent("assistant", myAgent)
    .build()
```

### 2. Agent 系统

Agent 系统是 Kastrax 的核心，提供了创建和管理 AI 代理的功能。主要组件包括：

- **Agent 接口**：定义了 Agent 的基本行为
- **LLMAgent**：基于大型语言模型的 Agent 实现
- **Agent 架构**：不同类型的 Agent 架构，如自适应、目标导向、层次化等
- **Agent 状态管理**：管理 Agent 的状态和版本

### 3. 记忆系统

记忆系统允许 Agent 存储和检索信息，包括：

- **Memory 接口**：定义了记忆系统的基本操作
- **记忆类型**：工作记忆、对话历史、语义记忆等
- **记忆处理器**：处理和优化记忆内容
- **存储后端**：支持不同的存储方式，如内存、数据库等

### 4. 工具系统

工具系统允许 Agent 执行各种操作，包括：

- **Tool 接口**：定义了工具的基本结构
- **ZodTool**：基于 Zod 的类型安全工具
- **内置工具**：文件操作、Web 搜索、日期时间等
- **工具注册和调用**：管理工具的注册和执行

### 5. LLM 集成

LLM 集成提供了与各种大型语言模型的连接，包括：

- **LlmProvider 接口**：定义了 LLM 提供者的基本操作
- **具体实现**：DeepSeek、OpenAI、Anthropic 等
- **请求和响应处理**：处理 LLM 的请求和响应

### 6. RAG 系统

RAG（检索增强生成）系统提供了基于文档的知识检索功能，包括：

- **文档处理**：加载和分割文档
- **向量存储**：存储文档的向量表示
- **检索策略**：语义检索、混合检索等
- **上下文构建**：构建适合 LLM 输入的上下文

### 7. 工作流系统

工作流系统允许定义和执行复杂的任务流程，包括：

- **Workflow 接口**：定义了工作流的基本操作
- **工作流步骤**：定义工作流中的各个步骤
- **工作流执行**：执行工作流并处理结果

### 8. Actor 模型

Actor 模型提供了分布式计算的能力，包括：

- **KastraxActor**：将 Agent 包装为 Actor
- **Actor 系统**：管理 Actor 的创建和通信
- **远程 Actor**：支持跨网络的 Actor 通信

## 模块结构

Kastrax 框架采用模块化设计，主要模块包括：

```
kastrax/
├── kastrax-core/               # 核心框架组件
├── kastrax-memory-api/         # 内存系统接口
├── kastrax-memory-impl/        # 内存系统实现
├── kastrax-rag/                # 检索增强生成
├── kastrax-actor/              # Actor 模型集成
├── kastrax-integrations/       # 第三方集成
│   ├── kastrax-openai/         # OpenAI 集成
│   ├── kastrax-deepseek/       # DeepSeek 集成
│   ├── kastrax-anthropic/      # Anthropic 集成
│   └── kastrax-gemini/         # Google Gemini 集成
└── examples/                   # 示例应用
```

## 组件交互

Kastrax 组件之间的交互遵循以下原则：

1. **松耦合**：组件之间通过接口交互，减少依赖
2. **可扩展**：可以轻松添加新的组件和功能
3. **可组合**：组件可以灵活组合，构建复杂应用

以下是一个典型的交互流程：

1. 用户通过 `KastraX` 创建和配置 Agent
2. Agent 接收用户输入，调用 LLM 生成响应
3. 在生成过程中，Agent 可能会：
   - 使用记忆系统存储和检索信息
   - 调用工具执行特定操作
   - 使用 RAG 系统检索相关知识
   - 执行工作流完成复杂任务
4. Agent 返回生成的响应给用户

## 扩展性设计

Kastrax 的扩展性设计体现在以下几个方面：

1. **接口抽象**：核心功能通过接口定义，可以有多种实现
2. **插件系统**：可以添加新的工具、记忆处理器等
3. **自定义组件**：可以自定义 Agent 架构、工作流步骤等
4. **集成能力**：可以集成各种 LLM 提供者和外部服务

## 总结

Kastrax 架构提供了构建 AI Agent 应用所需的全套组件，包括 Agent 系统、记忆系统、工具系统、RAG 系统、工作流系统和 Actor 模型。通过模块化设计和良好的扩展性，Kastrax 可以满足各种 AI Agent 应用的需求。
