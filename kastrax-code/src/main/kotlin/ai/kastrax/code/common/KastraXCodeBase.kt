package ai.kastrax.code.common

import io.github.oshai.kotlinlogging.KLogger

/**
 * KastraX Code 基础类
 *
 * 所有 KastraX Code 组件的基类
 */
abstract class KastraXCodeBase(
    /**
     * 组件名称
     */
    val component: String,
    
    /**
     * 组件实例名称
     */
    val name: String = component
) {
    /**
     * 日志记录器
     */
    abstract val logger: KLogger
    
    /**
     * 获取组件标识
     *
     * @return 组件标识
     */
    fun getComponentId(): String {
        return "$component:$name"
    }
}
