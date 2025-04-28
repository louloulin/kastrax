package ai.kastrax.a2a.adapter

import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.agent.A2AAgentImpl
import ai.kastrax.a2a.model.*
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.tools.Tool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

/**
 * A2A 代理适配器，用于将现有的 kastrax 代理转换为 A2A 代理
 */
class A2AAgentAdapter {
    /**
     * 将 kastrax 代理转换为 A2A 代理
     */
    fun adapt(agent: Agent, endpoint: String = "/a2a/v1/agents"): A2AAgent {
        val agentCard = createAgentCard(agent, endpoint)
        return A2AAgentImpl(agentCard, agent)
    }
    
    /**
     * 从 kastrax 代理创建 Agent Card
     */
    private fun createAgentCard(agent: Agent, endpoint: String): AgentCard {
        // 从代理工具创建能力
        val capabilities = agent.getTools().map { tool ->
            createCapability(tool)
        }
        
        return AgentCard(
            id = agent.name,
            name = agent.name,
            description = "KastraX Agent: ${agent.name}",
            version = "1.0.0",
            endpoint = "$endpoint/${agent.name}",
            capabilities = capabilities,
            authentication = Authentication(AuthType.API_KEY),
            metadata = mapOf(
                "agent_type" to "kastrax",
                "created_at" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * 从 kastrax 工具创建能力
     */
    private fun createCapability(tool: Tool): Capability {
        // 从工具输入 Schema 创建参数
        val parameters = createParameters(tool)
        
        return Capability(
            id = tool.id,
            name = tool.name,
            description = tool.description,
            parameters = parameters,
            returnType = "json",
            examples = emptyList() // 可以从工具示例创建能力示例
        )
    }
    
    /**
     * 从工具创建参数
     */
    private fun createParameters(tool: Tool): List<Parameter> {
        val parameters = mutableListOf<Parameter>()
        
        // 添加通用的提示参数
        parameters.add(
            Parameter(
                name = "prompt",
                type = "string",
                description = "The prompt to send to the agent",
                required = true
            )
        )
        
        // 从工具输入 Schema 创建参数
        val inputSchema = tool.inputSchema
        if (inputSchema is JsonObject) {
            for ((key, value) in inputSchema) {
                if (key == "properties" && value is JsonObject) {
                    for ((propName, propValue) in value) {
                        if (propValue is JsonObject) {
                            val type = propValue["type"]?.let {
                                if (it is JsonPrimitive) it.content else "string"
                            } ?: "string"
                            
                            val description = propValue["description"]?.let {
                                if (it is JsonPrimitive) it.content else ""
                            } ?: ""
                            
                            val required = propValue["required"]?.let {
                                if (it is JsonPrimitive) it.content.toBoolean() else false
                            } ?: false
                            
                            parameters.add(
                                Parameter(
                                    name = propName,
                                    type = type,
                                    description = description,
                                    required = required
                                )
                            )
                        }
                    }
                }
            }
        }
        
        return parameters
    }
}

/**
 * 获取代理的工具列表
 */
fun Agent.getTools(): List<Tool> {
    // 实际实现中，这里应该从 Agent 获取工具列表
    // 由于 Agent 接口可能没有直接暴露工具列表，这里返回一个空列表
    return emptyList()
}
