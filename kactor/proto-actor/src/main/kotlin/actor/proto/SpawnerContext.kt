package actor.proto

/**
 * SpawnerContext 是用于创建 Actor 的上下文接口
 * 提供了创建 Actor 所需的基本信息
 */
interface SpawnerContext {
    /**
     * 获取当前 Actor 的 PID
     * @return 当前 Actor 的 PID
     */
    val self: PID

    /**
     * 获取当前 Actor
     * @return 当前 Actor
     */
    val actor: Actor?

    /**
     * 创建一个新的 Actor
     * @param props Actor 的属性
     * @return 新 Actor 的 PID
     */
    fun spawn(props: Props): PID

    /**
     * 创建一个带前缀的新 Actor
     * @param props Actor 的属性
     * @param prefix Actor 名称的前缀
     * @return 新 Actor 的 PID
     */
    fun spawnPrefix(props: Props, prefix: String): PID

    /**
     * 创建一个指定名称的新 Actor
     * @param props Actor 的属性
     * @param name Actor 的名称
     * @return 新 Actor 的 PID
     */
    fun spawnNamed(props: Props, name: String): PID
}
