package actor.proto.plugin.persistence

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 内存持久化提供者
 * 用于测试和开发，数据存储在内存中
 */
class InMemoryPersistenceProvider : PersistenceProvider {
    private val events = ConcurrentHashMap<String, MutableList<Any>>()
    private val snapshots = ConcurrentHashMap<String, Pair<Any, Long>>()
    
    override fun getEvents(actorName: String, eventIndex: Long): List<Any> {
        val actorEvents = events[actorName] ?: return emptyList()
        return actorEvents.drop(eventIndex.toInt())
    }
    
    override fun persistEvent(actorName: String, eventIndex: Long, event: Any): CompletableFuture<Long> {
        val actorEvents = events.getOrPut(actorName) { mutableListOf() }
        actorEvents.add(event)
        return CompletableFuture.completedFuture(eventIndex + 1)
    }
    
    override fun getSnapshot(actorName: String): Pair<Any?, Long> {
        return snapshots[actorName] ?: Pair(null, 0)
    }
    
    override fun persistSnapshot(actorName: String, eventIndex: Long, snapshot: Any): CompletableFuture<Long> {
        snapshots[actorName] = Pair(snapshot, eventIndex)
        return CompletableFuture.completedFuture(eventIndex)
    }
    
    override fun deleteEvents(actorName: String, inclusiveToIndex: Long): CompletableFuture<Long> {
        val actorEvents = events[actorName] ?: return CompletableFuture.completedFuture(0)
        
        if (inclusiveToIndex >= actorEvents.size) {
            actorEvents.clear()
        } else {
            for (i in 0..inclusiveToIndex.toInt()) {
                if (actorEvents.isNotEmpty()) {
                    actorEvents.removeAt(0)
                }
            }
        }
        
        return CompletableFuture.completedFuture(inclusiveToIndex)
    }
    
    override fun deleteSnapshots(actorName: String, inclusiveToIndex: Long): CompletableFuture<Long> {
        val snapshot = snapshots[actorName]
        if (snapshot != null && snapshot.second <= inclusiveToIndex) {
            snapshots.remove(actorName)
        }
        
        return CompletableFuture.completedFuture(inclusiveToIndex)
    }
    
    /**
     * 清除所有数据
     */
    fun clear() {
        events.clear()
        snapshots.clear()
    }
    
    companion object {
        /**
         * 创建内存持久化提供者
         * @return 内存持久化提供者实例
         */
        fun create(): InMemoryPersistenceProvider = InMemoryPersistenceProvider()
    }
}
