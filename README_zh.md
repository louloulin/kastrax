# KastraX

KastraX 是一个用 Kotlin 构建的现代 AI 代理框架，受到 Mastra 和 Kastra 的启发。它提供了一套全面的工具和抽象，用于构建 AI 驱动的应用程序，注重类型安全、模块化和开发者体验。


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

### 安装

在您的 `build.gradle.kts` 文件中添加以下内容：

```kotlin
dependencies {
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-integrations:kastrax-openai:0.1.0") // 可选
}
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

## 许可证

本项目采用MIT许可证 - 详情请参阅LICENSE文件。
