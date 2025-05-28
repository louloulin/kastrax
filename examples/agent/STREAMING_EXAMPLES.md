# 流式处理示例

本目录包含了展示 KastraX Agent 流式处理功能的示例代码。

## 示例文件

### 1. StreamingAgentExample.kt
完整的流式处理示例，包含：
- 多个测试问题的流式响应
- 工具调用处理
- 交互式聊天功能
- 错误处理和状态显示

**主要功能：**
- `streamingAgentExample()`: 演示多个问题的流式处理
- `interactiveStreamingExample()`: 交互式聊天模式

### 2. SimpleStreamingExample.kt
简化的流式处理示例，专注于核心功能：
- 单个问题的流式响应
- 基本的打字效果模拟
- 简洁的代码结构

## 运行示例

### 前置条件
1. 设置环境变量 `DEEPSEEK_API_KEY`
2. 确保项目已正确构建

### 运行方法

```bash
# 运行完整流式处理示例
gradle :examples:agent:run -PmainClass=ai.kastrax.examples.agent.StreamingAgentExampleKt

# 运行简单流式处理示例
gradle :examples:agent:run -PmainClass=ai.kastrax.examples.agent.SimpleStreamingExampleKt
```

## 核心概念

### 流式响应
流式响应允许实时接收和显示生成的文本，而不是等待完整响应：

```kotlin
val response = agent.stream(question, AgentStreamOptions())
response.textStream?.collect { chunk ->
    print(chunk) // 实时显示文本块
}
```

### AgentStreamOptions
配置流式处理的选项：

```kotlin
val streamOptions = AgentStreamOptions(
    temperature = 0.8,
    maxTokens = 1500
)
```

### 错误处理
流式处理中的错误处理：

```kotlin
try {
    val response = agent.stream(question)
    // 处理流式响应
} catch (e: Exception) {
    println("生成回答时发生错误: ${e.message}")
}
```

## 特性展示

1. **实时文本生成**: 文本逐块显示，提供更好的用户体验
2. **打字效果**: 通过延迟模拟真实的打字效果
3. **工具调用支持**: 处理 Agent 的工具调用响应
4. **交互式聊天**: 支持连续对话的聊天模式
5. **错误恢复**: 优雅处理网络或 API 错误

## 自定义配置

### 模型参数
```kotlin
model = deepSeek {
    temperature(0.8)  // 控制创造性
    maxTokens(1500)   // 最大输出长度
    topP(0.95)        // 核采样参数
}
```

### Agent 指令
```kotlin
instructions = """
    你是一个智能助手，请：
    1. 提供准确的信息
    2. 保持回答的条理性
    3. 使用友好的语调
""".trimIndent()
```

## 注意事项

1. **API 密钥**: 确保正确设置 `DEEPSEEK_API_KEY` 环境变量
2. **网络连接**: 流式处理需要稳定的网络连接
3. **资源管理**: 长时间运行时注意内存和连接管理
4. **错误处理**: 实现适当的错误处理和重试机制

## 扩展示例

可以基于这些示例创建更复杂的应用：
- Web 聊天界面
- 命令行助手工具
- 文档生成系统
- 代码解释器

## 相关文档

- [KastraX Core 文档](../../../docs/core.md)
- [DeepSeek 集成文档](../../../docs/integrations/deepseek.md)
- [Agent 配置指南](../../../docs/agent-configuration.md)