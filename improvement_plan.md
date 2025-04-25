# KastraX MCP 模块改进计划

## 1. 问题概述

通过对KastraX项目的分析，特别是MCP（Model Context Protocol）模块，我们发现了以下主要问题：

1. **缺少MCPException类**：代码中引用了未定义的异常类，导致编译错误
2. **KastraX工具与MCP工具之间的类型不匹配**：两种不同的Tool实现之间存在类型转换问题，需要适配器
3. **JSON序列化问题**：在处理不同类型的数据时存在类型不兼容和歧义，导致序列化错误
4. **缺少SSE（Server-Sent Events）实现**：服务器端事件相关的功能未完全实现，缺少必要的类和方法
5. **整体实现不完整**：与Mastra的实现相比，缺少一些关键功能，如错误处理和连接管理
6. **命名冲突**：KastraX核心模块和MCP模块中都有Tool接口，导致类型混淆

## 2. 参考Mastra实现

Mastra项目提供了一个更成熟的MCP实现，具有以下特点：

- **完善的错误处理机制**：使用专门的错误代码和异常类处理各种错误情况
- **基于配置的客户端管理**：使用MCPConfiguration类管理多个MCP客户端
- **健壮的连接处理**：包括自动重连、超时处理和资源清理
- **完整的工具转换逻辑**：将MCP工具无缝转换为Mastra工具，处理各种数据类型
- **支持多种传输方式**：同时支持stdio和SSE传输，提供统一的接口
- **优雅的退出处理**：使用exitHook确保在应用退出时正确关闭连接

## 3. 改进计划

### 3.1 解决命名冲突

首先解决KastraX核心模块和MCP模块中的Tool接口冲突：

```kotlin
// 在需要同时使用两种Tool的文件中使用别名导入
import ai.kastrax.core.tools.Tool as KastraXTool
import ai.kastrax.mcp.protocol.Tool as MCPTool

// 在MCPToolWrapper.kt中实现KastraXTool接口
class MCPToolWrapper(private val mcpClient: MCPClient, private val mcpTool: MCPTool) : KastraXTool {
    // 实现细节...
}
```

### 3.2 创建MCPException类

创建完整的MCPException类，包含错误代码和数据：

```kotlin
package ai.kastrax.mcp.exception

import ai.kastrax.mcp.protocol.MCPErrorCodes

/**
 * MCP异常类，用于处理MCP操作中的错误
 */
class MCPException : Exception {
    val code: Int
    val data: String?

    constructor(message: String) : super(message) {
        this.code = MCPErrorCodes.INTERNAL_ERROR
        this.data = null
    }

    constructor(code: Int, message: String, data: String? = null) : super(message) {
        this.code = code
        this.data = data
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
        this.code = MCPErrorCodes.INTERNAL_ERROR
        this.data = null
    }

    constructor(cause: Throwable) : super(cause) {
        this.code = MCPErrorCodes.INTERNAL_ERROR
        this.data = null
    }
}
```

### 3.3 修复JSON序列化问题

使用Kotlin序列化库的DSL构建器改进序列化逻辑：

```kotlin
/**
 * 将任意值转换为JsonElement
 */
fun convertToJsonElement(value: Any?): JsonElement {
    return when (value) {
        is JsonElement -> value
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is Map<*, *> -> {
            buildJsonObject {
                @Suppress("UNCHECKED_CAST")
                (value as Map<String, Any?>).forEach { (k, v) ->
                    put(k, convertToJsonElement(v))
                }
            }
        }
        is List<*> -> {
            buildJsonArray {
                value.forEach { item ->
                    add(convertToJsonElement(item ?: JsonNull))
                }
            }
        }
        is Array<*> -> {
            buildJsonArray {
                value.forEach { item ->
                    add(convertToJsonElement(item ?: JsonNull))
                }
            }
        }
        null -> JsonNull
        else -> {
            // 尝试将对象转换为字符串
            try {
                JsonPrimitive(value.toString())
            } catch (e: Exception) {
                throw MCPException(MCPErrorCodes.SERIALIZATION_ERROR, "不支持的类型: ${value::class.java.name}")
            }
        }
    }
}

/**
 * 将JsonElement转换为Map<String, Any?>
 */
fun jsonElementToMap(element: JsonElement): Map<String, Any?> {
    return when (element) {
        is JsonObject -> element.mapValues { (_, value) -> jsonElementToAny(value) }
        else -> throw MCPException(MCPErrorCodes.INVALID_PARAMS, "需要JsonObject类型，实际是${element::class.java.name}")
    }
}

/**
 * 将JsonElement转换为原生类型
 */
fun jsonElementToAny(element: JsonElement): Any? {
    return when (element) {
        is JsonObject -> element.mapValues { (_, value) -> jsonElementToAny(value) }
        is JsonArray -> element.map { jsonElementToAny(it) }
        is JsonPrimitive -> {
            when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.boolean
                element.intOrNull != null -> element.int
                element.longOrNull != null -> element.long
                element.doubleOrNull != null -> element.double
                else -> element.content
            }
        }
        JsonNull -> null
    }
}
```

