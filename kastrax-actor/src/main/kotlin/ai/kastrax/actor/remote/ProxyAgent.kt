package ai.kastrax.actor.remote

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.llm.LlmMessage

/**
 * 代理 Agent 类，用于包装现有的 Agent 实例
 *
 * @property delegate 被代理的 Agent 实例
 */
class ProxyAgent(private val delegate: Agent) : Agent {
    override val name: String = delegate.name
    override val versionManager: AgentVersionManager? = delegate.versionManager

    override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
        return delegate.generate(messages, options)
    }

    override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse {
        return delegate.generate(prompt, options)
    }

    override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
        return delegate.stream(prompt, options)
    }

    override suspend fun reset() {
        delegate.reset()
    }

    override suspend fun getState(): AgentState? {
        return delegate.getState()
    }

    override suspend fun updateState(status: AgentStatus): AgentState? {
        return delegate.updateState(status)
    }

    override suspend fun createSession(title: String?, resourceId: String?, metadata: Map<String, String>): SessionInfo? {
        return delegate.createSession(title, resourceId, metadata)
    }

    override suspend fun getSession(sessionId: String): SessionInfo? {
        return delegate.getSession(sessionId)
    }

    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        return delegate.getSessionMessages(sessionId, limit)
    }

    override suspend fun createVersion(
        instructions: String,
        name: String?,
        description: String?,
        metadata: Map<String, String>,
        activateImmediately: Boolean
    ): AgentVersion? {
        return delegate.createVersion(instructions, name, description, metadata, activateImmediately)
    }

    override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
        return delegate.getVersions(limit, offset)
    }

    override suspend fun getActiveVersion(): AgentVersion? {
        return delegate.getActiveVersion()
    }

    override suspend fun activateVersion(versionId: String): AgentVersion? {
        return delegate.activateVersion(versionId)
    }

    override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
        return delegate.rollbackToVersion(versionId)
    }
}
