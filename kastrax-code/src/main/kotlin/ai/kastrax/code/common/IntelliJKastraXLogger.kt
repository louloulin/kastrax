package ai.kastrax.code.common

import ai.kastrax.core.common.KastraXBase
import com.intellij.openapi.diagnostic.Logger

/**
 * KastraXLogger implementation using IntelliJ IDEA's Logger.
 * This implementation is suitable for IntelliJ IDEA plugin environment.
 */
class IntelliJKastraXLogger(
    private val component: String,
    private val name: String
) : KastraXBase.KastraXLogger {
    private val logger: Logger = Logger.getInstance("$component:$name")

    override fun debug(message: String) {
        logger.debug(message)
    }

    override fun debug(message: String, throwable: Throwable) {
        logger.debug(message, throwable)
    }

    override fun info(message: String) {
        logger.info(message)
    }

    override fun info(message: String, throwable: Throwable) {
        logger.info(message, throwable)
    }

    override fun warn(message: String) {
        logger.warn(message)
    }

    override fun warn(message: String, throwable: Throwable) {
        logger.warn(message, throwable)
    }

    override fun error(message: String) {
        logger.error(message)
    }

    override fun error(message: String, throwable: Throwable) {
        logger.error(message, throwable)
    }
}
