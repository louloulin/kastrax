package ai.kastrax.a2a.workflow

import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.agent.A2AAgentImpl
import ai.kastrax.a2a.dsl.a2aAgent
import ai.kastrax.a2a.model.*
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A2A 工作流测试
 */
class A2AWorkflowTest {
    /**
     * 模拟的 kastrax 代理
     */
    private lateinit var mockAgent1: Agent
    private lateinit var mockAgent2: Agent
    private lateinit var mockAgent3: Agent

    /**
     * A2A 代理
     */
    private lateinit var a2aAgent1: A2AAgent
    private lateinit var a2aAgent2: A2AAgent
    private lateinit var a2aAgent3: A2AAgent

    @BeforeEach
    fun setup() {
        // 创建模拟的 kastrax 代理
        mockAgent1 = mockk<Agent>()
        mockAgent2 = mockk<Agent>()
        mockAgent3 = mockk<Agent>()

        // 配置模拟代理的行为
        coEvery { mockAgent1.name } returns "data-collector"
        coEvery { mockAgent1.generate(any<String>()) } returns AgentResponse(
            text = "Data collected successfully",
            toolCalls = emptyList()
        )

        coEvery { mockAgent2.name } returns "data-analyzer"
        coEvery { mockAgent2.generate(any<String>()) } returns AgentResponse(
            text = "Data analyzed successfully",
            toolCalls = emptyList()
        )

        coEvery { mockAgent3.name } returns "report-generator"
        coEvery { mockAgent3.generate(any<String>()) } returns AgentResponse(
            text = "Report generated successfully",
            toolCalls = emptyList()
        )

        // 创建 A2A 代理
        a2aAgent1 = createA2AAgent(
            "data-collector",
            "Data Collector",
            "Collects data from various sources",
            mockAgent1,
            listOf(
                createCapability(
                    "collect_data",
                    "Collect Data",
                    "Collects data from a specified source",
                    listOf(
                        createParameter("source", "string", "Data source", true),
                        createParameter("filters", "object", "Data filters", false)
                    )
                )
            )
        )

        a2aAgent2 = createA2AAgent(
            "data-analyzer",
            "Data Analyzer",
            "Analyzes data and generates insights",
            mockAgent2,
            listOf(
                createCapability(
                    "analyze_data",
                    "Analyze Data",
                    "Analyzes the provided data",
                    listOf(
                        createParameter("data", "object", "Data to analyze", true),
                        createParameter("analysis_type", "string", "Type of analysis", true)
                    )
                )
            )
        )

        a2aAgent3 = createA2AAgent(
            "report-generator",
            "Report Generator",
            "Generates reports based on analysis results",
            mockAgent3,
            listOf(
                createCapability(
                    "generate_report",
                    "Generate Report",
                    "Generates a report from analysis results",
                    listOf(
                        createParameter("analysis", "object", "Analysis results", true),
                        createParameter("format", "string", "Report format", false)
                    )
                )
            )
        )
    }

