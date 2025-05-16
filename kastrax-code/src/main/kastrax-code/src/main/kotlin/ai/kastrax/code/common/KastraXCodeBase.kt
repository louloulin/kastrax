package ai.kastrax.code.common

import com.intellij.openapi.diagnostic.Logger

/**
 * KastraX代码基类
 *
 * 所有KastraX代码组件的基类，提供通用功能
 *
 * @param component 组件类型（例如，"AGENT"，"TOOL"，"SERVICE"）
 */
abstract class KastraXCodeBase(
    protected val component: String
) {
    /**
     * 日志记录器
     */
    protected val logger: Logger = Logger.getInstance("$component:${this.javaClass.simpleName}")
    
    /**
     * 返回此组件的字符串表示形式
     */
    override fun toString(): String = "$component:${this.javaClass.simpleName}"
}
