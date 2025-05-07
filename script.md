# Kastrax-Script 模块计划

本文档概述了 kastrax-script 模块的设计和实现计划，该模块将为 Kastrax AI Agent 框架提供 Kotlin 脚本和 Notebook 功能支持，增强 AI Agent 的开发和交互体验。

## 1. 模块概述

**kastrax-script** 模块将提供以下功能：

1. **Kotlin 脚本执行引擎**：支持动态加载和执行 Kotlin 脚本（.kts 文件），实现 AI Agent 的动态配置和扩展
2. **AI Agent 脚本 DSL**：专门为 AI Agent 设计的领域特定语言，与现有的 Agent DSL 无缝集成
3. **Notebook 支持**：与 Jupyter Notebook 兼容的 Kotlin Notebook 功能，提供交互式 AI Agent 开发环境
4. **依赖管理**：支持在脚本中声明和解析 Maven 依赖，实现自包含的脚本生态系统
5. **交互式开发环境**：支持代码补全、错误检查等 IDE 功能，提升开发体验
6. **Actor 集成**：与 kactor 模块集成，支持脚本化的 Actor Agent 创建和管理

## 2. 技术架构

模块将基于以下技术构建：

1. **kotlin-scripting-jvm**：Kotlin 官方脚本 API，提供脚本编译和执行的核心功能
2. **kotlin-scripting-dependencies**：依赖解析支持，实现脚本的依赖管理
3. **kotlin-scripting-dependencies-maven**：Maven 依赖解析，支持从 Maven 仓库获取依赖
4. **kotlin-jupyter**：Jupyter Notebook 集成，提供交互式开发环境
5. **kotlinx-coroutines**：异步执行支持，实现非阻塞脚本执行
6. **GraalVM Polyglot**：多语言支持，允许在脚本中使用多种编程语言

## 3. 核心组件

1. **ScriptEngine**：脚本执行引擎，负责编译和执行 Kotlin 脚本，支持热重载和缓存
2. **AgentScriptDefinition**：AI Agent 脚本定义，包含特定的编译配置，与现有 Agent DSL 集成
3. **NotebookSupport**：Jupyter Notebook 集成支持，实现交互式 Agent 开发
4. **DependencyResolver**：脚本依赖解析器，支持动态加载 Maven 依赖
5. **ScriptEvaluator**：脚本评估和结果处理，支持各种输出格式
6. **ActorScriptIntegration**：与 kactor 模块的集成，支持脚本化的 Actor 创建
7. **VisualizationSupport**：可视化支持，包括图表、表格和交互式组件

## 4. 实现计划

### 阶段一：基础脚本引擎（2周）

1. 创建 kastrax-script 模块结构，集成到现有项目中
2. 实现基本的 Kotlin 脚本执行引擎，支持 .kts 文件的编译和执行
3. 添加依赖解析支持，实现 @file:DependsOn 和 @file:Repository 注解
4. 创建简单的脚本示例，测试基本功能
5. 与 GraalVM 集成，支持多语言脚本执行

### 阶段二：AI Agent DSL（2周）

1. 设计 AI Agent 专用 DSL，与现有 Agent DSL 兼容
2. 实现 DSL 编译和执行，支持动态加载和更新
3. 添加 Agent 上下文和状态管理，支持会话持久化
4. 创建 Agent 脚本示例，包括各种常见用例
5. 与 kactor 模块集成，支持脚本化的 Actor Agent 创建

### 阶段三：Notebook 支持（3周）

1. 集成 Kotlin Jupyter 内核，支持 .ipynb 格式文件
2. 实现 Notebook 单元格执行，支持代码和 Markdown 单元格
3. 添加输出渲染支持（文本、HTML、图表等），支持交互式可视化
4. 创建示例 Notebook，展示 AI Agent 开发和调试流程
5. 添加预定义库集成（%use 指令），简化常用库的导入

### 阶段四：高级功能和优化（3周）

1. 添加脚本热重载支持，实现实时更新
2. 实现脚本缓存机制，提高执行效率
3. 优化执行性能，支持并行执行
4. 添加调试支持，包括断点和变量检查
5. 实现与 IDE 的集成，提供代码补全和错误检查
6. 添加分布式执行支持，实现跨节点脚本执行

## 5. 文件结构

```
kastrax-script/
├── build.gradle.kts             # 构建配置
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── ai/
│   │   │       └── kastrax/
│   │   │           └── script/
│   │   │               ├── engine/           # 脚本引擎
│   │   │               │   ├── ScriptEngine.kt
│   │   │               │   ├── ScriptCompiler.kt
│   │   │               │   └── ScriptEvaluator.kt
│   │   │               ├── dsl/              # DSL 定义
│   │   │               │   ├── AgentDsl.kt
│   │   │               │   ├── AgentContext.kt
│   │   │               │   └── AgentScriptDefinition.kt
│   │   │               ├── notebook/         # Notebook 支持
│   │   │               │   ├── NotebookSupport.kt
│   │   │               │   ├── CellEvaluator.kt
│   │   │               │   └── OutputRenderer.kt
│   │   │               ├── dependencies/     # 依赖管理
│   │   │               │   ├── DependencyResolver.kt
│   │   │               │   └── MavenDependencyResolver.kt
│   │   │               └── api/              # 公共 API
│   │   │                   ├── ScriptApi.kt
│   │   │                   └── NotebookApi.kt
│   │   └── resources/
│   │       └── META-INF/
│   │           └── services/   # 服务提供者配置
│   └── test/
│       └── kotlin/
│           └── ai/
│               └── kastrax/
│                   └── script/
│                       ├── engine/
│                       ├── dsl/
│                       └── notebook/
└── examples/
    ├── simple-script.kts        # 简单脚本示例
    ├── agent-script.kts         # Agent 脚本示例
    └── notebook-example.ipynb   # Notebook 示例
```

