# ProtoActor-Kotlin 兼容性改造计划

本文档概述了使 ProtoActor-Kotlin 与 ProtoActor 协议兼容的计划，确保与 ProtoActor-Go 的互操作性。

## 当前状态分析

### ProtoActor-Go 实现

ProtoActor-Go 是一个成熟的 Actor 模型实现，具有以下关键组件：

1. **Actor System**：Actor 的中央管理单元，负责 Actor 的创建、监督和生命周期管理
2. **PID (Process ID)**：Actor 的标识符，包含 Address 和 ID，以及请求 ID
3. **Context**：Actor 操作的接口，提供消息处理和 Actor 交互能力
4. **Props**：Actor 创建的配置，定义 Actor 的行为和特性
5. **Remote Communication**：基于 gRPC 的远程 Actor 通信，支持跨网络的 Actor 交互
6. **Serialization**：使用 Protocol Buffers 进行消息序列化，支持多种序列化格式
7. **Cluster Support**：分布式 Actor 系统支持，包括节点发现、故障检测和负载均衡

Go 实现使用 Protocol Buffers 进行消息序列化，使用 gRPC 进行远程通信。它具有明确定义的远程 Actor 通信协议。

### ProtoActor-Kotlin 实现

ProtoActor-Kotlin 是 Actor 模型的一个实现，具有类似的组件：

1. **Actor 接口**：核心 Actor 行为定义，定义了消息处理方法
2. **PID**：类似于 Go 实现，但缺少 request_id 字段
3. **Context**：Actor 操作的接口，但功能较 Go 实现更简单
4. **Props**：Actor 创建的配置，但配置选项较少
5. **Remote Communication**：基于 gRPC，但协议定义与 Go 实现不同
6. **Serialization**：使用 Protocol Buffers，但序列化选项有限

## 主要差异和兼容性问题

1. **Protocol Buffer 定义**：
   - Go：使用更新的协议定义，包含额外字段
   - Kotlin：使用较旧的协议定义，缺少一些关键字段

2. **远程通信协议**：
   - Go：使用单一的 `Receive` 方法进行双向流式通信
   - Kotlin：为不同操作使用单独的方法

3. **消息信封结构**：
   - Go：包含消息头、请求 ID 和更多字段
   - Kotlin：更简单的结构，没有消息头和请求 ID

4. **Actor System**：
   - Go：有明确的 ActorSystem 类
   - Kotlin：使用全局函数和单例

5. **序列化**：
   - Go：更灵活的序列化选项
   - Kotlin：序列化选项有限

## 系统架构设计

### 整体架构

改造后的 ProtoActor-Kotlin 将采用以下分层架构：

1. **核心层**：
   - ActorSystem：Actor 系统核心，管理 Actor 生命周期
   - ProcessRegistry：进程注册表，管理 PID 映射
   - Mailbox：邮箱系统，处理消息队列
   - Dispatcher：调度器，管理 Actor 执行

2. **通信层**：
   - Remote：远程通信模块，处理跨网络 Actor 交互
   - Serialization：序列化模块，处理消息的序列化和反序列化
   - Endpoint：端点管理，处理远程连接

3. **集群层**：
   - Cluster：集群管理，处理节点发现和成员管理
   - Gossip：八卦协议，用于集群状态同步
   - Grain：虚拟 Actor 实现，提供位置透明性

4. **工具层**：
   - Logging：日志记录
   - Metrics：性能指标收集
   - Diagnostics：诊断工具

### 组件交互

