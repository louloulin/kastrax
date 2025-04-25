package ai.kastrax.mcp.exception

import ai.kastrax.mcp.protocol.MCPErrorCodes

/**
 * 工具未找到异常
 */
class ToolNotFoundException(toolId: String) : Exception("Tool not found: $toolId") {
    val code: Int = MCPErrorCodes.TOOL_NOT_FOUND
    val data: String? = null
}
