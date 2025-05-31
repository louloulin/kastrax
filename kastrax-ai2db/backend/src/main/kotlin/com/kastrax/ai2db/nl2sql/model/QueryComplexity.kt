package com.kastrax.ai2db.nl2sql.model

/**
 * SQL查询复杂度
 */
enum class QueryComplexity {
    SIMPLE,    // 简单查询：单表查询，基本条件
    MEDIUM,    // 中等复杂度：多表连接，聚合函数
    COMPLEX,   // 复杂查询：子查询，窗口函数，复杂逻辑
    VERY_COMPLEX // 非常复杂：多层嵌套，复杂分析函数
}