package ai.kastrax.core.workflow.dataflow

import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 增强的变量引用，支持更复杂的数据访问和转换。
 *
 * @property source 数据源
 * @property path JSON路径表达式
 * @property transform 数据转换函数
 * @property defaultValue 默认值
 * @property scope 变量作用域
 */
@Serializable
data class EnhancedVariableReference(
    val source: SourceType,
    val path: String,
    val transform: TransformType? = null,
    @Contextual val defaultValue: Any? = null,
    val scope: ScopeType = ScopeType.WORKFLOW
) {
    /**
     * 转换为标准变量引用。
     */
    fun toStandardReference(): VariableReference {
        val standardPath = when (source) {
            SourceType.INPUT -> "$.input.$path"
            SourceType.STEP -> "$.steps.$path"
            SourceType.VARIABLE -> "$.variables.$path"
            SourceType.CONSTANT -> path
        }
        return VariableReference(standardPath)
    }

    companion object {
        /**
         * 从标准变量引用创建增强的变量引用。
         */
        fun fromStandardReference(reference: VariableReference): EnhancedVariableReference {
            val path = reference.path
            return when {
                !path.startsWith("$") -> EnhancedVariableReference(
                    source = SourceType.CONSTANT,
                    path = path
                )
                path.startsWith("$.input.") -> EnhancedVariableReference(
                    source = SourceType.INPUT,
                    path = path.removePrefix("$.input.")
                )
                path.startsWith("$.steps.") -> EnhancedVariableReference(
                    source = SourceType.STEP,
                    path = path.removePrefix("$.steps.")
                )
                path.startsWith("$.variables.") -> EnhancedVariableReference(
                    source = SourceType.VARIABLE,
                    path = path.removePrefix("$.variables.")
                )
                else -> EnhancedVariableReference(
                    source = SourceType.CONSTANT,
                    path = path
                )
            }
        }

        /**
         * 创建输入变量引用。
         */
        fun input(path: String, defaultValue: Any? = null): EnhancedVariableReference {
            return EnhancedVariableReference(
                source = SourceType.INPUT,
                path = path,
                defaultValue = defaultValue
            )
        }

        /**
         * 创建步骤变量引用。
         */
        fun step(stepId: String, path: String, defaultValue: Any? = null): EnhancedVariableReference {
            return EnhancedVariableReference(
                source = SourceType.STEP,
                path = "$stepId.$path",
                defaultValue = defaultValue
            )
        }

        /**
         * 创建变量引用。
         */
        fun variable(name: String, defaultValue: Any? = null, scope: ScopeType = ScopeType.WORKFLOW): EnhancedVariableReference {
            return EnhancedVariableReference(
                source = SourceType.VARIABLE,
                path = name,
                defaultValue = defaultValue,
                scope = scope
            )
        }

        /**
         * 创建常量引用。
         */
        fun constant(value: Any?): EnhancedVariableReference {
            return EnhancedVariableReference(
                source = SourceType.CONSTANT,
                path = value.toString()
            )
        }
    }
}

/**
 * 数据源类型枚举。
 */
@Serializable
enum class SourceType {
    /**
     * 输入数据。
     */
    INPUT,

    /**
     * 步骤输出。
     */
    STEP,

    /**
     * 变量。
     */
    VARIABLE,

    /**
     * 常量值。
     */
    CONSTANT
}

/**
 * 数据转换类型枚举。
 */
@Serializable
enum class TransformType {
    /**
     * 转换为字符串。
     */
    TO_STRING,

    /**
     * 转换为整数。
     */
    TO_INT,

    /**
     * 转换为浮点数。
     */
    TO_FLOAT,

    /**
     * 转换为布尔值。
     */
    TO_BOOLEAN,

    /**
     * 转换为列表。
     */
    TO_LIST,

    /**
     * 转换为映射。
     */
    TO_MAP,

    /**
     * 自定义转换。
     */
    CUSTOM
}

/**
 * 变量解析器，用于解析增强的变量引用。
 */
