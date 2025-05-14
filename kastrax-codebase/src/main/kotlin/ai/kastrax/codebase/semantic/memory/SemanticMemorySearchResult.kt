package ai.kastrax.codebase.semantic.memory

/**
 * 语义记忆检索结果
 *
 * @property memory 语义记忆
 * @property score 相似度分数
 */
data class SemanticMemorySearchResult(
    val memory: SemanticMemory,
    val score: Double
)
