# ProtoActor-Kotlin 与 ProtoActor-Go 比较与改进计划

本文档对比分析了 ProtoActor-Kotlin 和 ProtoActor-Go 的功能差异，并提出了改进计划。

## 1. 核心功能比较

### 1.1 基础 Actor 系统

| 功能 | ProtoActor-Go | ProtoActor-Kotlin | 状态 | 优先级 |
|------|--------------|-------------------|------|--------|
| Actor 接口 | ✅ | ✅ | 完成 | - |
| Context 实现 | ✅ | ✅ | 完成 | - |
| PID | ✅ | ✅ | 完成 | - |
| Props | ✅ | ✅ | 完成 | - |
| Process Registry | ✅ | ✅ | 完成 | - |
| Supervision 策略 | ✅ | ✅ | 完成 | - |
| Mailbox 实现 | ✅ | ✅ | 完成 | - |
| Future/Task | ✅ | ✅ | 完成 | - |
| Behavior 切换 | ✅ | ✅ | 完成 | - |
| Middleware 链 | ✅ | ✅ | 完成 | - |
| Guardian Actor | ✅ | ✅ | 已实现 | 中 |
| Message Batch | ✅ | ✅ | 已实现 | 低 |
| PID Set | ✅ | ✅ | 完成 | - |
| Throttler | ✅ | ✅ | 已实现 | 低 |
| Bounded/Unbounded Mailbox | ✅ | ✅ | 完成 | - |
| Priority Queue | ✅ | ✅ | 完成 | - |

### 1.2 Router 功能

| 功能 | ProtoActor-Go | ProtoActor-Kotlin | 状态 | 优先级 |
|------|--------------|-------------------|------|--------|
| Broadcast Router | ✅ | ✅ | 完成 | - |
| Random Router | ✅ | ✅ | 完成 | - |
| RoundRobin Router | ✅ | ✅ | 完成 | - |
| Consistent Hash Router | ✅ | ✅ | 完成 | - |
| Pool Router | ✅ | ✅ | 完成 | - |
| Group Router | ✅ | ✅ | 完成 | - |
| Router Process | ✅ | ✅ | 完成 | - |

### 1.3 Remote 功能

| 功能 | ProtoActor-Go | ProtoActor-Kotlin | 状态 | 优先级 |
|------|--------------|-------------------|------|--------|
| Remote Actor 通信 | ✅ | ✅ | 完成 | - |
| Endpoint Manager | ✅ | ✅ | 完成 | - |
| Endpoint Reader | ✅ | ✅ | 完成 | - |
| Endpoint Writer | ✅ | ✅ | 完成 | - |
| Endpoint Watcher | ✅ | ✅ | 完成 | - |
| Serialization | ✅ | ✅ | 完成 | - |
| Activator | ✅ | ✅ | 完成 | - |
| Blocklist | ✅ | ✅ | 完成 | - |
| Response Status Code | ✅ | ✅ | 已实现 | 低 |

### 1.4 Cluster 功能

| 功能 | ProtoActor-Go | ProtoActor-Kotlin | 状态 | 优先级 |
|------|--------------|-------------------|------|--------|
| Cluster 基础功能 | ✅ | ✅ | 完成 | - |
| Member List | ✅ | ✅ | 完成 | - |
| Gossip 协议 | ✅ | ✅ | 完成 | - |
| Identity Lookup | ✅ | ✅ | 完成 | - |
| PID Cache | ✅ | ✅ | 完成 | - |
| Grain 支持 | ✅ | ✅ | 完成 | - |
| PubSub | ✅ | ✅ | 完成 | - |
| Consensus 机制 | ✅ | ✅ | 完成 | - |
| Informer | ✅ | ✅ | 完成 | - |
| PubSub Batch | ✅ | ✅ | 完成 | - |
| PubSub Extensions | ✅ | ✅ | 完成 | - |
| Rendezvous 哈希 | ✅ | ✅ | 已实现 | 低 |

### 1.5 Persistence 功能

| 功能 | ProtoActor-Go | ProtoActor-Kotlin | 状态 | 优先级 |
|------|--------------|-------------------|------|--------|
| 基础持久化功能 | ✅ | ✅ | 完成 | - |
| In-Memory Provider | ✅ | ✅ | 完成 | - |
| Protobuf Provider | ✅ | ✅ | 完成 | - |

### 1.6 其他功能

| 功能 | ProtoActor-Go | ProtoActor-Kotlin | 状态 | 优先级 |
|------|--------------|-------------------|------|--------|
| Scheduler/Timer | ✅ | ✅ | 完成 | - |
| EventStream | ✅ | ✅ | 完成 | - |
| Metrics | ✅ | ✅ | 已实现 | 高 |
| Stream 处理 | ✅ | ✅ | 已实现 | 中 |
| Native Image 支持 | ❌ | ✅ | 超前 | - |
| Context Extensions | ✅ | ✅ | 已实现 | 低 |
| Logging | ✅ | ✅ | 已实现 | 中 |