### 3.4 实现工具适配器

使用适配器模式实现KastraX工具和MCP工具的无缝集成：

```kotlin
/**
 * MCP工具适配器，将MCP工具包装为KastraX工具
 */
class MCPToolAdapter(private val mcpClient: MCPClient, private val mcpTool: MCPTool) : KastraXTool {
    override val id: String = mcpTool.id
    override val name: String = mcpTool.name
    override val description: String = mcpTool.description
    override val inputSchema: JsonElement = convertMCPSchemaToJsonSchema(mcpTool.parameters)
    override val outputSchema: JsonElement? = null // MCP工具没有输出模式

    override suspend fun execute(input: JsonElement): JsonElement {
        try {
            // 将JsonElement转换为Map<String, Any>
            val params = jsonElementToMap(input)

            // 调用MCP工具
            val result = mcpClient.callTool(mcpTool.id, params)

            // 尝试将结果解析为JSON
            return try {
                Json.parseToJsonElement(result)
            } catch (e: Exception) {
                JsonPrimitive(result)
            }
        } catch (e: MCPException) {
            return buildJsonObject {
                put("error", JsonPrimitive(e.message))
                put("code", JsonPrimitive(e.code))
                if (e.data != null) {
                    put("data", JsonPrimitive(e.data))
                }
            }
        } catch (e: Exception) {
            return buildJsonObject {
                put("error", JsonPrimitive(e.message ?: "Unknown error"))
            }
        }
    }
}

/**
 * 将MCP模式转换为JSON Schema
 */
fun convertMCPSchemaToJsonSchema(schema: JsonElement): JsonElement {
    // 实现从 MCP 参数定义到 JSON Schema 的转换
    return buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            // 处理参数
            if (schema is JsonObject) {
                schema.forEach { (paramName, paramDef) ->
                    if (paramDef is JsonObject) {
                        putJsonObject(paramName) {
                            // 复制类型信息
                            paramDef["type"]?.let { put("type", it) }
                            paramDef["description"]?.let { put("description", it) }
                            // 处理嵌套属性
                            paramDef["properties"]?.let { put("properties", it) }
                            paramDef["items"]?.let { put("items", it) }
                        }
                    }
                }
            }
        }
        // 添加必需字段
        putJsonArray("required") {
            if (schema is JsonObject) {
                schema.forEach { (paramName, paramDef) ->
                    if (paramDef is JsonObject && paramDef["required"]?.jsonPrimitive?.boolean == true) {
                        add(paramName)
                    }
                }
            }
        }
    }
}
```

### 3.4 实现SSE功能

添加缺少的SSE实现：