```
┌─────────────────────────────────────────────────────────────┐
│                        应用层                               │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      ActorSystem                            │
├─────────────┬─────────────┬────────────────┬───────────────┤
│ Actor 创建   │  监督管理   │  消息传递      │  生命周期管理  │
└─────────────┴──────┬──────┴────────────────┴───────────────┘
                     ▼
┌─────────────┬─────────────┬────────────────┬───────────────┐
│ ProcessRegistry│  Mailbox  │   Dispatcher   │    Context    │
└─────────────┴─────────────┴────────────────┴───────────────┘
        │                                            ▲
        │                                            │
        ▼                                            │
┌─────────────────────────────────────────────────────────────┐
│                        Remote                               │
├─────────────┬─────────────┬────────────────┬───────────────┤
│ EndpointManager│ EndpointWriter│ EndpointReader │ Serialization │
└─────────────┴─────────────┴────────────────┴───────────────┘
        │                                            ▲
        │                                            │
        ▼                                            │
┌─────────────────────────────────────────────────────────────┐
│                        Cluster                              │
├─────────────┬─────────────┬────────────────┬───────────────┤
│ MemberList  │   Gossip    │     Grain      │  Partitioning  │
└─────────────┴─────────────┴────────────────┴───────────────┘
```

## 功能点详细设计

### 1. 更新 Protocol Buffer 定义

1. **更新 actor.proto**：
   - 添加 `request_id` 字段到 PID
   - 添加 `TerminatedReason` 枚举
   - 添加新消息类型 (Touch, Touched, DeadLetterResponse)
   - 详细字段定义：
     ```protobuf
     message PID {
       string Address = 1;
       string Id = 2;
       uint32 request_id = 3;
     }

     enum TerminatedReason {
       Stopped = 0;
       AddressTerminated = 1;
       NotFound = 2;
     }
     ```