## 2. 代码质量与测试覆盖率

| 方面 | ProtoActor-Go | ProtoActor-Kotlin | 状态 | 优先级 |
|------|--------------|-------------------|------|--------|
| 单元测试覆盖率 | 高 | 中 | 需改进 | 高 |
| 集成测试 | ✅ | ❌ | 缺失 | 高 |
| 示例代码 | 丰富 | 有限 | 需改进 | 中 |
| 文档 | 中等 | 有限 | 需改进 | 高 |
| API 一致性 | 基准 | 部分不一致 | 需改进 | 中 |

## 3. 改进计划 (TODO List)

### 3.1 高优先级任务

1. **优化 Process Registry** (已完成)
   - 实现分片存储，减少锁竞争 (已实现)
   - 改进 ID 生成算法，使用更高效的 base64 编码 (已实现)
   - 将 ProcessRegistry 与 ActorSystem 关联，支持多实例 (已实现)
   - 改进错误处理机制，返回结果而非抛出异常 (已实现)
   - 实现进程移除时的死亡标记 (已实现)

2. **实现 Middleware 链** (已完成)
   - 添加 Middleware 接口和实现 (已实现)
   - 支持 Actor、Sender 和 Receiver Middleware (已实现)
   - 提供常用 Middleware 实现（日志、恢复、度量等） (已实现)

3. **实现 Metrics 系统** ✅
   - 添加核心度量接口 ✅
   - 实现 Actor 系统度量 ✅
   - 支持 Mailbox、Router 和 Remote 度量 ✅
   - 提供与 Prometheus 等系统的集成 ✅

4. **实现 Cluster Consensus 机制** ✅
   - 添加 Consensus 接口和基础实现 ✅
   - 实现 Consensus Check Builder ✅
   - 添加常用 Consensus Checks ✅

5. **提高测试覆盖率**
   - 为核心组件添加更多单元测试
   - 添加集成测试
   - 实现端到端测试

6. **改进文档**
   - 完善 API 文档
   - 添加架构文档
   - 提供更多使用示例

### 3.2 中优先级任务

1. **实现 Guardian Actor** ✅
   - 添加 Guardian Actor 实现 ✅
   - 支持 Root、System 和 User Guardian ✅

2. **实现 PID Set** ✅
   - 添加高效的 PID 集合实现 ✅
   - 支持常用集合操作 ✅

3. **实现 Priority Queue** ✅
   - 添加优先级队列实现 ✅
   - 支持优先级 Mailbox ✅

4. **实现 Remote Blocklist** ✅
   - 添加远程节点阻止列表功能 ✅
   - 支持自动阻止不可用节点 ✅

5. **实现 Cluster Informer** ✅
   - 添加 Cluster Informer 接口和实现 ✅
   - 支持集群状态变更通知 ✅

6. **实现 PubSub Batch 和 Extensions** ✅
   - 添加批处理消息支持 ✅
   - 实现 PubSub 扩展机制 ✅

7. **实现 Persistence Providers** ✅
   - 添加内存持久化提供程序 ✅
   - 添加 Protobuf 持久化提供程序 ✅

8. **实现 Consensus 机制** ✅
   - 添加 Consensus 接口和基础实现 ✅
   - 实现 Consensus Check Builder ✅
   - 添加常用 Consensus Checks ✅

9. **实现 Stream 处理** ✅
   - 添加类型化和非类型化流 ✅
   - 支持流处理操作 ✅

10. **实现 Logging 系统** ✅
    - 添加日志接口和实现 ✅
    - 支持可配置的日志级别和格式 ✅

### 3.3 低优先级任务

1. **实现 Message Batch** ✅
   - 添加消息批处理支持 ✅
   - 优化批量消息处理性能 ✅

2. **实现 Throttler** ✅
   - 添加消息节流功能 ✅
   - 支持可配置的节流策略 ✅

3. **实现 Response Status Code** ✅
   - 添加远程响应状态码 ✅
   - 支持错误处理和重试 ✅

4. **实现 Rendezvous 哈希** ✅
   - 添加 Rendezvous 哈希算法 ✅
   - 优化集群路由 ✅

5. **实现 Context Extensions** ✅
   - 添加上下文扩展机制 ✅
   - 支持自定义上下文功能 ✅

## 4. 实施时间表

### 第一阶段（1-3个月）
- 优化 Process Registry (已完成)
- 实现 Middleware 链
- 实现 Metrics 系统
- 提高测试覆盖率
- 改进文档

### 第二阶段（3-6个月）
- 实现 Cluster Consensus 机制
- 实现 Guardian Actor
- 实现 PID Set 和 Priority Queue
- 实现 Remote Blocklist
- 实现 Logging 系统 ✅

### 第三阶段（6-9个月）
- 实现 Cluster Informer
- 实现 PubSub Batch 和 Extensions
- 实现 Persistence Providers
- 实现 Stream 处理

