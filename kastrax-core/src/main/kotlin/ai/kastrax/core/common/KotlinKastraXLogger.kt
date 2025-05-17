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
    private val logger: KLogger = KotlinLogging.logger("${component.takeIf { it.isNotBlank() } ?: "KASTRAX"}:${name.takeIf { it.isNotBlank() } ?: "DEFAULT"}")

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

    // 添加支持lambda形式的日志方法
    fun debug(messageSupplier: () -> String) {
        logger.debug(messageSupplier)
    }

    fun debug(throwable: Throwable, messageSupplier: () -> String) {
        logger.debug(throwable, messageSupplier)
    }

    fun info(messageSupplier: () -> String) {
        logger.info(messageSupplier)
    }

    fun info(throwable: Throwable, messageSupplier: () -> String) {
        logger.info(throwable, messageSupplier)
    }

    fun warn(messageSupplier: () -> String) {
        logger.warn(messageSupplier)
    }

    fun warn(throwable: Throwable, messageSupplier: () -> String) {
        logger.warn(throwable, messageSupplier)
    }

    fun error(messageSupplier: () -> String) {
        logger.error(messageSupplier)
    }

    fun error(throwable: Throwable, messageSupplier: () -> String) {
        logger.error(throwable, messageSupplier)
    }
}
