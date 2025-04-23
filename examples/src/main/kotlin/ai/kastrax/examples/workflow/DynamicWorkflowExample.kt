package ai.kastrax.examples.workflow

import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.SubWorkflowStep
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowBuilder
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.composer.WorkflowComposer
import ai.kastrax.core.workflow.dynamic.DynamicWorkflowGenerator
import ai.kastrax.core.workflow.dynamic.dynamicStep
import ai.kastrax.core.workflow.engine.WorkflowEngine
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.core.workflow.subWorkflow
import ai.kastrax.core.workflow.template.workflowTemplate
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * 动态工作流示例，展示如何使用动态工作流和子工作流功能。
 */
fun main() = runBlocking {
    println("开始动态工作流示例...")
    
    // 创建工作流引擎
    val stateStorage = InMemoryWorkflowStateStorage()
    val workflowEngine = WorkflowEngine(
        workflows = mutableMapOf(), // 初始为空，后续动态添加
        stateStorage = stateStorage
    )
    
    // 创建动态工作流生成器
    val workflowGenerator = DynamicWorkflowGenerator()
    
    // 创建工作流组合器
    val workflowComposer = WorkflowComposer("示例组合器", workflowEngine)
    
    // 1. 创建基础工作流
    println("\n1. 创建基础工作流...")
    val basicWorkflow = createBasicWorkflow()
    
    // 注册到工作流引擎
    (workflowEngine as? WorkflowEngine)?.let {
        val field = it::class.java.getDeclaredField("workflows")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val workflows = field.get(it) as MutableMap<String, Workflow>
        workflows["BasicWorkflow"] = basicWorkflow
    }
    
    // 2. 创建动态工作流
    println("\n2. 创建动态工作流...")
    val dynamicWorkflow = createDynamicWorkflow(workflowGenerator)
    
    // 注册到工作流引擎
    (workflowEngine as? WorkflowEngine)?.let {
        val field = it::class.java.getDeclaredField("workflows")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val workflows = field.get(it) as MutableMap<String, Workflow>
        workflows["DynamicWorkflow"] = dynamicWorkflow
    }
    
    // 3. 创建子工作流
    println("\n3. 创建子工作流...")
    val parentWorkflow = createParentWorkflow(workflowEngine)
    
    // 注册到工作流引擎
    (workflowEngine as? WorkflowEngine)?.let {
        val field = it::class.java.getDeclaredField("workflows")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val workflows = field.get(it) as MutableMap<String, Workflow>
        workflows["ParentWorkflow"] = parentWorkflow
    }
    
    // 4. 创建工作流模板
    println("\n4. 创建工作流模板...")
    val template = createWorkflowTemplate()
    
    // 从模板创建工作流实例
    val templateParams = mapOf(
        "name" to "模板实例工作流",
        "stepCount" to 3,
        "delay" to 200L
    )
    
    val templateWorkflow = template.createWorkflow(templateParams)
    
    // 注册到工作流引擎
    (workflowEngine as? WorkflowEngine)?.let {
        val field = it::class.java.getDeclaredField("workflows")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val workflows = field.get(it) as MutableMap<String, Workflow>
        workflows["TemplateWorkflow"] = templateWorkflow
    }
    
    // 5. 创建组合工作流
    println("\n5. 创建组合工作流...")
    val sequentialWorkflow = workflowComposer.sequentialCompose(
        workflowName = "SequentialWorkflow",
        description = "顺序执行多个工作流",
        workflows = listOf(
            "BasicWorkflow" to "basic1",
            "DynamicWorkflow" to "dynamic1"
        )
    )
    
    // 注册到工作流引擎
    (workflowEngine as? WorkflowEngine)?.let {
        val field = it::class.java.getDeclaredField("workflows")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val workflows = field.get(it) as MutableMap<String, Workflow>
        workflows["SequentialWorkflow"] = sequentialWorkflow
    }
    
    val parallelWorkflow = workflowComposer.parallelCompose(
        workflowName = "ParallelWorkflow",
        description = "并行执行多个工作流",
        workflows = mapOf(
            "basic2" to "BasicWorkflow",
            "dynamic2" to "DynamicWorkflow"
        )
    )
    
    // 注册到工作流引擎
    (workflowEngine as? WorkflowEngine)?.let {
        val field = it::class.java.getDeclaredField("workflows")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val workflows = field.get(it) as MutableMap<String, Workflow>
        workflows["ParallelWorkflow"] = parallelWorkflow
    }
    
    // 6. 执行工作流
    println("\n6. 执行工作流...")
    
    // 执行基础工作流
    println("\n执行基础工作流...")
    val basicResult = workflowEngine.executeWorkflow(
        workflowId = "BasicWorkflow",
        input = mapOf("value" to 10)
    )
    println("基础工作流执行结果: ${basicResult.success}")
    println("输出: ${basicResult.output}")
    
    // 执行动态工作流
    println("\n执行动态工作流...")
    val dynamicResult = workflowEngine.executeWorkflow(
        workflowId = "DynamicWorkflow",
        input = mapOf("value" to 20)
    )
    println("动态工作流执行结果: ${dynamicResult.success}")
    println("输出: ${dynamicResult.output}")
    
    // 执行父工作流（包含子工作流）
    println("\n执行父工作流（包含子工作流）...")
    val parentResult = workflowEngine.executeWorkflow(
        workflowId = "ParentWorkflow",
        input = mapOf("value" to 30)
    )
    println("父工作流执行结果: ${parentResult.success}")
    println("输出: ${parentResult.output}")
    
    // 执行模板工作流
    println("\n执行模板工作流...")
    val templateResult = workflowEngine.executeWorkflow(
        workflowId = "TemplateWorkflow",
        input = mapOf("value" to 40)
    )
    println("模板工作流执行结果: ${templateResult.success}")
    println("输出: ${templateResult.output}")
    
    // 执行顺序组合工作流
    println("\n执行顺序组合工作流...")
    val sequentialResult = workflowEngine.executeWorkflow(
        workflowId = "SequentialWorkflow",
        input = mapOf("value" to 50)
    )
    println("顺序组合工作流执行结果: ${sequentialResult.success}")
    println("输出: ${sequentialResult.output}")
    
    // 执行并行组合工作流
    println("\n执行并行组合工作流...")
    val parallelResult = workflowEngine.executeWorkflow(
        workflowId = "ParallelWorkflow",
        input = mapOf("value" to 60)
    )
    println("并行组合工作流执行结果: ${parallelResult.success}")
    println("输出: ${parallelResult.output}")
    
    println("\n示例完成!")
}

