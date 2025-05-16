package ai.kastrax.code.model

/**
 * 详细程度枚举
 * 
 * 用于指定代码解释的详细程度
 */
enum class DetailLevel {
    /**
     * 基础级别 - 提供简要概述
     */
    BASIC,
    
    /**
     * 详细级别 - 提供更多细节和上下文
     */
    DETAILED,
    
    /**
     * 全面级别 - 提供全面的解释，包括实现细节和原理
     */
    COMPREHENSIVE
}
