package actor.proto

/**
 * 测试用的Props类
 */
class Props {
    var producer: (() -> Actor)? = null
    
    /**
     * 添加停止钩子
     * @param hook 停止钩子
     * @return Props实例
     */
    fun withOnStop(hook: (Context) -> Unit): Props {
        // 测试用的空实现
        return this
    }
}
