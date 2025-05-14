package ai.kastrax.codebase.symbol.model

/**
 * 符号类型
 */
enum class SymbolType {
    /**
     * 文件
     */
    FILE,

    /**
     * 包
     */
    PACKAGE,

    /**
     * 类
     */
    CLASS,

    /**
     * 接口
     */
    INTERFACE,

    /**
     * 枚举
     */
    ENUM,

    /**
     * 注解
     */
    ANNOTATION,

    /**
     * 方法
     */
    METHOD,

    /**
     * 构造函数
     */
    CONSTRUCTOR,

    /**
     * 字段
     */
    FIELD,

    /**
     * 属性
     */
    PROPERTY,

    /**
     * 参数
     */
    PARAMETER,

    /**
     * 函数
     */
    FUNCTION,

    /**
     * 变量
     */
    VARIABLE,

    /**
     * 局部变量
     */
    LOCAL_VARIABLE,

    /**
     * 导入
     */
    IMPORT,

    /**
     * 命名空间
     */
    NAMESPACE,

    /**
     * 模块
     */
    MODULE,

    /**
     * Lambda
     */
    LAMBDA,

    /**
     * 块
     */
    BLOCK,

    /**
     * 语句
     */
    STATEMENT,

    /**
     * 表达式
     */
    EXPRESSION,

    /**
     * 注释
     */
    COMMENT,

    /**
     * 未知
     */
    UNKNOWN
}
