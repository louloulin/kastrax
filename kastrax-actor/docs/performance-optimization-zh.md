# kastrax-actor 性能优化指南

本文档提供了 kastrax-actor 模块的性能优化指南，帮助您优化 Agent 网络的性能和资源使用。

## 目录

- [性能优化概述](#性能优化概述)
- [消息处理优化](#消息处理优化)
- [内存使用优化](#内存使用优化)
- [网络通信优化](#网络通信优化)
- [并发和并行处理](#并发和并行处理)
- [缓存策略](#缓存策略)
- [监控和分析](#监控和分析)
- [最佳实践](#最佳实践)

## 性能优化概述

kastrax-actor 模块的性能优化主要涉及以下几个方面：

1. **消息处理优化**：提高消息处理的效率和吞吐量
2. **内存使用优化**：减少内存占用，避免内存泄漏
3. **网络通信优化**：减少网络延迟和带宽使用
4. **并发和并行处理**：充分利用多核处理器
5. **缓存策略**：合理使用缓存提高响应速度

## 消息处理优化

### 邮箱配置

选择合适的邮箱类型和容量，可以显著影响消息处理性能：

```kotlin
// 使用无界邮箱（适用于消息量不大的场景）
actor {
    unboundedMailbox()
}

// 使用有界邮箱（适用于高负载场景，防止内存溢出）
actor {
    boundedMailbox(capacity = 1000)
}

// 使用优先级邮箱（适用于需要优先处理某些消息的场景）
actor {
    priorityMailbox { message ->
        when (message) {
            is HighPriorityMessage -> 0
            is MediumPriorityMessage -> 1
            else -> 2
        }
    }
}
```

### 消息批处理

对于大量小消息，可以使用批处理提高处理效率：

```kotlin
// 消息批处理
val batch = mutableListOf<AgentRequest>()
for (request in requests) {
    batch.add(request)
    if (batch.size >= BATCH_SIZE) {
        processBatch(batch)
        batch.clear()
    }
}
if (batch.isNotEmpty()) {
    processBatch(batch)
}
```

### 消息过滤

过滤不必要的消息，减少处理负担：

```kotlin
// 消息过滤
override suspend fun Context.receive(msg: Any) {
    // 过滤不需要处理的消息
    if (shouldIgnore(msg)) {
        return
    }
    
    // 处理消息
    // ...
}

private fun shouldIgnore(msg: Any): Boolean {
    return when (msg) {
        is HeartbeatMessage -> true // 忽略心跳消息
        is DebugMessage -> !isDebugMode // 非调试模式忽略调试消息
        else -> false
    }
}
```

## 内存使用优化

### 对象池

对于频繁创建和销毁的对象，使用对象池可以减少 GC 压力：

```kotlin
// 对象池实现
class MessagePool<T>(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
    private val initialSize: Int = 10
) {
    private val pool = ConcurrentLinkedQueue<T>()
    
    init {
        repeat(initialSize) {
            pool.add(factory())
        }
    }
    
    fun borrow(): T {
        return pool.poll() ?: factory()
    }
    
    fun recycle(obj: T) {
        reset(obj)
        pool.add(obj)
    }
}

// 使用对象池
val messagePool = MessagePool(
    factory = { AgentRequest("", emptyMap()) },
    reset = { it.prompt = ""; it.options.clear() }
)

// 借用对象
val request = messagePool.borrow()
request.prompt = "Hello"
request.options["depth"] = 0

// 使用完毕后回收
messagePool.recycle(request)
```

### 内存限制

设置合理的内存限制，防止内存溢出：

```kotlin
// 设置最大缓存大小
val maxCacheSize = 1000
val cache = LinkedHashMap<String, AgentResponse>(16, 0.75f, true)

// 添加缓存项时检查大小
fun addToCache(key: String, value: AgentResponse) {
    cache[key] = value
    if (cache.size > maxCacheSize) {
        // 移除最久未使用的项
        val oldestKey = cache.keys.first()
        cache.remove(oldestKey)
    }
}
```

### 避免闭包捕获

避免在闭包中捕获大对象，防止内存泄漏：

```kotlin
// 不好的做法：捕获整个 agent 对象
val agent = createLargeAgent()
val handler = { request: AgentRequest ->
    agent.process(request) // 捕获整个 agent 对象
}

// 好的做法：只捕获必要的引用
val agentId = agent.id
val handler = { request: AgentRequest ->
    val agent = getAgentById(agentId) // 通过 ID 获取 agent
    agent.process(request)
}
```

## 网络通信优化

### 消息压缩

对于大型消息，使用压缩可以减少网络传输量：

```kotlin
// 消息压缩
fun compressMessage(message: ByteArray): ByteArray {
    val baos = ByteArrayOutputStream()
    val gzos = GZIPOutputStream(baos)
    gzos.write(message)
    gzos.close()
    return baos.toByteArray()
}

// 消息解压
fun decompressMessage(compressed: ByteArray): ByteArray {
    val bais = ByteArrayInputStream(compressed)
    val gzis = GZIPInputStream(bais)
    return gzis.readBytes()
}
```

### 连接池

使用连接池管理远程连接，避免频繁创建和关闭连接：

```kotlin
// 连接池配置
val connectionPool = ConnectionPool(
    maxIdleConnections = 10,
    keepAliveDuration = 5, // 分钟
    timeUnit = TimeUnit.MINUTES
)

// 使用连接池创建客户端
val client = OkHttpClient.Builder()
    .connectionPool(connectionPool)
    .build()
```

### 批量请求

将多个请求合并为一个批量请求，减少网络往返：

```kotlin
// 批量请求
val batchRequest = BatchRequest(requests = listOf(
    AgentRequest("任务1", emptyMap()),
    AgentRequest("任务2", emptyMap()),
    AgentRequest("任务3", emptyMap())
))

// 发送批量请求
val batchResponse = remoteAgent.askBatch("batch-processor", batchRequest)

// 处理批量响应
for (response in batchResponse.responses) {
    println("响应: ${response.text}")
}
```

## 并发和并行处理

### 并行处理

利用协程进行并行处理，提高吞吐量：

```kotlin
// 并行处理多个请求
val results = requests.map { request ->
    async {
        processRequest(request)
    }
}.awaitAll()
```

### 工作池

使用工作池限制并发数量，避免资源耗尽：

```kotlin
// 创建工作池
val dispatcher = Dispatchers.IO.limitedParallelism(10) // 最多 10 个并发

// 使用工作池处理请求
val results = withContext(dispatcher) {
    requests.map { request ->
        async {
            processRequest(request)
        }
    }.awaitAll()
}
```

### 分片处理

对大型数据集进行分片处理，减少单个任务的负担：

```kotlin
// 分片处理
fun processLargeDataset(dataset: List<Data>, shardSize: Int = 100) {
    dataset.chunked(shardSize).forEach { shard ->
        launch {
            processShard(shard)
        }
    }
}
```

## 缓存策略

### 结果缓存

缓存计算结果，避免重复计算：

```kotlin
// 简单的内存缓存
val cache = ConcurrentHashMap<String, AgentResponse>()

// 使用缓存
fun getCachedResponse(request: AgentRequest): AgentResponse? {
    val cacheKey = "${request.prompt}-${request.options}"
    return cache[cacheKey]
}

fun cacheResponse(request: AgentRequest, response: AgentResponse) {
    val cacheKey = "${request.prompt}-${request.options}"
    cache[cacheKey] = response
}

// 使用缓存处理请求
fun processRequestWithCache(request: AgentRequest): AgentResponse {
    // 检查缓存
    getCachedResponse(request)?.let { return it }
    
    // 缓存未命中，处理请求
    val response = processRequest(request)
    
    // 缓存结果
    cacheResponse(request, response)
    
    return response
}
```

### 过期缓存

为缓存项设置过期时间，确保数据的新鲜度：

```kotlin
// 带过期时间的缓存项
data class CacheEntry<T>(
    val value: T,
    val expirationTime: Long
)

// 带过期时间的缓存
class ExpiringCache<K, V>(private val ttlMillis: Long) {
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    
    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        val now = System.currentTimeMillis()
        
        // 检查是否过期
        if (now > entry.expirationTime) {
            cache.remove(key)
            return null
        }
        
        return entry.value
    }
    
    fun put(key: K, value: V) {
        val expirationTime = System.currentTimeMillis() + ttlMillis
        cache[key] = CacheEntry(value, expirationTime)
    }
    
    fun clear() {
        cache.clear()
    }
    
    // 清理过期项
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        cache.entries.removeIf { (_, entry) -> now > entry.expirationTime }
    }
}

// 使用带过期时间的缓存
val responseCache = ExpiringCache<String, AgentResponse>(ttlMillis = 60 * 60 * 1000) // 1小时过期
```

### 分层缓存

实现分层缓存，平衡性能和内存使用：

```kotlin
// 分层缓存
class TieredCache<K, V> {
    // 一级缓存：内存中，访问速度快，容量小
    private val level1Cache = ConcurrentHashMap<K, V>()
    
    // 二级缓存：磁盘上，访问速度慢，容量大
    private val level2Cache = DiskCache<K, V>()
    
    fun get(key: K): V? {
        // 先查一级缓存
        level1Cache[key]?.let { return it }
        
        // 再查二级缓存
        level2Cache.get(key)?.let {
            // 提升到一级缓存
            level1Cache[key] = it
            return it
        }
        
        return null
    }
    
    fun put(key: K, value: V) {
        // 同时更新两级缓存
        level1Cache[key] = value
        level2Cache.put(key, value)
    }
}
```

## 监控和分析

### 性能指标收集

收集关键性能指标，帮助识别瓶颈：

```kotlin
// 性能指标收集
class PerformanceMetrics {
    private val messageProcessingTimes = ConcurrentLinkedQueue<Long>()
    private val memoryUsage = ConcurrentLinkedQueue<Long>()
    
    fun recordMessageProcessingTime(timeMillis: Long) {
        messageProcessingTimes.add(timeMillis)
        // 保持队列大小在合理范围内
        while (messageProcessingTimes.size > 1000) {
            messageProcessingTimes.poll()
        }
    }
    
    fun recordMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        memoryUsage.add(usedMemory)
        // 保持队列大小在合理范围内
        while (memoryUsage.size > 1000) {
            memoryUsage.poll()
        }
    }
    
    fun getAverageProcessingTime(): Double {
        return messageProcessingTimes.average()
    }
    
    fun getAverageMemoryUsage(): Double {
        return memoryUsage.average()
    }
}
```

### 性能分析

使用性能分析工具识别热点代码：

```kotlin
// 简单的性能分析工具
class SimpleProfiler {
    private val timings = mutableMapOf<String, MutableList<Long>>()
    
    fun profile(name: String, block: () -> Unit) {
        val start = System.nanoTime()
        try {
            block()
        } finally {
            val end = System.nanoTime()
            val duration = end - start
            timings.getOrPut(name) { mutableListOf() }.add(duration)
        }
    }
    
    fun getReport(): Map<String, ProfileReport> {
        return timings.mapValues { (_, durations) ->
            ProfileReport(
                count = durations.size,
                totalTimeNanos = durations.sum(),
                averageTimeNanos = durations.average(),
                minTimeNanos = durations.minOrNull() ?: 0,
                maxTimeNanos = durations.maxOrNull() ?: 0
            )
        }
    }
    
    fun printReport() {
        val report = getReport().toList().sortedByDescending { it.second.totalTimeNanos }
        println("性能分析报告:")
        println("-----------------------------")
        println("操作名称\t\t调用次数\t\t总时间(ms)\t\t平均时间(ms)\t\t最小时间(ms)\t\t最大时间(ms)")
        for ((name, stats) in report) {
            println("$name\t\t${stats.count}\t\t${stats.totalTimeNanos / 1_000_000}\t\t${stats.averageTimeNanos / 1_000_000}\t\t${stats.minTimeNanos / 1_000_000}\t\t${stats.maxTimeNanos / 1_000_000}")
        }
        println("-----------------------------")
    }
    
    data class ProfileReport(
        val count: Int,
        val totalTimeNanos: Long,
        val averageTimeNanos: Double,
        val minTimeNanos: Long,
        val maxTimeNanos: Long
    )
}

// 使用性能分析工具
val profiler = SimpleProfiler()

// 分析消息处理性能
profiler.profile("processMessage") {
    processMessage(message)
}

// 打印性能报告
profiler.printReport()
```

## 最佳实践

### 消息处理

1. **使用合适的邮箱类型**：根据消息量和处理需求选择合适的邮箱类型
2. **批处理小消息**：将多个小消息合并处理，减少开销
3. **过滤不必要的消息**：尽早过滤不需要处理的消息

### 内存管理

1. **使用对象池**：对于频繁创建和销毁的对象，使用对象池减少 GC 压力
2. **设置内存限制**：为缓存和集合设置合理的大小限制
3. **避免内存泄漏**：注意闭包捕获和长生命周期引用

### 网络通信

1. **压缩大型消息**：对大型消息进行压缩，减少网络传输量
2. **使用连接池**：管理远程连接，避免频繁创建和关闭连接
3. **批量请求**：将多个请求合并为一个批量请求，减少网络往返

### 并发处理

1. **合理使用并行**：利用协程进行并行处理，但避免过度并行
2. **使用工作池**：限制并发数量，避免资源耗尽
3. **分片处理大数据**：对大型数据集进行分片处理

### 缓存策略

1. **缓存计算结果**：避免重复计算
2. **设置过期时间**：确保数据的新鲜度
3. **实现分层缓存**：平衡性能和内存使用

### 监控和分析

1. **收集性能指标**：帮助识别瓶颈
2. **使用性能分析工具**：识别热点代码
3. **定期审查性能**：持续优化系统性能

通过遵循这些最佳实践和优化技巧，您可以显著提高 kastrax-actor 模块的性能和资源使用效率，为您的 Agent 网络提供更好的用户体验。