```kotlin
package ai.kastrax.mcp.transport.sse

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * SSE客户端实现
 */
class SSEClient(
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
    private val httpClient: HttpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
        }
    }
) {
    /**
     * 连接到SSE服务器并返回事件流
     */
    suspend fun connect(): Flow<SSEEvent> = flow {
        val response = httpClient.get(url) {
            headers {
                append(HttpHeaders.Accept, "text/event-stream")
                append(HttpHeaders.CacheControl, "no-cache")
                headers.forEach { (key, value) ->
                    append(key, value)
                }
            }
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Failed to connect to SSE server: ${response.status}")
        }

        val parser = SSEParser()
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(8192)

        while (!channel.isClosedForRead) {
            val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
            if (bytesRead < 0) break

            val chunk = buffer.copyOf(bytesRead).decodeToString()
            parser.processChunk(chunk).forEach { event ->
                emit(event)
            }
        }
    }
}

/**
 * SSE事件
 */
data class SSEEvent(
    val id: String? = null,
    val event: String? = null,
    val data: String,
    val retry: Long? = null
)

/**
 * SSE解析器
 */
class SSEParser {
    private var buffer = StringBuilder()
    private var eventId: String? = null
    private var eventType: String? = null
    private var data = StringBuilder()
    private var retry: Long? = null

    /**
     * 处理SSE数据块
     */
    fun processChunk(chunk: String): List<SSEEvent> {
        buffer.append(chunk)
        return processBuffer()
    }

    private fun processBuffer(): List<SSEEvent> {
        val events = mutableListOf<SSEEvent>()
        val lines = buffer.toString().split("\n")

        buffer.clear()

        for (i in lines.indices) {
            val line = lines[i]

            if (line.isEmpty()) {
                // 空行表示事件结束
                if (data.isNotEmpty()) {
                    events.add(SSEEvent(eventId, eventType, data.toString(), retry))
                    data.clear()
                    eventId = null
                    eventType = null
                    retry = null
                }
                continue
            }

            if (i == lines.size - 1 && !line.endsWith("\n")) {
                // 最后一行不完整，保存到缓冲区
                buffer.append(line)
                continue
            }

            when {
                line.startsWith("id:") -> {
                    eventId = line.substring(3).trim()
                }
                line.startsWith("event:") -> {
                    eventType = line.substring(6).trim()
                }
                line.startsWith("data:") -> {
                    if (data.isNotEmpty()) {
                        data.append("\n")
                    }
                    data.append(line.substring(5).trim())
                }
                line.startsWith("retry:") -> {
                    retry = line.substring(6).trim().toLongOrNull()
                }
            }
        }

        return events
    }
}
```

### 3.5 完善服务器实现

添加缺少的服务器功能：

```kotlin
package ai.kastrax.mcp.server.sse

import ai.kastrax.mcp.protocol.MCPMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SSE响应发送器
 */
class SSEResponder(private val call: ApplicationCall) {
    private val channel = Channel<String>(Channel.UNLIMITED)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 发送SSE消息
     */
    suspend fun send(message: MCPMessage) {
        val jsonStr = json.encodeToString(message)
        channel.send("data: $jsonStr\n\n")
    }

    /**
     * 开始SSE响应
     */
    suspend fun start() {
        call.response.cacheControl(CacheControl.NoCache(null))
        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            channel.receiveAsFlow().collect { event ->
                write(event)
                flush()
            }
        }
    }

    /**
     * 关闭SSE响应
     */
    fun close() {
        channel.close()
    }
}

/**
 * 配置Ktor路由以支持SSE
 */
fun Route.sseEndpoint(handler: suspend (ApplicationCall, SSEResponder) -> Unit) {
    get {
        val responder = SSEResponder(call)
        try {
            handler(call, responder)
        } finally {
            responder.close()
        }
    }
}
```

## 4. 实现细节

### 4.1 解决命名冲突

使用别名解决KastraX核心模块和MCP模块中的Tool接口冲突：

```kotlin
import ai.kastrax.core.tools.Tool as KastraXTool
import ai.kastrax.mcp.protocol.Tool as MCPTool
```

### 4.2 实现MCPException类

创建MCPException类，包含错误代码和数据：

```kotlin
package ai.kastrax.mcp.exception

/**
 * MCP异常类
 */
class MCPException : Exception {
    val code: Int
    val data: String?

    constructor(message: String) : super(message) {
        this.code = MCPErrorCodes.INTERNAL_ERROR
        this.data = null
    }

    constructor(code: Int, message: String, data: String? = null) : super(message) {
        this.code = code
        this.data = data
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
        this.code = MCPErrorCodes.INTERNAL_ERROR
        this.data = null
    }

    constructor(cause: Throwable) : super(cause) {
        this.code = MCPErrorCodes.INTERNAL_ERROR
        this.data = null
    }
}
```

### 4.3 改进工具集成

使用适配器模式实现KastraX工具和MCP工具的无缝集成：

