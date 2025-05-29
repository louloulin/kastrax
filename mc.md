# KastraX AI2DB Spring 到 Micronaut 迁移计划

## 项目概述

本文档详细描述了将 KastraX AI2DB 后端从 Spring Boot 3.2.5 迁移到最新版本 Micronaut 4.x 的完整计划，并参考 Kestra 的插件设计实现数据库驱动的插件化架构。

## 当前架构分析

### 现有技术栈
- **框架**: Spring Boot 3.2.5
- **语言**: Kotlin 2.1.10
- **JVM**: Java 17
- **数据库**: PostgreSQL, MySQL, SQL Server
- **依赖注入**: Spring IoC Container
- **数据访问**: Spring Data JPA + JDBC
- **安全**: Spring Security
- **测试**: Spring Boot Test + MockK

### 核心模块分析

#### 1. 连接管理模块 (`connection`)
- **DatabaseConnector**: 数据库连接器接口
- **ConnectionManager**: 连接管理器 (使用 @Service)
- **DatabaseConnectorRegistry**: 连接器注册表 (使用 @Component)
- **ConnectionPool**: 连接池管理
- **ConnectionConfigRepository**: 配置存储 (Spring Data JPA)

#### 2. NL2SQL 模块 (`nl2sql`)
- **NL2SQLConverter**: 自然语言转SQL接口
- **LLMAdapter**: LLM适配器
- **SQLParser**: SQL解析器
- **SQLPromptBuilder**: SQL提示构建器

#### 3. 模式管理模块 (`schema`)
- **SchemaManager**: 数据库模式管理接口
- **DatabaseSchema**: 数据库模式模型

### Spring 依赖使用情况
- 大量使用 `@Service`, `@Component`, `@Repository` 注解
- 依赖 Spring 的依赖注入容器
- 使用 Spring JDBC 的 `JdbcClient`
- 使用 Spring Data JPA 进行数据持久化
- 使用 Spring Security 进行安全控制

## Micronaut 迁移优势

### 1. 性能优势
- **启动时间**: Micronaut 启动时间比 Spring Boot 快 2-3 倍
- **内存占用**: 减少 30-50% 的内存使用
- **编译时优化**: AOT 编译时依赖注入，无运行时反射
- **Native Image 支持**: 更好的 GraalVM Native Image 兼容性

### 2. 插件系统优势
- **ServiceLoader 支持**: Micronaut 对 ServiceLoader 有更好的原生支持
- **模块化设计**: 更适合插件化架构
- **动态加载**: 支持运行时插件发现和加载

### 3. 云原生特性
- **微服务友好**: 专为微服务架构设计
- **配置管理**: 更灵活的配置系统
- **健康检查**: 内置健康检查和监控

## Kestra 插件架构分析

基于对 Kestra 的研究，其插件系统具有以下特点：<mcreference link="https://kestra.io/docs/architecture" index="2">2</mcreference>

### 1. 插件发现机制
- 使用 Java ServiceLoader 进行插件发现
- 支持运行时插件加载和卸载
- 插件通过 META-INF/services 文件注册

### 2. 插件生命周期管理
- 插件初始化和销毁钩子
- 依赖管理和版本控制
- 插件间通信机制

### 3. 插件隔离
- 类加载器隔离
- 配置隔离
- 资源管理隔离

## 迁移计划

### 阶段一：基础框架迁移 (2-3 周)

#### 1.1 项目结构调整
```
kastrax-ai2db-micronaut/
├── build.gradle.kts                 # Micronaut 依赖配置
├── src/main/kotlin/
│   ├── com/kastrax/ai2db/
│   │   ├── Application.kt           # Micronaut 应用入口
│   │   ├── config/                  # 配置类
│   │   ├── core/                    # 核心模块
│   │   └── plugins/                 # 插件系统
│   └── resources/
│       ├── application.yml          # Micronaut 配置
│       └── META-INF/services/       # ServiceLoader 配置
└── plugins/                         # 外部插件目录
    ├── kastrax-mysql-plugin/
    ├── kastrax-postgresql-plugin/
    └── kastrax-sqlserver-plugin/
```

#### 1.2 依赖配置更新
```kotlin
// build.gradle.kts
plugins {
    id("io.micronaut.application") version "4.2.1"
    id("io.micronaut.aot") version "4.2.1"
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.allopen") version "1.9.21"
    kotlin("kapt") version "1.9.21"
}

dependencies {
    // Micronaut 核心
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.kotlin:micronaut-kotlin-extension-functions")
    
    // 数据访问
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.flyway:micronaut-flyway")
    
    // 安全
    implementation("io.micronaut.security:micronaut-security-jwt")
    
    // 插件系统
    implementation("io.micronaut:micronaut-inject")
    
    // 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    
    // 测试
    testImplementation("io.micronaut.test:micronaut-test-kotlintest")
    testImplementation("io.mockk:mockk")
}
```

