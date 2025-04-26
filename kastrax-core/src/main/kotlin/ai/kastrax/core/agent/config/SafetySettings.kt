package ai.kastrax.core.agent.config

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 扩展函数，将 SafetySettings 转换为 JsonElement
 */
fun SafetySettings.toJsonElement(): JsonElement = this.toJsonElement()

/**
 * 安全设置配置，控制模型生成内容的安全过滤。
 */
class SafetySettings {
    private val settings = mutableListOf<SafetyCategory>()

    /**
     * 添加一个安全类别设置。
     *
     * @param category 安全类别
     * @param threshold 阈值级别
     */
    fun addCategory(category: Category, threshold: Threshold) {
        settings.add(SafetyCategory(category, threshold))
    }

    /**
     * 将安全设置转换为 JsonElement。
     */
    fun toJsonElement(): JsonElement {
        val settingsArray = JsonArray(settings.map { it.toJsonObject() })
        return settingsArray
    }

    /**
     * 安全类别。
     */
    enum class Category(val value: String) {
        HARASSMENT("harassment"),
        HATE_SPEECH("hate_speech"),
        SEXUAL_CONTENT("sexual_content"),
        DANGEROUS_CONTENT("dangerous_content"),
        SELF_HARM("self_harm"),
        VIOLENCE("violence"),
        MISINFORMATION("misinformation")
    }

    /**
     * 阈值级别。
     */
    enum class Threshold(val value: String) {
        BLOCK_NONE("block_none"),
        BLOCK_LOW("block_low"),
        BLOCK_MEDIUM("block_medium"),
        BLOCK_HIGH("block_high")
    }

    /**
     * 安全类别设置。
     */
    private data class SafetyCategory(val category: Category, val threshold: Threshold) {
        fun toJsonObject(): JsonElement {
            return JsonObject(mapOf(
                "category" to JsonPrimitive(category.value),
                "threshold" to JsonPrimitive(threshold.value)
            ))
        }
    }

    companion object {
        /**
         * 创建默认的安全设置。
         */
        fun default(): SafetySettings {
            val settings = SafetySettings()
            settings.addCategory(Category.HARASSMENT, Threshold.BLOCK_MEDIUM)
            settings.addCategory(Category.HATE_SPEECH, Threshold.BLOCK_MEDIUM)
            settings.addCategory(Category.SEXUAL_CONTENT, Threshold.BLOCK_MEDIUM)
            settings.addCategory(Category.DANGEROUS_CONTENT, Threshold.BLOCK_MEDIUM)
            return settings
        }

        /**
         * 创建严格的安全设置。
         */
        fun strict(): SafetySettings {
            val settings = SafetySettings()
            settings.addCategory(Category.HARASSMENT, Threshold.BLOCK_LOW)
            settings.addCategory(Category.HATE_SPEECH, Threshold.BLOCK_LOW)
            settings.addCategory(Category.SEXUAL_CONTENT, Threshold.BLOCK_LOW)
            settings.addCategory(Category.DANGEROUS_CONTENT, Threshold.BLOCK_LOW)
            settings.addCategory(Category.SELF_HARM, Threshold.BLOCK_LOW)
            settings.addCategory(Category.VIOLENCE, Threshold.BLOCK_LOW)
            settings.addCategory(Category.MISINFORMATION, Threshold.BLOCK_LOW)
            return settings
        }

        /**
         * 创建宽松的安全设置。
         */
        fun permissive(): SafetySettings {
            val settings = SafetySettings()
            settings.addCategory(Category.HARASSMENT, Threshold.BLOCK_HIGH)
            settings.addCategory(Category.HATE_SPEECH, Threshold.BLOCK_HIGH)
            settings.addCategory(Category.SEXUAL_CONTENT, Threshold.BLOCK_HIGH)
            settings.addCategory(Category.DANGEROUS_CONTENT, Threshold.BLOCK_HIGH)
            return settings
        }
    }
}