```kotlin
class MCPToolAdapter(private val mcpClient: MCPClient, private val mcpTool: MCPTool) : KastraXTool {
    override val id: String = mcpTool.id
    override val name: String = mcpTool.name
    override val description: String = mcpTool.description
    override val inputSchema: JsonElement = convertMCPSchemaToJsonSchema(mcpTool.parameters)
    override val outputSchema: JsonElement? = null // MCP工具没有输出模式

    override suspend fun execute(input: JsonElement): JsonElement {
        try {
            // 将JsonElement转换为Map<String, Any>
            val params = jsonElementToMap(input)

            // 调用MCP工具
            val result = mcpClient.callTool(mcpTool.id, params)

            // 尝试将结果解析为JSON
            return try {
                Json.parseToJsonElement(result)
            } catch (e: Exception) {
                JsonPrimitive(result)
            }
        } catch (e: MCPException) {
            return buildJsonObject {
                put("error", JsonPrimitive(e.message))
                put("code", JsonPrimitive(e.code))
                if (e.data != null) {
                    put("data", JsonPrimitive(e.data))
                }
            }
        } catch (e: Exception) {
            return buildJsonObject {
                put("error", JsonPrimitive(e.message ?: "Unknown error"))
            }
        }
    }
}
```

### 4.4 改进连接管理

参考Mastra的实现，添加自动重连和优雅退出功能：

```kotlin
class MCPClientImpl(/* ... */) : MCPClient {
    // ...

    private val isConnecting = AtomicBoolean(false)
    private val maxReconnectAttempts = 3
    private var reconnectAttempts = 0
    private val reconnectDelayMs = 1000L

    override suspend fun connect() {
        if (isConnected.get()) return
        if (isConnecting.getAndSet(true)) return

        try {
            reconnectAttempts = 0
            doConnect()

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(Thread {
                runBlocking {
                    disconnect()
                }
            })
        } finally {
            isConnecting.set(false)
        }
    }

    private suspend fun doConnect() {
        try {
            // 连接到传输层
            transport.connect()

            // 发送初始化请求
            val response = sendRequest<InitializeResult>(
                MCPMethods.INITIALIZE,
                InitializeParams(name, version, ClientCapabilities())
            )

            // 存储服务器能力
            serverCapabilities["resources"] = response.capabilities.resources
            serverCapabilities["tools"] = response.capabilities.tools
            serverCapabilities["prompts"] = response.capabilities.prompts

            // 启动接收线程
            startReceiving()

            isInitialized.set(true)
            isConnected.set(true)
            reconnectAttempts = 0
        } catch (e: Exception) {
            logger.error(e) { "Failed to connect to MCP server" }

            // 尝试重连
            if (reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++
                delay(reconnectDelayMs * reconnectAttempts)
                doConnect()
            } else {
                throw MCPException(MCPErrorCodes.CONNECTION_ERROR, "Failed to connect to MCP server after $maxReconnectAttempts attempts", e.message)
            }
        }
    }
}
```

## 5. 实施步骤

1. **第一阶段：基础修复** ✅
   - 创建MCPException类和错误代码 ✅
   - 解决命名冲突问题 ✅
   - 修复JSON序列化问题 ✅
   - 实现基本的类型转换 ✅
   - 修复AgentIntegration中的类型不匹配问题 ✅
   - 修复DevServer中的类型不匹配问题 ✅
   - 替换ProjectTemplate中的已弃用方法 ✅

2. **第二阶段：传输实现**
   - 完善StdioTransport实现
   - 实现SSETransport和SSE客户端
   - 添加SSE服务器支持
   - 实现连接管理和重连机制

3. **第三阶段：集成与测试**
   - 完善与KastraX代理的集成
   - 实现MCPConfiguration类管理多个客户端
   - 编写单元测试
   - 创建示例应用

4. **第四阶段：文档与优化**
   - 更新README和文档
   - 性能优化
   - 添加更多示例
   - 实现与Mastra兼容的API

## 6. 时间线

- **第一阶段**：1-2天
- **第二阶段**：2-3天
- **第三阶段**：2-3天
- **第四阶段**：1-2天

总计：6-10天

## 7. 参考资源

1. Mastra MCP实现：`@mastra/mcp`包
2. Model Context Protocol规范：https://modelcontextprotocol.io/
3. Ktor SSE文档：https://ktor.io/docs/server-sent-events.html
4. Kotlin协程文档：https://kotlinlang.org/docs/coroutines-overview.html
5. KastraX核心模块文档：https://kastrax.ai/docs/core/
6. MCP官方SDK：https://github.com/modelcontextprotocol/sdk
