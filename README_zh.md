# KastraX

KastraX 是一个用 Kotlin 构建的现代 AI 代理框架，提供了一套全面的工具和抽象，用于构建 AI 驱动的应用程序，注重类型安全、模块化和开发者体验。

## 特性

- **代理系统**：使用流畅的 DSL 创建 AI 代理
- **LLM 抽象**：统一的接口，支持不同的 LLM 提供商
- **工具系统**：允许代理与外部系统交互
- **类型安全**：整个框架中的强类型支持
- **Kotlin 优先**：利用 Kotlin 的语言特性，提供流畅的开发体验

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
- **kastrax-integrations**：第三方集成
  - **kastrax-openai**：OpenAI 集成 ✅
  - **kastrax-anthropic**：Anthropic 集成（即将推出）
  - **kastrax-gemini**：Google Gemini 集成（即将推出）
  - **kastrax-mistral**：Mistral 集成（即将推出）
- **kastrax-rag**：检索增强生成（即将推出）
- **kastrax-cli**：命令行工具（即将推出）
- **kastrax-evals**：评估框架（即将推出）

## 文档

详细文档请参阅 `docs` 目录：

- [快速入门指南](docs/quickstart_zh.md)
- [代理系统详解](docs/agents_zh.md)
- [工具系统详解](docs/tools_zh.md)
- [LLM 抽象层详解](docs/llm_abstraction_zh.md)
- [KastraX 完整设计文档](docs/kastrax_zh.md)

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 LICENSE 文件