/**
 * 创建基础工作流。
 */
private fun createBasicWorkflow(): Workflow {
    // 创建步骤
    val step1 = object : WorkflowStep {
        override val id: String = "step1"
        override val name: String = "步骤1"
        override val description: String = "基础工作流的第一个步骤"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行基础工作流步骤1...")
            val value = context.input["value"] as? Int ?: 0
            delay(100) // 模拟工作
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf("result" to (value * 2))
            )
        }
    }
    
    val step2 = object : WorkflowStep {
        override val id: String = "step2"
        override val name: String = "步骤2"
        override val description: String = "基础工作流的第二个步骤"
        override val after: List<String> = listOf("step1")
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行基础工作流步骤2...")
            val value = context.steps["step1"]?.output?.get("result") as? Int ?: 0
            delay(100) // 模拟工作
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf("result" to (value + 5))
            )
        }
    }
    
    // 创建工作流
    return SimpleWorkflow(
        workflowName = "BasicWorkflow",
        description = "基础工作流示例",
        steps = mapOf(
            step1.id to step1,
            step2.id to step2
        )
    )
}

/**
 * 创建动态工作流。
 */
private fun createDynamicWorkflow(generator: DynamicWorkflowGenerator): Workflow {
    // 使用DSL创建动态工作流
    return generator.createWorkflow(
        workflowName = "DynamicWorkflow",
        description = "动态创建的工作流示例"
    ) {
        // 动态步骤1
        dynamicStep {
            id = "dynamicStep1"
            name = "动态步骤1"
            description = "动态创建的第一个步骤"
            
            execute { context ->
                println("执行动态步骤1...")
                val value = context.input["value"] as? Int ?: 0
                delay(100) // 模拟工作
                
                WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf("result" to (value * 3))
                )
            }
        }
        
        // 动态步骤2
        dynamicStep {
            id = "dynamicStep2"
            name = "动态步骤2"
            description = "动态创建的第二个步骤"
            after("dynamicStep1")
            
            execute { context ->
                println("执行动态步骤2...")
                val value = context.steps["dynamicStep1"]?.output?.get("result") as? Int ?: 0
                delay(100) // 模拟工作
                
                WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf("result" to (value - 10))
                )
            }
        }
    }
}

/**
 * 创建父工作流（包含子工作流）。
 */
