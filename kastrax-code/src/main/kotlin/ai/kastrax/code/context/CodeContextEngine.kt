package ai.kastrax.code.context

import ai.kastrax.code.model.Context
import ai.kastrax.code.model.Location
import java.nio.file.Path

/**
 * 代码上下文引擎接口
 * 
 * 负责构建和管理代码上下文，为代码智能体提供必要的上下文信息
 */
interface CodeContextEngine {
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
    suspend fun getEditContext(
        filePath: Path, 
        position: Location, 
        maxResults: Int = 10, 
        minScore: Double = 0.0
    ): Context
    
    /**
     * 获取符号上下文
     *
     * @param symbolName 符号名称
     * @param maxResults 最大结果数量
     * @param minScore 最小相似度分数
     * @return 上下文
     */
    suspend fun getSymbolContext(
        symbolName: String, 
        maxResults: Int = 10, 
        minScore: Double = 0.0
    ): Context
    
    /**
     * 关闭上下文引擎
     */
    suspend fun close()
}
