package ai.kastrax.core.workflow.dataflow

import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStepResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 数据流和变量处理的测试。
 */
class DataFlowTest {

    /**
     * 测试变量作用域。
     */
    @Test
    fun testVariableScope() {
        // 创建作用域管理器
        val scopeManager = VariableScopeManager()

        // 获取全局作用域
        val globalScope = scopeManager.getGlobalScope()
        globalScope.set("globalVar", "Global Value")

        // 获取工作流作用域
        val workflowScope = scopeManager.getWorkflowScope("workflow1")
        workflowScope.set("workflowVar", "Workflow Value")

        // 获取步骤作用域
        val stepScope = scopeManager.getStepScope("workflow1", "step1")
        stepScope.set("stepVar", "Step Value")

        // 验证变量访问
        assertEquals("Global Value", globalScope.get("globalVar"))
        assertEquals("Global Value", workflowScope.get("globalVar"))
        assertEquals("Global Value", stepScope.get("globalVar"))

        assertEquals("Workflow Value", workflowScope.get("workflowVar"))
        assertEquals("Workflow Value", stepScope.get("workflowVar"))
        assertEquals(null, globalScope.get("workflowVar"))

        assertEquals("Step Value", stepScope.get("stepVar"))
        assertEquals(null, workflowScope.get("stepVar"))
        assertEquals(null, globalScope.get("stepVar"))
    }

    /**
     * 测试增强的变量引用。
     */
    @Test
    fun testEnhancedVariableReference() {
        // 创建工作流上下文
        val context = WorkflowContext(
            input = mapOf("inputVar" to "Input Value"),
            steps = mutableMapOf(
                "step1" to WorkflowStepResult(
                    stepId = "step1",
                    success = true,
                    output = mapOf(
                        "outputVar" to "Step Output",
                        "nested" to mapOf(
                            "value" to 42
                        )
                    )
                )
            ),
            variables = mutableMapOf("contextVar" to "Context Value")
        )

        // 创建变量解析器
        val resolver = VariableResolver()

        // 测试输入引用
        val inputRef = EnhancedVariableReference.input("inputVar")
        assertEquals("Input Value", resolver.resolve(inputRef, context))

        // 测试步骤引用
        val stepRef = EnhancedVariableReference.step("step1", "outputVar")
        assertEquals("Step Output", resolver.resolve(stepRef, context))

        // 测试嵌套路径
        val nestedRef = EnhancedVariableReference.step("step1", "nested.value")
        assertEquals(42, resolver.resolve(nestedRef, context))

        // 测试变量引用
        val varRef = EnhancedVariableReference.variable("contextVar")
        assertEquals("Context Value", resolver.resolve(varRef, context))

        // 测试常量引用
        val constRef = EnhancedVariableReference.constant("Constant Value")
        assertEquals("Constant Value", resolver.resolve(constRef, context))

        // 测试默认值
        val missingRef = EnhancedVariableReference.input("missingVar", defaultValue = "Default Value")
        assertEquals("Default Value", resolver.resolve(missingRef, context))

        // 测试转换
        val intRef = EnhancedVariableReference(
            source = SourceType.STEP,
            path = "step1.nested.value",
            transform = TransformType.TO_STRING
        )
        assertEquals("42", resolver.resolve(intRef, context))
    }

    /**
     * 测试数据转换器。
     */
    @Test
    fun testDataTransformer() {
        val transformer = DataTransformer()

        // 测试映射操作
        val mapResult = transformer.map(
            input = mapOf("key1" to "value1", "key2" to "value2"),
            mapping = mapOf(
                "mappedKey1" to EnhancedVariableReference.constant("value1"),
                "mappedKey2" to EnhancedVariableReference.constant("value2")
            ),
            context = WorkflowContext(input = emptyMap())
        )
        assertEquals("value1", mapResult["mappedKey1"])
        assertEquals("value2", mapResult["mappedKey2"])

        // 测试过滤操作
        val filterResult = transformer.filter(
            input = listOf(1, 2, 3, 4, 5),
            predicate = { (it as Int) % 2 == 0 }
        )
        assertEquals(listOf(2, 4), filterResult)

        // 测试聚合操作
        val aggregateResult = transformer.aggregate(
            input = listOf(1, 2, 3, 4, 5),
            initialValue = 0,
            operation = { acc, value -> acc + (value as Int) }
        )
        assertEquals(15, aggregateResult)

        // 测试分组操作
        val groupByResult = transformer.groupBy(
            input = listOf(1, 2, 3, 4, 5),
            keySelector = { (it as Int) % 2 }
        )
        assertEquals(listOf(1, 3, 5), groupByResult[1])
        assertEquals(listOf(2, 4), groupByResult[0])

        // 测试排序操作
        val sortResult = transformer.sort(
            input = listOf(5, 3, 1, 4, 2),
            comparator = Comparator { a, b -> (a as Int).compareTo(b as Int) }
        )
        assertEquals(listOf(1, 2, 3, 4, 5), sortResult)

        // 测试合并操作
        val mergeResult = transformer.merge(
            mapOf("key1" to "value1"),
            mapOf("key2" to "value2")
        )
        assertEquals("value1", mergeResult["key1"])
        assertEquals("value2", mergeResult["key2"])

        // 测试展平操作
        val flattenResult = transformer.flatten(
            listOf(listOf(1, 2), listOf(3, 4), 5)
        )
        assertEquals(listOf(1, 2, 3, 4, 5), flattenResult)
    }