### 第四阶段（9-12个月）
- 实现 Message Batch ✅
- 实现 Throttler ✅
- 实现 Response Status Code ✅
- 实现 Rendezvous 哈希 ✅
- 实现 Context Extensions ✅

## 5. Process Registry 详细分析与优化计划

### 5.1 当前实现的问题

ProtoActor-Kotlin 的 ProcessRegistry 实现存在以下问题：

1. **性能问题**
   - 使用单一的 ConcurrentHashMap 存储所有进程，在高并发情况下可能导致锁竞争
   - 而 ProtoActor-Go 使用分片技术（SliceMap），将进程分散到 1024 个桶中，减少了锁竞争

2. **ID 生成效率**
   - Kotlin 版本使用简单的递增整数生成 ID，格式为 `$1`, `$2` 等
   - Go 版本使用更高效的 base64 编码方式，生成更短、更高效的 ID

3. **架构问题**
   - Kotlin 版本使用单例模式（object）实现 ProcessRegistry，不支持多实例
   - Go 版本将 ProcessRegistry 与 ActorSystem 关联，支持多个 ActorSystem 实例

4. **错误处理**
   - Kotlin 版本在添加已存在的进程时抛出异常
   - Go 版本返回布尔值表示操作是否成功

5. **进程移除**
   - Kotlin 版本简单地从 Map 中移除进程
   - Go 版本在移除进程时，还会将 ActorProcess 标记为死亡（设置 dead 标志）

### 5.2 优化方案

#### 5.2.1 分片存储实现

```kotlin
class ShardedProcessMap(private val shardCount: Int = 1024) {
    private val shards = Array(shardCount) { ConcurrentHashMap<String, Process>() }

    private fun getShard(key: String): ConcurrentHashMap<String, Process> {
        val hash = MurmurHash3.hash32(key.toByteArray())
        val index = hash % shardCount
        return shards[index]
    }

    fun put(key: String, process: Process): Process? = getShard(key).put(key, process)

    fun get(key: String): Process? = getShard(key).get(key)

    fun remove(key: String): Process? = getShard(key).remove(key)

    fun keys(): Sequence<String> = shards.asSequence().flatMap { it.keys.asSequence() }
}
```

#### 5.2.2 高效 ID 生成

```kotlin
private val digits = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ~+"

fun uint64ToId(u: Long): String {
    val buf = CharArray(13)
    var i = 13
    var value = u

    // base is power of 2: use shifts and masks instead of / and %
    while (value >= 64) {
        i--
        buf[i] = digits[(value and 0x3f).toInt()]
        value = value shr 6
    }

    // u < base
    i--
    buf[i] = digits[value.toInt()]
    i--
    buf[i] = '$'

    return String(buf, i, 13 - i)
}
```

#### 5.2.3 与 ActorSystem 关联

```kotlin
class ProcessRegistry(val actorSystem: ActorSystem) {
    var address: String = noHost
    private val hostResolvers: MutableList<(PID) -> Process?> = mutableListOf()
    private val processLookup = ShardedProcessMap()
    private val sequenceId = AtomicLong(0)

    // 其他方法...
}
```

#### 5.2.4 改进错误处理

```kotlin
fun put(id: String, process: Process): Pair<PID, Boolean> {
    val pid = PID(address, id)
    pid.cachedProcess_ = process
    val success = processLookup.putIfAbsent(pid.id, process) == null
    return Pair(pid, success)
}
```

#### 5.2.5 进程移除时标记死亡

```kotlin
fun remove(pid: PID) {
    val process = processLookup.remove(pid.id)
    if (process is ActorProcess) {
        process.markAsDead()
    }
}
```

### 5.3 实现计划 (已完成)

1. 创建 ShardedProcessMap 类，实现分片存储 (已实现)
2. 实现高效的 ID 生成算法 (已实现)
3. 将 ProcessRegistry 改为类，与 ActorSystem 关联 (已实现)
4. 改进错误处理机制，返回结果而非抛出异常 (已实现)
5. 为 ActorProcess 添加 dead 标志，并在移除时设置 (已实现)
6. 更新相关的测试用例 (已实现)
7. 更新文档 (已实现)

## 6. 结论

ProtoActor-Kotlin 已经实现了与 ProtoActor-Go 大部分核心功能的兼容，但仍有一些重要功能需要实现和优化。通过执行上述改进计划，ProtoActor-Kotlin 将能够提供与 ProtoActor-Go 相当的功能，同时利用 Kotlin 语言的优势提供更好的开发体验。

特别是，优化 Process Registry、实现 Middleware 链、Metrics 系统和 Cluster Consensus 机制将显著提高 ProtoActor-Kotlin 的性能、功能性和可用性。同时，提高测试覆盖率和改进文档将确保代码质量和用户体验。

此外，ProtoActor-Kotlin 在 Native Image 支持方面已经超越了 ProtoActor-Go，这为在资源受限环境中部署 Actor 系统提供了重要优势。
