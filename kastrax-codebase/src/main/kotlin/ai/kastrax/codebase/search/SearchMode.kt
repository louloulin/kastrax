package ai.kastrax.codebase.search

/**
 * 搜索模式
 */
enum class SearchMode {
    /**
     * 语义搜索
     */
    SEMANTIC,
    
    /**
     * 关键词搜索
     */
    KEYWORD,
    
    /**
     * 混合搜索
     */
    HYBRID
}

/**
 * 匹配类型
 */
enum class MatchType {
    /**
     * 语义匹配
     */
    SEMANTIC,
    
    /**
     * 关键词匹配
     */
    KEYWORD,
    
    /**
     * 混合匹配
     */
    HYBRID
}
