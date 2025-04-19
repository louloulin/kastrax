package ai.kastrax.integrations.deepseek

/**
 * DeepSeek 模型枚举，包含所有可用的 DeepSeek 模型。
 */
enum class DeepSeekModel(val id: String) {
    // DeepSeek Chat 模型
    DEEPSEEK_CHAT("deepseek-chat"),
    DEEPSEEK_CHAT_V1("deepseek-chat-v1"),
    DEEPSEEK_CHAT_V1_5("deepseek-chat-v1.5"),

    // DeepSeek Coder 模型
    DEEPSEEK_CODER("deepseek-coder"),
    DEEPSEEK_CODER_V1("deepseek-coder-v1"),
    DEEPSEEK_CODER_V1_5("deepseek-coder-v1.5"),

    // DeepSeek Math 模型
    DEEPSEEK_MATH("deepseek-math"),

    // DeepSeek Lite 模型
    DEEPSEEK_LITE("deepseek-lite"),
    DEEPSEEK_LITE_V1("deepseek-lite-v1"),
    DEEPSEEK_LITE_V1_5("deepseek-lite-v1.5");

    companion object {
        /**
         * 根据模型 ID 获取模型枚举。
         * 如果找不到匹配的预定义模型，则返回 DEEPSEEK_CHAT。
         */
        fun fromId(id: String): DeepSeekModel {
            return entries.find { it.id == id } ?: DEEPSEEK_CHAT
        }
    }
}
