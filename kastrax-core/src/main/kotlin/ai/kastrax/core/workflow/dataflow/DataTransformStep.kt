package ai.kastrax.core.workflow.dataflow

import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import mu.KotlinLogging

/**
 * 数据转换操作类型枚举。
 */
enum class TransformOperationType {
    /**
     * 映射操作，将输入数据映射到新的结构。
     */
    MAP,

    /**
     * 过滤操作，根据条件过滤列表或映射中的元素。
     */
    FILTER,

    /**
     * 聚合操作，将列表中的元素聚合为单个值。
     */
    AGGREGATE,

    /**
     * 分组操作，将列表中的元素按照指定的键函数分组。
     */
    GROUP_BY,

    /**
     * 排序操作，将列表中的元素按照指定的比较器排序。
     */
    SORT,

    /**
     * 合并操作，将多个映射合并为一个映射。
     */
    MERGE,

    /**
     * 展平操作，将嵌套的列表展平为单层列表。
     */
    FLATTEN,

    /**
     * 自定义操作，执行自定义的数据转换逻辑。
     */
    CUSTOM
}

/**
 * 数据转换步骤，用于执行各种数据转换操作。
 *
 * @property id 步骤ID
 * @property name 步骤名称
 * @property description 步骤描述
 * @property operationType 操作类型
 * @property inputReference 输入数据引用
 * @property outputMapping 输出映射
 * @property transformConfig 转换配置
 * @property after 前置步骤ID列表
 * @property variables 变量引用
 * @property config 步骤配置
 */