#### 1.3 应用入口迁移
```kotlin
// Application.kt
package com.kastrax.ai2db

import io.micronaut.runtime.Micronaut

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
            .args(*args)
            .packages("com.kastrax.ai2db")
            .start()
    }
}
```

### 阶段二：核心模块迁移 (3-4 周)

#### 2.1 依赖注入注解迁移

**Spring → Micronaut 注解映射**:
- `@Service` → `@Singleton`
- `@Component` → `@Singleton`
- `@Repository` → `@Singleton`
- `@Autowired` → `@Inject`
- `@Value` → `@Property`
- `@PostConstruct` → `@PostConstruct` (保持不变)

#### 2.2 连接管理模块迁移

```kotlin
// DatabaseConnectorRegistry.kt
package com.kastrax.ai2db.connection.manager

import io.micronaut.context.annotation.Singleton
import jakarta.annotation.PostConstruct
import jakarta.inject.Inject

@Singleton
class DatabaseConnectorRegistry(
    private val connectors: List<DatabaseConnector>
) {
    private val connectorMap = mutableMapOf<DatabaseType, DatabaseConnector>()

    @PostConstruct
    fun initialize() {
        // 插件发现逻辑
        discoverPlugins()
        
        // 注册内置连接器
        for (connector in connectors) {
            registerConnector(connector)
        }
    }
    
    private fun discoverPlugins() {
        // 使用 ServiceLoader 发现插件
        val pluginConnectors = ServiceLoader.load(DatabaseConnector::class.java)
        pluginConnectors.forEach { connector ->
            registerConnector(connector)
        }
    }
    
    private fun registerConnector(connector: DatabaseConnector) {
        val type = connector.getSupportedType()
        connectorMap[type] = connector
    }
}
```

#### 2.3 数据访问层迁移

```kotlin
// ConnectionConfigRepository.kt
package com.kastrax.ai2db.connection.repository

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ConnectionConfigRepository : CrudRepository<ConnectionConfigEntity, String> {
    
    fun findByName(name: String): List<ConnectionConfigEntity>
    
    fun findByNameContainingIgnoreCase(name: String): List<ConnectionConfigEntity>
    
    fun findByType(type: DatabaseType): List<ConnectionConfigEntity>
}
```

### 阶段三：插件系统实现 (4-5 周)

#### 3.1 插件接口设计

```kotlin
// DatabaseConnectorPlugin.kt
package com.kastrax.ai2db.plugins

import com.kastrax.ai2db.connection.connector.DatabaseConnector

/**
 * 数据库连接器插件接口
 * 参考 Kestra 的插件设计
 */
interface DatabaseConnectorPlugin {
    /**
     * 插件名称
     */
    fun getName(): String
    
    /**
     * 插件版本
     */
    fun getVersion(): String
    
    /**
     * 支持的数据库类型
     */
    fun getSupportedTypes(): List<DatabaseType>
    
    /**
     * 创建数据库连接器实例
     */
    fun createConnector(type: DatabaseType): DatabaseConnector
    
    /**
     * 插件初始化
     */
    fun initialize(context: PluginContext)
    
    /**
     * 插件销毁
     */
    fun destroy()
}
```

#### 3.2 插件管理器实现

```kotlin
// PluginManager.kt
package com.kastrax.ai2db.plugins

import io.micronaut.context.annotation.Singleton
import jakarta.annotation.PostConstruct
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Singleton
class PluginManager {
    private val loadedPlugins = ConcurrentHashMap<String, DatabaseConnectorPlugin>()
    private val pluginClassLoaders = ConcurrentHashMap<String, ClassLoader>()
    
    @PostConstruct
    fun initialize() {
        discoverAndLoadPlugins()
    }
    
    private fun discoverAndLoadPlugins() {
        // 使用 ServiceLoader 发现插件
        val serviceLoader = ServiceLoader.load(DatabaseConnectorPlugin::class.java)
        
        serviceLoader.forEach { plugin ->
            try {
                plugin.initialize(createPluginContext())
                loadedPlugins[plugin.getName()] = plugin
                logger.info("Loaded plugin: ${plugin.getName()} v${plugin.getVersion()}")
            } catch (e: Exception) {
                logger.error("Failed to load plugin: ${plugin.getName()}", e)
            }
        }
    }
    
    fun getPlugin(name: String): DatabaseConnectorPlugin? {
        return loadedPlugins[name]
    }
    
    fun getAllPlugins(): Collection<DatabaseConnectorPlugin> {
        return loadedPlugins.values
    }
    
    fun reloadPlugin(name: String): Boolean {
        // 插件热重载逻辑
        return try {
            unloadPlugin(name)
            // 重新加载插件
            discoverAndLoadPlugins()
            true
        } catch (e: Exception) {
            logger.error("Failed to reload plugin: $name", e)
            false
        }
    }
    
    private fun unloadPlugin(name: String) {
        loadedPlugins[name]?.destroy()
        loadedPlugins.remove(name)
        pluginClassLoaders.remove(name)
    }
}
```

