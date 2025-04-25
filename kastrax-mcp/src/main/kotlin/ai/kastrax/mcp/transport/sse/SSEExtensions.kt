package ai.kastrax.mcp.transport.sse

import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * 将应用程序调用转换为SSE会话
 */
suspend fun ApplicationCall.respondSSE(): SSESession {
    // 设置响应头
    response.header("Content-Type", "text/event-stream")
    response.header("Cache-Control", "no-cache")
    response.header("Connection", "keep-alive")

    return object : SSESession {
        override suspend fun receive(): String? {
            // Ktor的SSE实现不支持接收
            return null
        }

        override suspend fun close() {
            // 关闭连接
            response.pipeline.complete()
        }

        override suspend fun send(event: String, data: String) {
            // 发送SSE事件
            val eventString = buildString {
                append("event: ").append(event).append("\n")
                append("data: ").append(data).append("\n\n")
            }
            respondTextWriter {
                write(eventString)
                flush()
            }
        }
    }

/**
 * 扩展函数，用于发送SSE事件
 */
suspend fun SSESession.send(event: String? = null, data: String) {
    if (event != null) {
        send(event, data)
    } else {
        send("message", data)
    }
}
