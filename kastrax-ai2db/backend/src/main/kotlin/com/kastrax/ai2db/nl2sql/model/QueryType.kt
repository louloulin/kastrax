package com.kastrax.ai2db.nl2sql.model

/**
 * SQL查询类型
 */
enum class QueryType {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    CREATE,
    DROP,
    ALTER,
    UNKNOWN
}