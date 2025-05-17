package ai.kastrax.code.common

import ai.kastrax.core.common.KastraXBase

/**
 * KastraX Code 基础类
 *
 * 所有 KastraX Code 组件的基类
 */
abstract class KastraXCodeBase(
    /**
     * 组件名称
     */
    protected val component: String,

    /**
     * 组件实例名称
     */
    name: String = component
) : KastraXBase(component, name) {

    /**
     * 创建 IntelliJ IDEA 平台的日志记录器
     */
    override val logger: KastraXLogger = IntelliJKastraXLogger(component, name)

    /**
     * 获取组件标识
     *
     * @return 组件标识
     */
    fun getComponentId(): String {
        return "$component:$name"
    }
}
