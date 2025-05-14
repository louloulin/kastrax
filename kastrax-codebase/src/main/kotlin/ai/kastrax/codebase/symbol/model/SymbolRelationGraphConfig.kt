package ai.kastrax.codebase.symbol.model

/**
 * 符号关系图配置
 *
 * @property enableCaching 是否启用缓存
 * @property maxCacheSize 最大缓存大小
 * @property includeInheritance 是否包含继承关系
 * @property includeImplementation 是否包含实现关系
 * @property includeUsage 是否包含使用关系
 * @property includeDependency 是否包含依赖关系
 * @property includeOverride 是否包含重写关系
 * @property includeReference 是否包含引用关系
 * @property includeImport 是否包含导入关系
 * @property includeFiles 是否包含文件
 * @property includePackages 是否包含包
 * @property includeClasses 是否包含类
 * @property includeInterfaces 是否包含接口
 * @property includeEnums 是否包含枚举
 * @property includeAnnotations 是否包含注解
 * @property includeMethods 是否包含方法
 * @property includeConstructors 是否包含构造函数
 * @property includeFields 是否包含字段
 * @property includeProperties 是否包含属性
 * @property includeParameters 是否包含参数
 * @property includeFunctions 是否包含函数
 * @property includeVariables 是否包含变量
 * @property includeLocalVariables 是否包含局部变量
 * @property includeImports 是否包含导入
 * @property includeNamespaces 是否包含命名空间
 * @property includeModules 是否包含模块
 * @property includeLambdas 是否包含Lambda
 * @property includeBlocks 是否包含块
 * @property includeStatements 是否包含语句
 * @property includeExpressions 是否包含表达式
 * @property includeComments 是否包含注释
 */
data class SymbolRelationGraphConfig(
    val enableCaching: Boolean = true,
    val maxCacheSize: Int = 1000,
    val includeInheritance: Boolean = true,
    val includeImplementation: Boolean = true,
    val includeUsage: Boolean = true,
    val includeDependency: Boolean = true,
    val includeOverride: Boolean = true,
    val includeReference: Boolean = true,
    val includeImport: Boolean = true,
    val includeFiles: Boolean = true,
    val includePackages: Boolean = true,
    val includeClasses: Boolean = true,
    val includeInterfaces: Boolean = true,
    val includeEnums: Boolean = true,
    val includeAnnotations: Boolean = true,
    val includeMethods: Boolean = true,
    val includeConstructors: Boolean = true,
    val includeFields: Boolean = true,
    val includeProperties: Boolean = true,
    val includeParameters: Boolean = true,
    val includeFunctions: Boolean = true,
    val includeVariables: Boolean = true,
    val includeLocalVariables: Boolean = true,
    val includeImports: Boolean = true,
    val includeNamespaces: Boolean = true,
    val includeModules: Boolean = true,
    val includeLambdas: Boolean = true,
    val includeBlocks: Boolean = true,
    val includeStatements: Boolean = true,
    val includeExpressions: Boolean = true,
    val includeComments: Boolean = true
)
