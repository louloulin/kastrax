package ai.kastrax.mcp.transport

import ai.kastrax.mcp.protocol.MCPMessage
import kotlinx.coroutines.flow.Flow

/**
 * MCP 传输层接口，用于在客户端和服务器之间传输消息。
 */
interface Transport {
    /**
     * 连接到远程端点
     */
    suspend fun connect()
    
    /**
     * 断开与远程端点的连接
     */
    suspend fun disconnect()
    
    /**
     * 发送消息
     * 
     * @param message 要发送的消息
     */
    suspend fun send(message: MCPMessage)
    
    /**
     * 接收消息流
     * 
     * @return 消息流
     */
    fun receive(): Flow<MCPMessage>
    
    /**
     * 检查连接是否活跃
     * 
     * @return 连接是否活跃
     */
    fun isConnected(): Boolean
}
