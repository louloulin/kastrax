package ai.kastrax.a2a.examples

import ai.kastrax.a2a.a2a
import ai.kastrax.a2a.dsl.a2aAgent
import ai.kastrax.a2a.model.AuthType
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * A2A 示例
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

            example {
                input("dataset_url", "https://example.com/data.csv")
                input("analysis_type", "summary")
                output("mean", "42.5")
                output("median", "40.0")
                description = "计算数据集的基本统计信息"
            }
        }

        authentication {
            type = AuthType.API_KEY
        }
    }

    // 创建 A2A 实例
    val a2aInstance = a2a {
        // 注册 kastrax 代理
        agent(assistantAgent)

        // 注册 A2A 代理
        a2aAgent {
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

            authentication {
                type = AuthType.API_KEY
            }
        }

        // 配置服务器
        server {
            port = 8080
            enableCors = true
        }

        // 添加服务器到发现服务
        discovery("http://localhost:8080")
    }

    // 创建 A2A 客户端
    val client = a2aInstance.createClient("http://localhost:8080")

    // 获取代理卡片
    val agentCard = client.getAgentCard()
    println("Agent Card: $agentCard")

    // 获取代理能力
    val capabilities = client.getCapabilities("data-analysis-agent")
    println("Capabilities: $capabilities")

    // 调用代理能力
    val response = client.invoke(
        agentId = "data-analysis-agent",
        capabilityId = "data_analysis",
        parameters = mapOf(
            "prompt" to JsonPrimitive("分析这个数据集并告诉我主要趋势"),
            "dataset_url" to JsonPrimitive("https://example.com/data.csv"),
            "analysis_type" to JsonPrimitive("trend")
        )
    )
    println("Response: $response")

    // 关闭客户端
    client.close()

    // 停止服务器
    a2aInstance.stopServer()
}
