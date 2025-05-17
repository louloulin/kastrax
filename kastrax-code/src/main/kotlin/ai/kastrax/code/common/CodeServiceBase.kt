package ai.kastrax.code.common

/**
 * 代码服务基类
 *
 * 为代码相关服务提供基础功能
 */
class CodeServiceBase(
    component: String,
    name: String
) : KastraXCodeBase(component, name) {
    /**
     * 获取日志记录器
     */
    fun getLoggerInstance(): KastraXLogger {
        return logger
    }
}