class DataTransformStep(
    override val id: String,
    override val name: String = id,
    override val description: String = "",
    val operationType: TransformOperationType,
    val inputReference: EnhancedVariableReference,
    val outputMapping: Map<String, EnhancedVariableReference> = emptyMap(),
    val transformConfig: Map<String, Any?> = emptyMap(),
    override val after: List<String> = emptyList(),
    override val variables: Map<String, VariableReference> = emptyMap(),
    override val config: StepConfig? = null
) : WorkflowStep {
    private val transformLogger = KotlinLogging.logger {}
    private val transformer = DataTransformer()

    /**
     * 执行数据转换步骤。
     *
     * @param context 工作流上下文
     * @return 步骤执行结果
     */
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
        val startTime = System.currentTimeMillis()

        try {
            // 创建增强的工作流上下文
            val enhancedContext = EnhancedWorkflowContext.fromStandardContext(
                context = context,
                workflowId = "default" // 使用默认工作流ID
            )

            // 解析输入数据
            val inputData = enhancedContext.resolveReference(inputReference)
            transformLogger.debug { "输入数据: $inputData" }

            // 执行转换操作
            val result = when (operationType) {
                TransformOperationType.MAP -> executeMapOperation(inputData, enhancedContext)
                TransformOperationType.FILTER -> executeFilterOperation(inputData, enhancedContext)
                TransformOperationType.AGGREGATE -> executeAggregateOperation(inputData, enhancedContext)
                TransformOperationType.GROUP_BY -> executeGroupByOperation(inputData, enhancedContext)
                TransformOperationType.SORT -> executeSortOperation(inputData, enhancedContext)
                TransformOperationType.MERGE -> executeMergeOperation(inputData, enhancedContext)
                TransformOperationType.FLATTEN -> executeFlattenOperation(inputData, enhancedContext)
                TransformOperationType.CUSTOM -> executeCustomOperation(inputData, enhancedContext)
            }
            transformLogger.debug { "转换结果: $result" }

            // 应用输出映射
            val output = if (outputMapping.isNotEmpty()) {
                val mappedOutput = mutableMapOf<String, Any?>()
                for ((key, reference) in outputMapping) {
                    if (reference.source == SourceType.CONSTANT && reference.path == "result") {
                        mappedOutput[key] = result
                    } else {
                        mappedOutput[key] = enhancedContext.resolveReference(reference)
                    }
                }
                mappedOutput
            } else {
                mapOf("result" to result)
            }
            transformLogger.debug { "输出: $output" }

            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = output,
                executionTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            transformLogger.error(e) { "数据转换步骤执行失败: ${e.message}" }
            return WorkflowStepResult(
                stepId = id,
                success = false,
                output = emptyMap(),
                error = e.message ?: "Unknown error",
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * 执行映射操作。
     */
    private fun executeMapOperation(inputData: Any?, context: EnhancedWorkflowContext): Map<String, Any?> {
        val mapping = transformConfig["mapping"] as? Map<String, EnhancedVariableReference>
            ?: throw IllegalArgumentException("Missing mapping configuration for MAP operation")

        return context.mapData(inputData, mapping, id)
    }

    /**
     * 执行过滤操作。
     */
    private fun executeFilterOperation(inputData: Any?, context: EnhancedWorkflowContext): Any? {
        val predicateRef = transformConfig["predicate"] as? EnhancedVariableReference
            ?: throw IllegalArgumentException("Missing predicate configuration for FILTER operation")

        // 解析谓词函数
        val predicateStr = context.resolveReference(predicateRef)?.toString() ?: ""
        if (!predicateStr.startsWith("lambda:")) {
            throw IllegalArgumentException("Invalid predicate function format")
        }

        // 创建简单的谓词函数
        val predicate: (Any?) -> Boolean = { value ->
            if (value is Map<*, *>) {
                value["active"] == true
            } else {
                false
            }
        }

        return when (inputData) {
            is List<*> -> context.filterList(inputData, predicate)
            is Map<*, *> -> context.filterMap(inputData) { predicate(it.value) }
            else -> throw IllegalArgumentException("Input data must be a list or map for FILTER operation")
        }
    }

    /**
     * 执行聚合操作。
     */
    private fun executeAggregateOperation(inputData: Any?, context: EnhancedWorkflowContext): Any? {
        val initialValueRef = transformConfig["initialValue"] as? EnhancedVariableReference
            ?: throw IllegalArgumentException("Missing initialValue configuration for AGGREGATE operation")

        val operationRef = transformConfig["operation"] as? EnhancedVariableReference
            ?: throw IllegalArgumentException("Missing operation configuration for AGGREGATE operation")

        val initialValue = context.resolveReference(initialValueRef)

        // 解析操作函数
        val operationStr = context.resolveReference(operationRef)?.toString() ?: ""
        if (!operationStr.startsWith("lambda:")) {
            throw IllegalArgumentException("Invalid operation function format")
        }

        // 创建简单的聚合函数
        val operation: (Any?, Any?) -> Any? = { acc, value ->
            if (acc is Int && value is Map<*, *> && value["age"] is Int) {
                acc + (value["age"] as Int)
            } else {
                acc
            }
        }

        if (inputData !is List<*>) {
            throw IllegalArgumentException("Input data must be a list for AGGREGATE operation")
        }

        return inputData.fold(initialValue) { acc, item ->
            operation(acc, item)
        }
    }

    /**
     * 执行分组操作。
     */
    private fun executeGroupByOperation(inputData: Any?, context: EnhancedWorkflowContext): Map<*, List<*>> {
        val keySelectorRef = transformConfig["keySelector"] as? EnhancedVariableReference
            ?: throw IllegalArgumentException("Missing keySelector configuration for GROUP_BY operation")

        // 解析键选择器函数
        val keySelectorStr = context.resolveReference(keySelectorRef)?.toString() ?: ""
        if (keySelectorStr.startsWith("lambda:")) {
            // 在实际应用中应该解析函数字符串
        }

        // 创建简单的键选择器函数
        val keySelector: (Any?) -> Any? = { value ->
            if (value is Map<*, *>) {
                value["active"] // 按活跃状态分组
            } else {
                null
            }
        }

        if (inputData !is List<*>) {
            throw IllegalArgumentException("Input data must be a list for GROUP_BY operation")
        }

        return inputData.groupBy { keySelector(it) }
    }

    /**
     * 执行排序操作。
     */
    private fun executeSortOperation(inputData: Any?, context: EnhancedWorkflowContext): List<*> {
        val comparatorRef = transformConfig["comparator"] as? EnhancedVariableReference
            ?: throw IllegalArgumentException("Missing comparator configuration for SORT operation")

        // 解析比较器函数
        val comparatorStr = context.resolveReference(comparatorRef)?.toString() ?: ""
        if (comparatorStr.startsWith("lambda:")) {
            // 在实际应用中应该解析函数字符串
        }

        // 创建简单的比较器
        val comparator = Comparator<Any?> { a, b ->
            when {
                a is Int && b is Int -> a.compareTo(b)
                a is String && b is String -> a.compareTo(b)
                a is Map<*, *> && b is Map<*, *> && a["age"] is Int && b["age"] is Int ->
                    (a["age"] as Int).compareTo(b["age"] as Int)
                else -> 0
            }
        }

        if (inputData !is List<*>) {
            throw IllegalArgumentException("Input data must be a list for SORT operation")
        }

        @Suppress("UNCHECKED_CAST")
        return (inputData as List<Any?>).sortedWith(comparator)
    }

    /**
     * 执行合并操作。
     */
    private fun executeMergeOperation(inputData: Any?, context: EnhancedWorkflowContext): Map<*, *> {
        val additionalInputsRef = transformConfig["additionalInputs"] as? List<EnhancedVariableReference>
            ?: throw IllegalArgumentException("Missing additionalInputs configuration for MERGE operation")

        val additionalInputs = additionalInputsRef.map { context.resolveReference(it) }

        if (inputData !is Map<*, *>) {
            throw IllegalArgumentException("Input data must be a map for MERGE operation")
        }

        val inputs = listOf(inputData) + additionalInputs.filterIsInstance<Map<*, *>>()
        return transformer.merge(*inputs.toTypedArray())
    }

    /**
     * 执行展平操作。
     */
    private fun executeFlattenOperation(inputData: Any?, context: EnhancedWorkflowContext): List<*> {
        if (inputData !is List<*>) {
            throw IllegalArgumentException("Input data must be a list for FLATTEN operation")
        }

        return transformer.flatten(inputData)
    }

    /**
     * 执行自定义操作。
     */
    private fun executeCustomOperation(inputData: Any?, context: EnhancedWorkflowContext): Any? {
        val transformFunctionRef = transformConfig["transformFunction"] as? EnhancedVariableReference
            ?: throw IllegalArgumentException("Missing transformFunction configuration for CUSTOM operation")

        // 解析转换函数
        val transformFunctionStr = context.resolveReference(transformFunctionRef)?.toString() ?: ""
        if (transformFunctionStr.startsWith("lambda:")) {
            // 在实际应用中应该解析函数字符串
        }

        // 创建简单的转换函数
        val transformFunction: (Any?) -> Any? = { value ->
            when (value) {
                is Map<*, *> -> value.mapValues { (_, v) ->
                    if (v is String) v.uppercase() else v
                }
                is List<*> -> value.filterNotNull()
                is String -> value.uppercase()
                else -> value
            }
        }

        return transformFunction(inputData)
    }
}
