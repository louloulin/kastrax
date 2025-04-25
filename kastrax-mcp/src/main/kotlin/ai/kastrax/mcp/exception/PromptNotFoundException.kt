package ai.kastrax.mcp.exception

import ai.kastrax.mcp.protocol.MCPErrorCodes

/**
 * 提示未找到异常
 */
class PromptNotFoundException(promptId: String) : Exception("Prompt not found: $promptId") {
    val code: Int = MCPErrorCodes.PROMPT_NOT_FOUND
    val data: String? = null
}
