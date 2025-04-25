package ai.kastrax.mcp.transport

import ai.kastrax.mcp.protocol.MCPMessage
import ai.kastrax.mcp.protocol.MCPRequest
import ai.kastrax.mcp.protocol.MCPResponse
import ai.kastrax.mcp.protocol.MCPNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 基于标准输入/输出的传输层
 *
 * @param command 要执行的命令
 * @param args 命令参数
 * @param env 环境变量
 */
class StdioTransport(
    private val command: String,
    private val args: List<String> = emptyList(),
    private val env: Map<String, String> = emptyMap()
) : Transport {
    private val json = Json { ignoreUnknownKeys = true }
    private val isConnected = AtomicBoolean(false)
    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    override suspend fun connect() {
        if (isConnected.get()) {
            return
        }

        withContext(Dispatchers.IO) {
            val processBuilder = ProcessBuilder(listOf(command) + args)
            processBuilder.environment().putAll(env)

            process = processBuilder.start()
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))

            isConnected.set(true)
        }
    }

    override suspend fun disconnect() {
        if (!isConnected.get()) {
            return
        }

        withContext(Dispatchers.IO) {
            writer?.close()
            reader?.close()
            process?.destroy()

            writer = null
            reader = null
            process = null

            isConnected.set(false)
        }
    }

    override suspend fun send(message: MCPMessage) {
        if (!isConnected.get()) {
            throw IllegalStateException("Not connected")
        }

        withContext(Dispatchers.IO) {
            val messageJson = when (message) {
                is MCPRequest -> json.encodeToString(MCPRequest.serializer(), message)
                is MCPResponse -> json.encodeToString(MCPResponse.serializer(), message)
                is MCPNotification -> json.encodeToString(MCPNotification.serializer(), message)
                else -> throw IllegalArgumentException("Unknown message type: ${message::class.java.name}")
            }

            writer?.write(messageJson)
            writer?.newLine()
            writer?.flush()
        }
    }

    override fun receive(): Flow<MCPMessage> = flow {
        if (!isConnected.get()) {
            throw IllegalStateException("Not connected")
        }

        while (isConnected.get()) {
            val line = withContext(Dispatchers.IO) {
                reader?.readLine()
            } ?: break

            if (line.isBlank()) {
                continue
            }

            try {
                // 尝试解析为请求
                val request = json.decodeFromString<MCPRequest>(line)
                emit(request)
                continue
            } catch (e: Exception) {
                // 不是请求，继续尝试其他类型
            }

            try {
                // 尝试解析为响应
                val response = json.decodeFromString<MCPResponse>(line)
                emit(response)
                continue
            } catch (e: Exception) {
                // 不是响应，继续尝试其他类型
            }

            try {
                // 尝试解析为通知
                val notification = json.decodeFromString<MCPNotification>(line)
                emit(notification)
                continue
            } catch (e: Exception) {
                // 不是通知，忽略
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun isConnected(): Boolean {
        return isConnected.get()
    }
}
