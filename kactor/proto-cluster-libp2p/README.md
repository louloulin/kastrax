# protoactor-kotlin libp2p 集群

这个模块提供了基于 libp2p 的 protoactor-kotlin 分布式集群实现。

## 特性

- 完全去中心化的架构，没有单点故障
- 自动节点发现（使用 mDNS 和种子节点）
- 基于 gossip 的集群状态同步
- 虚拟 Actor 定位和激活
- 安全的点对点通信

## 使用方法

### 添加依赖

```kotlin
dependencies {
    implementation("io.libp2p:jvm-libp2p-minimal:0.9.0")
    implementation("actor.proto:proto-cluster-libp2p:0.1.0")
}
```

### 创建和启动集群

```kotlin
// 创建 Actor 系统
val system = ActorSystem("node1")

// 创建 P2P 集群配置
val p2pConfig = P2PClusterConfig(
    clusterName = "my-cluster",
    enableMDns = true,
    listenPort = 4001,
    seedNodes = listOf("QmYyQSo1c1Ym7orWxLYvCrM2EmxFTANf8wXmmE7DWjhx5N@/ip4/192.168.1.10/tcp/4001")
)

// 创建集群提供者
val clusterProvider = P2PClusterProvider(p2pConfig)

// 创建集群配置
val clusterConfig = ClusterConfig(
    clusterName = "my-cluster",
    clusterProvider = clusterProvider
)

// 创建集群
val cluster = Cluster(system, clusterConfig)

// 注册 Actor 类型
val greetingProps = Props.fromProducer { GreetingActor() }
cluster.registerKind("greeting", ClusterKind.fromProps("greeting", greetingProps))

// 启动集群
cluster.startMember()
```

### 获取和使用虚拟 Actor

```kotlin
// 获取虚拟 Actor
val pid = cluster.get("user-123", "greeting")

// 发送消息
system.root.send(pid, "Hello!")

// 发送请求并等待响应
val response = system.root.requestAwait<String>(pid, "Hello!", Duration.ofSeconds(5))
println("Response: $response")
```

### 关闭集群

```kotlin
// 优雅关闭集群
cluster.shutdown(true)
system.shutdown()
```

## 运行示例

1. 启动第一个节点：

```bash
java -cp proto-cluster-libp2p.jar actor.proto.cluster.libp2p.examples.P2PClusterExample node1 4001
```

2. 启动第二个节点，使用第一个节点作为种子节点：

```bash
java -cp proto-cluster-libp2p.jar actor.proto.cluster.libp2p.examples.P2PClusterExample node2 4002 <node1-peer-id>@/ip4/127.0.0.1/tcp/4001
```

3. 使用命令行交互：

```
> members
Cluster members: [Member(id=node1, host=QmYyQSo1c1Ym7orWxLYvCrM2EmxFTANf8wXmmE7DWjhx5N, port=4001, kinds=[greeting]), Member(id=node2, host=QmZMxNdpMkewiVZLMRxaNxUeZpDUb34pWjZ1kZvsd16Zic, port=4002, kinds=[greeting])]

> get user-123
Got PID: PID(address=node1, id=greeting/user-123)

> send user-123 Hello, virtual actor!
Response: Hello from GreetingActor: Hello, virtual actor!
```

## 配置选项

`P2PClusterConfig` 提供了以下配置选项：

- `clusterName`: 集群名称，用于区分不同的集群
- `enableMDns`: 是否启用 mDNS 发现（默认为 true）
- `seedNodes`: 种子节点列表，格式为 "PeerId@/ip4/address/tcp/port"
- `bootstrapTimeout`: 引导超时时间（默认为 30 秒）
- `heartbeatInterval`: 心跳间隔时间（默认为 5 秒）
- `monitorInterval`: 监控间隔时间（默认为 15 秒）
- `gossipFanout`: Gossip 扇出数量（默认为 3）
- `listenAddress`: 监听地址（默认为 0.0.0.0）
- `listenPort`: 监听端口（默认为 0，随机端口）