2. **更新 remote.proto**：
   - 与 Go 实现对齐
   - 添加 MessageHeader 支持
   - 更新 MessageBatch 和 MessageEnvelope 结构
   - 添加新的请求/响应类型
   - 详细字段定义：
     ```protobuf
     message MessageHeader {
       map<string, string> header_data = 1;
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

### 2. 实现新的远程通信协议

1. **更新 Remote 服务**：
   - 实现双向流式通信，使用单一的 `Receive` 方法
   - 支持新的消息类型和结构
   - API 设计：
     ```kotlin
     interface RemotingService {
         fun receive(stream: Flow<RemoteMessage>): Flow<RemoteMessage>
         fun listProcesses(request: ListProcessesRequest): ListProcessesResponse
         fun getProcessDiagnostics(request: GetProcessDiagnosticsRequest): GetProcessDiagnosticsResponse
     }
     ```

2. **更新端点管理**：
   - 修改 EndpointWriter 以支持新协议
   - 更新 EndpointReader 以处理新的消息格式
   - 实现连接管理和重连逻辑
   - 类设计：
     ```kotlin
     class EndpointManager(private val remote: Remote) {
         fun start()
         fun stop()
         fun getEndpoint(address: String): Endpoint
     }

     class EndpointWriter(private val address: String, private val config: RemoteConfig) {
         suspend fun sendEnvelopes(batch: MessageBatch)
         suspend fun connect()
         suspend fun disconnect()
     }
     ```

### 3. 添加消息头支持

1. **实现 MessageHeader**：
   - 在消息中添加头部支持
   - 更新序列化以包含头部
   - 类设计：
     ```kotlin
     class MessageHeader {
         private val headers: MutableMap<String, String> = mutableMapOf()

         fun set(key: String, value: String)
         fun get(key: String): String?
         fun remove(key: String)
         fun clear()
     }
     ```

2. **更新消息处理**：
   - 修改消息处理以处理头部
   - 添加头部访问 API
   - 接口扩展：
     ```kotlin
     interface Context {
         // 现有方法...
         fun messageHeaders(): MessageHeader
         fun setMessageHeader(key: String, value: String)
     }
     ```

### 4. 实现请求 ID 支持

1. **更新 PID 类**：
   - 添加 request_id 字段
   - 更新序列化/反序列化
   - 类设计：
     ```kotlin
     data class PID(
         val address: String,
         val id: String,
         val requestId: UInt = 0u
     )
     ```

2. **实现请求-响应模式**：
   - 在消息处理中添加请求 ID 支持
   - 实现请求和响应之间的关联
   - API 设计：
     ```kotlin
     interface ActorSystem {
         // 现有方法...
         suspend fun <T> requestAsync(pid: PID, message: Any, timeout: Duration): T
     }
     ```

### 5. 增强序列化

1. **扩展序列化选项**：
   - 支持更多序列化格式（JSON, CBOR, Protocol Buffers）
   - 使序列化更可插拔
   - 接口设计：
     ```kotlin
     interface Serializer {
         fun serialize(obj: Any): ByteArray
         fun deserialize(bytes: ByteArray, type: Class<*>): Any
         val identifier: Int
     }
     ```

2. **更新序列化逻辑**：
   - 处理新的消息结构
   - 支持头部和请求 ID
   - 注册机制：
     ```kotlin
     object Serialization {
         fun registerSerializer(serializer: Serializer)
         fun findSerializerById(id: Int): Serializer
         fun findSerializerForType(type: Class<*>): Serializer
     }
     ```

### 6. 引入 Actor System 类

1. **创建 ActorSystem 类**：
   - 将全局函数移至 ActorSystem 类
   - 使 Actor 创建和管理显式化
   - 类设计：
     ```kotlin
     class ActorSystem(val name: String) {
         fun actorOf(props: Props, name: String): PID
         fun deadLetter(): PID
         fun root(): PID
         fun stop(pid: PID)
         fun poison(pid: PID)
         fun registerHostResolver(resolver: (PID) -> Process?)
         // 更多方法...
     }
     ```

2. **更新 API**：
   - 修改现有代码以使用 ActorSystem
   - 提供向后兼容性
   - 适配层：
     ```kotlin
     // 全局默认 ActorSystem 实例
     val defaultActorSystem = ActorSystem("default")

     // 向后兼容的全局函数
     fun spawn(props: Props): PID = defaultActorSystem.actorOf(props)
     fun stop(pid: PID) = defaultActorSystem.stop(pid)
     // 更多兼容函数...
     ```

### 7. 添加来自 Go 实现的新功能

1. **实现诊断功能**：
   - 添加进程诊断支持
   - 实现 ListProcesses 功能
   - API 设计：
     ```kotlin
     interface Diagnostics {
         fun getProcessInfo(pid: PID): ProcessInfo
         fun listProcesses(pattern: String, matchType: MatchType): List<PID>
     }
     ```

2. **添加集群支持**：
   - 实现类似于 Go 的集群功能
   - 支持八卦协议和成员管理
   - 类设计：
     ```kotlin
     class Cluster(val actorSystem: ActorSystem) {
         fun start(config: ClusterConfig)
         fun join(address: String)
         fun leave()
         fun registerMemberStatusListener(listener: MemberStatusListener)
     }
     ```

## 实现计划（Todo List）

### 阶段 1：协议对齐（2周）

- [x] **更新 Protocol Buffer 定义**
  - [x] 更新 actor.proto 文件
  - [x] 更新 remote.proto 文件
  - [x] 生成新的代码

- [x] **更新基本消息结构**
  - [x] 更新 PID 类添加 request_id
  - [x] 实现 TerminatedReason 枚举
  - [x] 添加新的系统消息类型

- [x] **基本兼容性测试**
  - [x] 编写单元测试验证新的消息结构
  - [x] 测试序列化/反序列化
  - [x] 测试基本功能

### 阶段 2：远程通信（3周）

- [x] **实现新的 Remote 服务**
  - [x] 创建 RemotingService 接口
  - [x] 实现双向流式通信
  - [x] 添加连接管理

- [x] **更新端点管理**
  - [x] 重构 EndpointManager
  - [x] 更新 EndpointWriter
  - [x] 更新 EndpointReader
  - [x] 测试基本功能

- [x] **添加消息头支持**
  - [x] 实现 MessageHeader 类
  - [x] 更新消息处理逻辑
  - [x] 添加头部访问 API
  - [x] 测试消息头功能

- [x] **添加请求 ID 支持**
  - [x] 更新消息路由
  - [x] 实现请求-响应关联
  - [x] 添加超时处理

- [x] **与 Go 实现的集成测试**
  - [x] 测试基本消息传递
  - [x] 测试远程 Actor 创建
  - [x] 测试请求-响应模式
  - [x] 添加兼容性测试

### 阶段 3：Actor System 和 API 增强（2周）

- [x] **实现 ActorSystem 类**
  - [x] 创建基本 ActorSystem 结构
  - [x] 移植全局函数
  - [x] 实现 Actor 生命周期管理

- [x] **更新 API**
  - [x] 创建新的 API 接口
  - [x] 实现向后兼容层
  - [x] 更新示例代码

- [x] **添加诊断支持**
  - [x] 实现进程信息收集
  - [x] 添加 ListProcesses 功能
  - [x] 创建诊断 API
  - [x] 添加文档

- [x] **全面测试**
  - [x] 单元测试
  - [x] 集成测试
  - [x] 性能测试

### 阶段 4：高级功能（4周）

- [x] **实现集群支持**
  - [x] 创建集群基础结构
  - [x] 实现成员管理
  - [x] 添加节点发现

- [x] **添加八卦协议**
  - [x] 实现状态同步
  - [x] 添加故障检测
  - [x] 优化网络通信

- [x] **实现 Grain 支持**
  - [x] 创建虚拟 Actor 框架
  - [x] 实现位置透明性
  - [x] 添加状态持久化

- [x] **实现 Actor 重入支持**
  - [x] 添加 Future 类
  - [x] 实现 reenterAfter 方法
  - [x] 添加 FutureContinuation 消息
  - [x] 编写单元测试
  - [x] 添加文档
  - [x] 修复编译错误并验证功能

- [x] **性能优化**
  - [x] 优化消息传递
  - [x] 减少内存使用
  - [x] 提高并发性能

- [x] **支持 Native 编译**
  - [x] 添加 GraalVM Native Image 支持
  - [x] 创建反射配置
  - [x] 实现简单的基准测试
  - [x] 添加构建脚本
  - [x] 编写文档

- [x] **与 Go 实现的全面兼容性测试**
  - [x] 测试集群功能
  - [x] 测试 Grain 功能
  - [x] 测试高负载场景

## 测试策略

1. **单元测试**：
   - [x] 隔离测试每个组件
   - [x] 验证 Protocol Buffer 序列化/反序列化
   - [x] 测试工具：JUnit 5, Mockk

2. **集成测试**：
   - [x] 测试 Kotlin Actor 之间的通信
   - [x] 使用更新的协议进行测试
   - [x] 测试工具：TestContainers, Awaitility

3. **跨语言测试**：
   - [x] 测试 Kotlin 和 Go Actor 之间的通信
   - [x] 验证消息传递和远程 Actor 创建
   - [x] 测试环境：Docker Compose

4. **性能测试**：
   - [x] 基准测试消息吞吐量
   - [x] 与 Go 实现进行比较
   - [x] 测试工具：JMH, Gatling

## 向后兼容性

1. **API 兼容性**：
   - [x] 为现有代码提供适配层
   - [x] 逐步弃用旧 API
   - [x] 提供迁移工具

2. **迁移指南**：
   - [x] 记录变更和迁移路径
   - [x] 提供更新现有代码的示例
   - [x] 创建迁移教程

## 结论

本计划概述了使 ProtoActor-Kotlin 与 ProtoActor 协议兼容所需的步骤，确保与 ProtoActor-Go 的互操作性。实现将分阶段进行，每个阶段都进行测试以确保兼容性和性能。通过这些改进，ProtoActor-Kotlin 将成为一个更强大、更灵活的 Actor 框架，能够与 Go 实现无缝集成，为 Kotlin 开发者提供高性能的分布式系统开发工具。
