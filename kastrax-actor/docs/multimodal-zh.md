# Kastrax Actor 多模态数据传输

本文档详细介绍了 Kastrax Actor 模块的多模态数据传输功能，包括多模态消息类型、消息处理和 DSL 扩展。

## 目录

- [概述](#概述)
- [多模态消息类型](#多模态消息类型)
- [多模态消息处理](#多模态消息处理)
- [DSL 扩展](#dsl-扩展)
- [示例](#示例)
- [最佳实践](#最佳实践)

## 概述

多模态数据传输功能允许在 Actor 之间传输多种类型的数据，如文本、图像、音频、视频和文件。这使得 Agent 能够处理更加丰富的信息，提供更加智能的服务。

主要特点：

- **支持多种数据类型**：文本、图像、音频、视频、文件和混合类型
- **统一的接口**：提供统一的接口来处理多模态数据
- **DSL 扩展**：提供 DSL 扩展，简化多模态数据的传输
- **高效可靠**：确保数据传输的效率和可靠性

## 多模态消息类型

多模态消息支持以下类型：

### 文本 (TEXT)

文本类型的多模态消息，包含文本内容。

```kotlin
data class Text(val text: String) : MultimodalContent()
```

### 图像 (IMAGE)

图像类型的多模态消息，可以包含图像数据、格式、URL 或文件。

```kotlin
data class Image(
    val data: ByteArray? = null,
    val format: String? = null,
    val url: URL? = null,
    val file: File? = null
) : MultimodalContent()
```

### 音频 (AUDIO)

音频类型的多模态消息，可以包含音频数据、格式、URL 或文件。

```kotlin
data class Audio(
    val data: ByteArray? = null,
    val format: String? = null,
    val url: URL? = null,
    val file: File? = null
) : MultimodalContent()
```

### 视频 (VIDEO)

视频类型的多模态消息，可以包含视频数据、格式、URL 或文件。

```kotlin
data class Video(
    val data: ByteArray? = null,
    val format: String? = null,
    val url: URL? = null,
    val file: File? = null
) : MultimodalContent()
```

### 文件 (FILE)

文件类型的多模态消息，可以包含文件数据、名称、类型、URL 或文件对象。

```kotlin
data class FileContent(
    val data: ByteArray? = null,
    val name: String? = null,
    val type: String? = null,
    val url: URL? = null,
    val file: File? = null
) : MultimodalContent()
```

### 混合 (MIXED)

混合类型的多模态消息，包含多种类型的数据。

```kotlin
data class Mixed(val contents: List<MultimodalContent>) : MultimodalContent()
```

## 多模态消息处理

多模态消息处理由 `MultimodalProcessor` 类提供，它包含以下主要功能：

### 创建多模态内容

```kotlin
// 创建文本内容
val text = MultimodalProcessor.createTextContent("这是一段文本")

// 从文件创建图像内容
val image = MultimodalProcessor.createImageFromFile("path/to/image.jpg")

// 从 URL 创建图像内容
val imageFromUrl = MultimodalProcessor.createImageFromUrl("https://example.com/image.jpg")

// 创建混合内容
val mixed = MultimodalProcessor.createMixedContent(text, image)
```

### 创建多模态消息

```kotlin
// 创建多模态消息
val message = MultimodalProcessor.createMultimodalMessage(
    content = text,
    type = MultimodalType.TEXT
)
```

## DSL 扩展

多模态数据传输功能提供了一系列 DSL 扩展，简化多模态数据的传输。

### 发送多模态消息

```kotlin
// 发送文本消息
system.sendTextMessage(agentPid, "你好，我是用户")

// 发送图像消息
system.sendImageMessage(agentPid, image)

// 发送音频消息
system.sendAudioMessage(agentPid, audio)

// 发送视频消息
system.sendVideoMessage(agentPid, video)

// 发送文件消息
system.sendFileMessage(agentPid, file)

// 发送混合消息
system.sendMixedMessage(agentPid, mixed)
```

### 请求-响应模式

```kotlin
// 发送文本请求并等待响应
val response = system.askTextMessage(agentPid, "巴黎的人口是多少？")

// 发送图像请求并等待响应
val imageResponse = system.askImageMessage(agentPid, image)

// 发送混合请求并等待响应
val mixedResponse = system.askMixedMessage(agentPid, mixed)
```

### 创建多模态消息

```kotlin
// 创建文本消息
val textMessage = text("这是一段文本")

// 从文件创建图像消息
val imageMessage = imageFromFile("path/to/image.jpg")

// 从 URL 创建图像消息
val imageFromUrlMessage = imageFromUrl("https://example.com/image.jpg")

// 创建混合消息
val mixedMessage = mixed(textContent, imageContent)
```

## 示例

### 基本使用

```kotlin
// 创建 Actor 系统
val system = ActorSystem("multimodal-example")

// 创建模拟 Agent
val mockAgent = MultimodalMockAgent()

// 创建 Actor
val props = fromProducer { KastraxActor(mockAgent) }
val agentPid = system.root.spawn(props)

// 发送文本消息
system.sendTextMessage(agentPid, "你好，我是用户")

// 请求-响应模式，发送文本请求并等待响应
val response = system.askTextMessage(agentPid, "巴黎的人口是多少？")
println("回答: ${(response.message.content as String)}")

// 发送图像消息
val image = MultimodalProcessor.createImageFromFile("path/to/image.jpg")
system.sendImageMessage(agentPid, image)

// 使用 DSL 创建和发送多模态消息
val textMessage = text("这是一段文本")
val imageMessage = imageFromFile("path/to/image.jpg")
val mixedMessage = mixed(
    MultimodalProcessor.createTextContent("这是文本部分"),
    MultimodalProcessor.createImageFromFile("path/to/image.jpg")
)

// 发送混合消息
val mixedResponse = system.askMultimodalMessage(
    agentPid,
    mixedMessage.content,
    mixedMessage.type
)
```

## 最佳实践

### 选择合适的数据类型

根据需求选择合适的数据类型，避免不必要的数据转换和传输。

### 优化数据大小

对于大型数据，如图像、音频和视频，考虑使用 URL 或文件路径而不是直接传输数据，以减少网络负载。

### 处理不支持的类型

在处理多模态数据时，要考虑 Agent 可能不支持某些类型的数据，提供适当的错误处理和回退机制。

### 使用 DSL 简化代码

使用提供的 DSL 扩展来简化多模态数据的创建和传输，提高代码的可读性和可维护性。

### 测试多模态功能

在使用多模态功能时，确保进行充分的测试，特别是对于不同类型的数据和不同的 Agent 实现。