## 6. API 设计

### 脚本 API

```kotlin
// 脚本执行 API
fun executeScript(scriptFile: File): ScriptResult
fun executeScript(scriptText: String): ScriptResult
fun evalScript(scriptText: String, context: Map<String, Any>): Any?

// 脚本定义 API
@KotlinScript(
    fileExtension = "agent.kts",
    compilationConfiguration = AgentScriptConfiguration::class
)
abstract class AgentScript(val context: AgentContext) {
    // 基础方法
    fun agent(name: String, block: AgentBuilder.() -> Unit): Agent
    fun tool(name: String, block: ToolBuilder.() -> Unit): Tool
    fun memory(block: MemoryBuilder.() -> Unit): Memory
}

// DSL 示例
fun agent(name: String, init: AgentBuilder.() -> Unit): Agent {
    val builder = AgentBuilder(name)
    builder.init()
    return builder.build()
}
```

### Notebook API

```kotlin
// Notebook 支持 API
fun executeCell(cellId: String, code: String): CellResult
fun renderOutput(result: Any?): RenderedOutput

// 输出渲染
interface OutputRenderer {
    fun render(value: Any?): RenderedOutput
}

// 单元格结果
data class CellResult(
    val output: RenderedOutput,
    val executionCount: Int,
    val executionTime: Long
)
```

## 7. 示例用法

### 简单脚本示例

```kotlin
// simple-script.kts
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import kotlinx.coroutines.*

println("Hello from Kotlin Script!")

runBlocking {
    val result = async {
        delay(1000)
        "Async result"
    }
    println(result.await())
}
```

### Agent 脚本示例

```kotlin
// agent-script.kts
@file:Repository("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
@file:DependsOn("ai.kastrax:kastrax-core:0.1.0")

import ai.kastrax.core.agent.*
import ai.kastrax.core.tools.*
import kotlinx.html.*
import kotlinx.html.stream.*

// 定义 Agent
val myAgent = agent("MyAgent") {
    model = "deepseek-chat"
    temperature = 0.7

    // 添加工具
    tool("calculator") {
        description = "执行简单的数学计算"
        execute { input ->
            val expression = input.get("expression").asString()
            val result = evaluateExpression(expression)
            mapOf("result" to result)
        }
    }

    // 定义行为
    behavior {
        onMessage { message ->
            respond("收到消息: $message")
        }
    }
}

// 运行 Agent
myAgent.run()

// 辅助函数
fun evaluateExpression(expression: String): Double {
    // 简单实现
    return 42.0
}
```

### Notebook 示例

```
// notebook-example.ipynb (JSON 格式)
{
  "cells": [
    {
      "cell_type": "markdown",
      "metadata": {},
      "source": [
        "# Kastrax Agent Notebook 示例\n",
        "\n",
        "这是一个使用 Kastrax Script 的 Notebook 示例。"
      ]
    },
    {
      "cell_type": "code",
      "metadata": {},
      "source": [
        "// 导入依赖\n",
        "@file:DependsOn(\"ai.kastrax:kastrax-core:0.1.0\")\n",
        "\n",
        "import ai.kastrax.core.agent.*\n",
        "import ai.kastrax.core.tools.*\n",
        "\n",
        "println(\"Hello from Kastrax Notebook!\")"
      ]
    },
    {
      "cell_type": "code",
      "metadata": {},
      "source": [
        "// 创建一个简单的 Agent\n",
        "val agent = agent(\"NotebookAgent\") {\n",
        "    model = \"deepseek-chat\"\n",
        "    temperature = 0.7\n",
        "}\n",
        "\n",
        "agent"
      ]
    }
  ],
  "metadata": {
    "kernelspec": {
      "display_name": "Kotlin",
      "language": "kotlin",
      "name": "kotlin"
    }
  },
  "nbformat": 4,
  "nbformat_minor": 4
}
```

## 8. 与现有模块的集成

kastrax-script 模块将与以下现有模块集成：

1. **kastrax-core**：使用核心 Agent 功能
2. **kastrax-zod**：集成 Zod 工具定义
3. **kastrax-memory-api**：使用记忆系统
4. **kastrax-integrations**：集成各种 LLM 提供商

## 9. 未来扩展计划

1. **可视化编辑器**：提供基于 Web 的脚本编辑器
2. **脚本市场**：创建脚本共享和发现平台
3. **分布式执行**：支持在分布式环境中执行脚本
4. **多语言支持**：添加对其他语言的支持（如 Python、JavaScript）

## 10. 参考资料

1. [Kotlin 自定义脚本教程](https://kotlinlang.org/docs/custom-script-deps-tutorial.html)
2. [Kotlin Jupyter](https://github.com/Kotlin/kotlin-jupyter)
3. [Kotlin Notebook 介绍](https://blog.jetbrains.com/kotlin/2023/07/introducing-kotlin-notebook/)
4. [Kotlin 脚本 API](https://github.com/JetBrains/kotlin/tree/master/libraries/scripting)
