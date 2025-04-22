# KastraX 缓存系统

## 1. 概述

KastraX 缓存系统是 KastraX 框架的核心组件之一，用于提高应用程序的性能和可扩展性。缓存系统支持内存缓存和分布式缓存（Redis），可以根据需要选择合适的缓存类型。

缓存系统的设计目标是提供一个统一的接口，使开发者能够轻松地使用不同类型的缓存，而无需了解每种缓存的具体实现细节。

## 2. 核心组件

### 2.1 缓存接口（Cache）

所有缓存实现都实现了 `Cache` 接口：

```kotlin
interface Cache<K, V> {
    val name: String
    
    suspend fun get(key: K): V?
    suspend fun set(key: K, value: V, ttl: Duration? = null)
    suspend fun delete(key: K): Boolean
    suspend fun exists(key: K): Boolean
    suspend fun clear()
    suspend fun size(): Int
    suspend fun keys(): Set<K>
    suspend fun stats(): CacheStats
}
```

### 2.2 缓存统计信息（CacheStats）

缓存统计信息用于监控缓存的性能：

```kotlin
data class CacheStats(
    val hitCount: Long = 0,
    val missCount: Long = 0,
    val evictionCount: Long = 0,
    val size: Int = 0,
    val averageLoadPenalty: Double = 0.0
) {
    fun hitRate(): Double
    fun missRate(): Double
}
```

### 2.3 缓存类型（CacheType）

缓存类型枚举定义了支持的缓存类型：

```kotlin
enum class CacheType {
    MEMORY,
    REDIS
}
```

## 3. 内置缓存实现

### 3.1 内存缓存（MemoryCache）

内存缓存是一种本地缓存，数据存储在应用程序的内存中：

```kotlin
val cache = CacheFactory.createCache<String, String>(
    type = CacheType.MEMORY,
    name = "my-cache",
    config = 1000, // 最大缓存项数量
    defaultTtl = Duration.ofMinutes(10)
)
```

功能特点：
- 支持过期时间
- 支持最大缓存项数量限制
- 支持 LRU（最近最少使用）淘汰策略
- 线程安全

### 3.2 Redis 缓存（RedisCache）

Redis 缓存是一种分布式缓存，数据存储在 Redis 服务器中：

```kotlin
val cache = CacheFactory.createCache<String, String>(
    type = CacheType.REDIS,
    name = "my-cache",
    config = RedisCacheConfig(
        host = "localhost",
        port = 6379,
        password = "password",
        database = 0,
        keyPrefix = "app:"
    ),
    defaultTtl = Duration.ofMinutes(10)
)
```

功能特点：
- 支持过期时间
- 支持分布式部署
- 支持键前缀
- 支持 JSON 序列化

## 4. 使用示例

### 4.1 基本用法

```kotlin
import ai.kastrax.deployer.cache.CacheFactory
import ai.kastrax.deployer.cache.CacheType
import kotlinx.coroutines.runBlocking
import java.time.Duration

fun main() = runBlocking {
    // 创建缓存
    val cache = CacheFactory.createCache<String, String>(
        type = CacheType.MEMORY,
        name = "my-cache",
        defaultTtl = Duration.ofMinutes(10)
    )
    
    // 设置缓存项
    cache.set("key1", "value1")
    
    // 获取缓存项
    val value = cache.get("key1")
    println("Value: $value")
    
    // 检查缓存项是否存在
    val exists = cache.exists("key1")
    println("Exists: $exists")
    
    // 删除缓存项
    val deleted = cache.delete("key1")
    println("Deleted: $deleted")
    
    // 获取缓存统计信息
    val stats = cache.stats()
    println("Hit rate: ${stats.hitRate()}")
}
```

### 4.2 使用过期时间

```kotlin
// 设置带过期时间的缓存项
cache.set("key1", "value1", Duration.ofSeconds(30))

// 30 秒后，缓存项将自动过期
delay(31000)
val value = cache.get("key1") // 返回 null
```

### 4.3 使用 Redis 缓存

```kotlin
// 创建 Redis 缓存
val redisCache = CacheFactory.createCache<String, String>(
    type = CacheType.REDIS,
    name = "my-redis-cache",
    config = RedisCacheConfig(
        host = "localhost",
        port = 6379
    )
)

// 使用方式与内存缓存相同
redisCache.set("key1", "value1")
val value = redisCache.get("key1")
```

## 5. 扩展缓存系统

要创建自定义缓存实现，只需实现 `Cache` 接口：

```kotlin
class CustomCache<K, V>(
    override val name: String
) : Cache<K, V> {
    // 实现接口方法
    override suspend fun get(key: K): V? {
        // 实现获取逻辑
    }
    
    override suspend fun set(key: K, value: V, ttl: Duration?) {
        // 实现设置逻辑
    }
    
    // 实现其他方法...
}
```

## 6. 最佳实践

### 6.1 选择合适的缓存类型

- 对于单实例应用，使用内存缓存
- 对于分布式应用，使用 Redis 缓存
- 对于需要持久化的数据，使用 Redis 缓存

### 6.2 设置合理的过期时间

```kotlin
// 短期缓存（1分钟）
cache.set("user-status", status, Duration.ofMinutes(1))

// 中期缓存（1小时）
cache.set("user-profile", profile, Duration.ofHours(1))

// 长期缓存（1天）
cache.set("app-config", config, Duration.ofDays(1))
```

### 6.3 监控缓存性能

```kotlin
// 定期检查缓存统计信息
val stats = cache.stats()
println("Cache size: ${stats.size}")
println("Hit rate: ${stats.hitRate()}")
println("Miss rate: ${stats.missRate()}")
println("Eviction count: ${stats.evictionCount}")

// 如果命中率过低，可能需要调整缓存策略
if (stats.hitRate() < 0.5) {
    println("Warning: Low hit rate, consider adjusting cache strategy")
}
```

### 6.4 缓存预热

```kotlin
// 应用启动时预热缓存
suspend fun warmupCache() {
    val popularItems = repository.getPopularItems()
    popularItems.forEach { item ->
        cache.set("item:${item.id}", item)
    }
    println("Cache warmed up with ${popularItems.size} items")
}
```

### 6.5 缓存穿透防护

```kotlin
suspend fun getUser(id: String): User? {
    // 从缓存获取
    val cachedUser = cache.get("user:$id")
    if (cachedUser != null) {
        return cachedUser
    }
    
    // 从数据库获取
    val user = repository.findUser(id)
    
    // 即使用户不存在，也缓存空值（防止缓存穿透）
    if (user == null) {
        cache.set("user:$id", NULL_USER, Duration.ofMinutes(5))
        return null
    }
    
    // 缓存用户
    cache.set("user:$id", user, Duration.ofHours(1))
    return user
}

// 定义一个表示空值的对象
private val NULL_USER = User(id = "", name = "")
```
