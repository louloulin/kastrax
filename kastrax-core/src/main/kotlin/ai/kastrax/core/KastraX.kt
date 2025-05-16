package ai.kastrax.core

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.common.KastraXBase

/**
 * Main entry point for the KastraX framework.
 * Manages agents and other components.
 *
 * @property agents Map of registered agents
 */
class KastraX(
    private val agents: Map<String, Agent> = emptyMap()
) : KastraXBase(component = "KASTRAX", name = "kastrax") {

    init {
        logger.info("Initializing KastraX v${getVersion()}")

        // Register agents
        for ((name, agent) in agents) {
            logger.debug("Registered agent: $name")
        }
    }

    /**
     * Get an agent by ID.
     *
     * @param agentId The agent ID
     * @return The agent instance
     * @throws IllegalArgumentException if the agent is not found
     */
    fun getAgent(agentId: String): Agent {
        return agents[agentId] ?: throw IllegalArgumentException("Agent not found: $agentId")
    }

    /**
     * Get all registered agents.
     *
     * @return Map of agent IDs to agent instances
     */
    fun getAgents(): Map<String, Agent> {
        return agents
    }

    companion object {
        /**
         * Get the KastraX version.
         *
         * @return Version string
         */
        fun getVersion(): String {
            return "0.1.0"
        }
    }
}

/**
 * Builder for creating KastraX instances.
 */
class KastraXBuilder {
    private val agents = mutableMapOf<String, Agent>()

    /**
     * Add an agent.
     *
     * @param name Agent name/ID
     * @param agent Agent instance
     */
    fun agent(name: String, agent: Agent) {
        agents[name] = agent
    }

    /**
     * Build the KastraX instance.
     *
     * @return KastraX instance
     */
    fun build(): KastraX {
        return KastraX(agents)
    }
}

/**
 * DSL function for creating a KastraX instance.
 *
 * @param init Builder initialization block
 * @return KastraX instance
 */
fun kastrax(init: KastraXBuilder.() -> Unit): KastraX {
    val builder = KastraXBuilder()
    builder.init()
    return builder.build()
}
