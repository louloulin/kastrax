package ai.kastrax.a2a.examples

import ai.kastrax.a2a.a2a
import ai.kastrax.a2a.dsl.a2aAgent
import ai.kastrax.a2a.model.AuthType
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.model.DeepSeekModel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * A2A 示例
 */
fun main() = runBlocking {
    // 创建 kastrax 代理
    val assistantAgent = agent {
        name = "助手代理"
        instructions = """
            你是一个有用的助手，可以回答用户的问题并使用工具来获取信息。
            始终以友好、专业的方式回答，并提供准确的信息。
        """.trimIndent()
        
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "sk-85e83081df28490b9ae63188f0cb4f79")
            timeout(120)
            temperature(0.7)
            maxTokens(1000)
        }
    }
    
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
