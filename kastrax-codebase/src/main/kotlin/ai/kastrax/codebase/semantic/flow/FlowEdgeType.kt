package ai.kastrax.codebase.semantic.flow

/**
 * 流边类型
 */
enum class FlowEdgeType {
    SEQUENTIAL,    // 顺序执行
    CONDITIONAL,   // 条件执行
    LOOP_BACK,     // 循环回边
    CALL,          // 函数调用
    RETURN,        // 函数返回
    EXCEPTION,     // 异常流
    DATA_FLOW,     // 数据流
    UNKNOWN        // 未知类型
}