private fun createParentWorkflow(workflowEngine: WorkflowEngine): Workflow {
    // 使用DSL创建包含子工作流的工作流
    val builder = WorkflowBuilder()
    builder.name = "ParentWorkflow"
    builder.description = "包含子工作流的父工作流示例"
    
    // 步骤1
    builder.dynamicStep {
        id = "parentStep1"
        name = "父工作流步骤1"
        description = "父工作流的第一个步骤"
        
        execute { context ->
            println("执行父工作流步骤1...")
            val value = context.input["value"] as? Int ?: 0
            delay(100) // 模拟工作
            
            WorkflowStepResult.success(
                stepId = id,
                output = mapOf("result" to (value + 15))
            )
        }
    }
    
    // 子工作流步骤
    builder.subWorkflow(workflowEngine) {
        id = "childWorkflow"
        name = "子工作流步骤"
        description = "执行基础工作流作为子工作流"
        workflowId = "BasicWorkflow"
        after("parentStep1")
        
        // 输入映射：将父工作流上下文映射到子工作流输入
        inputMapping = { context ->
            val parentValue = context.steps["parentStep1"]?.output?.get("result") as? Int ?: 0
            mapOf("value" to parentValue)
        }
        
        // 输出映射：将子工作流结果映射到步骤输出
        outputMapping = { result ->
            val childResult = result.output["result"] as? Int ?: 0
            mapOf(
                "childResult" to childResult,
                "processed" to true
            )
        }
    }
    
    // 最终步骤
    builder.dynamicStep {
        id = "finalStep"
        name = "最终步骤"
        description = "处理子工作流结果的最终步骤"
        after("childWorkflow")
        
        execute { context ->
            println("执行最终步骤...")
            val childResult = context.steps["childWorkflow"]?.output?.get("childResult") as? Int ?: 0
            val parentResult = context.steps["parentStep1"]?.output?.get("result") as? Int ?: 0
            delay(100) // 模拟工作
            
            WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "finalResult" to (childResult + parentResult),
                    "parentValue" to parentResult,
                    "childValue" to childResult
                )
            )
        }
    }
    
    return builder.build()
}

/**
 * 创建工作流模板。
 */
private fun createWorkflowTemplate() = workflowTemplate {
    name = "DynamicStepsTemplate"
    description = "动态生成指定数量步骤的工作流模板"
    
    // 定义模板参数
    parameter(
        name = "name",
        description = "工作流名称",
        type = String::class.java,
        required = true
    )
    
    parameter(
        name = "stepCount",
        description = "步骤数量",
        type = Int::class.java,
        required = true,
        validator = { it in 1..10 } // 限制步骤数量在1-10之间
    )
    
    parameter(
        name = "delay",
        description = "每个步骤的延迟时间（毫秒）",
        type = Long::class.java,
        required = false,
        defaultValue = 100L
    )
    
    // 定义工作流构建器
    workflowBuilderDsl { params ->
        {
            name = params["name"] as String
            description = "从模板生成的工作流，包含 ${params["stepCount"]} 个步骤"
            
            val stepCount = params["stepCount"] as Int
            val delay = params["delay"] as Long
            
            // 创建指定数量的步骤
            for (i in 1..stepCount) {
                dynamicStep {
                    id = "templateStep$i"
                    name = "模板步骤 $i"
                    description = "从模板生成的步骤 $i"
                    
                    // 设置前置步骤（除了第一个步骤）
                    if (i > 1) {
                        after("templateStep${i-1}")
                    }
                    
                    execute { context ->
                        println("执行模板步骤 $i...")
                        
                        // 获取输入值或前一步骤的结果
                        val value = if (i == 1) {
                            context.input["value"] as? Int ?: 0
                        } else {
                            context.steps["templateStep${i-1}"]?.output?.get("result") as? Int ?: 0
                        }
                        
                        // 模拟工作
                        delay(delay)
                        
                        // 根据步骤索引执行不同的操作
                        val result = when (i % 3) {
                            0 -> value + i // 加法
                            1 -> value * i // 乘法
                            else -> value - i // 减法
                        }
                        
                        WorkflowStepResult.success(
                            stepId = id,
                            output = mapOf(
                                "result" to result,
                                "operation" to when (i % 3) {
                                    0 -> "加法"
                                    1 -> "乘法"
                                    else -> "减法"
                                },
                                "stepIndex" to i
                            )
                        )
                    }
                }
            }
        }
    }
}
