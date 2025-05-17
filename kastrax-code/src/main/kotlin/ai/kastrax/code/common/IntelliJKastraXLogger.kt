package ai.kastrax.code.common

import ai.kastrax.core.common.KastraXBase
import org.slf4j.LoggerFactory

/**
 * KastraXLogger implementation using IntelliJ IDEA's Logger.
 * This implementation is suitable for IntelliJ IDEA plugin environment.
 *
 * 注意：在测试环境中，会使用 SLF4J 的 Logger 作为后备，以避免依赖 IntelliJ IDEA 平台
 */
class IntelliJKastraXLogger(
    private val component: String,
    private val name: String
) : KastraXBase.KastraXLogger {
    // 使用 SLF4J 的 Logger 作为后备，以避免在测试环境中依赖 IntelliJ IDEA 平台
    private val logger = try {
        // 尝试使用 IntelliJ IDEA 的 Logger
        com.intellij.openapi.diagnostic.Logger.getInstance("${component ?: "KASTRAX_CODE"}:${name ?: "DEFAULT"}")
    } catch (e: Throwable) {
        // 如果失败，使用 SLF4J 的 Logger 作为后备
        null
    }

    // SLF4J 的 Logger 作为后备
    private val slf4jLogger = LoggerFactory.getLogger("${component ?: "KASTRAX_CODE"}:${name ?: "DEFAULT"}")

    override fun debug(message: String) {
        if (logger != null) {
            logger.debug(message)
        } else {
            slf4jLogger.debug(message)
        }
    }

    override fun debug(message: String, throwable: Throwable) {
        if (logger != null) {
            logger.debug(message, throwable)
        } else {
            slf4jLogger.debug(message, throwable)
        }
    }

    override fun info(message: String) {
        if (logger != null) {
            logger.info(message)
        } else {
            slf4jLogger.info(message)
        }
    }

    override fun info(message: String, throwable: Throwable) {
        if (logger != null) {
            logger.info(message, throwable)
        } else {
            slf4jLogger.info(message, throwable)
        }
    }

    override fun warn(message: String) {
        if (logger != null) {
            logger.warn(message)
        } else {
            slf4jLogger.warn(message)
        }
    }

    override fun warn(message: String, throwable: Throwable) {
        if (logger != null) {
            logger.warn(message, throwable)
        } else {
            slf4jLogger.warn(message, throwable)
        }
    }

    override fun error(message: String) {
        if (logger != null) {
            logger.error(message)
        } else {
            slf4jLogger.error(message)
        }
    }

    override fun error(message: String, throwable: Throwable) {
        if (logger != null) {
            logger.error(message, throwable)
        } else {
            slf4jLogger.error(message, throwable)
        }
    }
}
