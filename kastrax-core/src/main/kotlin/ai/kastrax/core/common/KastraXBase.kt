package ai.kastrax.core.common

/**
 * Base class for all KastraX components.
 * Provides common functionality like logging.
 *
 * @param component The component type (e.g., "AGENT", "TOOL", "WORKFLOW")
 * @param name The name of the component instance
 */
abstract class KastraXBase(
    protected open val component: String,
    val name: String
) {

    companion object {
        /**
         * Create a KastraXLogger instance suitable for the current environment.
         * This method can be overridden by platform-specific implementations.
         *
         * @param component The component type
         * @param name The name of the component instance
         * @return A KastraXLogger instance
         */
        fun createLogger(component: String, name: String): KastraXLogger {
            return KotlinKastraXLogger(component, name)
        }
    }
    /**
     * Logger interface for KastraX components.
     * This is a platform-independent logging facade.
     * Concrete implementations should provide their own logger implementation.
     */
    interface KastraXLogger {
        /**
         * Log a debug message.
         */
        fun debug(message: String)

        /**
         * Log a debug message with an exception.
         */
        fun debug(message: String, throwable: Throwable)

        /**
         * Log an info message.
         */
        fun info(message: String)

        /**
         * Log an info message with an exception.
         */
        fun info(message: String, throwable: Throwable)

        /**
         * Log a warning message.
         */
        fun warn(message: String)

        /**
         * Log a warning message with an exception.
         */
        fun warn(message: String, throwable: Throwable)

        /**
         * Log an error message.
         */
        fun error(message: String)

        /**
         * Log an error message with an exception.
         */
        fun error(message: String, throwable: Throwable)
    }

    /**
     * Extension functions for KastraXLogger to support lambda-style logging.
     */
    fun KastraXLogger.debug(messageSupplier: () -> String) {
        this.debug(messageSupplier())
    }

    fun KastraXLogger.debug(throwable: Throwable, messageSupplier: () -> String) {
        this.debug(messageSupplier(), throwable)
    }

    fun KastraXLogger.info(messageSupplier: () -> String) {
        this.info(messageSupplier())
    }

    fun KastraXLogger.info(throwable: Throwable, messageSupplier: () -> String) {
        this.info(messageSupplier(), throwable)
    }

    fun KastraXLogger.warn(messageSupplier: () -> String) {
        this.warn(messageSupplier())
    }

    fun KastraXLogger.warn(throwable: Throwable, messageSupplier: () -> String) {
        this.warn(messageSupplier(), throwable)
    }

    fun KastraXLogger.error(messageSupplier: () -> String) {
        this.error(messageSupplier())
    }

    fun KastraXLogger.error(throwable: Throwable, messageSupplier: () -> String) {
        this.error(messageSupplier(), throwable)
    }

    /**
     * Default logger implementation.
     * Subclasses can override this with their own implementation.
     */
    protected open val logger: KastraXLogger = createLogger(component, name)

    /**
     * Returns a string representation of this component.
     */
    override fun toString(): String = "$component:$name"
}
