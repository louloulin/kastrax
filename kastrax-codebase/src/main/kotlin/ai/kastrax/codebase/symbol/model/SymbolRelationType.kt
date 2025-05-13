package ai.kastrax.codebase.symbol.model

/**
 * 符号关系类型
 */
enum class SymbolRelationType {
    /**
     * 包含
     */
    CONTAINS,

    /**
     * 继承
     */
    EXTENDS,

    /**
     * 实现
     */
    IMPLEMENTS,

    /**
     * 引用
     */
    REFERENCES,

    /**
     * 调用
     */
    CALLS,

    /**
     * 重写
     */
    OVERRIDES,

    /**
     * 使用
     */
    USES,

    /**
     * 导入
     */
    IMPORTS,

    /**
     * 依赖于
     */
    DEPENDS_ON,

    /**
     * 定义于
     */
    DEFINED_BY,

    /**
     * 声明于
     */
    DECLARED_BY,

    /**
     * 实例化
     */
    INSTANTIATES,

    /**
     * 注解
     */
    ANNOTATES,

    /**
     * 抛出
     */
    THROWS,

    /**
     * 捕获
     */
    CATCHES,

    /**
     * 返回
     */
    RETURNS,

    /**
     * 赋值
     */
    ASSIGNS,

    /**
     * 继承关系
     */
    INHERITANCE,

    /**
     * 实现关系
     */
    IMPLEMENTATION,

    /**
     * 使用关系
     */
    USAGE,

    /**
     * 依赖关系
     */
    DEPENDENCY,

    /**
     * 包含关系
     */
    CONTAINMENT,

    /**
     * 调用关系
     */
    INVOCATION,

    /**
     * 引用关系
     */
    REFERENCE,

    /**
     * 关联关系
     */
    ASSOCIATION,

    /**
     * 聚合关系
     */
    AGGREGATION,

    /**
     * 组合关系
     */
    COMPOSITION,

    /**
     * 未知
     */
    UNKNOWN,

    /**
     * 其他关系
     */
    OTHER
}
