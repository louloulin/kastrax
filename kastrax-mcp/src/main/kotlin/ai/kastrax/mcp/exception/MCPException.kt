package ai.kastrax.mcp.exception

import ai.kastrax.mcp.protocol.MCPErrorCodes

/**
 * MCP异常类，用于处理MCP操作中的错误
 */
open class MCPException : Exception {
    val code: Int
    val data: String?
    
    constructor(message: String) : super(message) {
        this.code = MCPErrorCodes.INTERNAL_ERROR
        this.data = null
    }
    
    constructor(code: Int, message: String, data: String? = null) : super(message) {
        this.code = code
        this.data = data
    }
    
    constructor(message: String, cause: Throwable) : super(message, cause) {
        this.code = MCPErrorCodes.INTERNAL_ERROR
        this.data = null
    }
    
    constructor(cause: Throwable) : super(cause) {
        this.code = MCPErrorCodes.INTERNAL_ERROR
        this.data = null
    }
}
