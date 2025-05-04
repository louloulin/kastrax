# Kastrax Actor 协作网络

本文档详细介绍了 Kastrax Actor 模块的 Agent 协作网络功能，包括网络拓扑、协作协议和使用方法。

## 目录

- [概述](#概述)
- [网络拓扑](#网络拓扑)
- [协作协议](#协作协议)
- [使用方法](#使用方法)
- [示例](#示例)
- [最佳实践](#最佳实践)

## 概述

Agent 协作网络是 Kastrax Actor 模块的一个高级功能，它允许多个 Agent 组成一个协作网络，共同解决复杂问题。通过定义 Agent 之间的关系和协作协议，可以实现更加灵活和强大的 AI 系统。

主要特点：

- **灵活的网络拓扑**：支持多种 Agent 关系类型，如对等、主从、监督等
- **多样的协作协议**：提供顺序、层次、共识等多种协作协议
- **可扩展的架构**：易于扩展新的协作协议和关系类型
- **基于 Actor 模型**：利用 Actor 模型的并发和分布式特性

## 网络拓扑

网络拓扑定义了 Agent 之间的连接关系，由 `NetworkTopology` 类管理。

### Agent 关系类型

Agent 之间可以建立多种类型的关系，由 `AgentRelation` 枚举定义：

- **PEER**：对等关系，Agent 之间平等协作
- **MASTER**：主从关系，源节点是主节点
- **SLAVE**：主从关系，源节点是从节点
- **COLLABORATOR**：协作关系，Agent 之间紧密协作
- **SUPERVISOR**：监督关系，源节点是监督者
- **SUPERVISED**：监督关系，源节点是被监督者

### 网络操作

`NetworkTopology` 类提供了以下主要操作：

- **添加节点**：`addNode(nodeId: String)`
- **移除节点**：`removeNode(nodeId: String)`
- **添加边**：`addEdge(fromId: String, toId: String, relation: AgentRelation)`
- **移除边**：`removeEdge(fromId: String, toId: String)`
- **获取连接节点**：`getConnectedNodes(nodeId: String): List<String>`
- **获取特定关系节点**：`getNodesByRelation(nodeId: String, relation: AgentRelation): List<String>`

## 协作协议

协作协议定义了 Agent 之间的协作方式，由 `CollaborationProtocol` 接口定义。

### 协作结果

协作的结果由 `CollaborationResult` 类表示，包含以下信息：

- **success**：是否成功
- **result**：结果内容
- **participants**：参与者列表
- **steps**：协作步骤

### 协作步骤

每个协作步骤由 `CollaborationStep` 类表示，包含以下信息：

- **agentId**：执行步骤的 Agent ID
- **input**：输入内容
- **output**：输出内容
- **timestamp**：时间戳

### 内置协议

Kastrax Actor 模块提供了以下内置协议：

#### 顺序协议 (SequentialProtocol)

按照预定义的顺序依次执行任务，每个 Agent 的输出作为下一个 Agent 的输入。

```kotlin
val protocol = SequentialProtocol(
    agentSequence = listOf("agent1", "agent2", "agent3")
)
```

#### 层次协议 (HierarchicalProtocol)

由主 Agent 分配任务给从 Agent，然后整合结果。

```kotlin
val protocol = HierarchicalProtocol()
```

#### 共识协议 (ConsensusProtocol)

所有 Agent 对同一任务提出解决方案，然后通过评估和改进达成共识。

```kotlin
val protocol = ConsensusProtocol()
```

## 使用方法

### 创建 Agent 网络

```kotlin
// 创建 Actor 系统
val system = ActorSystem("agent-network")

// 创建 Agent 网络
val network = AgentNetwork(system)
```

### 添加 Agent

```kotlin
// 创建 Agent
val agent1 = MyAgent("agent1")
val agent2 = MyAgent("agent2")

// 添加 Agent 到网络
network.addAgent(agent1)
network.addAgent(agent2)
```

### 建立 Agent 关系

```kotlin
// 建立对等关系
network.connectAgents("agent1", "agent2", AgentRelation.PEER)

// 建立主从关系
network.connectAgents("agent1", "agent3", AgentRelation.MASTER)
```

### 执行协作任务

```kotlin
// 创建协作协议
val protocol = SequentialProtocol(
    agentSequence = listOf("agent1", "agent2", "agent3")
)

// 执行协作任务
val result = network.collaborate(
    protocol = protocol,
    initiatorId = "agent1",
    task = "解决问题 XYZ"
)

// 处理结果
println("协作结果: ${result.result}")
println("参与者: ${result.participants.joinToString()}")
println("步骤数: ${result.steps.size}")
```

### 关闭网络

```kotlin
// 关闭网络
network.shutdown()
```

## 示例

### 顺序协作示例

```kotlin
// 创建顺序协作协议
val sequentialProtocol = SequentialProtocol(
    agentSequence = listOf("researcher", "writer", "critic", "manager")
)

// 执行协作任务
val result = network.collaborate(
    protocol = sequentialProtocol,
    initiatorId = "manager",
    task = "撰写一篇关于人工智能在医疗领域应用的文章"
)
```

### 层次协作示例

```kotlin
// 创建层次协作协议
val hierarchicalProtocol = HierarchicalProtocol()

// 执行协作任务
val result = network.collaborate(
    protocol = hierarchicalProtocol,
    initiatorId = "manager",
    task = "撰写一篇关于人工智能在教育领域应用的文章"
)
```

### 共识协作示例

```kotlin
// 创建共识协作协议
val consensusProtocol = ConsensusProtocol()

// 执行协作任务
val result = network.collaborate(
    protocol = consensusProtocol,
    initiatorId = "manager",
    task = "提出一个解决气候变化的创新方案"
)
```

## 网络监控与管理

### 监控功能

Kastrax Actor 模块提供了全面的网络监控功能，帮助您实时了解网络状态和性能。

```kotlin
// 创建网络监控器
val monitor = NetworkMonitor(network)

// 获取网络状态
val status = monitor.getNetworkStatus()
println("活跃节点数量: ${status.activeNodes}")
println("消息处理速率: ${status.messagesPerSecond} 消息/秒")
println("平均响应时间: ${status.averageResponseTime} 毫秒")

// 设置监控回调
monitor.setStatusCallback { status ->
    if (status.averageResponseTime > 1000) {
        println("警告: 响应时间过长!")
    }
}

// 获取特定节点的状态
val nodeStatus = monitor.getNodeStatus("agent1")
println("节点状态: ${nodeStatus.status}")
println("处理的消息数: ${nodeStatus.processedMessages}")
println("失败的消息数: ${nodeStatus.failedMessages}")
```

### 故障处理

Kastrax Actor 模块提供了多种故障处理机制，确保网络的可靠性和弹性。

```kotlin
// 设置故障处理策略
network.setFailureStrategy(FailureStrategy.RESTART_NODE)

// 添加故障处理回调
network.addFailureHandler { nodeId, exception ->
    println("节点 $nodeId 发生故障: ${exception.message}")
    // 执行自定义恢复操作
    network.restartNode(nodeId)
}

// 模拟节点故障
network.simulateNodeFailure("agent1", FailureType.CRASH)

// 检查节点健康状态
val healthStatus = network.checkNodeHealth("agent1")
if (healthStatus.isHealthy) {
    println("节点健康")
} else {
    println("节点不健康: ${healthStatus.reason}")
}
```

### 动态网络调整

Kastrax Actor 模块支持动态调整网络结构，应对不断变化的需求。

```kotlin
// 动态添加节点
val newAgent = MyAgent("new-agent")
network.addAgent(newAgent)

// 动态调整节点关系
network.updateRelation("agent1", "new-agent", AgentRelation.COLLABORATOR)

// 动态移除节点
network.removeAgent("old-agent")

// 动态更新协作协议
val newProtocol = CustomProtocol()
network.updateProtocol(newProtocol)
```

## 最佳实践

### 网络设计

1. **合理划分 Agent 职责**：每个 Agent 应该有明确的职责和专长
2. **适当设置 Agent 关系**：根据任务需求设置合适的 Agent 关系
3. **避免过于复杂的网络**：网络结构应该简单明了，避免过于复杂
4. **考虑扩展性**：设计网络时考虑未来的扩展需求
5. **实现容错设计**：考虑节点故障的情况，实现容错设计

### 协议选择

1. **顺序协议**：适用于步骤明确、依赖性强的任务
2. **层次协议**：适用于可以分解为独立子任务的复杂任务
3. **共识协议**：适用于需要多方观点和评估的决策任务
4. **自定义协议**：对于特殊需求，可以实现自定义协议

### 性能优化

1. **减少协作步骤**：尽量减少不必要的协作步骤
2. **优化消息大小**：避免传递过大的消息
3. **合理设置超时**：根据任务复杂度设置合适的超时时间
4. **使用缓存**：对于重复的请求，使用缓存提高性能
5. **并行处理**：尽可能并行处理独立的任务

### 安全性考虑

1. **身份验证**：实现 Agent 之间的身份验证机制
2. **消息加密**：对敏感信息进行加密传输
3. **访问控制**：实现基于角色的访问控制
4. **安全审计**：记录和审计重要的网络操作
