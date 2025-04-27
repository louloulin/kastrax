package ai.kastrax.core.agent.autonomy

import ai.kastrax.core.agent.Agent

/**
 * 将Agent转换为CreativeAgent
 */
fun Agent.toCreativeAgent(config: AutonomyConfig = AutonomyConfig()): CreativeAgent {
    return CreativeAgent(this, config)
}

/**
 * 将Agent转换为CreativeAgent，使用构建器配置
 */
fun Agent.toCreativeAgent(init: AgentAutonomyBuilder.() -> Unit): CreativeAgent {
    val builder = AgentAutonomyBuilder()
    builder.init()
    return CreativeAgent(this, builder.build())
}