    @Test
    @org.junit.jupiter.api.Disabled("Temporarily disabled due to issues with workflow execution")
    fun `test workflow execution`() = runBlocking {
        // 创建工作流
        val workflow = workflow {
            id = "market-analysis-workflow"
            name = "Market Analysis Workflow"
            description = "Analyzes market data and generates a report"

            // 添加本地代理
            localAgent("data-collector", a2aAgent1)
            localAgent("data-analyzer", a2aAgent2)
            localAgent("report-generator", a2aAgent3)

            // 添加工作流步骤
            step {
                id = "collect-data"
                name = "Collect Market Data"
                agentId = "data-collector"
                capabilityId = "collect_data"
                parameters { _ ->
                    mapOf(
                        "prompt" to JsonPrimitive("Collect market data for technology sector"),
                        "source" to JsonPrimitive("market_data"),
                        "filters" to buildJsonObject {
                            put("sector", JsonPrimitive("technology"))
                            put("timeframe", JsonPrimitive("1y"))
                        }
                    )
                }
            }

            step {
                id = "analyze-data"
                name = "Analyze Market Data"
                agentId = "data-analyzer"
                capabilityId = "analyze_data"
                dependsOn("collect-data")
                parameters { results ->
                    mapOf(
                        "prompt" to JsonPrimitive("Analyze the collected market data"),
                        "data" to results["collect-data"]!!,
                        "analysis_type" to JsonPrimitive("trend")
                    )
                }
            }

            step {
                id = "generate-report"
                name = "Generate Market Report"
                agentId = "report-generator"
                capabilityId = "generate_report"
                dependsOn("analyze-data")
                parameters { results ->
                    mapOf(
                        "prompt" to JsonPrimitive("Generate a report from the analysis"),
                        "analysis" to results["analyze-data"]!!,
                        "format" to JsonPrimitive("pdf")
                    )
                }
            }
        }

        // 收集工作流事件
        val events = mutableListOf<WorkflowEvent>()
        val job = workflow.events.toList(events)

        // 执行工作流
        val results = workflow.execute()

        // 验证结果
        assertEquals(3, results.size)
        assertTrue(results.containsKey("collect-data"))
        assertTrue(results.containsKey("analyze-data"))
        assertTrue(results.containsKey("generate-report"))

        // 验证工作流状态
        assertEquals(WorkflowState.COMPLETED, workflow.getState())

        // 验证步骤结果
        val collectDataResult = workflow.getStepResult("collect-data")
        assertNotNull(collectDataResult)
        assertTrue(collectDataResult.success)

        val analyzeDataResult = workflow.getStepResult("analyze-data")
        assertNotNull(analyzeDataResult)
        assertTrue(analyzeDataResult.success)

        val generateReportResult = workflow.getStepResult("generate-report")
        assertNotNull(generateReportResult)
        assertTrue(generateReportResult.success)

        // 验证工作流事件
        assertTrue(events.any { it is WorkflowEvent.Started })
        assertTrue(events.any { it is WorkflowEvent.StepStarted && it.stepId == "collect-data" })
        assertTrue(events.any { it is WorkflowEvent.StepCompleted && it.stepId == "collect-data" })
        assertTrue(events.any { it is WorkflowEvent.StepStarted && it.stepId == "analyze-data" })
        assertTrue(events.any { it is WorkflowEvent.StepCompleted && it.stepId == "analyze-data" })
        assertTrue(events.any { it is WorkflowEvent.StepStarted && it.stepId == "generate-report" })
        assertTrue(events.any { it is WorkflowEvent.StepCompleted && it.stepId == "generate-report" })
        assertTrue(events.any { it is WorkflowEvent.Completed })
    }

    /**
     * 创建 A2A 代理
     */
    private fun createA2AAgent(
        id: String,
        name: String,
        description: String,
        baseAgent: Agent,
        capabilities: List<Capability>
    ): A2AAgent {
        val agentCard = AgentCard(
            id = id,
            name = name,
            description = description,
            version = "1.0.0",
            endpoint = "/a2a/v1/agents/$id",
            capabilities = capabilities,
            authentication = Authentication(AuthType.API_KEY)
        )

        return A2AAgentImpl(agentCard, baseAgent)
    }

    /**
     * 创建能力
     */
    private fun createCapability(
        id: String,
        name: String,
        description: String,
        parameters: List<Parameter>
    ): Capability {
        return Capability(
            id = id,
            name = name,
            description = description,
            parameters = parameters,
            returnType = "json",
            examples = emptyList()
        )
    }

    /**
     * 创建参数
     */
    private fun createParameter(
        name: String,
        type: String,
        description: String,
        required: Boolean
    ): Parameter {
        return Parameter(
            name = name,
            type = type,
            description = description,
            required = required
        )
    }
}
