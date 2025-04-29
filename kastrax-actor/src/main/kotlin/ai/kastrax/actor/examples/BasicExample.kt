package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import actor.proto.fromProducer
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse as CoreAgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.llm.LlmToolCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.minutes

/**
 * 基本使用示例
 */
fun main() = runBlocking {
    // 创建 Actor 系统
    val system = ActorSystem("kastrax-system")

    // 创建模拟 Agent
    val mockAgent = MockAgent()

    // 创建 Actor
    val props = fromProducer { ai.kastrax.actor.KastraxActor(mockAgent) }
    val agentPid = system.root.spawn(props)

    // 发送消息
    system.root.send(agentPid, AgentRequest("你能帮我计算 2 + 2 吗？"))

    // 请求-响应模式
    val response = system.root.requestAwait<AgentResponse>(agentPid, AgentRequest("巴黎的人口是多少？"))
    println("回答: ${response.text}")

    // 关闭系统
    system.shutdown()
}

/**
 * 模拟 Agent 实现，用于测试
 */
class MockAgent : Agent {
    override val name: String = "MockAgent"
    override val versionManager = null

    override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
        return CoreAgentResponse(
            text = "这是对“$prompt”的模拟响应",
            toolCalls = emptyList()
        )
    }

    override suspend fun generate(messages: List<ai.kastrax.core.llm.LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
        return generate(messages.lastOrNull()?.content ?: "", options)
    }

    override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
        return CoreAgentResponse(
            text = "这是对“$prompt”的模拟流式响应",
            textStream = flowOf("这", "是", "对", "“$prompt”", "的", "模拟", "流式", "响应")
        )
    }

    override suspend fun reset() {
        // 什么也不做
    }

    override suspend fun getState() = null

    override suspend fun updateState(status: ai.kastrax.core.agent.AgentStatus) = null

    override suspend fun createSession(title: String?, resourceId: String?, metadata: Map<String, String>) = null

    override suspend fun getSession(sessionId: String) = null

    override suspend fun createVersion(instructions: String, name: String?, description: String?, metadata: Map<String, String>, activateImmediately: Boolean) = null

    override suspend fun getVersions(limit: Int, offset: Int) = emptyList<ai.kastrax.core.agent.version.AgentVersion>()

    override suspend fun getActiveVersion() = null

    override suspend fun activateVersion(versionId: String) = null

    override suspend fun rollbackToVersion(versionId: String) = null

    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        return null
    }
}
