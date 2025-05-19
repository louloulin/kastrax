package ai.kastrax.runtime.coroutines

/**
 * kastrax调度器工厂
 *
 * 为不同类型的任务选择合适的调度器
 */
object KastraxDispatcherFactory {
    /**
     * 任务类型
     */
    enum class TaskType {
        /**
         * IO密集型任务，如文件操作、网络请求等
         */
        IO,

        /**
         * 计算密集型任务，如复杂计算、数据处理等
         */
        COMPUTE,

        /**
         * UI相关任务，如更新UI、处理用户输入等
         */
        UI,

        /**
         * 默认任务类型，根据上下文自动选择合适的调度器
         */
        DEFAULT
    }

    /**
     * 获取适合指定任务类型的调度器
     *
     * @param taskType 任务类型
     * @return 调度器
     */
    fun getDispatcher(taskType: TaskType): KastraxDispatcher {
        val runtime = KastraxCoroutineGlobal.getRuntime()
        return when (taskType) {
            TaskType.IO -> runtime.ioDispatcher()
            TaskType.COMPUTE -> runtime.computeDispatcher()
            TaskType.UI -> runtime.uiDispatcher()
            TaskType.DEFAULT -> runtime.computeDispatcher()
        }
    }

    /**
     * 获取IO调度器
     *
     * @return IO调度器
     */
    fun io(): KastraxDispatcher {
        return getDispatcher(TaskType.IO)
    }

    /**
     * 获取计算调度器
     *
     * @return 计算调度器
     */
    fun compute(): KastraxDispatcher {
        return getDispatcher(TaskType.COMPUTE)
    }

    /**
     * 获取UI调度器
     *
     * @return UI调度器
     */
    fun ui(): KastraxDispatcher {
        return getDispatcher(TaskType.UI)
    }

    /**
     * 获取默认调度器
     *
     * @return 默认调度器
     */
    fun default(): KastraxDispatcher {
        return getDispatcher(TaskType.DEFAULT)
    }
}