    /**
     * 测试增强的工作流上下文。
     */
    @Test
    fun testEnhancedWorkflowContext() {
        // 创建标准工作流上下文
        val standardContext = WorkflowContext(
            input = mapOf("inputVar" to "Input Value"),
            steps = mutableMapOf(
                "step1" to WorkflowStepResult(
                    stepId = "step1",
                    success = true,
                    output = mapOf("outputVar" to "Step Output")
                )
            ),
            variables = mutableMapOf("contextVar" to "Context Value")
        )

        // 创建增强的工作流上下文
        val enhancedContext = EnhancedWorkflowContext.fromStandardContext(
            context = standardContext,
            workflowId = "workflow1"
        )

        // 测试变量访问
        assertEquals("Context Value", enhancedContext.getWorkflowVariable("contextVar"))

        // 测试设置变量
        enhancedContext.setWorkflowVariable("newVar", "New Value")
        assertEquals("New Value", enhancedContext.getWorkflowVariable("newVar"))

        // 测试步骤变量
        enhancedContext.setStepVariable("step1", "stepVar", "Step Variable")
        assertEquals("Step Variable", enhancedContext.getStepVariable("step1", "stepVar"))

        // 测试全局变量
        enhancedContext.setGlobalVariable("globalVar", "Global Variable")
        assertEquals("Global Variable", enhancedContext.getGlobalVariable("globalVar"))

        // 测试步骤输出访问
        assertEquals("Step Output", enhancedContext.getStepOutputField("step1", "outputVar"))

        // 测试变量解析
        val reference = EnhancedVariableReference.input("inputVar")
        assertEquals("Input Value", enhancedContext.resolveReference(reference))

        // 测试变量映射
        val variables = mapOf(
            "var1" to EnhancedVariableReference.input("inputVar"),
            "var2" to EnhancedVariableReference.step("step1", "outputVar")
        )
        val resolvedVariables = enhancedContext.resolveVariables(variables)
        assertEquals("Input Value", resolvedVariables["var1"])
        assertEquals("Step Output", resolvedVariables["var2"])

        // 测试转换回标准上下文
        val convertedContext = enhancedContext.toStandardContext()
        assertEquals("Input Value", convertedContext.input["inputVar"])
        assertEquals("Step Output", convertedContext.steps["step1"]?.output?.get("outputVar"))
        assertEquals("Context Value", convertedContext.variables["contextVar"])
        assertEquals("New Value", convertedContext.variables["newVar"])
        assertEquals("Global Variable", convertedContext.variables["globalVar"])
    }

    /**
     * 测试数据转换步骤。
     */
    @Test
    fun testDataTransformStep() = runTest {
        // 创建数据转换步骤
        val transformStep = DataTransformStep(
            id = "transform",
            name = "Transform Step",
            description = "Test data transformation",
            operationType = TransformOperationType.MAP,
            inputReference = EnhancedVariableReference.input("data"),
            outputMapping = mapOf(
                "transformedData" to EnhancedVariableReference(
                    source = SourceType.CONSTANT,
                    path = "result"
                )
            ),
            transformConfig = mapOf(
                "mapping" to mapOf(
                    "name" to EnhancedVariableReference(
                        source = SourceType.INPUT,
                        path = "data.name",
                        transform = TransformType.TO_STRING
                    ),
                    "age" to EnhancedVariableReference(
                        source = SourceType.INPUT,
                        path = "data.age",
                        transform = TransformType.TO_INT
                    ),
                    "isActive" to EnhancedVariableReference(
                        source = SourceType.INPUT,
                        path = "data.active",
                        transform = TransformType.TO_BOOLEAN
                    )
                )
            )
        )

        // 创建工作流上下文
        val context = WorkflowContext(
            input = mapOf(
                "data" to mapOf(
                    "name" to "John Doe",
                    "age" to 30,
                    "active" to true
                )
            )
        )

        // 执行步骤
        val result = transformStep.execute(context)

        // 验证结果
        assertTrue(result.success)
        assertNotNull(result.output["transformedData"])
        
        @Suppress("UNCHECKED_CAST")
        val transformedData = result.output["transformedData"] as Map<String, Any?>
        
        assertEquals("John Doe", transformedData["name"])
        assertEquals(30, transformedData["age"])
        assertEquals(true, transformedData["isActive"])
    }
}
