package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryManager

/**
 * 记忆管理器工厂，用于创建不同类型的记忆管理器实现。
 */
object MemoryManagerFactory {
    /**
     * 创建内存中的记忆管理器。
     *
     * @return 内存中的记忆管理器
     */
    fun createInMemoryManager(): MemoryManager {
        return MemoryManagerImpl(InMemoryStorage())
    }
    
    /**
     * 创建基于SQLite的记忆管理器。
     *
     * @param dbPath 数据库文件路径
     * @return 基于SQLite的记忆管理器
     */
    fun createSQLiteManager(dbPath: String): MemoryManager {
        val storage = SQLiteMemoryStorage(dbPath)
        return MemoryManagerImpl(storage)
    }
    
    /**
     * 从现有的存储创建记忆管理器。
     *
     * @param storage 存储实现
     * @return 记忆管理器
     */
    fun createManager(storage: MemoryStorage): MemoryManager {
        return MemoryManagerImpl(storage)
    }
}

/**
 * 创建内存中的记忆管理器的DSL函数。
 */
fun inMemoryManager(): MemoryManager {
    return MemoryManagerFactory.createInMemoryManager()
}

/**
 * 从现有的存储创建记忆管理器的DSL函数。
 */
fun memoryManager(storage: MemoryStorage): MemoryManager {
    return MemoryManagerFactory.createManager(storage)
}