#### 3.3 MySQL 插件实现示例

```kotlin
// MySQLConnectorPlugin.kt
package com.kastrax.ai2db.plugins.mysql

import com.kastrax.ai2db.connection.connector.DatabaseConnector
import com.kastrax.ai2db.connection.model.DatabaseType
import com.kastrax.ai2db.plugins.DatabaseConnectorPlugin
import com.kastrax.ai2db.plugins.PluginContext

class MySQLConnectorPlugin : DatabaseConnectorPlugin {
    
    override fun getName(): String = "mysql-connector"
    
    override fun getVersion(): String = "1.0.0"
    
    override fun getSupportedTypes(): List<DatabaseType> = listOf(DatabaseType.MYSQL)
    
    override fun createConnector(type: DatabaseType): DatabaseConnector {
        return when (type) {
            DatabaseType.MYSQL -> MySQLConnector()
            else -> throw IllegalArgumentException("Unsupported database type: $type")
        }
    }
    
    override fun initialize(context: PluginContext) {
        // 插件初始化逻辑
        context.getLogger().info("Initializing MySQL connector plugin")
    }
    
    override fun destroy() {
        // 清理资源
    }
}
```

#### 3.4 ServiceLoader 配置

```
# src/main/resources/META-INF/services/com.kastrax.ai2db.plugins.DatabaseConnectorPlugin
com.kastrax.ai2db.plugins.mysql.MySQLConnectorPlugin
com.kastrax.ai2db.plugins.postgresql.PostgreSQLConnectorPlugin
com.kastrax.ai2db.plugins.sqlserver.SQLServerConnectorPlugin
```

### 阶段四：配置和安全迁移 (2-3 周)

#### 4.1 配置系统迁移

```yaml
# application.yml
micronaut:
  application:
    name: kastrax-ai2db
  server:
    port: 8080
  security:
    authentication: bearer
    token:
      jwt:
        signatures:
          secret:
            generator:
              secret: "${JWT_SECRET:your-secret-key}"

datasources:
  default:
    url: jdbc:postgresql://localhost:5432/kastrax_ai2db
    username: ${DB_USERNAME:kastrax}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver

kastrax:
  ai2db:
    plugins:
      directory: "${PLUGIN_DIR:./plugins}"
      auto-discovery: true
    llm:
      provider: "${LLM_PROVIDER:openai}"
      api-key: "${LLM_API_KEY:}"
```

#### 4.2 安全配置迁移

```kotlin
// SecurityConfiguration.kt
package com.kastrax.ai2db.config

import io.micronaut.context.annotation.Factory
import io.micronaut.security.config.SecurityConfiguration
import io.micronaut.security.config.SecurityConfigurationProperties
import jakarta.inject.Singleton

@Factory
class SecurityConfiguration {
    
    @Singleton
    fun securityConfiguration(): SecurityConfigurationProperties {
        return SecurityConfigurationProperties().apply {
            authentication = SecurityConfiguration.AuthenticationMode.BEARER
            // 其他安全配置
        }
    }
}
```

### 阶段五：测试迁移 (2-3 周)

#### 5.1 单元测试迁移

```kotlin
// ConnectionManagerTest.kt
package com.kastrax.ai2db.connection.manager

import io.micronaut.test.extensions.kotlintest.annotation.MicronautTest
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jakarta.inject.Inject

@MicronautTest
class ConnectionManagerTest : StringSpec({
    
    @Inject
    lateinit var connectionManager: ConnectionManager
    
    "should create connection successfully" {
        // 测试逻辑
    }
})
```

#### 5.2 集成测试迁移

```kotlin
// PluginIntegrationTest.kt
package com.kastrax.ai2db.plugins

import io.micronaut.test.extensions.kotlintest.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import io.kotlintest.specs.StringSpec
import jakarta.inject.Inject

@MicronautTest
class PluginIntegrationTest : StringSpec(), TestPropertyProvider {
    
    @Inject
    lateinit var pluginManager: PluginManager
    
    override fun getProperties(): Map<String, String> {
        return mapOf(
            "kastrax.ai2db.plugins.directory" to "./test-plugins"
        )
    }
    
    init {
        "should load plugins from directory" {
            // 测试插件加载
        }
    }
}
```

### 阶段六：性能优化和部署 (2-3 周)

#### 6.1 AOT 编译优化

