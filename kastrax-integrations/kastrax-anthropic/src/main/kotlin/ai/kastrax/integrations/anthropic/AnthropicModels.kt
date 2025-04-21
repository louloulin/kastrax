package ai.kastrax.integrations.anthropic

/**
 * Anthropic 模型枚举，包含所有可用的 Anthropic Claude 模型。
 */
enum class AnthropicModel(val id: String) {
    // Claude 3 系列模型
    CLAUDE_3_OPUS("claude-3-opus-20240229"),
    CLAUDE_3_SONNET("claude-3-sonnet-20240229"),
    CLAUDE_3_HAIKU("claude-3-haiku-20240307"),
    
    // Claude 2 系列模型
    CLAUDE_2("claude-2.1"),
    CLAUDE_2_0("claude-2.0"),
    
    // Claude 1 系列模型
    CLAUDE_1("claude-1"),
    CLAUDE_1_3("claude-1.3"),
    CLAUDE_1_2("claude-1.2"),
    CLAUDE_1_0("claude-1.0"),
    
    // Claude Instant 系列模型
    CLAUDE_INSTANT("claude-instant-1.2"),
    CLAUDE_INSTANT_1_1("claude-instant-1.1"),
    CLAUDE_INSTANT_1_0("claude-instant-1.0");

    companion object {
        /**
         * 根据模型 ID 获取模型枚举。
         * 如果找不到匹配的预定义模型，则返回 CLAUDE_3_SONNET。
         */
        fun fromId(id: String): AnthropicModel {
            return entries.find { it.id == id } ?: CLAUDE_3_SONNET
        }
    }
}
