package ai.kastrax.core.workflow.io

import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * 工作流导入/导出测试。
 */
class WorkflowIOTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    @Test
    fun `test export and import workflow as JSON`() {
        // 创建测试工作流
        val workflow = createTestWorkflow()
        
        // 创建工作流IO
        val workflowIO = WorkflowIO()
        
        // 导出为JSON
        val json = workflowIO.exportToJson(workflow)
        
        // 验证JSON
        assertNotNull(json)
        assertTrue(json.contains("\"name\":\"Test Workflow\""))
        assertTrue(json.contains("\"description\":\"A test workflow\""))
        
        // 导入工作流
        val importedWorkflow = workflowIO.importFromJson(json, mapOf(
            "ai.kastrax.core.workflow.io.TestStep" to TestStepProvider()
        ))
        
        // 验证导入的工作流
        assertNotNull(importedWorkflow)
        assertEquals("Test Workflow", importedWorkflow.javaClass.getField("name").get(importedWorkflow))
        assertEquals("A test workflow", importedWorkflow.javaClass.getField("description").get(importedWorkflow))
        
        // 获取步骤
        val stepsField = importedWorkflow.javaClass.getDeclaredField("steps")
        stepsField.isAccessible = true
        
        @Suppress("UNCHECKED_CAST")
        val steps = stepsField.get(importedWorkflow) as Map<String, WorkflowStep>
        
        // 验证步骤
        assertEquals(2, steps.size)
        assertTrue(steps.containsKey("step1"))
        assertTrue(steps.containsKey("step2"))
        
        val step1 = steps["step1"]!!
        val step2 = steps["step2"]!!
        
        assertEquals("step1", step1.id)
        assertEquals("Step 1", step1.name)
        assertEquals("Test step 1", step1.description)
        assertTrue(step1.after.isEmpty())
        
        assertEquals("step2", step2.id)
        assertEquals("Step 2", step2.name)
        assertEquals("Test step 2", step2.description)
        assertEquals(listOf("step1"), step2.after)
    }
    
    @Test
    fun `test export and import workflow as file`() {
        // 创建测试工作流
        val workflow = createTestWorkflow()
        
        // 创建工作流IO
        val workflowIO = WorkflowIO()
        
        // 导出到文件
        val file = File(tempDir.toFile(), "workflow.json")
        workflowIO.exportToFile(workflow, file)
        
        // 验证文件
        assertTrue(file.exists())
        assertTrue(file.isFile)
        assertTrue(file.length() > 0)
        
        // 导入工作流
        val importedWorkflow = workflowIO.importFromFile(file, stepProviders = mapOf(
            "ai.kastrax.core.workflow.io.TestStep" to TestStepProvider()
        ))
        
        // 验证导入的工作流
        assertNotNull(importedWorkflow)
        assertEquals("Test Workflow", importedWorkflow.javaClass.getField("name").get(importedWorkflow))
        assertEquals("A test workflow", importedWorkflow.javaClass.getField("description").get(importedWorkflow))
    }
    
    @Test
    fun `test export and import workflow as JSON element`() {
        // 创建测试工作流
        val workflow = createTestWorkflow()
        
        // 创建工作流IO
        val workflowIO = WorkflowIO()
        
        // 导出为JSON元素
        val jsonElement = workflowIO.exportToJsonElement(workflow)
        
        // 验证JSON元素
        assertNotNull(jsonElement)
        assertTrue(jsonElement is JsonObject)
        
        val jsonObject = jsonElement as JsonObject
        assertEquals("Test Workflow", jsonObject["name"]?.toString()?.trim('"'))
        assertEquals("A test workflow", jsonObject["description"]?.toString()?.trim('"'))
        
        // 导入工作流
        val importedWorkflow = workflowIO.importFromJsonElement(jsonElement, mapOf(
            "ai.kastrax.core.workflow.io.TestStep" to TestStepProvider()
        ))
        
        // 验证导入的工作流
        assertNotNull(importedWorkflow)
        assertEquals("Test Workflow", importedWorkflow.javaClass.getField("name").get(importedWorkflow))
        assertEquals("A test workflow", importedWorkflow.javaClass.getField("description").get(importedWorkflow))
    }
    
    /**
     * 创建测试工作流。
     */
    private fun createTestWorkflow(): SimpleWorkflow {
        // 创建步骤
        val step1 = TestStep(
            id = "step1",
            name = "Step 1",
            description = "Test step 1",
            after = emptyList()
        )
        
        val step2 = TestStep(
            id = "step2",
            name = "Step 2",
            description = "Test step 2",
            after = listOf("step1")
        )
        
        // 创建工作流
        return SimpleWorkflow(
            workflowName = "Test Workflow",
            description = "A test workflow",
            steps = mapOf(
                "step1" to step1,
                "step2" to step2
            )
        )
    }
}

/**
 * 测试步骤，用于测试工作流导入/导出。
 */
class TestStep(
    override val id: String,
    override val name: String,
    override val description: String,
    override val after: List<String>,
    override val variables: Map<String, VariableReference> = emptyMap(),
    override val config: StepConfig? = null
) : WorkflowStep {
    
    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
        return WorkflowStepResult(
            stepId = id,
            success = true,
            output = mapOf("result" to "Test result"),
            executionTime = 100
        )
    }
}

/**
 * 测试步骤提供者，用于测试工作流导入/导出。
 */
class TestStepProvider : StepProvider {
    
    override fun createStep(
        id: String,
        name: String,
        description: String,
        after: List<String>,
        variables: Map<String, VariableReference>,
        config: StepConfig?
    ): WorkflowStep {
        return TestStep(
            id = id,
            name = name,
            description = description,
            after = after,
            variables = variables,
            config = config
        )
    }
}
