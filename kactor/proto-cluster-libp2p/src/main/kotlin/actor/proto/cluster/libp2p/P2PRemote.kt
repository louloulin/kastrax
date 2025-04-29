package actor.proto.cluster.libp2p

import actor.proto.ActorSystem
import actor.proto.MessageEnvelope
import actor.proto.MessageHeader
import actor.proto.PID
import actor.proto.cluster.Cluster
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.Deflater
import java.util.zip.Inflater

private val logger = KotlinLogging.logger {}

/**
 * P2PRemote 负责远程通信
 */
class P2PRemote(
    private val actorSystem: ActorSystem,
    private val cluster: Cluster
) {
    companion object {
        const val PROTOCOL_ID = "/protoactor/remote/1.0.0"

        // 批处理配置
        private const val DEFAULT_BATCH_SIZE = 100
        private const val DEFAULT_BATCH_INTERVAL_MS = 50L

        // 压缩配置
        private const val COMPRESSION_THRESHOLD = 1024 // 1KB以上的消息进行压缩
        private const val COMPRESSION_LEVEL = Deflater.BEST_SPEED

        // 消息头部键
        const val HEADER_REQUEST_ID = "requestId"
        const val HEADER_SENDER = "sender"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<Any>>()
    private val streamCache = ConcurrentHashMap<String, RemoteConnection>()
    private val streamCacheTTL = ConcurrentHashMap<String, Long>()

    // 消息批处理
    private val messageBatches = ConcurrentHashMap<String, MessageBatch>()
    private val batchSize = AtomicInteger(DEFAULT_BATCH_SIZE)
    private val batchIntervalMs = AtomicInteger(DEFAULT_BATCH_INTERVAL_MS.toInt())

    // 统计信息
    private val messagesSent = AtomicInteger(0)
    private val messagesReceived = AtomicInteger(0)
    private val bytesSent = AtomicInteger(0)
    private val bytesReceived = AtomicInteger(0)
    private val compressionRatio = AtomicInteger(0)

    /**
     * 启动远程通信服务
     */
    fun start() {
        // 启动批处理任务
        startBatchProcessingTask()

        // 启动定期清理缓存的任务
        startCacheCleanupTask()

        logger.info { "P2P remote started" }
    }

    /**
     * 停止远程通信服务
     */
    fun stop() {
        // 关闭所有缓存的连接
        streamCache.forEach { (_, connection) ->
            try {
                connection.close()
            } catch (e: Exception) {
                logger.error(e) { "Error closing connection" }
            }
        }
        streamCache.clear()
        streamCacheTTL.clear()

        // 取消所有未完成的请求
        pendingRequests.forEach { (_, deferred) ->
            if (!deferred.isCompleted) {
                deferred.completeExceptionally(Exception("Remote service shutting down"))
            }
        }
        pendingRequests.clear()

        logger.info { "P2P remote stopped" }
    }

    /**
     * 启动批处理任务
     */
    private fun startBatchProcessingTask() {
        scope.launch {
            while (true) {
                try {
                    // 处理所有批次
                    processAllBatches()

                    // 等待下一个批处理间隔
                    delay(batchIntervalMs.get().toLong())
                } catch (e: Exception) {
                    logger.error(e) { "Error processing message batches" }
                }
            }
        }
    }

    /**
     * 处理所有批次
     */
    private fun processAllBatches() {
        val now = System.currentTimeMillis()
        val expiredBatches = mutableListOf<String>()

        // 处理所有批次
        messageBatches.forEach { (target, batch) ->
            if (batch.messages.size >= batchSize.get() || now - batch.createdAt > batchIntervalMs.get()) {
                // 发送批次
                try {
                    sendBatch(target, batch)
                } catch (e: Exception) {
                    logger.error(e) { "Error sending batch to $target" }
                }
                expiredBatches.add(target)
            }
        }

        // 移除已处理的批次
        expiredBatches.forEach { messageBatches.remove(it) }
    }

    /**
     * 发送批次
     */
    private fun sendBatch(target: String, batch: MessageBatch) {
        if (batch.messages.isEmpty()) return

        try {
            // 序列化批次
            val serializedBatch = serializeBatch(batch)

            // 获取或创建连接
            val connection = getOrCreateConnection(target)

            // 发送批次
            connection.send(serializedBatch)

            // 更新统计信息
            messagesSent.addAndGet(batch.messages.size)
            bytesSent.addAndGet(serializedBatch.remaining())
        } catch (e: Exception) {
            logger.error(e) { "Error sending batch to $target" }

            // 重新入队消息
            batch.messages.forEach { message ->
                when (message) {
                    is RemoteMessage.Send -> {
                        scope.launch {
                            send(PID(target, message.target), message.envelope.message)
                        }
                    }
                    is RemoteMessage.Request -> {
                        val deferred = pendingRequests[message.requestId]
                        if (deferred != null && !deferred.isCompleted) {
                            deferred.completeExceptionally(e)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 序列化批次
     */
    private fun serializeBatch(batch: MessageBatch): ByteBuffer {
        // 计算总大小
        var totalSize = 4 // 消息数量 (4字节)

        // 计算每个消息的大小
        val messageSizes = batch.messages.map { message ->
            val serialized = serializeMessage(message)
            totalSize += 4 + serialized.remaining() // 消息大小 (4字节) + 消息内容
            serialized
        }

        // 创建缓冲区
        val buffer = ByteBuffer.allocate(totalSize)

        // 写入消息数量
        buffer.putInt(batch.messages.size)

        // 写入每个消息
        messageSizes.forEach { serialized ->
            buffer.putInt(serialized.remaining())
            buffer.put(serialized)
        }

        // 设置位置并返回
        buffer.flip()

        // 如果超过压缩阈值，进行压缩
        if (buffer.remaining() > COMPRESSION_THRESHOLD) {
            return compressBuffer(buffer)
        }

        return buffer
    }

    /**
     * 压缩缓冲区
     */
    private fun compressBuffer(buffer: ByteBuffer): ByteBuffer {
        val input = ByteArray(buffer.remaining())
        buffer.get(input)

        val deflater = Deflater(COMPRESSION_LEVEL)
        deflater.setInput(input)
        deflater.finish()

        val output = ByteArray(input.size)
        val compressedSize = deflater.deflate(output)
        deflater.end()

        // 如果压缩后更大，返回原始缓冲区
        if (compressedSize >= input.size) {
            buffer.flip()
            return buffer
        }

        // 更新压缩比
        val ratio = (input.size - compressedSize) * 100 / input.size
        compressionRatio.set(ratio)

        // 创建压缩后的缓冲区
        val compressedBuffer = ByteBuffer.allocate(compressedSize + 5)
        compressedBuffer.put(1.toByte()) // 压缩标志
        compressedBuffer.putInt(input.size) // 原始大小
        compressedBuffer.put(output, 0, compressedSize)
        compressedBuffer.flip()

        return compressedBuffer
    }

    /**
     * 解压缩缓冲区
     */
    private fun decompressBuffer(buffer: ByteBuffer): ByteBuffer {
        val compressed = buffer.get() == 1.toByte()

        if (!compressed) {
            buffer.position(buffer.position() - 1)
            return buffer
        }

        val originalSize = buffer.getInt()
        val input = ByteArray(buffer.remaining())
        buffer.get(input)

        val inflater = Inflater()
        inflater.setInput(input)

        val output = ByteArray(originalSize)
        val decompressedSize = inflater.inflate(output)
        inflater.end()

        return ByteBuffer.wrap(output, 0, decompressedSize)
    }

    /**
     * 启动定期清理缓存的任务
     */
    private fun startCacheCleanupTask() {
        scope.launch {
            while (true) {
                try {
                    // 清理过期的流缓存
                    cleanupStreamCache()

                    // 等待一段时间
                    delay(60000) // 每分钟清理一次
                } catch (e: Exception) {
                    logger.error(e) { "Error cleaning up stream cache" }
                }
            }
        }
    }

    /**
     * 清理过期的流缓存
     */
    private fun cleanupStreamCache() {
        val now = System.currentTimeMillis()
        val expiredKeys = mutableListOf<String>()

        // 找出过期的键
        streamCacheTTL.forEach { (key, timestamp) ->
            if (now - timestamp > 300000) { // 5分钟过期
                expiredKeys.add(key)
            }
        }

        // 关闭并移除过期的流
        expiredKeys.forEach { key ->
            val connection = streamCache.remove(key)
            streamCacheTTL.remove(key)

            if (connection != null) {
                try {
                    connection.close()
                } catch (e: Exception) {
                    logger.error(e) { "Error closing expired connection" }
                }
            }
        }

        if (expiredKeys.isNotEmpty()) {
            logger.debug { "Cleaned up ${expiredKeys.size} expired connections" }
        }
    }

    /**
     * 发送消息到远程 Actor
     */
    suspend fun send(pid: PID, message: Any) {
        try {
            // 创建消息信封
            val envelope = MessageEnvelope(message, null, null as MessageHeader?)

            // 创建远程消息
            val remoteMessage = RemoteMessage.Send(pid.id, envelope)

            // 添加到批处理队列
            addToBatch(pid.address, remoteMessage)
        } catch (e: Exception) {
            logger.error(e) { "Error sending message to ${pid.address}/${pid.id}" }
            throw e
        }
    }

    /**
     * 发送请求并等待响应
     */
    suspend fun request(pid: PID, message: Any): Any {
        try {
            // 创建请求 ID
            val requestId = UUID.randomUUID().toString()

            // 创建 CompletableDeferred 用于等待响应
            val deferred = CompletableDeferred<Any>()
            pendingRequests[requestId] = deferred

            // 创建消息信封
            val header = MessageHeader()
            header.set(HEADER_REQUEST_ID, requestId)
            header.set(HEADER_SENDER, actorSystem.address)
            val envelope = MessageEnvelope(message, null, header)

            // 创建远程消息
            val remoteMessage = RemoteMessage.Request(pid.id, envelope, requestId)

            // 添加到批处理队列
            addToBatch(pid.address, remoteMessage)

            // 等待响应
            return deferred.await()
        } catch (e: Exception) {
            logger.error(e) { "Error sending request to ${pid.address}/${pid.id}" }
            throw e
        }
    }

    /**
     * 添加到批处理队列
     */
    private fun addToBatch(target: String, message: RemoteMessage) {
        val batch = messageBatches.computeIfAbsent(target) {
            MessageBatch(System.currentTimeMillis())
        }
        batch.messages.add(message)
    }

    /**
     * 获取或创建连接
     */
    private fun getOrCreateConnection(target: String): RemoteConnection {
        // 检查缓存中是否有可用的连接
        val cachedConnection = streamCache[target]
        if (cachedConnection != null && cachedConnection.isActive()) {
            // 更新最后使用时间
            streamCacheTTL[target] = System.currentTimeMillis()
            return cachedConnection
        }

        // 创建新的连接
        val connection = createConnection(target)

        // 缓存连接
        streamCache[target] = connection
        streamCacheTTL[target] = System.currentTimeMillis()

        return connection
    }

    /**
     * 创建连接
     */
    private fun createConnection(target: String): RemoteConnection {
        // 在实际实现中，这里会使用 libp2p 创建连接
        // 这里使用模拟实现
        return object : RemoteConnection {
            private var active = true

            override fun isActive(): Boolean = active

            override fun send(data: ByteBuffer) {
                // 模拟发送数据
                logger.debug { "Sending ${data.remaining()} bytes to $target" }
            }

            override fun close() {
                active = false
                logger.debug { "Closed connection to $target" }
            }
        }
    }

    /**
     * 处理接收到的响应
     */
    private fun handleResponse(requestId: String, message: Any) {
        val deferred = pendingRequests.remove(requestId)
        deferred?.complete(message)
    }

    /**
     * 序列化消息
     */
    private fun serializeMessage(message: RemoteMessage): ByteBuffer {
        val buffer = ByteBuffer.allocate(1024) // 初始分配 1KB

        when (message) {
            is RemoteMessage.Send -> {
                // 消息类型: 1 字节
                buffer.put(1)

                // 目标长度: 2 字节
                val targetBytes = message.target.toByteArray(StandardCharsets.UTF_8)
                buffer.putShort(targetBytes.size.toShort())

                // 目标
                buffer.put(targetBytes)

                // 序列化消息内容
                val messageBytes = serializeMessageContent(message.envelope.message)

                // 消息长度: 4 字节
                buffer.putInt(messageBytes.size)

                // 消息内容
                buffer.put(messageBytes)
            }
            is RemoteMessage.Request -> {
                // 消息类型: 1 字节
                buffer.put(2)

                // 目标长度: 2 字节
                val targetBytes = message.target.toByteArray(StandardCharsets.UTF_8)
                buffer.putShort(targetBytes.size.toShort())

                // 目标
                buffer.put(targetBytes)

                // 请求 ID 长度: 2 字节
                val requestIdBytes = message.requestId.toByteArray(StandardCharsets.UTF_8)
                buffer.putShort(requestIdBytes.size.toShort())

                // 请求 ID
                buffer.put(requestIdBytes)

                // 发送者长度: 2 字节
                val senderBytes = (message.envelope.header?.get(HEADER_SENDER) ?: "").toByteArray(StandardCharsets.UTF_8)
                buffer.putShort(senderBytes.size.toShort())

                // 发送者
                buffer.put(senderBytes)

                // 序列化消息内容
                val messageBytes = serializeMessageContent(message.envelope.message)

                // 消息长度: 4 字节
                buffer.putInt(messageBytes.size)

                // 消息内容
                buffer.put(messageBytes)
            }
            is RemoteMessage.Response -> {
                // 消息类型: 1 字节
                buffer.put(3)

                // 请求 ID 长度: 2 字节
                val requestIdBytes = message.requestId.toByteArray(StandardCharsets.UTF_8)
                buffer.putShort(requestIdBytes.size.toShort())

                // 请求 ID
                buffer.put(requestIdBytes)

                // 序列化消息内容
                val messageBytes = serializeMessageContent(message.message)

                // 消息长度: 4 字节
                buffer.putInt(messageBytes.size)

                // 消息内容
                buffer.put(messageBytes)
            }
        }

        // 设置位置并返回
        buffer.flip()
        return buffer
    }

    /**
     * 序列化消息内容
     */
    private fun serializeMessageContent(message: Any): ByteArray {
        // 如果是字符串，直接转换
        if (message is String) {
            return message.toByteArray(StandardCharsets.UTF_8)
        }

        // 如果是原始类型，转换为字符串
        if (message is Number || message is Boolean) {
            return message.toString().toByteArray(StandardCharsets.UTF_8)
        }

        // 其他类型使用 JSON 序列化
        // 在实际实现中，应该使用更高效的序列化方式，如 Protocol Buffers
        val json = message.toString()
        return json.toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * 反序列化消息
     */
    private fun deserializeMessage(bytes: ByteArray): RemoteMessage {
        val buffer = ByteBuffer.wrap(bytes)

        // 读取消息类型
        val type = buffer.get()

        return when (type.toInt()) {
            1 -> { // Send
                // 读取目标
                val targetLength = buffer.getShort().toInt()
                val targetBytes = ByteArray(targetLength)
                buffer.get(targetBytes)
                val target = String(targetBytes, StandardCharsets.UTF_8)

                // 读取消息
                val messageLength = buffer.getInt()
                val messageBytes = ByteArray(messageLength)
                buffer.get(messageBytes)
                val message = deserializeMessageContent(messageBytes)

                RemoteMessage.Send(target, MessageEnvelope(message, null, null as MessageHeader?))
            }
            2 -> { // Request
                // 读取目标
                val targetLength = buffer.getShort().toInt()
                val targetBytes = ByteArray(targetLength)
                buffer.get(targetBytes)
                val target = String(targetBytes, StandardCharsets.UTF_8)

                // 读取请求 ID
                val requestIdLength = buffer.getShort().toInt()
                val requestIdBytes = ByteArray(requestIdLength)
                buffer.get(requestIdBytes)
                val requestId = String(requestIdBytes, StandardCharsets.UTF_8)

                // 读取发送者
                val senderLength = buffer.getShort().toInt()
                val senderBytes = ByteArray(senderLength)
                buffer.get(senderBytes)
                val sender = if (senderLength > 0) String(senderBytes, StandardCharsets.UTF_8) else null

                // 读取消息
                val messageLength = buffer.getInt()
                val messageBytes = ByteArray(messageLength)
                buffer.get(messageBytes)
                val message = deserializeMessageContent(messageBytes)

                val header = if (sender != null) {
                    val h = MessageHeader()
                    h.set(HEADER_REQUEST_ID, requestId)
                    h.set(HEADER_SENDER, sender)
                    h
                } else null
                RemoteMessage.Request(target, MessageEnvelope(message, null, header), requestId)
            }
            3 -> { // Response
                // 读取请求 ID
                val requestIdLength = buffer.getShort().toInt()
                val requestIdBytes = ByteArray(requestIdLength)
                buffer.get(requestIdBytes)
                val requestId = String(requestIdBytes, StandardCharsets.UTF_8)

                // 读取消息
                val messageLength = buffer.getInt()
                val messageBytes = ByteArray(messageLength)
                buffer.get(messageBytes)
                val message = deserializeMessageContent(messageBytes)

                RemoteMessage.Response(requestId, message)
            }
            else -> throw IllegalArgumentException("Unknown message type: $type")
        }
    }

    /**
     * 反序列化消息内容
     */
    private fun deserializeMessageContent(bytes: ByteArray): Any {
        // 尝试将字节数组转换为字符串
        val str = String(bytes, StandardCharsets.UTF_8)

        // 尝试解析为数字
        str.toIntOrNull()?.let { return it }
        str.toLongOrNull()?.let { return it }
        str.toDoubleOrNull()?.let { return it }

        // 尝试解析为布尔值
        if (str == "true") return true
        if (str == "false") return false

        // 其他情况下返回字符串
        return str
    }

    /**
     * 获取统计信息
     */
    fun getStats(): RemoteStats {
        return RemoteStats(
            messagesSent = messagesSent.get(),
            messagesReceived = messagesReceived.get(),
            bytesSent = bytesSent.get(),
            bytesReceived = bytesReceived.get(),
            compressionRatio = compressionRatio.get(),
            pendingRequests = pendingRequests.size,
            activeBatches = messageBatches.size,
            activeConnections = streamCache.size
        )
    }

    /**
     * 设置批处理大小
     */
    fun setBatchSize(size: Int) {
        require(size > 0) { "Batch size must be positive" }
        batchSize.set(size)
    }

    /**
     * 设置批处理间隔
     */
    fun setBatchInterval(intervalMs: Int) {
        require(intervalMs > 0) { "Batch interval must be positive" }
        batchIntervalMs.set(intervalMs)
    }
}

/**
 * 远程连接接口
 */
interface RemoteConnection {
    /**
     * 检查连接是否活跃
     */
    fun isActive(): Boolean

    /**
     * 发送数据
     */
    fun send(data: ByteBuffer)

    /**
     * 关闭连接
     */
    fun close()
}

/**
 * 消息批次
 */
data class MessageBatch(
    val createdAt: Long,
    val messages: ConcurrentLinkedQueue<RemoteMessage> = ConcurrentLinkedQueue()
)

/**
 * 远程消息
 */
sealed class RemoteMessage {
    /**
     * 发送消息
     */
    data class Send(val target: String, val envelope: MessageEnvelope) : RemoteMessage()

    /**
     * 请求消息
     */
    data class Request(val target: String, val envelope: MessageEnvelope, val requestId: String) : RemoteMessage()

    /**
     * 响应消息
     */
    data class Response(val requestId: String, val message: Any) : RemoteMessage()
}

/**
 * 远程统计信息
 */
data class RemoteStats(
    val messagesSent: Int,
    val messagesReceived: Int,
    val bytesSent: Int,
    val bytesReceived: Int,
    val compressionRatio: Int,
    val pendingRequests: Int,
    val activeBatches: Int,
    val activeConnections: Int
)
