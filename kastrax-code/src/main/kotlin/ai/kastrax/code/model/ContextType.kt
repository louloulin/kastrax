package ai.kastrax.code.model

/**
 * 上下文类型
 */
enum class ContextType {
    /**
     * 当前文件
     */
    CURRENT_FILE,
    
    /**
     * 相关文件
     */
    RELATED_FILES,
    
    /**
     * 符号
     */
    SYMBOL,
    
    /**
     * 相关符号
     */
    RELATED_SYMBOLS,
    
    /**
     * 语义搜索
     */
    SEMANTIC_SEARCH,
    
    /**
     * 关键词搜索
     */
    KEYWORD_SEARCH,
    
    /**
     * 项目概览
     */
    PROJECT_OVERVIEW
}
