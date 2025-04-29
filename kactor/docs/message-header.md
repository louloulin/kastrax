# ProtoActor-Kotlin 消息头

本文档描述了 ProtoActor-Kotlin 的消息头功能。

## 概述

消息头是一种在消息中添加元数据的机制，允许在不修改消息内容的情况下传递上下文信息。这对于实现跟踪、日志记录、认证等功能非常有用。

## 实现

MessageHeader 类的实现如下：

```kotlin
class MessageHeader {
    private val headers: MutableMap<String, String> = mutableMapOf()

    fun set(key: String, value: String) {
        headers[key] = value
    }

    fun get(key: String): String? = headers[key]

    fun getOrDefault(key: String, default: String): String = headers.getOrDefault(key, default)

    fun remove(key: String) {
        headers.remove(key)
    }

    fun clear() {
        headers.clear()
    }

    fun keys(): List<String> = headers.keys.toList()

    fun length(): Int = headers.size

    fun toMap(): Map<String, String> = headers.toMap()

    companion object {
        val EMPTY = MessageHeader()
    }
}
```

## 使用方法

### 创建消息头

```kotlin
val header = MessageHeader()
header.set("correlation-id", "123")
header.set("user-id", "user123")
```

### 在 Context 中访问消息头

```kotlin
class MyActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        val headers = headers
        val correlationId = headers?.get("correlation-id")
        println("Correlation ID: $correlationId")
    }
}
```

### 在消息信封中使用消息头

```kotlin
val envelope = MessageEnvelope("hello", sender, header)
system.send(target, envelope)
```

### 使用空消息头

```kotlin
val emptyHeader = MessageHeader.EMPTY
```

## 与 Go 实现的互操作性

ProtoActor-Kotlin 的消息头与 ProtoActor-Go 的消息头完全兼容，允许在跨语言通信中传递上下文信息：

```kotlin
// Kotlin 代码
val header = MessageHeader()
header.set("language", "kotlin")
val envelope = MessageEnvelope("hello", sender, header)
system.send(goPid, envelope)
```

```go
// Go 代码
type MyActor struct{}

func (a *MyActor) Receive(ctx actor.Context) {
    switch msg := ctx.Message().(type) {
    case string:
        language := ctx.Headers().Get("language")
        fmt.Printf("Received message from %s: %s\n", language, msg)
    }
}
```

## 最佳实践

1. **使用有意义的键名**：使用描述性的键名，如 "correlation-id"、"user-id" 等。

2. **避免存储大量数据**：消息头应该只用于存储少量的元数据，不应该用于存储大量数据。

3. **考虑序列化**：在远程通信中，消息头会被序列化，因此应该只存储可序列化的数据。

4. **使用前缀**：使用前缀来避免键名冲突，如 "app.correlation-id"、"app.user-id" 等。

5. **处理缺失的头部**：始终检查头部是否存在，使用 `getOrDefault` 方法提供默认值。

## 示例

### 跟踪请求

```kotlin
class TracingActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        // 获取跟踪 ID
        val traceId = headers?.get("trace-id") ?: UUID.randomUUID().toString()
        
        // 处理消息
        println("Processing message with trace ID: $traceId")
        
        // 转发消息，保留跟踪 ID
        val nextActor = spawnChild(fromProducer { NextActor() })
        val envelope = MessageEnvelope(msg, self)
        envelope.header = MessageHeader()
        envelope.header?.set("trace-id", traceId)
        send(nextActor, envelope)
    }
}
```

### 认证

```kotlin
class AuthenticatedActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        // 检查认证令牌
        val token = headers?.get("auth-token")
        if (token == null || !isValidToken(token)) {
            println("Unauthorized access")
            return
        }
        
        // 处理消息
        println("Processing authenticated message")
    }
    
    private fun isValidToken(token: String): Boolean {
        // 验证令牌
        return token == "valid-token"
    }
}
```
