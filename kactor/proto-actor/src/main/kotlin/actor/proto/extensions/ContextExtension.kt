package actor.proto.extensions

import java.util.concurrent.atomic.AtomicInteger

/**
 * 上下文扩展ID，用于标识不同的上下文扩展
 */
class ContextExtensionID private constructor(val id: Int) {
    companion object {
        private val currentId = AtomicInteger(0)
        
        /**
         * 生成下一个上下文扩展ID
         * @return 新的上下文扩展ID
         */
        fun next(): ContextExtensionID {
            val id = currentId.incrementAndGet()
            return ContextExtensionID(id)
        }
    }
}

/**
 * 上下文扩展接口，所有上下文扩展必须实现此接口
 */
interface ContextExtension {
    /**
     * 获取扩展ID
     * @return 扩展ID
     */
    fun extensionId(): ContextExtensionID
}

/**
 * 上下文扩展容器，用于存储和管理上下文扩展
 */
class ContextExtensions {
    private val extensions = mutableMapOf<ContextExtensionID, ContextExtension>()
    
    /**
     * 获取指定ID的扩展
     * @param id 扩展ID
     * @return 扩展实例，如果不存在则返回null
     */
    fun get(id: ContextExtensionID): ContextExtension? {
        return extensions[id]
    }
    
    /**
     * 设置扩展
     * @param extension 扩展实例
     */
    fun set(extension: ContextExtension) {
        extensions[extension.extensionId()] = extension
    }
}
