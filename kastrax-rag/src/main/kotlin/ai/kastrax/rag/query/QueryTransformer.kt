package ai.kastrax.rag.query

/**
 * 查询转换器接口，定义了转换查询的方法。
 */
interface QueryTransformer {
    /**
     * 转换查询。
     *
     * @param query 查询文本
     * @return 转换后的查询文本
     */
    fun transform(query: String): String
}
