package ai.kastrax.integrations.deepseek

/**
 * DeepSeek 模型枚举，包含所有可用的 DeepSeek 模型。
 */
enum class DeepSeekModel(val id: String) {
    // DeepSeek Chat 模型
    DEEPSEEK_CHAT("deepseek-chat"),

    // DeepSeek Coder 模型
    DEEPSEEK_CODER("deepseek-reasoner");
    // DeepSeek Math 模型

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
