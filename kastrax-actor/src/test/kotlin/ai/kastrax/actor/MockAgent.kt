package ai.kastrax.actor

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmToolCall
import kotlinx.coroutines.flow.flowOf

/**
 * Mock Agent implementation for testing
 */
class MockAgent : Agent {
    override val name: String = "Mock Agent"
    // No instructions property in Agent interface
    override val versionManager = null

    override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse {
        println("MockAgent received generate request with prompt: $prompt and options: $options")
        return AgentResponse("测试回复", emptyList()).also {
            println("MockAgent returning response: $it")
        }
    }

    override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
        return generate(messages.lastOrNull()?.content ?: "", options)
    }

    override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
        return AgentResponse("测试流式回复", emptyList(), emptyMap(), null, null, flowOf("测试", "流式", "回复"))
    }

    // This method is not in the Agent interface

    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        return null
    }

    override suspend fun reset() {}

    override suspend fun getState() = null

    override suspend fun updateState(status: ai.kastrax.core.agent.AgentStatus) = null

    override suspend fun createSession(title: String?, resourceId: String?, metadata: Map<String, String>) = null

    override suspend fun getSession(sessionId: String) = null

    override suspend fun createVersion(instructions: String, name: String?, description: String?, metadata: Map<String, String>, activateImmediately: Boolean) = null

    override suspend fun getVersions(limit: Int, offset: Int) = emptyList<ai.kastrax.core.agent.version.AgentVersion>()

    override suspend fun getActiveVersion() = null

    override suspend fun activateVersion(versionId: String) = null

    override suspend fun rollbackToVersion(versionId: String) = null
}
