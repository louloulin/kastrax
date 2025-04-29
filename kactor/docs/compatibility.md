# ProtoActor-Kotlin 兼容性文档

本文档描述了 ProtoActor-Kotlin 与 ProtoActor-Go 的兼容性实现。

## 协议兼容性

ProtoActor-Kotlin 现在与 ProtoActor-Go 使用相同的协议定义，确保两个实现之间的互操作性。主要的协议更新包括：

### 1. PID 结构更新

PID 结构现在包含一个 `request_id` 字段，用于支持请求-响应模式：

```protobuf
message PID {
    string Address = 1;
    string Id = 2;
    uint32 request_id = 3;
}
```

### 2. 终止原因枚举

添加了 `TerminatedReason` 枚举，用于指定 Actor 终止的原因：

```protobuf
enum TerminatedReason {
    Stopped = 0;
    AddressTerminated = 1;
    NotFound = 2;
}
```

### 3. 新的消息类型

添加了新的消息类型，包括：

- `Touch`：用于检查 Actor 是否存活
- `Touched`：Actor 存活的响应
- `DeadLetterResponse`：死信响应

### 4. 远程通信协议

远程通信协议已更新为使用双向流式通信，使用单一的 `Receive` 方法：

```protobuf
service Remoting {
  rpc Receive (stream RemoteMessage) returns (stream RemoteMessage) {}
  rpc ListProcesses(ListProcessesRequest) returns (ListProcessesResponse) {}
  rpc GetProcessDiagnostics(GetProcessDiagnosticsRequest) returns (GetProcessDiagnosticsResponse) {}
}
```

### 5. 消息头支持

添加了消息头支持，允许在消息中包含元数据：

```protobuf
message MessageHeader {
  map<string, string> header_data = 1;
}
```

## API 兼容性

为了确保向后兼容性，我们提供了一个兼容性层，允许现有代码继续工作。

### 旧 API

旧的全局函数仍然可用，但现在它们委托给默认的 ActorSystem 实例：

```kotlin
// 旧 API
fun spawn(props: Props): PID
fun spawnNamed(props: Props, name: String): PID
fun send(pid: PID, message: Any)
fun request(pid: PID, message: Any, sender: PID)
suspend fun <T> requestAwait(pid: PID, message: Any, timeout: Duration): T
```

### 新 API

新的 API 使用 ActorSystem 类，提供更明确的 Actor 管理：

```kotlin
// 新 API
val system = ActorSystem("my-system")
val pid = system.actorOf(props)
system.send(pid, message)
val response = system.requestAsync<String>(pid, message, timeout)
```

## 消息头支持

Context 接口现在支持消息头，允许在消息中包含元数据：

```kotlin
interface Context {
    // 现有方法...
    
    fun messageHeaders(): Map<String, String>
    fun setMessageHeader(key: String, value: String)
}
```

## 请求-响应模式

ActorSystem 现在支持请求-响应模式，允许等待 Actor 的响应：

```kotlin
val response = system.requestAsync<String>(pid, message, timeout)
```

## 远程通信

远程通信已更新为使用新的协议，支持双向流式通信：

```kotlin
val remote = Remote.get(system)
remote.start("localhost", 8090)
```

## 示例

### 使用新 API 创建 Actor

```kotlin
val system = ActorSystem("my-system")
val props = fromProducer { MyActor() }
val pid = system.actorOf(props)
system.send(pid, "hello")
```

### 使用请求-响应模式

```kotlin
val system = ActorSystem("my-system")
val props = fromProducer { ResponderActor() }
val pid = system.actorOf(props)
val response = system.requestAsync<String>(pid, "request", Duration.ofSeconds(5))
```

### 使用消息头

```kotlin
class MyActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is String -> {
                val headers = messageHeaders()
                val correlationId = headers["correlation-id"]
                setMessageHeader("response-id", correlationId)
                respond("response")
            }
        }
    }
}
```

## 迁移指南

要迁移到新的 API，请按照以下步骤操作：

1. 创建一个 ActorSystem 实例：

```kotlin
val system = ActorSystem("my-system")
```

2. 将 `spawn` 调用替换为 `system.actorOf`：

```kotlin
// 旧代码
val pid = spawn(props)

// 新代码
val pid = system.actorOf(props)
```

3. 将 `send` 调用替换为 `system.send`：

```kotlin
// 旧代码
send(pid, message)

// 新代码
system.send(pid, message)
```

4. 将 `requestAwait` 调用替换为 `system.requestAsync`：

```kotlin
// 旧代码
val response = requestAwait<String>(pid, message, timeout)

// 新代码
val response = system.requestAsync<String>(pid, message, timeout)
```
