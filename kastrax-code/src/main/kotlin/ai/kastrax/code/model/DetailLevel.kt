package ai.kastrax.code.model

/**
 * 详细程度枚举
 *
 * 用于指定代码解释的详细程度
 */
enum class DetailLevel {
    /**
     * 简要 - 提供简要概述
     */
    BRIEF,

    /**
     * 正常 - 提供标准详细程度
     */
    NORMAL,

    /**
     * 详细 - 提供全面的解释，包括实现细节和原理
     */
    DETAILED
}
