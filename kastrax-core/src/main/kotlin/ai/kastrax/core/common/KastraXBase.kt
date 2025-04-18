package ai.kastrax.core.common

import mu.KLogger
import mu.KotlinLogging

/**
 * Base class for all KastraX components.
 * Provides common functionality like logging.
 *
 * @param component The component type (e.g., "AGENT", "TOOL", "WORKFLOW")
 * @param name The name of the component instance
 */
abstract class KastraXBase(
    protected val component: String,
    val name: String
) {
    /**
     * Logger instance for this component.
     */
    protected val logger: KLogger = KotlinLogging.logger("$component:$name")
    
    /**
     * Returns a string representation of this component.
     */
    override fun toString(): String = "$component:$name"
}
