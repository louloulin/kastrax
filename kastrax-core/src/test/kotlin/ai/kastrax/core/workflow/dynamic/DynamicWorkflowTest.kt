package ai.kastrax.core.workflow.dynamic

import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.SubWorkflowStep
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.composer.WorkflowComposer
import ai.kastrax.core.workflow.engine.WorkflowEngine
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.core.workflow.template.workflowTemplate
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DynamicWorkflowTest {
    
    private lateinit var workflowEngine: WorkflowEngine
    private lateinit var workflowGenerator: DynamicWorkflowGenerator
    private lateinit var workflowComposer: WorkflowComposer
    
    @BeforeEach
    fun setup() {
        // 创建工作流引擎
        workflowEngine = WorkflowEngine(
            workflows = mutableMapOf(),
            stateStorage = InMemoryWorkflowStateStorage()
        )
        
        // 创建动态工作流生成器
        workflowGenerator = DynamicWorkflowGenerator()
        
        // 创建工作流组合器
        workflowComposer = WorkflowComposer("测试组合器", workflowEngine)
        
        // 创建并注册基础工作流
        val basicWorkflow = createTestWorkflow()
        
        // 注册到工作流引擎
        (workflowEngine as? WorkflowEngine)?.let {
            val field = it::class.java.getDeclaredField("workflows")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val workflows = field.get(it) as MutableMap<String, ai.kastrax.core.workflow.Workflow>
            workflows["TestWorkflow"] = basicWorkflow
        }
    }
    
    @Test
    fun `test dynamic workflow creation and execution`() = runBlocking {
        // 创建动态工作流
        val dynamicWorkflow = workflowGenerator.createWorkflow(
            workflowName = "DynamicWorkflow",
            description = "动态创建的工作流"
        ) {
            dynamicStep {
                id = "step1"
                name = "步骤1"
                description = "动态步骤1"
                
                execute { context ->
                    val value = context.input["value"] as? Int ?: 0
                    WorkflowStepResult.success(
                        stepId = id,
                        output = mapOf("result" to (value * 2))
                    )
                }
            }
            
            dynamicStep {
                id = "step2"
                name = "步骤2"
                description = "动态步骤2"
                after("step1")
                
                execute { context ->
                    val value = context.steps["step1"]?.output?.get("result") as? Int ?: 0
                    WorkflowStepResult.success(
                        stepId = id,
                        output = mapOf("result" to (value + 10))
                    )
                }
            }
        }
        
        // 注册到工作流引擎
        (workflowEngine as? WorkflowEngine)?.let {
            val field = it::class.java.getDeclaredField("workflows")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val workflows = field.get(it) as MutableMap<String, ai.kastrax.core.workflow.Workflow>
            workflows["DynamicWorkflow"] = dynamicWorkflow
        }
        
        // 执行动态工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "DynamicWorkflow",
            input = mapOf("value" to 5)
        )
        
        // 验证结果
        assertTrue(result.success)
        assertEquals(2, result.steps.size)
        assertEquals(20, result.steps["step2"]?.output?.get("result"))
    }
    
    @Test
    fun `test sub-workflow execution`() = runBlocking {
        // 创建包含子工作流的工作流
        val parentWorkflow = workflowGenerator.createWorkflow(
            workflowName = "ParentWorkflow",
            description = "包含子工作流的工作流"
        ) {
            dynamicStep {
                id = "parentStep"
                name = "父步骤"
                description = "父工作流步骤"
                
                execute { context ->
                    val value = context.input["value"] as? Int ?: 0
                    WorkflowStepResult.success(
                        stepId = id,
                        output = mapOf("result" to (value + 5))
                    )
                }
            }
            
            // 添加子工作流步骤
            val subWorkflowStep = SubWorkflowStep(
                id = "subWorkflow",
                name = "子工作流",
                description = "执行子工作流",
                after = listOf("parentStep"),
                workflowId = "TestWorkflow",
                inputMapping = { context ->
                    val parentValue = context.steps["parentStep"]?.output?.get("result") as? Int ?: 0
                    mapOf("value" to parentValue)
                },
                workflowEngine = workflowEngine
            )
            
            // 使用反射访问私有字段
            val stepsField = this::class.java.getDeclaredField("steps")
            stepsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val steps = stepsField.get(this) as MutableMap<String, WorkflowStep>
            steps[subWorkflowStep.id] = subWorkflowStep
            
            dynamicStep {
                id = "finalStep"
                name = "最终步骤"
                description = "最终步骤"
                after("subWorkflow")
                
                execute { context ->
                    val subResult = context.steps["subWorkflow"]?.output?.get("result") as? Int ?: 0
                    WorkflowStepResult.success(
                        stepId = id,
                        output = mapOf("finalResult" to subResult)
                    )
                }
            }
        }
        
        // 注册到工作流引擎
        (workflowEngine as? WorkflowEngine)?.let {
            val field = it::class.java.getDeclaredField("workflows")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val workflows = field.get(it) as MutableMap<String, ai.kastrax.core.workflow.Workflow>
            workflows["ParentWorkflow"] = parentWorkflow
        }
        
        // 执行包含子工作流的工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "ParentWorkflow",
            input = mapOf("value" to 10)
        )
        
        // 验证结果
        assertTrue(result.success)
        assertNotNull(result.steps["subWorkflow"])
        assertNotNull(result.steps["finalStep"])
    }
    
    @Test
    fun `test workflow template`() = runBlocking {
        // 创建工作流模板
        val template = workflowTemplate {
            name = "TestTemplate"
            description = "测试模板"
            
            parameter(
                name = "stepCount",
                description = "步骤数量",
                type = Int::class.java,
                required = true
            )
            
            workflowBuilderDsl { params ->
                {
                    name = "TemplateWorkflow"
                    description = "从模板创建的工作流"
                    
                    val stepCount = params["stepCount"] as Int
                    
                    for (i in 1..stepCount) {
                        dynamicStep {
                            id = "step$i"
                            name = "步骤$i"
                            description = "模板步骤$i"
                            
                            if (i > 1) {
                                after("step${i-1}")
                            }
                            
                            execute { context ->
                                val value = if (i == 1) {
                                    context.input["value"] as? Int ?: 0
                                } else {
                                    context.steps["step${i-1}"]?.output?.get("result") as? Int ?: 0
                                }
                                
                                WorkflowStepResult.success(
                                    stepId = id,
                                    output = mapOf("result" to (value + i))
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 从模板创建工作流
        val templateWorkflow = template.createWorkflow(mapOf("stepCount" to 3))
        
        // 注册到工作流引擎
        (workflowEngine as? WorkflowEngine)?.let {
            val field = it::class.java.getDeclaredField("workflows")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val workflows = field.get(it) as MutableMap<String, ai.kastrax.core.workflow.Workflow>
            workflows["TemplateWorkflow"] = templateWorkflow
        }
        
        // 执行从模板创建的工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "TemplateWorkflow",
            input = mapOf("value" to 5)
        )
        
        // 验证结果
        assertTrue(result.success)
        assertEquals(3, result.steps.size)
        assertEquals(11, result.steps["step3"]?.output?.get("result"))
    }
    
    @Test
    fun `test workflow composition`() = runBlocking {
        // 创建动态工作流
        val dynamicWorkflow = workflowGenerator.createWorkflow(
            workflowName = "CompositionWorkflow",
            description = "用于组合的工作流"
        ) {
            dynamicStep {
                id = "compStep"
                name = "组合步骤"
                description = "用于组合的步骤"
                
                execute { context ->
                    val value = context.input["value"] as? Int ?: 0
                    WorkflowStepResult.success(
                        stepId = id,
                        output = mapOf("result" to (value * 3))
                    )
                }
            }
        }
        
        // 注册到工作流引擎
        (workflowEngine as? WorkflowEngine)?.let {
            val field = it::class.java.getDeclaredField("workflows")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val workflows = field.get(it) as MutableMap<String, ai.kastrax.core.workflow.Workflow>
            workflows["CompositionWorkflow"] = dynamicWorkflow
        }
        
        // 创建顺序组合工作流
        val sequentialWorkflow = workflowComposer.sequentialCompose(
            workflowName = "SequentialWorkflow",
            description = "顺序组合工作流",
            workflows = listOf(
                "TestWorkflow" to "test1",
                "CompositionWorkflow" to "comp1"
            )
        )
        
        // 注册到工作流引擎
        (workflowEngine as? WorkflowEngine)?.let {
            val field = it::class.java.getDeclaredField("workflows")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val workflows = field.get(it) as MutableMap<String, ai.kastrax.core.workflow.Workflow>
            workflows["SequentialWorkflow"] = sequentialWorkflow
        }
        
        // 执行顺序组合工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "SequentialWorkflow",
            input = mapOf("value" to 5)
        )
        
        // 验证结果
        assertTrue(result.success)
        assertEquals(2, result.steps.size)
        assertTrue(result.steps.containsKey("test1"))
        assertTrue(result.steps.containsKey("comp1"))
    }
    
    /**
     * 创建测试工作流。
     */
    private fun createTestWorkflow(): ai.kastrax.core.workflow.Workflow {
        // 创建步骤
        val step1 = object : WorkflowStep {
            override val id: String = "step1"
            override val name: String = "步骤1"
            override val description: String = "测试步骤1"
            override val after: List<String> = emptyList()
            override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap()
            
            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value = context.input["value"] as? Int ?: 0
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf("result" to (value * 2))
                )
            }
        }
        
        val step2 = object : WorkflowStep {
            override val id: String = "step2"
            override val name: String = "步骤2"
            override val description: String = "测试步骤2"
            override val after: List<String> = listOf("step1")
            override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap()
            
            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value = context.steps["step1"]?.output?.get("result") as? Int ?: 0
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf("result" to (value + 5))
                )
            }
        }
        
        // 创建工作流
        return SimpleWorkflow(
            workflowName = "TestWorkflow",
            description = "测试工作流",
            steps = mapOf(
                step1.id to step1,
                step2.id to step2
            )
        )
    }
}
