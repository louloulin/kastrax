package ai.kastrax.code.common

import ai.kastrax.core.common.KastraXBase
import com.intellij.openapi.diagnostic.Logger

/**
 * KastraX代码基类
 *
 * 所有KastraX代码组件的基类，提供通用功能
 *
 * @param component 组件类型（例如，"AGENT"，"TOOL"，"SERVICE"）
 */
abstract class KastraXCodeBase(
    component: String
) : KastraXBase(component, "CODE") {
    /**
     * IntelliJ平台的日志记录器
     */
    private val intellijLogger: Logger = Logger.getInstance("$component:${this.javaClass.simpleName}")

    /**
     * 实现KastraXLogger接口，使用IntelliJ平台的Logger
     */
    override val logger: KastraXLogger = object : KastraXLogger {
        override fun debug(message: String) {
            intellijLogger.debug(message)
        }

        override fun debug(message: String, throwable: Throwable) {
            intellijLogger.debug(message, throwable)
        }

        override fun info(message: String) {
            intellijLogger.info(message)
        }

        override fun info(message: String, throwable: Throwable) {
            intellijLogger.info(message, throwable)
        }

        override fun warn(message: String) {
            intellijLogger.warn(message)
        }

        override fun warn(message: String, throwable: Throwable) {
            intellijLogger.warn(message, throwable)
        }

        override fun error(message: String) {
            intellijLogger.error(message)
        }

        override fun error(message: String, throwable: Throwable) {
            intellijLogger.error(message, throwable)
        }
    }
}
