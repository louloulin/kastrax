package ai.kastrax.codebase.engine

import ai.kastrax.codebase.context.Context
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.Location
import java.nio.file.Path

/**
 * 上下文引擎接口
 *
 * 提供代码库的上下文检索和构建功能
 */
interface ContextEngine {
    /**
     * 索引代码库
     *
     * @param path 代码库路径
     * @return 是否成功索引
     */
    suspend fun indexCodebase(path: Path): Boolean

    /**
     * 获取查询上下文
     *
     * @param query 查询文本
     * @param maxResults 最大结果数量
     * @param minScore 最小相似度分数
     * @param includeRelated 是否包含相关元素
     * @return 上下文
     */
    suspend fun getQueryContext(
        query: String,
        maxResults: Int = 10,
        minScore: Double = 0.0,
        includeRelated: Boolean = true
    ): Context

    /**
     * 获取文件上下文
     *
     * @param filePath 文件路径
     * @param maxResults 最大结果数量
     * @return 上下文
     */
    suspend fun getFileContext(filePath: Path, maxResults: Int = 10): Context

    /**
     * 获取编辑上下文
     *
     * @param filePath 文件路径
     * @param position 位置
     * @param maxResults 最大结果数量
     * @param minScore 最小相似度分数
     * @return 上下文
     */
    suspend fun getEditContext(filePath: Path, position: Location, maxResults: Int = 10, minScore: Double = 0.0): Context

    /**
     * 获取符号上下文
     *
     * @param symbolName 符号名称
     * @param maxResults 最大结果数量
     * @param minScore 最小相似度分数
     * @return 上下文
     */
    suspend fun getSymbolContext(symbolName: String, maxResults: Int = 10, minScore: Double = 0.0): Context

    /**
     * 获取代码元素
     *
     * @param id 元素ID
     * @return 代码元素，如果不存在则返回null
     */
    suspend fun getCodeElement(id: String): CodeElement?

    /**
     * 获取代码元素列表
     *
     * @param ids 元素ID列表
     * @return 代码元素列表
     */
    suspend fun getCodeElements(ids: List<String>): List<CodeElement>

    /**
     * 获取状态
     *
     * @return 状态信息
     */
    suspend fun getStatus(): Map<String, Any>

    /**
     * 关闭上下文引擎
     */
    suspend fun close()
}
