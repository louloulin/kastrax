package ai.kastrax.code.model

import java.nio.file.Path

/**
 * 上下文元素
 *
 * 表示上下文中的代码元素
 *
 * @property id 元素ID
 * @property name 元素名称
 * @property type 元素类型
 * @property content 元素内容
 * @property filePath 文件路径
 * @property location 位置
 * @property score 相关性分数
 */
data class ContextElement(
    val id: String,
    val name: String,
    val type: String,
    val content: String,
    val filePath: Path? = null,
    val location: Location? = null,
    val score: Double = 0.0
) {
    /**
     * 获取元素位置字符串
     *
     * @return 元素位置字符串，格式为 "filePath:line:column"
     */
    fun getLocationString(): String {
        return if (filePath != null && location != null) {
            "${filePath.fileName}:${location.toRangeString()}"
        } else if (filePath != null) {
            filePath.fileName.toString()
        } else if (location != null) {
            location.toRangeString()
        } else {
            ""
        }
    }
    
    /**
     * 获取元素摘要
     *
     * @param maxLength 最大长度
     * @return 元素摘要
     */
    fun getSummary(maxLength: Int = 100): String {
        val summary = content.replace("\n", " ").trim()
        return if (summary.length > maxLength) {
            summary.substring(0, maxLength) + "..."
        } else {
            summary
        }
    }
    
    override fun toString(): String {
        val locationString = getLocationString()
        return if (locationString.isNotEmpty()) {
            "[$type] $name ($locationString)"
        } else {
            "[$type] $name"
        }
    }
}
