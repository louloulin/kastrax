package ai.kastrax.core.common

import mu.KLogger
import mu.KotlinLogging

/**
 * KastraXLogger implementation using Kotlin Logging.
 * This implementation is suitable for non-IntelliJ environments.
 */
class KotlinKastraXLogger(
    private val component: String,
    private val name: String
) : KastraXBase.KastraXLogger {
    private val logger: KLogger = KotlinLogging.logger("$component:$name")
    
    override fun debug(message: String) {
        logger.debug { message }
    }
    
    override fun debug(message: String, throwable: Throwable) {
        logger.debug(throwable) { message }
    }
    
    override fun info(message: String) {
        logger.info { message }
    }
    
    override fun info(message: String, throwable: Throwable) {
        logger.info(throwable) { message }
    }
    
    override fun warn(message: String) {
        logger.warn { message }
    }
    
    override fun warn(message: String, throwable: Throwable) {
        logger.warn(throwable) { message }
    }
    
    override fun error(message: String) {
        logger.error { message }
    }
    
    override fun error(message: String, throwable: Throwable) {
        logger.error(throwable) { message }
    }
}
