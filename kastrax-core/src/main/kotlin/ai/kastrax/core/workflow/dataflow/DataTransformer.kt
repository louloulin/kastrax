package ai.kastrax.core.workflow.dataflow

import ai.kastrax.core.workflow.WorkflowContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 数据转换器，用于执行复杂的数据转换操作。
 */
class DataTransformer {
    /**
     * 映射操作，将输入数据映射到新的结构。
     *
     * @param input 输入数据
     * @param mapping 映射规则
     * @param context 工作流上下文
     * @param workflowId 工作流ID
     * @param stepId 步骤ID
     * @return 映射后的数据
     */
    fun map(
        input: Any?,
        mapping: Map<String, EnhancedVariableReference>,
        context: WorkflowContext,
        workflowId: String? = null,
        stepId: String? = null
    ): Map<String, Any?> {
        val resolver = VariableResolver()
        return mapping.mapValues { (_, reference) ->
            resolver.resolve(reference, context, workflowId, stepId)
        }
    }

    /**
     * 过滤操作，根据条件过滤列表中的元素。
     *
     * @param input 输入列表
     * @param predicate 过滤条件
     * @return 过滤后的列表
     */
    fun filter(input: List<*>, predicate: (Any?) -> Boolean): List<*> {
        return input.filter(predicate)
    }

    /**
     * 过滤操作，根据条件过滤映射中的元素。
     *
     * @param input 输入映射
     * @param predicate 过滤条件
     * @return 过滤后的映射
     */
    fun filter(input: Map<*, *>, predicate: (Map.Entry<*, *>) -> Boolean): Map<*, *> {
        return input.entries.filter(predicate).associate { it.key to it.value }
    }

    /**
     * 聚合操作，将列表中的元素聚合为单个值。
     *
     * @param input 输入列表
     * @param initialValue 初始值
     * @param operation 聚合操作
     * @return 聚合结果
     */
    fun <T, R> aggregate(input: List<T>, initialValue: R, operation: (R, T) -> R): R {
        return input.fold(initialValue, operation)
    }

    /**
     * 分组操作，将列表中的元素按照指定的键函数分组。
     *
     * @param input 输入列表
     * @param keySelector 键选择器函数
     * @return 分组结果
     */
    fun <T, K> groupBy(input: List<T>, keySelector: (T) -> K): Map<K, List<T>> {
        return input.groupBy(keySelector)
    }

    /**
     * 排序操作，将列表中的元素按照指定的比较器排序。
     *
     * @param input 输入列表
     * @param comparator 比较器
     * @return 排序后的列表
     */
    fun <T> sort(input: List<T>, comparator: Comparator<T>): List<T> {
        return input.sortedWith(comparator)
    }

    /**
     * 合并操作，将多个映射合并为一个映射。
     *
     * @param inputs 输入映射列表
     * @return 合并后的映射
     */
    fun merge(vararg inputs: Map<*, *>): Map<*, *> {
        val result = mutableMapOf<Any?, Any?>()
        for (input in inputs) {
            result.putAll(input)
        }
        return result
    }

    /**
     * 展平操作，将嵌套的列表展平为单层列表。
     *
     * @param input 输入嵌套列表
     * @return 展平后的列表
     */
    fun flatten(input: List<*>): List<*> {
        val result = mutableListOf<Any?>()
        for (item in input) {
            if (item is List<*>) {
                result.addAll(flatten(item))
            } else {
                result.add(item)
            }
        }
        return result
    }

    /**
     * 转换为JSON对象。
     *
     * @param input 输入数据
     * @return JSON对象
     */
    fun toJsonObject(input: Map<*, *>): JsonObject {
        return buildJsonObject {
            for ((key, value) in input) {
                if (key is String) {
                    put(key, toJsonElement(value))
                }
            }
        }
    }

    /**
     * 转换为JSON数组。
     *
     * @param input 输入列表
     * @return JSON数组
     */
    fun toJsonArray(input: List<*>): JsonArray {
        return buildJsonArray {
            for (item in input) {
                add(toJsonElement(item))
            }
        }
    }

    /**
     * 转换为JSON元素。
     *
     * @param value 输入值
     * @return JSON元素
     */
    private fun toJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonPrimitive("null")
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> toJsonObject(value)
            is List<*> -> toJsonArray(value)
            is JsonElement -> value
            else -> JsonPrimitive(value.toString())
        }
    }

    /**
     * 从JSON对象转换为Map。
     *
     * @param input JSON对象
     * @return Map
     */
    fun fromJsonObject(input: JsonObject): Map<String, Any?> {
        return input.entries.associate { (key, value) ->
            key to fromJsonElement(value)
        }
    }

    /**
     * 从JSON数组转换为List。
     *
     * @param input JSON数组
     * @return List
     */
    fun fromJsonArray(input: JsonArray): List<Any?> {
        return input.map { fromJsonElement(it) }
    }

    /**
     * 从JSON元素转换为原生类型。
     *
     * @param element JSON元素
     * @return 原生类型值
     */
    private fun fromJsonElement(element: JsonElement): Any? {
        return when (element) {
            is JsonObject -> fromJsonObject(element)
            is JsonArray -> fromJsonArray(element)
            is JsonPrimitive -> {
                when {
                    element.jsonPrimitive.isString -> element.jsonPrimitive.content
                    element.jsonPrimitive.content == "null" -> null
                    element.jsonPrimitive.content == "true" -> true
                    element.jsonPrimitive.content == "false" -> false
                    else -> {
                        val intValue = element.jsonPrimitive.content.toIntOrNull()
                        if (intValue != null) {
                            intValue
                        } else {
                            element.jsonPrimitive.content.toDoubleOrNull() ?: element.jsonPrimitive.content
                        }
                    }
                }
            }
            else -> null
        }
    }
}
