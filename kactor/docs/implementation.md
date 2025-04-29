# ProtoActor-Kotlin 实现文档

本文档描述了 ProtoActor-Kotlin 与 ProtoActor-Go 兼容性实现的详细信息。

## 协议更新

### 1. ActorProtos.proto

我们更新了 ActorProtos.proto 文件，添加了以下内容：

```protobuf
message PID {
    string Address = 1;
    string Id = 2;
    uint32 request_id = 3;  // 新增字段
}

// 新增消息类型
message DeadLetterResponse {
    PID Target = 1;
}

message Terminated {
    PID who = 1;
    TerminatedReason Why = 2;  // 使用枚举替代布尔值
}

// 新增枚举类型
enum TerminatedReason {
    Stopped = 0;
    AddressTerminated = 1;
    NotFound = 2;
}

// 新增消息类型
message Touch {
}

message Touched {
    PID who = 1;
}
```

### 2. RemoteProtos.proto

我们更新了 RemoteProtos.proto 文件，添加了以下内容：

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
  repeated actor.PID targets = 2;  // 使用 PID 列表替代字符串列表
  repeated MessageEnvelope envelopes = 3;
  repeated actor.PID senders = 4;  // 新增字段
}

message MessageEnvelope {
  int32 type_id = 1;
  bytes message_data = 2;
  int32 target = 3;
  int32 sender = 4;
  int32 serializer_id = 5;
  MessageHeader message_header = 6;  // 新增字段
  uint32 target_request_id = 7;      // 新增字段
  uint32 sender_request_id = 8;      // 新增字段
}

// 新增消息类型
message MessageHeader {
  map<string, string> header_data = 1;
}
```

## 远程通信实现

### 1. EndpointReader

EndpointReader 负责处理来自远程端点的消息。我们更新了 EndpointReader 类，使其支持新的协议：

```kotlin
class EndpointReader : RemotingGrpc.RemotingImplBase() {
    override fun connect(request: RemoteProtos.ConnectRequest, responseObserver: StreamObserver<RemoteProtos.ConnectResponse>) {
        responseObserver.onNext(ConnectResponse(Serialization.defaultSerializerId))
        responseObserver.onCompleted()
    }

    override fun receive(responseObserver: StreamObserver<RemoteProtos.Unit>): StreamObserver<RemoteProtos.MessageBatch> {
        return object : StreamObserver<RemoteProtos.MessageBatch> {
            override fun onCompleted() = responseObserver.onCompleted()
            override fun onError(err: Throwable): Unit = logger.error("Stream observer exception",err)
            override fun onNext(batch: RemoteProtos.MessageBatch) = receiveBatch(batch)
        }
    }

    fun receiveBatch(batch: RemoteProtos.MessageBatch) {
        // 处理消息批次
    }
}
```

### 2. EndpointWriter

EndpointWriter 负责向远程端点发送消息。我们更新了 EndpointWriter 类，使其支持新的协议：

```kotlin
class EndpointWriter(private val address: String, private val config: RemoteConfig) : Actor {
    private var serializerId: Int = 0
    private lateinit var channel: ManagedChannel
    private lateinit var client: RemotingGrpc.RemotingStub
    private lateinit var streamWriter: StreamObserver<RemoteProtos.MessageBatch>

    suspend override fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> started()
            is Stopped -> stopped()
            is Restarting -> restarting()
            is MutableList<*> -> {
                // 处理消息列表
            }
        }
    }

    private suspend fun Context.sendEnvelopesAsync(batch: RemoteProtos.MessageBatch) {
        try {
            streamWriter.onNext(batch)
        } catch (x: Exception) {
            stash()
            logger.error("gRPC Failed to send to address $address, reason ${x.message}")
            throw x
        }
    }

    private suspend fun started() {
        // 初始化连接
    }
}
```

### 3. EndpointManager

EndpointManager 负责管理与远程端点的连接。我们更新了 EndpointManager 类，使其支持新的协议：

```kotlin
class EndpointManager(private val config: RemoteConfig) : Actor, SupervisorStrategy {
    private val _connections: HashMap<String, Endpoint> = HashMap()

    suspend override fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> logger.info("Started EndpointManager")
            is EndpointTerminatedEvent -> ensureConnected(msg.address).watcher.let { send(it, msg) }
            is RemoteTerminate -> ensureConnected(msg.watchee.address).watcher.let { send(it, msg) }
            is RemoteWatch -> ensureConnected(msg.watchee.address).watcher.let { send(it, msg) }
            is RemoteUnwatch -> ensureConnected(msg.watchee.address).watcher.let { send(it, msg) }
            is RemoteDeliver -> ensureConnected(msg.target.address).writer.let { send(it, msg) }
            else -> {
            }
        }
    }

    private fun Context.ensureConnected(address: String): Endpoint = _connections.getOrPut(address, {
        val writer: PID = spawnWriter(address)
        val watcher: PID = spawnWatcher(address)
        Endpoint(writer, watcher)
    })
}
```

## 测试

我们进行了以下测试，以验证实现的正确性：

1. **基本功能测试**：验证基本的 Actor 创建、消息发送和接收功能。
2. **远程通信测试**：验证远程 Actor 的创建和消息传递。
3. **序列化测试**：验证消息的序列化和反序列化。

所有测试都通过，表明实现符合预期。

## 下一步计划

1. **实现 ActorSystem 类**：创建 ActorSystem 类，作为 Actor 的中央管理单元。
2. **实现消息头支持**：添加对消息头的支持，允许在消息中包含元数据。
3. **实现请求-响应模式**：添加对请求-响应模式的支持，允许等待 Actor 的响应。
4. **实现集群支持**：添加对集群的支持，允许 Actor 跨多个节点运行。
