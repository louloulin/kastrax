# ProtoActor-Kotlin 远程通信

本文档描述了 ProtoActor-Kotlin 的远程通信功能。

## 概述

ProtoActor-Kotlin 的远程通信功能允许 Actor 跨网络边界进行通信。它使用 gRPC 作为传输层，使用 Protocol Buffers 进行消息序列化。

## 架构

远程通信功能由以下组件组成：

1. **Remote**：远程通信的主要入口点，管理 gRPC 服务器和端点
2. **EndpointManager**：管理与远程端点的连接
3. **EndpointWriter**：负责向远程端点发送消息
4. **EndpointReader**：负责从远程端点接收消息
5. **Serialization**：负责消息的序列化和反序列化

## 使用方法

### 启动远程服务器

```kotlin
val system = ActorSystem("my-system")
val remote = Remote.get(system)
remote.start("localhost", 8090)
```

### 注册已知类型

```kotlin
remote.registerKnownKind("my-actor", fromProducer { MyActor() })
```

### 创建远程 Actor

```kotlin
val address = "localhost:8090"
val pid = remote.spawnNamed(address, "my-actor", "my-actor-name", Duration.ofSeconds(5))
```

### 向远程 Actor 发送消息

```kotlin
system.send(pid, "hello")
```

### 使用请求-响应模式

```kotlin
val response = system.requestAsync<String>(pid, "request", Duration.ofSeconds(5))
```

## 协议

远程通信使用以下 Protocol Buffers 定义：

```protobuf
message RemoteMessage {
  oneof message_type {
    MessageBatch message_batch = 1;
    ConnectRequest connect_request = 2;
    ConnectResponse connect_response = 3;
    DisconnectRequest disconnect_request = 4;
  }
}

message MessageBatch {
  repeated string type_names = 1;
  repeated actor.PID targets = 2;
  repeated MessageEnvelope envelopes = 3;
  repeated actor.PID senders = 4;
}

message MessageEnvelope {
  int32 type_id = 1;
  bytes message_data = 2;
  int32 target = 3;
  int32 sender = 4;
  int32 serializer_id = 5;
  MessageHeader message_header = 6;
  uint32 target_request_id = 7;
  uint32 sender_request_id = 8;
}
```

## 序列化

远程通信使用 Protocol Buffers 进行消息序列化。默认情况下，它使用 Protocol Buffers 序列化器，但也可以注册自定义序列化器：

```kotlin
Serialization.registerSerializer(MySerializer())
```

## 消息头

远程通信支持消息头，允许在消息中包含元数据：

```kotlin
val header = MessageHeader()
header.set("correlation-id", "123")
```

## 错误处理

远程通信包含错误处理机制，当远程端点不可用时，它会发布 `EndpointTerminatedEvent` 事件：

```kotlin
EventStream.subscribe<EndpointTerminatedEvent> { event ->
    println("Endpoint terminated: ${event.address}")
}
```

## 与 Go 实现的互操作性

ProtoActor-Kotlin 的远程通信功能与 ProtoActor-Go 完全兼容，允许 Kotlin 和 Go 实现之间的互操作性。

### 从 Kotlin 到 Go

```kotlin
// Kotlin 代码
val system = ActorSystem("kotlin-system")
val remote = Remote.get(system)
remote.start("localhost", 8090)

// 连接到 Go 服务器
val goPid = PID("go-server:8090", "go-actor")
system.send(goPid, "hello from kotlin")
```

### 从 Go 到 Kotlin

```go
// Go 代码
system := actor.NewActorSystem()
remote := remote.NewRemote(system, remote.Configure())
remote.Start("go-server", 8090)

// 连接到 Kotlin 服务器
kotlinPid := actor.NewPID("kotlin-server:8090", "kotlin-actor")
system.Send(kotlinPid, "hello from go")
```
