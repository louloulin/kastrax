package ai.kastrax.code.model

/**
 * 任务类型
 */
enum class TaskType {
    /**
     * 代码生成
     */
    CODE_GENERATION,
    
    /**
     * 代码解释
     */
    CODE_EXPLANATION,
    
    /**
     * 代码重构
     */
    CODE_REFACTORING,
    
    /**
     * 测试生成
     */
    TEST_GENERATION,
    
    /**
     * 未知任务
     */
    UNKNOWN
}
