package ai.kastrax.codebase.indexing

import ai.kastrax.codebase.semantic.model.CodeElement
import kotlinx.coroutines.flow.Flow

/**
 * 代码索引器接口
 *
 * 用于索引和检索代码元素
 */
interface CodeIndexer {
    /**
     * 索引代码元素
     *
     * @param element 代码元素
     */
    suspend fun indexElement(element: CodeElement)

    /**
     * 批量索引代码元素
     *
     * @param elements 代码元素集合
     */
    suspend fun indexElements(elements: Collection<CodeElement>)

    /**
     * 获取所有代码元素
     *
     * @return 代码元素集合
     */
    suspend fun getAllElements(): Collection<CodeElement>

    /**
     * 获取代码元素流
     *
     * @return 代码元素流
     */
    suspend fun getElementsFlow(): Flow<CodeElement>

    /**
     * 根据ID获取代码元素
     *
     * @param id 元素ID
     * @return 代码元素，如果不存在则返回null
     */
    suspend fun getElementById(id: String): CodeElement?

    /**
     * 根据名称获取代码元素
     *
     * @param name 元素名称
     * @return 代码元素集合
     */
    suspend fun getElementsByName(name: String): Collection<CodeElement>

    /**
     * 根据类型获取代码元素
     *
     * @param type 元素类型
     * @return 代码元素集合
     */
    suspend fun getElementsByType(type: String): Collection<CodeElement>

    /**
     * 根据类型获取代码元素
     *
     * @param type 元素类型
     * @return 代码元素集合
     */
    suspend fun getElementsByType(type: ai.kastrax.codebase.semantic.model.CodeElementType): Collection<CodeElement>

    /**
     * 根据文件路径获取代码元素
     *
     * @param filePath 文件路径
     * @return 代码元素集合
     */
    suspend fun getElementsByFilePath(filePath: java.nio.file.Path): Collection<CodeElement>

    /**
     * 根据关键词搜索代码元素
     *
     * @param keywords 关键词
     * @param limit 限制数量
     * @param minScore 最小分数
     * @return 代码元素集合
     */
    suspend fun searchByKeywords(keywords: List<String>, limit: Int = 10, minScore: Float = 0.5f): List<Pair<CodeElement, Float>>

    /**
     * 清除索引
     */
    suspend fun clearIndex()
}
