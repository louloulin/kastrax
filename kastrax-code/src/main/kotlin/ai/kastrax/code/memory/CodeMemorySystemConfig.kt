package ai.kastrax.code.memory

/**
 * 代码记忆系统配置
 *
 * @property maxMemoryItems 最大记忆项数量
 * @property semanticRecall 是否启用语义召回
 * @property minScore 最小相似度分数
 */
data class CodeMemorySystemConfig(
    val maxMemoryItems: Int = 100,
    val semanticRecall: Boolean = true,
    val minScore: Double = 0.7
)
