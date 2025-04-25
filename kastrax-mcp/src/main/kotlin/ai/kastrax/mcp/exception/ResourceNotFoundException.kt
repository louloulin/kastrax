package ai.kastrax.mcp.exception

import ai.kastrax.mcp.protocol.MCPErrorCodes

/**
 * 资源未找到异常
 */
class ResourceNotFoundException(resourceId: String) : Exception("Resource not found: $resourceId") {
    val code: Int = MCPErrorCodes.RESOURCE_NOT_FOUND
    val data: String? = null
}
