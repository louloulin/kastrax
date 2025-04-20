package ai.kastrax.rag.document

/**
 * 表示一个文档，包含内容和元数据。
 *
 * @property content 文档的内容
 * @property metadata 文档的元数据
 */
data class Document(
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 创建一个新的文档，添加或更新元数据。
     *
     * @param key 元数据键
     * @param value 元数据值
     * @return 带有更新元数据的新文档
     */
    fun withMetadata(key: String, value: Any): Document {
        val newMetadata = metadata.toMutableMap().apply {
            put(key, value)
        }
        return copy(metadata = newMetadata)
    }

    /**
     * 创建一个新的文档，添加或更新多个元数据项。
     *
     * @param metadata 要添加或更新的元数据
     * @return 带有更新元数据的新文档
     */
    fun withMetadata(metadata: Map<String, Any>): Document {
        val newMetadata = this.metadata.toMutableMap().apply {
            putAll(metadata)
        }
        return copy(metadata = newMetadata)
    }

    companion object {
        /**
         * 创建一个新的文档。
         *
         * @param content 文档内容
         * @param metadata 文档元数据
         * @return 新的文档实例
         */
        fun create(content: String, metadata: Map<String, Any> = emptyMap()): Document {
            return Document(content, metadata)
        }
    }
}
