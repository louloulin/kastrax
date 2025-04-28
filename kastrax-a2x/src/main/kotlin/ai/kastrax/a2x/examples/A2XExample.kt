package ai.kastrax.a2x.examples

import ai.kastrax.a2a.a2a
import ai.kastrax.a2a.dsl.a2aAgent
import ai.kastrax.a2x.a2x
import ai.kastrax.a2x.model.EntityType
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * A2X 示例
 */
fun main() = runBlocking {
    // 创建模拟的 kastrax 代理
    val assistantAgent = mockk<Agent>()

    // 配置模拟代理的行为
    coEvery { assistantAgent.name } returns "助手代理"
    coEvery { assistantAgent.generate(any<String>()) } returns AgentResponse(
        text = "这是一个模拟响应",
        toolCalls = emptyList()
    )

    // 创建 A2X 实例
    val a2xInstance = a2x {
        // 注册 kastrax 代理
        agent(assistantAgent)

        // 注册 A2A 代理
        val a2aInstance = a2a {
            // 创建 A2A 代理
            val dataAnalysisAgent = a2aAgent {
                id = "data-analysis-agent"
                name = "数据分析代理"
                description = "提供数据分析和可视化能力的代理"
                baseAgent = assistantAgent

                capability {
                    id = "data_analysis"
                    name = "数据分析"
                    description = "分析提供的数据集并返回统计结果"

                    parameter {
                        name = "dataset_url"
                        type = "string"
                        description = "数据集URL"
                        required = true
                    }

                    parameter {
                        name = "analysis_type"
                        type = "string"
                        description = "分析类型"
                        required = true
                    }

                    returnType = "json"
                }
            }

            // 启动代理
            dataAnalysisAgent.start()

            // 返回代理
            dataAnalysisAgent
        }

        // 注册 A2A 代理
        a2aAgent(a2aInstance)

        // 配置服务器
        server {
            port = 8080
            enableCors = true
        }

        // 添加服务器到发现服务
        discovery("http://localhost:8080")
    }

    // 获取系统实体
    val systemEntities = a2xInstance.getEntitiesByType(EntityType.SYSTEM)
    println("系统实体: ${systemEntities.map { it.getEntityCard().name }}")

    // 获取代理实体
    val agentEntities = a2xInstance.getEntitiesByType(EntityType.AGENT)
    println("代理实体: ${agentEntities.map { it.getEntityCard().name }}")

    // 获取数据分析代理
    val dataAnalysisEntity = a2xInstance.getEntity("data-analysis-agent")
    if (dataAnalysisEntity != null) {
        println("数据分析代理能力: ${dataAnalysisEntity.getCapabilities().map { it.name }}")

        // 调用数据分析能力
        val request = ai.kastrax.a2x.model.InvokeRequest(
            id = "test-request",
            source = a2xInstance.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2xInstance.createLocalEntityReference("data-analysis-agent", EntityType.AGENT),
            capabilityId = "data_analysis",
            parameters = mapOf(
                "dataset_url" to JsonPrimitive("https://example.com/data.csv"),
                "analysis_type" to JsonPrimitive("trend")
            )
        )

        val response = dataAnalysisEntity.invoke(request)
        println("响应: ${response.result}")
    }

    // 获取系统实体
    val systemEntity = systemEntities.firstOrNull()
    if (systemEntity != null) {
        println("系统实体能力: ${systemEntity.getCapabilities().map { it.name }}")

        // 获取系统信息
        val request = ai.kastrax.a2x.model.InvokeRequest(
            id = "system-info-request",
            source = a2xInstance.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2xInstance.createLocalEntityReference(systemEntity.getEntityCard().id, EntityType.SYSTEM),
            capabilityId = "system_info",
            parameters = emptyMap()
        )

        val response = systemEntity.invoke(request)
        println("系统信息: ${response.result}")
    }

    // 发送事件
    val event = ai.kastrax.a2x.model.EventMessage(
        id = "test-event",
        source = a2xInstance.createLocalEntityReference("test-client", EntityType.AGENT),
        target = a2xInstance.createLocalEntityReference("*", EntityType.AGENT),
        eventType = "test_event",
        data = buildJsonObject {
            put("message", JsonPrimitive("这是一个测试事件"))
        }
    )

    a2xInstance.sendEvent(event)
    println("事件已发送")

    // 等待一段时间
    delay(2000)

    // 停止服务器
    a2xInstance.stopServer()
}
