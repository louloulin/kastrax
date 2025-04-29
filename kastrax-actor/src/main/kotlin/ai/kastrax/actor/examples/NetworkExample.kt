package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.agentNetwork
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking

/**
 * Agent 网络示例
 */
fun main() = runBlocking {
    // 创建 Actor 系统
    val system = ActorSystem("kastrax-system")
    
    // 创建 Agent 网络
    val network = system.agentNetwork {
        // 创建协调者
        coordinator {
            agent {
                name = "协调者"
                instructions = "你是一个协调多个专家的协调者。"
                model = ai.kastrax.integrations.deepseek.deepSeek {
                    model(DeepSeekModel.DEEPSEEK_CHAT)
                    apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
                }
            }
            actor {
                oneForOneStrategy {
                    maxRetries = 5
                }
            }
        }
        
        // 创建专家 Agent
        agent("researcher") {
            agent {
                name = "研究员"
                instructions = "你是一个专业的研究员。"
                model = ai.kastrax.integrations.deepseek.deepSeek {
                    model(DeepSeekModel.DEEPSEEK_CHAT)
                    apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
                }
            }
        }
        
        agent("analyst") {
            agent {
                name = "分析师"
                instructions = "你是一个数据分析专家。"
                model = ai.kastrax.integrations.deepseek.deepSeek {
                    model(DeepSeekModel.DEEPSEEK_CHAT)
                    apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
                }
            }
        }
        
        agent("writer") {
            agent {
                name = "作家"
                instructions = "你是一个专业的内容创作者。"
                model = ai.kastrax.integrations.deepseek.deepSeek {
                    model(DeepSeekModel.DEEPSEEK_CHAT)
                    apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
                }
            }
        }
    }
    
    // 发送消息给协调者
    network.sendToCoordinator(AgentRequest("我需要一份关于气候变化的研究报告"))
    
    // 发送消息给特定 Agent
    network.send("researcher", AgentRequest("收集气候变化的最新数据"))
    
    // 请求-响应模式
    val response = network.ask("analyst", AgentRequest("分析这些气候数据的趋势"))
    println("分析结果: ${(response as AgentResponse).text}")
    
    // 广播消息
    network.broadcast(AgentRequest("项目截止日期是下周五"))
    
    // 关闭系统
    system.shutdown()
}