```kotlin
// build.gradle.kts
micronaut {
    runtime("netty")
    testRuntime("kotlintest")
    processing {
        incremental(true)
        annotations("com.kastrax.ai2db.*")
    }
    aot {
        optimizeServiceLoading = true
        convertYamlToJava = true
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
    }
}
```

#### 6.2 Native Image 配置

```json
// native-image.properties
Args = --no-fallback \
       --enable-http \
       --enable-https \
       --enable-all-security-services \
       --report-unsupported-elements-at-runtime \
       --allow-incomplete-classpath
```

#### 6.3 Docker 配置

```dockerfile
# Dockerfile
FROM ghcr.io/graalvm/native-image:ol8-java17-22.3.0 AS builder

COPY . /app
WORKDIR /app

RUN ./gradlew nativeCompile

FROM gcr.io/distroless/base

COPY --from=builder /app/build/native/nativeCompile/kastrax-ai2db /app/kastrax-ai2db

EXPOSE 8080

ENTRYPOINT ["/app/kastrax-ai2db"]
```

## 插件开发指南

### 1. 插件项目结构

```
kastrax-mysql-plugin/
├── build.gradle.kts
├── src/main/kotlin/
│   └── com/kastrax/ai2db/plugins/mysql/
│       ├── MySQLConnectorPlugin.kt
│       ├── MySQLConnector.kt
│       └── MySQLDialect.kt
└── src/main/resources/
    └── META-INF/services/
        └── com.kastrax.ai2db.plugins.DatabaseConnectorPlugin
```

### 2. 插件依赖配置

```kotlin
// build.gradle.kts
dependencies {
    compileOnly("com.kastrax:kastrax-ai2db-api:1.0.0")
    implementation("mysql:mysql-connector-java:8.0.33")
}
```

### 3. 插件打包和分发

```kotlin
// build.gradle.kts
tasks.jar {
    archiveClassifier.set("plugin")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

## 迁移时间表

| 阶段 | 任务 | 预计时间 | 负责人 |
|------|------|----------|--------|
| 1 | 基础框架迁移 | 2-3 周 | 后端团队 |
| 2 | 核心模块迁移 | 3-4 周 | 后端团队 |
| 3 | 插件系统实现 | 4-5 周 | 架构师 + 后端团队 |
| 4 | 配置和安全迁移 | 2-3 周 | 后端团队 |
| 5 | 测试迁移 | 2-3 周 | QA + 后端团队 |
| 6 | 性能优化和部署 | 2-3 周 | DevOps + 后端团队 |
| **总计** | | **15-21 周** | |

## 风险评估和缓解策略

### 1. 技术风险

**风险**: Micronaut 生态系统相对较新，可能缺少某些 Spring 特性
**缓解**: 
- 提前进行技术调研和 POC
- 准备 Spring 兼容层作为备选方案
- 逐步迁移，保持向后兼容

**风险**: 插件系统复杂性可能导致稳定性问题
**缓解**:
- 实现完善的插件隔离机制
- 添加插件健康检查和故障恢复
- 提供插件开发最佳实践文档

### 2. 业务风险

**风险**: 迁移期间可能影响现有功能
**缓解**:
- 采用蓝绿部署策略
- 保持 Spring 版本作为回退方案
- 充分的测试覆盖

### 3. 人员风险

**风险**: 团队对 Micronaut 不熟悉
**缓解**:
- 提供 Micronaut 培训
- 安排技术分享和代码审查
- 建立知识库和最佳实践文档

## 成功指标

### 1. 性能指标
- 启动时间减少 50% 以上
- 内存使用减少 30% 以上
- 响应时间保持或改善

### 2. 功能指标
- 所有现有功能正常工作
- 插件系统稳定运行
- 支持热插拔数据库驱动

### 3. 开发效率指标
- 插件开发周期缩短
- 代码可维护性提升
- 部署效率提升

## 后续规划

### 1. 插件生态建设
- 开发更多数据库插件 (Oracle, MongoDB, Redis 等)
- 建立插件市场和分发机制
- 提供插件开发工具和模板

### 2. 云原生优化
- Kubernetes 原生支持
- 服务网格集成
- 可观测性增强

### 3. AI 能力增强
- LLM 插件化
- 多模态支持
- 智能运维功能

## 结论

通过将 KastraX AI2DB 从 Spring 迁移到 Micronaut，并参考 Kestra 的插件设计实现数据库驱动的插件化，我们将获得：

1. **更好的性能**: 启动时间和内存使用显著改善
2. **插件化架构**: 支持动态加载数据库驱动，提高系统扩展性
3. **云原生支持**: 更好的容器化和微服务支持
4. **开发效率**: 简化的配置和更好的开发体验

这次迁移将为 KastraX AI2DB 的长期发展奠定坚实的技术基础，使其能够更好地适应快速变化的技术环境和业务需求。