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

## 最佳实践

### 网络设计

1. **合理划分 Agent 职责**：每个 Agent 应该有明确的职责和专长
2. **适当设置 Agent 关系**：根据任务需求设置合适的 Agent 关系
3. **避免过于复杂的网络**：网络结构应该简单明了，避免过于复杂

### 协议选择

1. **顺序协议**：适用于步骤明确、依赖性强的任务
2. **层次协议**：适用于可以分解为独立子任务的复杂任务
3. **共识协议**：适用于需要多方观点和评估的决策任务

### 性能优化

1. **减少协作步骤**：尽量减少不必要的协作步骤
2. **优化消息大小**：避免传递过大的消息
3. **合理设置超时**：根据任务复杂度设置合适的超时时间
