package ai.kastrax.core.agent.autonomy

import ai.kastrax.core.agent.Agent

/**
 * Agent自主性构建器
 */
class AgentAutonomyBuilder {
    private var mode: AutonomyMode = AutonomyMode.REACTIVE
    private var level: AutonomyLevel = AutonomyLevel.MEDIUM
    private var creativityLevel: Double = 0.5
    private var explorationRate: Double = 0.3
    private var learningRate: Double = 0.2
    private var maxGoals: Int = 3
    private val enabledCapabilities = mutableSetOf<AutonomyCapability>()
    private val constraints = mutableListOf<String>()
    
    /**
     * 设置自主性模式
     */
    fun mode(mode: AutonomyMode) {
        this.mode = mode
    }
    
    /**
     * 设置自主性级别
     */
    fun level(level: AutonomyLevel) {
        this.level = level
    }
    
    /**
     * 设置创造力级别
     */
    fun creativityLevel(level: Double) {
        this.creativityLevel = level.coerceIn(0.0, 1.0)
    }
    
    /**
     * 设置探索率
     */
    fun explorationRate(rate: Double) {
        this.explorationRate = rate.coerceIn(0.0, 1.0)
    }
    
    /**
     * 设置学习率
     */
    fun learningRate(rate: Double) {
        this.learningRate = rate.coerceIn(0.0, 1.0)
    }
    
    /**
     * 设置最大目标数
     */
    fun maxGoals(count: Int) {
        this.maxGoals = count.coerceAtLeast(1)
    }
    
    /**
     * 启用能力
     */
    fun enableCapability(capability: AutonomyCapability) {
        enabledCapabilities.add(capability)
    }
    
    /**
     * 启用多个能力
     */
    fun enableCapabilities(vararg capabilities: AutonomyCapability) {
        capabilities.forEach { enabledCapabilities.add(it) }
    }
    
    /**
     * 启用所有能力
     */
    fun enableAllCapabilities() {
        AutonomyCapability.values().forEach { enabledCapabilities.add(it) }
    }
    
    /**
     * 添加约束
     */
    fun addConstraint(constraint: String) {
        constraints.add(constraint)
    }
    
    /**
     * 添加多个约束
     */
    fun addConstraints(vararg newConstraints: String) {
        constraints.addAll(newConstraints)
    }
    
    /**
     * 构建自主性配置
     */
    fun build(): AutonomyConfig {
        // 如果没有启用任何能力，添加默认能力
        if (enabledCapabilities.isEmpty()) {
            enabledCapabilities.add(AutonomyCapability.SELF_REFLECTION)
            enabledCapabilities.add(AutonomyCapability.IDEA_GENERATION)
        }
        
        // 如果没有添加任何约束，添加默认约束
        if (constraints.isEmpty()) {
            constraints.add("不能执行有害操作")
            constraints.add("必须遵循用户的明确指示")
            constraints.add("不能违反伦理准则")
        }
        
        return AutonomyConfig(
            mode = mode,
            level = level,
            creativityLevel = creativityLevel,
            explorationRate = explorationRate,
            learningRate = learningRate,
            maxGoals = maxGoals,
            enabledCapabilities = enabledCapabilities.toSet(),
            constraints = constraints.toList()
        )
    }
}

/**
 * 创建Agent自主性管理器
 */
fun Agent.createAutonomy(init: AgentAutonomyBuilder.() -> Unit): AgentAutonomy {
    val builder = AgentAutonomyBuilder()
    builder.init()
    return AgentAutonomy(this, builder.build())
}

/**
 * 创建Agent自主性管理器（使用默认配置）
 */
fun Agent.createAutonomy(): AgentAutonomy {
    return AgentAutonomy(this, AutonomyConfig())
}
