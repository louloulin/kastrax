# KastraX

<div align="center">
  <img src="docs/assets/kastrax-logo.png" alt="KastraX Logo" width="200" height="auto" />
  <br>
  <p><strong>基于 Kotlin 构建的现代 AI 代理框架</strong></p>
  
  [![构建状态](https://img.shields.io/github/workflow/status/kastrax-ai/kastrax/CI)](https://github.com/kastrax-ai/kastrax/actions)
  [![许可证](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
  [![Kotlin](https://img.shields.io/badge/kotlin-2.1.10-blue.svg)](https://kotlinlang.org)
  [![Maven Central](https://img.shields.io/maven-central/v/ai.kastrax/kastrax-core.svg)](https://search.maven.org/search?q=g:ai.kastrax)
  [![Discord](https://img.shields.io/discord/1234567890?color=5865F2&label=discord)](https://discord.gg/kastrax)
</div>

KastraX 是一个用 Kotlin 构建的现代 AI 代理框架，受到 Mastra的启发。它提供了一套全面的工具和抽象，用于构建 AI 驱动的应用程序，注重类型安全、模块化和开发者体验。

## 目录

- [特性](#特性)
- [快速开始](#快速开始)
  - [前提条件](#前提条件)
  - [安装](#安装)
  - [基本用法](#基本用法)
- [项目结构](#项目结构)
- [文档](#文档)
- [示例](#示例)
- [高级用例](#高级用例)
- [路线图](#路线图)
- [参与贡献](#参与贡献)
- [社区](#社区)
- [许可证](#许可证)
- [灵感与致谢](#灵感与致谢)

## 特性

- **代理系统**：使用流畅的 DSL 创建 AI 代理
- **LLM 抽象**：统一的接口，支持不同的 LLM 提供商
- **工具系统**：允许代理与外部系统交互
- **类型安全**：整个框架中的强类型支持
- **Kotlin 优先**：利用 Kotlin 的语言特性，提供流畅的开发体验
- **RAG 支持**：检索增强生成能力
- **内存系统**：灵活的代理内存管理机制
- **代理间通信**：构建具有结构化通信的多代理系统
- **向量存储**：多种向量存储集成，用于嵌入存储
- **可观察性**：内置监控和日志记录工具

## 快速开始

### 前提条件

- JDK 17 或更高版本
- Gradle 8.0 或更高版本
- 您选择的 LLM 提供商（OpenAI、Anthropic 等）的 API 密钥

### 安装

#### Gradle (Kotlin DSL)

在您的 `build.gradle.kts` 文件中添加以下内容：

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-integrations:kastrax-openai:0.1.0") // 可选
}
```

#### Maven

```xml
<dependencies>
    <dependency>
        <groupId>ai.kastrax</groupId>
        <artifactId>kastrax-core</artifactId>
        <version>0.1.0</version>
    </dependency>
    <!-- 可选的 OpenAI 集成 -->
    <dependency>
        <groupId>ai.kastrax</groupId>
        <artifactId>kastrax-integrations-kastrax-openai</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### 基本用法

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.openai.openAi

// 创建代理
val myAgent = agent {
    name = "助手"
    instructions = "你是一个有帮助的助手。"
    model = openAi(
        model = "gpt-4o",
        // API 密钥从环境变量 OPENAI_API_KEY 获取
    )

    // 添加工具
    tools {
        tool {
            id = "calculator"
            name = "计算器"
            description = "执行数学计算"
            // 定义输入/输出模式和执行逻辑
            // ...
        }
    }
}

// 使用代理
val response = myAgent.generate("法国的首都是什么？")
println(response.text)

// 流式响应
myAgent.stream("讲个故事") { chunk ->
    print(chunk)
}
```

## 项目结构

KastraX 遵循模块化架构，包含以下组件：

- **kastrax-core**：核心框架组件 ✅
- **kastrax-memory-api**：内存系统接口 ✅
- **kastrax-memory-impl**：内存系统实现 ✅
- **kastrax-zod**：模式验证系统 ✅
- **kastrax-integrations**：第三方集成
  - **kastrax-openai**：OpenAI 集成 ✅
  - **kastrax-deepseek**：DeepSeek 集成 ✅
  - **kastrax-anthropic**：Anthropic 集成 ✅
  - **kastrax-gemini**：Google Gemini 集成 ✅
- **kastrax-rag**：检索增强生成 ✅
- **kastrax-server**：用于托管代理的服务器组件 ✅
- **kastrax-cli**：命令行工具 ✅
- **kastrax-evals**：评估框架 ✅
- **kastrax-datasource**：数据源连接器 ✅
- **kastrax-observability**：监控和可观察性工具 ✅
- **kastrax-a2a**：代理间通信系统 ✅
- **kastrax-ai2db**：AI到数据库查询生成 ✅
- **kastrax-store**：向量存储集成 ✅
  - **memory**：内存向量存储 ✅
  - **chroma**：Chroma集成 ✅
  - **pinecone**：Pinecone集成 ✅
  - **qdrant**：Qdrant集成 ✅
  - **postgres**：PostgreSQL集成 ✅
  - **lancedb**：LanceDB集成 ✅
- **kastrax-runtime**：运行时执行环境 ✅
  - **kastrax-runtime-api**：运行时API接口 ✅
  - **kastrax-runtime-jvm**：JVM运行时实现 ✅

## 文档

详细文档可在 `docs` 目录和 `kastrax-doc` 模块中找到，后者包含我们文档网站的源代码。

访问我们的[官方文档](https://kastrax-doc.vercel.app/zh/docs)获取全面的指南和 API 参考。

主要文档主题包括：

- 代理创建和配置
- 工具构建和集成
- LLM提供商集成
- 内存系统
- RAG实现
- 代理间通信
- 使用Zod进行模式验证
- 服务器部署选项
- 最佳实践和模式

## 灵感与致谢

KastraX 受到了 AI 代理生态系统中几个优秀项目的启发：

### Mastra 框架

我们要感谢 [Mastra](https://github.com/mas-3/mastra)，这是一个创新的基于 TypeScript 的 AI 代理框架。KastraX 从 Mastra 的架构模式和代理设计理念中汲取了重要灵感，同时调整这些概念以利用 Kotlin 的独特优势。我们的团队对 Mastra 的结构和功能进行了深入分析，这极大地影响了 KastraX 的设计
### Augment Code

KastraX 的编程助手功能参考了 [Augment Code](https://github.com/augment-code) 的开发，特别是其 IDE 集成和代码分析方法。kastrax-code 模块实现了许多类似的功能，提供由 LLM 驱动的智能编码辅助。

### 其他灵感来源

- Kotlin 的协程和 DSL 功能，使我们能够设计富有表现力的 API
- kactor 库实现的 Actor 模型
- 开源 LLM 生态系统及其快速发展
- 更广泛的 AI 研究社区及其对代理架构的贡献

## 示例

`examples` 和 `examples-modules` 目录包含展示 KastraX 不同功能的各种示例项目：

- 简单聊天代理
- 工具使用示例
- RAG 实现
- 多代理系统
- 与数据库集成
- 工作流自动化

每个示例都包含详细的注释和解释，帮助您理解如何在不同场景中使用 KastraX。

## 高级用例

KastraX 可以用于构建各种 AI 驱动的应用：

### 多代理协作
```kotlin
// 创建一组专业化代理，共同协作
val researchAgent = agent { /* 配置 */ }
val writerAgent = agent { /* 配置 */ }
val editorAgent = agent { /* 配置 */ }

// 使用 A2A 系统连接代理
val workflow = agentWorkflow {
    step("research", researchAgent)
    step("write", writerAgent)
    step("edit", editorAgent)
    
    flow {
        "research" to "write"
        "write" to "edit"
    }
}
```

### 数据库查询生成
```kotlin
// 创建一个将自然语言转换为 SQL 的 AI2DB 代理
val dbAgent = ai2dbAgent {
    databaseConnector {
        type = PostgreSQL
        url = "jdbc:postgresql://localhost:5432/mydatabase"
        // 认证详情
    }
}

val query = "查找上个月购买金额超过 1000 元的所有客户"
val result = dbAgent.query(query)
```

## 路线图

- [x] 核心代理框架
- [x] 多 LLM 提供商集成
- [x] 基础 RAG 功能
- [x] 向量存储集成
- [x] 代理间通信
- [x] 高级代理反思和规划
- [x] 浏览器自动化工具
- [ ] 原生移动端 SDK
- [ ] 联邦学习能力
- [ ] 企业级安全功能

## 参与贡献

我们欢迎社区贡献！请查看我们的[贡献指南](CONTRIBUTING.md)了解如何参与。

### 开发环境设置

1. 克隆仓库
```bash
git clone https://github.com/kastrax-ai/kastrax.git
cd kastrax
```

2. 构建项目
```bash
./gradlew build
```

3. 运行测试
```bash
./gradlew test
```

## 社区

- 加入我们的 [Discord 服务器](https://discord.gg/kastrax)参与讨论和获取支持
- 在 [Twitter](https://twitter.com/kastraxai) 上关注我们获取最新动态
- 查看我们的[博客](https://blog.kastrax.ai)获取教程和公告

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。
