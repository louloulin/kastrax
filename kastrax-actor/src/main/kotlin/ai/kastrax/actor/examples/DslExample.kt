package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import ai.kastrax.actor.*
import ai.kastrax.actor.network.*
import ai.kastrax.actor.network.protocols.*
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.llm.LlmMessage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.time.Duration

/**
 * DSL 使用示例
 * 
 * 本示例展示了如何使用 kastrax-actor 模块提供的 DSL 功能，包括：
 * 1. Actor 化 Agent DSL
 * 2. Agent 网络 DSL
 * 3. 消息传递 DSL
 */
object DslExample {

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建 Actor 系统
        val system = ActorSystem("dsl-example")

        try {
            // 1. Actor 化 Agent DSL 示例
            actorAgentDslExample(system)

            // 2. Agent 网络 DSL 示例
            agentNetworkDslExample(system)

            // 3. 消息传递 DSL 示例
            messagePassingDslExample(system)
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }

    /**
     * Actor 化 Agent DSL 示例
     */
    private suspend fun actorAgentDslExample(system: ActorSystem) {
        println("=== Actor 化 Agent DSL 示例 ===")

        // 使用 actorAgent DSL 创建 Actor 化的 Agent
        val agentPid = system.actorAgent {
            // 配置 Agent 部分
            agent {
                // 设置 Agent 名称
                agentBuilder.name = "助手"
                // 设置 Agent 指令
                agentBuilder.instructions = "你是一个有帮助的助手。"
            }

            // 配置 Actor 部分
            actor {
                // 设置监督策略
                oneForOneStrategy {
                    maxRetries = 3
                    withinTimeRange = Duration.ofMinutes(1)
                }
                // 设置邮箱类型
                unboundedMailbox()
            }
        }

        println("创建了 Actor 化的 Agent，PID: $agentPid")
        println()
    }

    /**
     * Agent 网络 DSL 示例
     */
    private suspend fun agentNetworkDslExample(system: ActorSystem) {
        println("=== Agent 网络 DSL 示例 ===")

        // 使用 agentNetwork DSL 创建 Agent 网络
        val network = system.agentNetwork {
            // 设置协调者
            coordinator {
                // 配置 Agent 部分
                agent {
                    agentBuilder.name = "协调者"
                    agentBuilder.instructions = "你是一个协调多个专家的协调者。"
                }

                // 配置 Actor 部分
                actor {
                    oneForOneStrategy {
                        maxRetries = 5
                    }
                }
            }

            // 添加专家 Agent
            agent("researcher") {
                agent {
                    agentBuilder.name = "研究员"
                    agentBuilder.instructions = "你是一个专业的研究员。"
                }
            }

            agent("writer") {
                agent {
                    agentBuilder.name = "作家"
                    agentBuilder.instructions = "你是一个专业的内容创作者。"
                }
            }

            agent("critic") {
                agent {
                    agentBuilder.name = "评论家"
                    agentBuilder.instructions = "你是一个专业的评论家。"
                }
            }
        }

        // 发送消息给协调者
        network.sendToCoordinator(AgentRequest("我需要一份关于气候变化的研究报告"))

        // 发送消息给特定 Agent
        network.send("researcher", AgentRequest("收集气候变化的最新数据"))

        // 请求-响应模式
        val response = network.ask("writer", AgentRequest("写一篇关于气候变化的文章"))
        println("作家的回应: ${(response as AgentResponse).text}")

        // 广播消息
        network.broadcast(AgentRequest("项目截止日期是下周五"))

        println()
    }

    /**
     * 消息传递 DSL 示例
     */
    private suspend fun messagePassingDslExample(system: ActorSystem) {
        println("=== 消息传递 DSL 示例 ===")

        // 创建一个 Agent
        val mockAgent = MockAgent()

        // 创建 Actor
        val props = actor.proto.fromProducer { KastraxActor(mockAgent) }
        val agentPid = system.root.spawn(props)

        // 使用 sendMessage DSL 发送消息
        system.sendMessage(agentPid, "你好，我是用户")

        // 使用 askMessage DSL 发送请求并等待响应
        val response = system.askMessage(agentPid, "巴黎的人口是多少？")
        println("回答: $response")

        // 使用 streamMessage DSL 发送流式请求
        system.streamMessage(agentPid, "讲个故事") { chunk ->
            print(chunk)
        }

        println()
    }

    /**
     * 模拟 Agent 实现
     */
    private class MockAgent : Agent {
        override val name: String = "模拟助手"
        override val versionManager: AgentVersionManager? = null

        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }

        override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse {
            // 模拟响应
            val sender = options.metadata?.get("sender") ?: "unknown"
            
            // 如果发送者是自己，返回简单响应以避免递归
            if (sender == name) {
                return AgentResponse(
                    text = "[自我处理] $prompt",
                    toolCalls = emptyList()
                )
            }
            
            return AgentResponse(
                text = "[$name] 回复: $prompt",
                toolCalls = emptyList()
            )
        }

        override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
            // 创建一个安全的选项对象，避免递归
            val safeOptions = AgentGenerateOptions(metadata = null)
            val response = generate(prompt, safeOptions)
            
            // 模拟流式响应
            val chunks = response.text.chunked(10)
            
            return AgentResponse(
                text = response.text,
                toolCalls = emptyList(),
                textStream = flowOf(*chunks.toTypedArray())
            )
        }

        override suspend fun reset() {
            // 不做任何操作
        }

        override suspend fun getState(): AgentState? {
            return null
        }

        override suspend fun updateState(status: AgentStatus): AgentState? {
            return null
        }

        override suspend fun createSession(title: String?, resourceId: String?, metadata: Map<String, String>): SessionInfo? {
            return null
        }

        override suspend fun getSession(sessionId: String): SessionInfo? {
            return null
        }

        override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
            return emptyList()
        }

        override suspend fun createVersion(
            instructions: String,
            name: String?,
            description: String?,
            metadata: Map<String, String>,
            activateImmediately: Boolean
        ): AgentVersion? {
            return null
        }

        override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
            return emptyList()
        }

        override suspend fun getActiveVersion(): AgentVersion? {
            return null
        }

        override suspend fun activateVersion(versionId: String): AgentVersion? {
            return null
        }

        override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
            return null
        }
    }
}
