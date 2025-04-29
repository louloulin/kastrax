# Kastrax Actor 集群功能

本文档详细介绍了 Kastrax Actor 模块的集群功能，包括配置、使用方法和最佳实践。

## 目录

- [概述](#概述)
- [集群配置](#集群配置)
- [创建集群](#创建集群)
- [集群操作](#集群操作)
- [集群 Agent](#集群-agent)
- [高级功能](#高级功能)
- [性能优化](#性能优化)
- [示例](#示例)
- [常见问题](#常见问题)

## 概述

Kastrax Actor 模块的集群功能基于 kactor 的集群实现，提供了分布式 Actor 系统的能力。通过集群功能，您可以：

- 创建分布式 Actor 系统
- 在集群中注册和发现 Agent
- 实现 Agent 之间的高效通信
- 构建可扩展的 AI 应用

## 集群配置

集群配置通过 `ClusterConfig` 类进行，主要参数包括：

```kotlin
data class ClusterConfig(
    val hostname: String = "0.0.0.0",
    val port: Int = 8090,
    val clusterName: String = "kastrax-cluster",
    val seeds: List<String> = emptyList(),
    val requestTimeout: Duration = Duration.ofSeconds(5),
    val gossipInterval: Duration = Duration.ofMillis(300)
)
```

参数说明：

- `hostname`：主机名，默认为 "0.0.0.0"
- `port`：端口号，默认为 8090
- `clusterName`：集群名称，默认为 "kastrax-cluster"
- `seeds`：种子节点列表，默认为空列表
- `requestTimeout`：请求超时时间，默认为 5 秒
- `gossipInterval`：Gossip 间隔，默认为 300 毫秒

## 创建集群

使用 `configureCluster` 函数创建集群：

```kotlin
// 配置集群
val config = ClusterConfig(
    hostname = "localhost",
    port = 8090,
    clusterName = "kastrax-demo-cluster",
    seeds = listOf("localhost:8090")
)

// 创建集群系统
val system = configureCluster("seed-node", config)
```

## 集群操作

### 加入集群

```kotlin
// 加入集群
system.joinCluster()
```

### 离开集群

```kotlin
// 离开集群
system.leaveCluster()
```

### 获取集群成员

```kotlin
// 获取集群成员列表
val members = system.getClusterMembers()
println("当前集群成员: $members")
```

## 集群 Agent

### 注册 Agent

```kotlin
// 创建 Agent
val agent = MyAgent("my-agent")

// 注册到集群
val pid = system.registerClusterAgent(agent, "my-kind", "my-id")
```

### 获取 Agent

```kotlin
// 获取 Agent
val pid = system.getClusterAgent("my-kind", "my-id")
```

### 发送消息

```kotlin
// 发送消息
system.root.send(pid, MyMessage("Hello"))
```

### 请求-响应

```kotlin
// 发送请求并等待响应
val response = system.root.requestAwait<MyResponse>(
    pid,
    MyRequest("Hello"),
    Duration.ofSeconds(5)
)
```

### 广播消息

```kotlin
// 广播消息给所有特定类型的 Agent
system.broadcastToCluster("my-kind", MyMessage("Hello to all"))
```

## 高级功能

### 多种类型的 Agent

您可以在集群中注册不同类型的 Agent，每种类型负责不同的任务：

```kotlin
// 注册管理 Agent
system.registerClusterAgent(managerAgent, "manager", "manager-1")

// 注册工作 Agent
system.registerClusterAgent(workerAgent, "worker", "worker-1")

// 注册分析 Agent
system.registerClusterAgent(analyzerAgent, "analyzer", "analyzer-1")
```

### 负载均衡

集群会自动在多个节点之间分配负载：

```kotlin
// 获取虚拟 Actor（会自动分配到合适的节点）
val pid = cluster.get("my-request", "worker")
```

### 故障检测和恢复

集群会自动检测节点故障并进行恢复：

```kotlin
// 监控节点状态
val members = system.getClusterMembers()
members.forEach { member ->
    val status = getNodeStatus(member)
    if (status == "UNAVAILABLE") {
        // 处理节点故障
        handleNodeFailure(member)
    }
}
```

## 性能优化

### 缓存 Cluster 实例

`getCluster` 方法会自动缓存 Cluster 实例，避免重复创建：

```kotlin
// 获取集群实例（会自动缓存）
val cluster = system.getCluster()
```

### 批量处理消息

尽量批量处理消息，减少网络开销：

```kotlin
// 批量发送消息
val messages = listOf(
    MyMessage("Message 1"),
    MyMessage("Message 2"),
    MyMessage("Message 3")
)

// 发送批量消息
system.root.send(pid, BatchMessages(messages))
```

### 使用本地 Actor

对于不需要分布式的操作，尽量使用本地 Actor：

```kotlin
// 创建本地 Actor
val props = fromProducer { MyActor() }
val pid = system.root.spawn(props)
```

## 示例

### 基本示例

参见 `ClusterExample.kt`，演示了基本的集群功能：

```kotlin
// 启动种子节点
ClusterExample.startSeedNode()

// 启动工作节点
ClusterExample.startWorkerNode(8091, "worker-1")

// 启动客户端
ClusterExample.startClient()
```

### 高级示例

参见 `AdvancedClusterExample.kt`，演示了更复杂的集群功能：

```kotlin
// 启动管理节点
AdvancedClusterExample.startManagerNode()

// 启动处理器节点
AdvancedClusterExample.startWorkerNode(8091, "processor-1", "processor")

// 启动分析器节点
AdvancedClusterExample.startWorkerNode(8092, "analyzer-1", "analyzer")

// 启动客户端
AdvancedClusterExample.startClient()
```

## 常见问题

### 集群节点无法连接

检查以下几点：

1. 确保种子节点已启动
2. 确保网络连接正常
3. 确保端口未被占用
4. 检查防火墙设置

### 消息发送失败

可能的原因：

1. 目标节点不可用
2. 消息超时
3. 序列化错误

解决方法：

1. 检查节点状态
2. 增加超时时间
3. 确保消息可序列化

### 性能问题

优化建议：

1. 使用缓存
2. 批量处理消息
3. 优化消息大小
4. 增加节点数量分散负载
