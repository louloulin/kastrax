package ai.kastrax.integrations.gemini

/**
 * Gemini 模型枚举，包含所有可用的 Google Gemini 模型。
 */
enum class GeminiModel(val id: String) {
    // Gemini 1.5 系列模型
    GEMINI_1_5_PRO("gemini-1.5-pro"),
    GEMINI_1_5_FLASH("gemini-1.5-flash"),
    
    // Gemini 1.0 系列模型
    GEMINI_1_0_PRO("gemini-1.0-pro"),
    GEMINI_1_0_PRO_VISION("gemini-1.0-pro-vision"),
    GEMINI_1_0_ULTRA("gemini-1.0-ultra");

    companion object {
        /**
         * 根据模型 ID 获取模型枚举。
         * 如果找不到匹配的预定义模型，则返回 GEMINI_1_5_PRO。
         */
        fun fromId(id: String): GeminiModel {
            return entries.find { it.id == id } ?: GEMINI_1_5_PRO
        }
    }
}
