package ai.kastrax.codebase.indexing

/**
 * 索引任务类型
 */
enum class IndexTaskType {
    /**
     * 添加文件
     */
    ADD,
    
    /**
     * 更新文件
     */
    UPDATE,
    
    /**
     * 删除文件
     */
    DELETE,
    
    /**
     * 分支变更
     */
    BRANCH_CHANGE,
    
    /**
     * 完全重新索引
     */
    FULL_REINDEX
}