class VariableResolver(
    private val scopeManager: VariableScopeManager = VariableScopeManager()
) {
    /**
     * 解析变量引用。
     *
     * @param reference 变量引用
     * @param context 工作流上下文
     * @param workflowId 工作流ID
     * @param stepId 步骤ID
     * @return 解析后的值
     */
    fun resolve(
        reference: EnhancedVariableReference,
        context: WorkflowContext,
        workflowId: String? = null,
        stepId: String? = null
    ): Any? {
        // 获取原始值
        val rawValue = when (reference.source) {
            SourceType.INPUT -> resolveInputReference(reference.path, context)
            SourceType.STEP -> resolveStepReference(reference.path, context)
            SourceType.VARIABLE -> resolveVariableReference(reference.path, context, workflowId, stepId)
            SourceType.CONSTANT -> reference.path
        } ?: reference.defaultValue

        // 应用转换
        return applyTransform(rawValue, reference.transform)
    }

    /**
     * 解析输入引用。
     */
    private fun resolveInputReference(path: String, context: WorkflowContext): Any? {
        return if (path.contains(".")) {
            val parts = path.split(".", limit = 2)
            val key = parts[0]
            val subPath = parts[1]
            val value = context.input[key]
            resolveNestedPath(value, subPath)
        } else {
            context.input[path]
        }
    }

    /**
     * 解析步骤引用。
     */
    private fun resolveStepReference(path: String, context: WorkflowContext): Any? {
        val parts = path.split(".", limit = 2)
        if (parts.size < 2) return null

        val stepId = parts[0]
        val outputPath = parts[1]
        val stepResult = context.steps[stepId] ?: return null

        return if (outputPath == "output") {
            stepResult.output
        } else if (outputPath.startsWith("output.")) {
            val subPath = outputPath.removePrefix("output.")
            resolveNestedPath(stepResult.output, subPath)
        } else {
            stepResult.output[outputPath]
        }
    }

    /**
     * 解析变量引用。
     */
    private fun resolveVariableReference(
        path: String,
        context: WorkflowContext,
        workflowId: String?,
        stepId: String?
    ): Any? {
        // 如果没有指定工作流ID和步骤ID，直接从上下文中获取
        if (workflowId == null) {
            return context.variables[path]
        }

        // 根据作用域获取变量
        val scope = when {
            stepId != null -> scopeManager.getStepScope(workflowId, stepId)
            else -> scopeManager.getWorkflowScope(workflowId)
        }

        return scope.get(path)
    }

    /**
     * 解析嵌套路径。
     */
    private fun resolveNestedPath(value: Any?, path: String): Any? {
        if (value == null) return null

        val parts = path.split(".")
        var current: Any? = value

        for (part in parts) {
            current = when (current) {
                is Map<*, *> -> (current as Map<*, *>)[part]
                is JsonObject -> current.jsonObject[part]
                is List<*> -> {
                    val index = part.toIntOrNull()
                    if (index != null && index >= 0 && index < current.size) {
                        current[index]
                    } else {
                        null
                    }
                }
                else -> null
            }

            if (current == null) break
        }

        return current
    }

    /**
     * 应用转换。
     */
    private fun applyTransform(value: Any?, transform: TransformType?): Any? {
        if (value == null || transform == null) return value

        return when (transform) {
            TransformType.TO_STRING -> value.toString()
            TransformType.TO_INT -> {
                when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull()
                    is JsonPrimitive -> value.jsonPrimitive.content.toIntOrNull()
                    else -> null
                }
            }
            TransformType.TO_FLOAT -> {
                when (value) {
                    is Number -> value.toFloat()
                    is String -> value.toFloatOrNull()
                    is JsonPrimitive -> value.jsonPrimitive.content.toFloatOrNull()
                    else -> null
                }
            }
            TransformType.TO_BOOLEAN -> {
                when (value) {
                    is Boolean -> value
                    is String -> value.lowercase() == "true"
                    is Number -> value.toInt() != 0
                    is JsonPrimitive -> value.jsonPrimitive.content.lowercase() == "true"
                    else -> false
                }
            }
            TransformType.TO_LIST -> {
                when (value) {
                    is List<*> -> value
                    is Array<*> -> value.toList()
                    is String -> value.split(",").map { it.trim() }
                    else -> listOf(value)
                }
            }
            TransformType.TO_MAP -> {
                when (value) {
                    is Map<*, *> -> value
                    is JsonObject -> value.jsonObject.toMap()
                    else -> null
                }
            }
            TransformType.CUSTOM -> value // 自定义转换在外部处理
        }
    }

    /**
     * 将JsonObject转换为Map。
     */
    private fun JsonObject.toMap(): Map<String, Any?> {
        return this.entries.associate { (key, value) ->
            key to when (value) {
                is JsonObject -> value.toMap()
                is JsonPrimitive -> {
                    when {
                        value.jsonPrimitive.isString -> value.jsonPrimitive.content
                        else -> value
                    }
                }
                else -> value
            }
        }
    }
}
