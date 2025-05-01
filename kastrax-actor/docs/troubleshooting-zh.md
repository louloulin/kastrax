# 故障排除指南

本文档提供了 kastrax-actor 模块中常见问题的故障排除指南，帮助您诊断和解决在使用过程中可能遇到的问题。

## 目录

- [常见错误](#常见错误)
  - [堆栈溢出错误](#堆栈溢出错误)
  - [ProcessNameExistException 错误](#processnameexistexception-错误)
  - [远程连接问题](#远程连接问题)
  - [消息传递超时](#消息传递超时)
- [性能问题](#性能问题)
  - [高延迟](#高延迟)
  - [内存泄漏](#内存泄漏)
- [调试技巧](#调试技巧)
  - [日志记录](#日志记录)
  - [监控工具](#监控工具)
- [最佳实践](#最佳实践)

## 常见错误

### 堆栈溢出错误

**症状**：
- 应用程序抛出 `StackOverflowError` 异常
- 日志中出现递归调用的痕迹
- 应用程序突然崩溃

**可能原因**：
1. Agent 之间的循环引用
2. 消息处理中的无限递归
3. 递归深度超过系统限制

**解决方案**：
1. **检查 Agent 引用关系**：
   ```kotlin
   // 避免这种循环引用
   val network = system.agentNetwork {
       coordinator {
           // 协调者配置
       }
       
       agent("agent1") {
           // agent1 引用 agent2
       }
       
       agent("agent2") {
           // agent2 引用 agent1
       }
   }
   ```

2. **增加最大递归深度**：
   ```kotlin
   // 在 KastraxActor.kt 中修改 MAX_RECURSION_DEPTH 的值
   companion object {
       /**
        * 最大递归深度，防止无限递归
        */
       private const val MAX_RECURSION_DEPTH = 10 // 增加到 10
   }
   ```

3. **添加详细日志**：
   ```kotlin
   // 添加详细日志，帮助诊断问题
   println("$LOG_PREFIX Actor ${self.id} processing message: ${msg::class.simpleName}, depth: ${metadata["depth"] ?: 0}")
   ```

4. **使用断路器模式**：
   ```kotlin
   // 实现断路器模式，防止无限递归
   if (circuitBreaker.isOpen()) {
       println("$LOG_PREFIX Circuit breaker is open, rejecting request")
       respond(AgentResponse("系统暂时无法处理请求，请稍后再试"))
       return
   }
   ```

### ProcessNameExistException 错误

**症状**：
- 应用程序抛出 `ProcessNameExistException` 异常
- 无法注册新的 Actor
- 测试失败，提示进程名称已存在

**可能原因**：
1. 尝试使用相同的名称注册多个 Actor
2. 测试环境中未正确清理之前的 Actor
3. 系统中存在名称冲突

**解决方案**：
1. **使用唯一名称**：
   ```kotlin
   // 使用唯一名称注册 Actor
   val uniqueName = "actor-${UUID.randomUUID()}"
   val pid = system.root.spawnNamed(props, uniqueName)
   ```

2. **在测试中使用随机名称**：
   ```kotlin
   // 在测试中使用随机名称
   @BeforeEach
   fun setup() {
       val uniqueId = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
       systemName = "test-system-$uniqueId"
       actorName = "test-actor-$uniqueId"
   }
   ```

3. **使用自动命名功能**：
   ```kotlin
   // 使用自动命名功能
   val pid = system.root.spawn(props) // 不指定名称，系统会自动生成
   ```

4. **在测试后清理资源**：
   ```kotlin
   // 在测试后清理资源
   @AfterEach
   fun cleanup() {
       system.shutdown()
   }
   ```

### 远程连接问题

**症状**：
- 无法连接到远程系统
- 连接超时
- 连接被拒绝

**可能原因**：
1. 网络连接问题
2. 端口被占用或被防火墙阻止
3. 远程系统未启动或配置错误

**解决方案**：
1. **检查网络连接**：
   ```bash
   # 检查网络连接
   ping <远程主机>
   telnet <远程主机> <端口>
   ```

2. **使用可用端口**：
   ```kotlin
   // 使用可用的随机端口
   fun findAvailablePort(): Int {
       val serverSocket = ServerSocket(0)
       val port = serverSocket.localPort
       serverSocket.close()
       return port
   }
   
   val port = findAvailablePort()
   val system = configureRemoteActorSystem(port)
   ```

3. **检查防火墙设置**：
   确保防火墙允许指定端口的通信。

4. **增加连接超时时间**：
   ```kotlin
   // 增加连接超时时间
   val config = RemoteActorConfig(
       hostname = "0.0.0.0",
       port = 8090,
       connectionTimeout = 30.seconds
   )
   ```

### 消息传递超时

**症状**：
- 消息发送后长时间无响应
- 请求超时异常
- 系统响应缓慢

**可能原因**：
1. 网络延迟
2. 远程系统负载过高
3. 消息处理时间过长

**解决方案**：
1. **设置合理的超时时间**：
   ```kotlin
   // 设置合理的超时时间
   val response = remoteAgent.askWithTimeout("remote-assistant", request, 60.seconds)
   ```

2. **实现重试机制**：
   ```kotlin
   // 实现重试机制
   val response = withRetry(maxRetries = 3) {
       remoteAgent.ask("remote-assistant", request)
   }
   ```

3. **使用异步处理**：
   ```kotlin
   // 使用异步处理
   val deferred = async {
       remoteAgent.ask("remote-assistant", request)
   }
   
   // 设置超时
   withTimeout(30.seconds) {
       val response = deferred.await()
       // 处理响应
   }
   ```

4. **监控系统负载**：
   定期监控系统负载，确保系统资源充足。

## 性能问题

### 高延迟

**症状**：
- 系统响应缓慢
- 消息处理时间长
- 用户体验差

**可能原因**：
1. 网络延迟
2. 系统资源不足
3. 消息处理逻辑复杂

**解决方案**：
1. **优化网络配置**：
   - 使用更快的网络连接
   - 减少网络跳数
   - 使用 CDN 或边缘计算

2. **增加系统资源**：
   - 增加 CPU 和内存
   - 使用更高性能的硬件
   - 优化系统配置

3. **优化消息处理逻辑**：
   ```kotlin
   // 优化消息处理逻辑
   override suspend fun Context.receive(msg: Any) {
       when (msg) {
           is AgentRequest -> {
               // 使用更高效的算法
               // 避免不必要的计算
               // 使用缓存
           }
           // 其他消息类型
       }
   }
   ```

4. **使用缓存**：
   ```kotlin
   // 使用缓存
   val cache = ConcurrentHashMap<String, AgentResponse>()
   
   // 检查缓存
   val cacheKey = "${msg.prompt}-${msg.options}"
   val cachedResponse = cache[cacheKey]
   if (cachedResponse != null) {
       respond(cachedResponse)
       return
   }
   
   // 处理请求并缓存结果
   val response = processRequest(msg)
   cache[cacheKey] = response
   respond(response)
   ```

### 内存泄漏

**症状**：
- 系统内存使用量持续增加
- 系统性能随时间推移而下降
- 最终导致 OutOfMemoryError

**可能原因**：
1. 未释放的资源
2. 长时间运行的协程
3. 大量未处理的消息积压

**解决方案**：
1. **及时释放资源**：
   ```kotlin
   // 使用 use 函数自动关闭资源
   someResource.use { resource ->
       // 使用资源
   }
   ```

2. **取消长时间运行的协程**：
   ```kotlin
   // 使用超时取消长时间运行的协程
   withTimeout(30.seconds) {
       // 长时间运行的操作
   }
   ```

3. **限制邮箱大小**：
   ```kotlin
   // 使用有界邮箱限制消息数量
   actor {
       boundedMailbox(capacity = 1000)
   }
   ```

4. **定期清理缓存**：
   ```kotlin
   // 定期清理缓存
   val cache = ExpiringCache<String, AgentResponse>(
       maxSize = 1000,
       expirationTime = 1.hours
   )
   ```

## 调试技巧

### 日志记录

有效的日志记录是诊断问题的关键：

1. **添加详细日志**：
   ```kotlin
   // 添加详细日志
   println("$LOG_PREFIX Actor ${self.id} received message: ${msg::class.simpleName} from ${sender?.id ?: "null"}")
   ```

2. **使用结构化日志**：
   ```kotlin
   // 使用结构化日志
   logger.info {
       "Actor ${self.id} received message: ${msg::class.simpleName}" +
       " from ${sender?.id ?: "null"}" +
       " with metadata: ${metadata}"
   }
   ```

3. **记录关键事件**：
   ```kotlin
   // 记录关键事件
   logger.debug { "Starting message processing" }
   val result = processMessage(msg)
   logger.debug { "Finished message processing, result: $result" }
   ```

4. **记录异常**：
   ```kotlin
   // 记录异常
   try {
       // 可能抛出异常的代码
   } catch (e: Exception) {
       logger.error(e) { "Error processing message: ${e.message}" }
       throw e
   }
   ```

### 监控工具

使用监控工具可以帮助您实时了解系统状态：

1. **JVM 监控**：
   - 使用 VisualVM、JConsole 等工具监控 JVM
   - 关注内存使用、GC 活动、线程状态等

2. **系统监控**：
   - 使用 Prometheus、Grafana 等工具监控系统
   - 关注 CPU 使用率、内存使用、网络流量等

3. **日志分析**：
   - 使用 ELK Stack (Elasticsearch, Logstash, Kibana) 分析日志
   - 设置告警，及时发现异常

4. **分布式追踪**：
   - 使用 Zipkin、Jaeger 等工具进行分布式追踪
   - 分析请求流程和性能瓶颈

## 最佳实践

1. **合理设计 Actor 层次结构**：
   - 避免过深的 Actor 层次结构
   - 明确 Actor 职责，避免职责重叠

2. **优化消息传递**：
   - 减少不必要的消息传递
   - 使用批量处理减少消息数量
   - 优化消息大小

3. **实现监督策略**：
   ```kotlin
   // 实现监督策略
   actor {
       oneForOneStrategy {
           maxRetries = 3
           withinTimeRange = 1.minutes
           decider { exception ->
               when (exception) {
                   is IllegalArgumentException -> SupervisorDirective.RESUME
                   is IOException -> SupervisorDirective.RESTART
                   else -> SupervisorDirective.STOP
               }
           }
       }
   }
   ```

4. **定期测试和监控**：
   - 编写全面的单元测试和集成测试
   - 定期进行压力测试和性能测试
   - 持续监控系统状态

5. **文档和注释**：
   - 为代码添加详细的注释
   - 维护最新的文档
   - 记录已知问题和解决方案

通过遵循这些最佳实践和故障排除指南，您可以更有效地诊断和解决 kastrax-actor 模块中的问题，提高系统的稳定性和可靠性。
