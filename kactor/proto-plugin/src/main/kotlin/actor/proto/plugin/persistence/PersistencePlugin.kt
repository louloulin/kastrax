package actor.proto.plugin.persistence

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.plugin.ActorDecoratorExtension
import actor.proto.plugin.ProtoPlugin
import actor.proto.plugin.PluginInterface
import actor.proto.plugin.ActorDecoratorPluginInterface
import org.pf4j.Extension
import org.pf4j.PluginWrapper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 持久化提供者接口
 * 用于存储和检索事件和快照
 */
interface PersistenceProvider {
    /**
     * 获取事件
     * @param actorName Actor名称
     * @param eventIndex 事件索引
     * @return 事件列表
     */
    fun getEvents(actorName: String, eventIndex: Long): List<Any>

    /**
     * 持久化事件
     * @param actorName Actor名称
     * @param eventIndex 事件索引
     * @param event 事件
     * @return 完成后的Future
     */
    fun persistEvent(actorName: String, eventIndex: Long, event: Any): CompletableFuture<Long>

    /**
     * 获取快照
     * @param actorName Actor名称
     * @return 快照和索引
     */
    fun getSnapshot(actorName: String): Pair<Any?, Long>

    /**
     * 持久化快照
     * @param actorName Actor名称
     * @param eventIndex 事件索引
     * @param snapshot 快照
     * @return 完成后的Future
     */
    fun persistSnapshot(actorName: String, eventIndex: Long, snapshot: Any): CompletableFuture<Long>

    /**
     * 删除事件
     * @param actorName Actor名称
     * @param inclusiveToIndex 包含的最大索引
     * @return 完成后的Future
     */
    fun deleteEvents(actorName: String, inclusiveToIndex: Long): CompletableFuture<Long>

    /**
     * 删除快照
     * @param actorName Actor名称
     * @param inclusiveToIndex 包含的最大索引
     * @return 完成后的Future
     */
    fun deleteSnapshots(actorName: String, inclusiveToIndex: Long): CompletableFuture<Long>
}

/**
 * 持久化接口
 * 提供给Actor使用的持久化操作
 */
interface Persistence {
    /**
     * 持久化事件
     * @param event 事件
     */
    suspend fun persist(event: Any)

    /**
     * 持久化快照
     * @param snapshot 快照
     */
    suspend fun persistSnapshot(snapshot: Any)

    /**
     * 删除事件
     * @param inclusiveToIndex 包含的最大索引
     */
    suspend fun deleteEvents(inclusiveToIndex: Long)

    /**
     * 删除快照
     * @param inclusiveToIndex 包含的最大索引
     */
    suspend fun deleteSnapshots(inclusiveToIndex: Long)

    /**
     * 获取当前事件索引
     * @return 事件索引
     */
    fun getEventIndex(): Long
}

/**
 * 持久化上下文
 * 实现持久化接口，管理Actor的持久化状态
 * @param provider 持久化提供者
 * @param actorName Actor名称
 */
class PersistenceContext(
    private val provider: PersistenceProvider,
    private val actorName: String
) : Persistence {
    private var eventIndex: Long = 0
    private var recovering = false

    /**
     * 初始化
     * @param actor 持久化Actor
     * @param context Actor上下文
     */
    suspend fun init(actor: PersistentActor, context: Context) {
        // 获取快照
        val (snapshot, index) = provider.getSnapshot(actorName)
        eventIndex = index

        if (snapshot != null) {
            // 恢复快照
            recovering = true
            actor.onRecoverSnapshot(context, snapshot)
            recovering = false
        }

        // 获取事件
        val events = provider.getEvents(actorName, eventIndex)
        if (events.isNotEmpty()) {
            recovering = true
            events.forEach { event ->
                actor.onRecover(context, event)
                eventIndex++
            }
            recovering = false
        }
    }

    override suspend fun persist(event: Any) {
        if (recovering) return

        provider.persistEvent(actorName, eventIndex, event).get()
        eventIndex++
    }

    override suspend fun persistSnapshot(snapshot: Any) {
        if (recovering) return

        provider.persistSnapshot(actorName, eventIndex, snapshot).get()
    }

    override suspend fun deleteEvents(inclusiveToIndex: Long) {
        if (recovering) return

        provider.deleteEvents(actorName, inclusiveToIndex).get()
    }

    override suspend fun deleteSnapshots(inclusiveToIndex: Long) {
        if (recovering) return

        provider.deleteSnapshots(actorName, inclusiveToIndex).get()
    }

    override fun getEventIndex(): Long = eventIndex
}

