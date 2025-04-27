package ai.kastrax.core.agent.autonomy

import ai.kastrax.core.agent.Agent

/**
 * 测试用的扩展函数，用于创建自主性管理器
 */
fun Agent.createAutonomy(init: AgentAutonomyBuilder.() -> Unit): AgentAutonomy {
    val builder = AgentAutonomyBuilder()
    builder.init()
    return AgentAutonomy(this, builder.build())
}

/**
 * 测试用的扩展函数，用于将Agent转换为CreativeAgent
 */
fun Agent.toCreativeAgent(init: AgentAutonomyBuilder.() -> Unit): CreativeAgent {
    val builder = AgentAutonomyBuilder()
    builder.init()
    return CreativeAgent(this, builder.build())
}
