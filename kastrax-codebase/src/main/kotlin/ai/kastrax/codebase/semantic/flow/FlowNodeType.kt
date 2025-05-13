package ai.kastrax.codebase.semantic.flow

/**
 * 流节点类型
 */
enum class FlowNodeType {
    ENTRY,          // 入口节点
    EXIT,           // 出口节点
    STATEMENT,      // 语句节点
    CONDITION,      // 条件节点
    LOOP_START,     // 循环开始节点
    LOOP_END,       // 循环结束节点
    TRY_START,      // try 开始节点
    CATCH_START,    // catch 开始节点
    FINALLY_START,  // finally 开始节点
    EXCEPTION_EXIT, // 异常出口节点
    METHOD_CALL,    // 方法调用节点
    RETURN          // 返回节点
}
