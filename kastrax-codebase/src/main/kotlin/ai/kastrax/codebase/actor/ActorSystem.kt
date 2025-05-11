package ai.kastrax.codebase.actor

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Actor 系统
 *
 * 简化版的 Actor 系统，用于演示目的
 */
class ActorSystem {
    /**
     * 启动 Actor 系统
     */
    fun start() {
        logger.info { "启动 Actor 系统" }
    }
    
    /**
     * 停止 Actor 系统
     */
    suspend fun stop() {
        logger.info { "停止 Actor 系统" }
    }
    
    companion object {
        /**
         * 创建 Actor 系统
         *
         * @return Actor 系统
         */
        fun create(): ActorSystem {
            return ActorSystem()
        }
    }
}