/**
 * 持久化Actor接口
 * 需要持久化的Actor必须实现此接口
 */
interface PersistentActor : Actor {
    /**
     * 恢复事件
     * @param context 上下文
     * @param event 事件
     */
    suspend fun onRecover(context: Context, event: Any)

    /**
     * 恢复快照
     * @param context 上下文
     * @param snapshot 快照
     */
    suspend fun onRecoverSnapshot(context: Context, snapshot: Any)

    /**
     * 持久化事件后的回调
     * @param context 上下文
     * @param event 事件
     */
    suspend fun onPersist(context: Context, event: Any)
}

/**
 * 持久化插件
 */
class PersistencePlugin(wrapper: PluginWrapper) : ProtoPlugin(wrapper) {
    private lateinit var persistenceManager: PersistenceManager

    override fun start() {
        persistenceManager = PersistenceManager()
        log.info("PersistencePlugin started")
    }

    override fun stop() {
        persistenceManager.shutdown()
        log.info("PersistencePlugin stopped")
    }

    override fun init(system: ActorSystem) {
        log.info("PersistencePlugin initialized with ActorSystem")
    }

    /**
     * 设置持久化提供者
     * @param provider 持久化提供者
     */
    fun setProvider(provider: PersistenceProvider) {
        persistenceManager.setProvider(provider)
    }

    /**
     * 持久化管理器
     * 管理持久化上下文和提供者
     */
    class PersistenceManager {
        private val persistenceContexts = ConcurrentHashMap<String, PersistenceContext>()
        private var provider: PersistenceProvider? = null

        /**
         * 设置持久化提供者
         * @param persistenceProvider 持久化提供者
         */
        fun setProvider(persistenceProvider: PersistenceProvider) {
            provider = persistenceProvider
        }

        /**
         * 获取持久化上下文
         * @param actorName Actor名称
         * @return 持久化上下文
         */
        fun getPersistenceContext(actorName: String): PersistenceContext? {
            val currentProvider = provider ?: return null

            return persistenceContexts.getOrPut(actorName) {
                PersistenceContext(currentProvider, actorName)
            }
        }

        /**
         * 关闭
         */
        fun shutdown() {
            persistenceContexts.clear()
            provider = null
        }
    }

    /**
     * 持久化Actor装饰器扩展
     */
    @Extension
    class PersistentActorDecorator : ActorDecoratorExtension {
        override fun id(): String = "persistent-actor-decorator"

        override fun version(): String = "1.0.0"

        override fun init(system: ActorSystem) {
            // 默认实现为空
        }

        override fun shutdown() {
            // 默认实现为空
        }
        override fun decorateActor(actor: Actor): Actor {
            if (actor is PersistentActor) {
                return PersistentActorWrapper(actor as PersistentActor)
            }
            return actor
        }
    }

    /**
     * 持久化Actor包装器
     * @param actor 原始持久化Actor
     */
    class PersistentActorWrapper(private val actor: PersistentActor) : Actor {
        private var persistenceContext: PersistenceContext? = null

        override suspend fun Context.receive(msg: Any) {
            // 获取插件和持久化上下文
            val systemField = javaClass.getDeclaredField("system")
            systemField.isAccessible = true
            val system = systemField.get(this) as ActorSystem
            val plugin = system.getPlugin("persistence") as? PersistencePlugin
            val manager = plugin?.persistenceManager

            // 初始化持久化上下文
            if (persistenceContext == null) {
                // 使用反射获取self.id
                val selfField = self.javaClass.getDeclaredField("id")
                selfField.isAccessible = true
                val id = selfField.get(self) as String
                persistenceContext = manager?.getPersistenceContext(id)
                persistenceContext?.init(actor as PersistentActor, this)
            }

            // 调用原始Actor的receive方法
            with(actor) {
                this@receive.receive(msg)
            }
        }
    }
}

/**
 * 配置持久化插件
 * @param provider 持久化提供者
 */
fun ActorSystem.configurePersistence(provider: PersistenceProvider) {
    val plugin = getPlugin("persistence") as? PersistencePlugin
    plugin?.setProvider(provider)
}
