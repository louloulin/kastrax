# kastrax全局多协程运行时抽象支持方案

## 1. 背景与挑战

kastrax框架需要在多种环境中运行，包括：
- 标准JVM应用
- IntelliJ IDEA插件
- Android应用
- 服务器端应用
- GraalVM原生镜像

每种环境对协程的支持和限制各不相同，特别是在插件环境中，不应捆绑自己的协程库版本，而应使用宿主平台提供的协程支持。这带来了以下挑战：

1. **协程API版本差异**：不同平台提供的协程库版本可能不同
2. **调度器可用性差异**：不同平台支持的调度器类型不同
3. **线程模型差异**：特别是UI线程模型在各平台有显著差异
4. **类加载器隔离**：插件环境中的类加载器隔离可能导致协程相关类的冲突
5. **生命周期管理**：不同平台的组件生命周期管理机制不同

## 2. 设计目标

设计一个抽象层，使kastrax能够：

1. **平台无关性**：核心逻辑不依赖于特定平台的协程实现
2. **运行时适配**：根据运行环境自动选择合适的协程实现
3. **最小依赖**：减少对特定协程API版本的硬依赖
4. **最佳实践兼容**：符合各平台的协程使用最佳实践
5. **向后兼容**：保持与现有kastrax代码的兼容性

## 3. 抽象设计

### 3.1 核心抽象接口

创建一套协程抽象接口，作为kastrax与具体协程实现之间的桥梁：

```kotlin
// 核心协程抽象接口
package ai.kastrax.core.coroutines

/**
 * kastrax协程运行时抽象
 * 提供跨平台的协程支持
 */
interface KastraxCoroutineRuntime {
    /**
     * 获取适合当前平台的协程作用域
     */
    fun getScope(owner: Any): KastraxCoroutineScope
    
    /**
     * 获取IO调度器
     */
    fun ioDispatcher(): KastraxDispatcher
    
    /**
     * 获取计算调度器
     */
    fun computeDispatcher(): KastraxDispatcher
    
    /**
     * 获取UI调度器
     */
    fun uiDispatcher(): KastraxDispatcher
    
    /**
     * 执行阻塞操作
     */
    fun <T> runBlocking(block: suspend () -> T): T
    
    /**
     * 创建可取消的作用域
     */
    fun createCancellableScope(owner: Any): KastraxCoroutineScope
}

/**
 * kastrax协程作用域抽象
 */
interface KastraxCoroutineScope {
    /**
     * 启动协程
     */
    fun launch(block: suspend () -> Unit): KastraxJob
    
    /**
     * 异步执行并返回结果
     */
    fun <T> async(block: suspend () -> T): KastraxDeferred<T>
    
    /**
     * 取消作用域中的所有协程
     */
    fun cancel()
    
    /**
     * 检查作用域是否活跃
     */
    fun isActive(): Boolean
}

/**
 * kastrax协程作业抽象
 */
interface KastraxJob {
    /**
     * 取消作业
     */
    fun cancel()
    
    /**
     * 等待作业完成
     */
    suspend fun join()
    
    /**
     * 检查作业是否活跃
     */
    fun isActive(): Boolean
}

/**
 * kastrax延迟结果抽象
 */
interface KastraxDeferred<T> : KastraxJob {
    /**
     * 等待并获取结果
     */
    suspend fun await(): T
}

/**
 * kastrax调度器抽象
 */
interface KastraxDispatcher {
    /**
     * 在此调度器上执行代码块
     */
    suspend fun <T> withContext(block: suspend () -> T): T
}
```

### 3.2 平台特定实现

为每个目标平台创建特定的实现：

#### 3.2.1 标准JVM实现

```kotlin
package ai.kastrax.core.coroutines.jvm

import ai.kastrax.core.coroutines.*
import kotlinx.coroutines.*

/**
 * 标准JVM环境的协程运行时实现
 */
class JvmCoroutineRuntime : KastraxCoroutineRuntime {
    override fun getScope(owner: Any): KastraxCoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return JvmCoroutineScope(scope)
    }
    
    override fun ioDispatcher(): KastraxDispatcher {
        return JvmDispatcher(Dispatchers.IO)
    }
    
    override fun computeDispatcher(): KastraxDispatcher {
        return JvmDispatcher(Dispatchers.Default)
    }
    
    override fun uiDispatcher(): KastraxDispatcher {
        // 在标准JVM中，没有UI调度器，使用Unconfined作为后备
        return JvmDispatcher(Dispatchers.Unconfined)
    }
    
    override fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }
    
    override fun createCancellableScope(owner: Any): KastraxCoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return JvmCoroutineScope(scope)
    }
}

// 其他JVM特定实现类...
```

#### 3.2.2 IntelliJ IDEA插件实现

```kotlin
package ai.kastrax.core.coroutines.idea

import ai.kastrax.core.coroutines.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.coroutines.runBlockingCancellable
import kotlinx.coroutines.*

/**
 * IntelliJ IDEA插件环境的协程运行时实现
 */
class IdeaCoroutineRuntime : KastraxCoroutineRuntime {
    override fun getScope(owner: Any): KastraxCoroutineScope {
        // 如果owner是Project或Application，使用平台提供的作用域
        // 否则创建一个新的作用域
        val scope = when (owner) {
            is com.intellij.openapi.project.Project -> {
                // 获取项目级协程作用域
                owner.getService(kotlinx.coroutines.CoroutineScope::class.java)
            }
            is com.intellij.openapi.application.Application -> {
                // 获取应用级协程作用域
                owner.getService(kotlinx.coroutines.CoroutineScope::class.java)
            }
            is Disposable -> {
                // 创建可释放的作用域
                val job = SupervisorJob()
                val disposableScope = CoroutineScope(job + Dispatchers.Default)
                owner.register {
                    job.cancel()
                }
                disposableScope
            }
            else -> {
                // 创建默认作用域
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            }
        }
        
        return IdeaCoroutineScope(scope)
    }
    
    override fun ioDispatcher(): KastraxDispatcher {
        return IdeaDispatcher(Dispatchers.IO)
    }
    
    override fun computeDispatcher(): KastraxDispatcher {
        return IdeaDispatcher(Dispatchers.Default)
    }
    
    override fun uiDispatcher(): KastraxDispatcher {
        // 使用IntelliJ平台的EDT调度器
        val edtDispatcher = ApplicationManager.getApplication().getCoroutineScope().coroutineContext[CoroutineDispatcher]
            ?: Dispatchers.Default
        return IdeaDispatcher(edtDispatcher)
    }
    
    override fun <T> runBlocking(block: suspend () -> T): T {
        // 使用IntelliJ平台的runBlockingCancellable
        return runBlockingCancellable { block() }
    }
    
    override fun createCancellableScope(owner: Any): KastraxCoroutineScope {
        return getScope(owner)
    }
}

// 其他IntelliJ IDEA特定实现类...
```

#### 3.2.3 Android实现

```kotlin
package ai.kastrax.core.coroutines.android

import ai.kastrax.core.coroutines.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

/**
 * Android环境的协程运行时实现
 */
class AndroidCoroutineRuntime : KastraxCoroutineRuntime {
    override fun getScope(owner: Any): KastraxCoroutineScope {
        val scope = when (owner) {
            is LifecycleOwner -> {
                // 使用Android生命周期感知的作用域
                owner.lifecycleScope
            }
            else -> {
                // 创建默认作用域
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
            }
        }
        
        return AndroidCoroutineScope(scope)
    }
    
    override fun ioDispatcher(): KastraxDispatcher {
        return AndroidDispatcher(Dispatchers.IO)
    }
    
    override fun computeDispatcher(): KastraxDispatcher {
        return AndroidDispatcher(Dispatchers.Default)
    }
    
    override fun uiDispatcher(): KastraxDispatcher {
        return AndroidDispatcher(Dispatchers.Main)
    }
    
    override fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }
    
    override fun createCancellableScope(owner: Any): KastraxCoroutineScope {
        return getScope(owner)
    }
}

// 其他Android特定实现类...
```

### 3.3 运行时工厂与自动检测

创建一个工厂类，根据运行环境自动选择合适的运行时实现：

```kotlin
package ai.kastrax.core.coroutines

/**
 * kastrax协程运行时工厂
 * 负责创建适合当前环境的协程运行时实现
 */
object KastraxCoroutineRuntimeFactory {
    private var runtime: KastraxCoroutineRuntime? = null
    
    /**
     * 获取当前环境的协程运行时
     * 如果尚未初始化，将自动检测环境并创建合适的实现
     */
    @Synchronized
    fun getRuntime(): KastraxCoroutineRuntime {
        if (runtime == null) {
            runtime = detectRuntime()
        }
        return runtime!!
    }
    
    /**
     * 显式设置协程运行时实现
     * 用于测试或特殊环境
     */
    @Synchronized
    fun setRuntime(customRuntime: KastraxCoroutineRuntime) {
        runtime = customRuntime
    }
    
    /**
     * 自动检测当前环境并创建合适的运行时实现
     */
    private fun detectRuntime(): KastraxCoroutineRuntime {
        return when {
            // 检测IntelliJ IDEA环境
            isIntelliJEnvironment() -> {
                try {
                    Class.forName("ai.kastrax.core.coroutines.idea.IdeaCoroutineRuntime")
                        .getDeclaredConstructor()
                        .newInstance() as KastraxCoroutineRuntime
                } catch (e: Exception) {
                    // 回退到JVM实现
                    createJvmRuntime()
                }
            }
            
            // 检测Android环境
            isAndroidEnvironment() -> {
                try {
                    Class.forName("ai.kastrax.core.coroutines.android.AndroidCoroutineRuntime")
                        .getDeclaredConstructor()
                        .newInstance() as KastraxCoroutineRuntime
                } catch (e: Exception) {
                    // 回退到JVM实现
                    createJvmRuntime()
                }
            }
            
            // 默认使用JVM实现
            else -> createJvmRuntime()
        }
    }
    
    /**
     * 创建JVM运行时实现
     */
    private fun createJvmRuntime(): KastraxCoroutineRuntime {
        return Class.forName("ai.kastrax.core.coroutines.jvm.JvmCoroutineRuntime")
            .getDeclaredConstructor()
            .newInstance() as KastraxCoroutineRuntime
    }
    
    /**
     * 检测是否在IntelliJ IDEA环境中运行
     */
    private fun isIntelliJEnvironment(): Boolean {
        return try {
            Class.forName("com.intellij.openapi.application.Application")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    /**
     * 检测是否在Android环境中运行
     */
    private fun isAndroidEnvironment(): Boolean {
        return try {
            Class.forName("android.app.Activity")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
```

## 4. 使用方式

### 4.1 在kastrax核心代码中使用

kastrax核心代码应该只依赖抽象接口，不直接依赖具体实现：

```kotlin
package ai.kastrax.core.agent

import ai.kastrax.core.coroutines.KastraxCoroutineRuntimeFactory

class Agent(private val owner: Any) {
    // 获取协程运行时
    private val runtime = KastraxCoroutineRuntimeFactory.getRuntime()
    
    // 获取协程作用域
    private val scope = runtime.getScope(owner)
    
    fun process(input: String, callback: (String) -> Unit) {
        // 启动协程处理请求
        scope.launch {
            try {
                // 在IO调度器上执行耗时操作
                val result = runtime.ioDispatcher().withContext {
                    // 处理逻辑
                    processInput(input)
                }
                
                // 在UI调度器上更新UI
                runtime.uiDispatcher().withContext {
                    callback(result)
                }
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }
    
    private suspend fun processInput(input: String): String {
        // 处理逻辑
        return "Processed: $input"
    }
}
```

### 4.2 在IntelliJ IDEA插件中使用

```kotlin
package ai.kastrax.code.ui

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.coroutines.KastraxCoroutineRuntimeFactory
import com.intellij.openapi.project.Project

class CodeGenerationAction(private val project: Project) {
    // 创建Agent，传入project作为owner
    private val agent = Agent(project)
    
    // 获取协程运行时
    private val runtime = KastraxCoroutineRuntimeFactory.getRuntime()
    
    fun generateCode(prompt: String) {
        // 使用Agent处理请求
        agent.process(prompt) { result ->
            // 回调处理结果
            showResult(result)
        }
    }
    
    private fun showResult(result: String) {
        // 显示结果
    }
}
```

### 4.3 在Android应用中使用

```kotlin
package ai.kastrax.android.ui

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.coroutines.KastraxCoroutineRuntimeFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    // 创建Agent，传入activity作为owner
    private lateinit var agent: Agent
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化Agent
        agent = Agent(this)
    }
    
    fun onGenerateButtonClick() {
        val prompt = promptEditText.text.toString()
        
        // 使用Agent处理请求
        agent.process(prompt) { result ->
            // 回调处理结果
            resultTextView.text = result
        }
    }
}
```

## 5. 模块结构与依赖管理

### 5.1 模块结构

```
kastrax-coroutines/
├── kastrax-coroutines-api/        # 核心抽象接口
├── kastrax-coroutines-jvm/        # 标准JVM实现
├── kastrax-coroutines-idea/       # IntelliJ IDEA插件实现
├── kastrax-coroutines-android/    # Android实现
└── kastrax-coroutines-test/       # 测试工具
```

### 5.2 依赖配置

#### 5.2.1 kastrax-coroutines-api

```kotlin
dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
```

#### 5.2.2 kastrax-coroutines-jvm

```kotlin
dependencies {
    implementation(project(":kastrax-coroutines-api"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
```

#### 5.2.3 kastrax-coroutines-idea

```kotlin
dependencies {
    implementation(project(":kastrax-coroutines-api"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    compileOnly("com.jetbrains.intellij.platform:core-api:$intellijVersion")
}
```

#### 5.2.4 kastrax-coroutines-android

```kotlin
dependencies {
    implementation(project(":kastrax-coroutines-api"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
}
```

### 5.3 在kastrax核心模块中的依赖配置

```kotlin
dependencies {
    api(project(":kastrax-coroutines-api"))
    
    // 根据构建变体选择性包含实现
    jvmImplementation(project(":kastrax-coroutines-jvm"))
    ideaImplementation(project(":kastrax-coroutines-idea"))
    androidImplementation(project(":kastrax-coroutines-android"))
}
```

## 6. 迁移策略

### 6.1 渐进式迁移

1. **创建抽象层**：首先实现抽象接口和基本实现
2. **适配现有代码**：创建适配器，使现有代码能够使用新的抽象层
3. **逐步替换**：逐个模块替换直接协程调用为抽象层调用
4. **测试与验证**：每次替换后进行测试，确保功能正常

### 6.2 兼容性包装器

为了支持渐进式迁移，可以创建兼容性包装器：

```kotlin
package ai.kastrax.core.coroutines.compat

import ai.kastrax.core.coroutines.KastraxCoroutineRuntimeFactory
import kotlinx.coroutines.*

/**
 * 兼容性工具类
 * 提供与kotlinx.coroutines API兼容的方法
 */
object CoroutineCompat {
    private val runtime = KastraxCoroutineRuntimeFactory.getRuntime()
    
    /**
     * 兼容性launch方法
     */
    fun CoroutineScope.launchCompat(block: suspend CoroutineScope.() -> Unit): Job {
        val kastraxScope = runtime.getScope(this)
        return kastraxScope.launch { block(this@launchCompat) } as Job
    }
    
    /**
     * 兼容性withContext方法
     */
    suspend fun <T> withContextCompat(
        dispatcher: CoroutineDispatcher,
        block: suspend CoroutineScope.() -> T
    ): T {
        val kastraxDispatcher = when (dispatcher) {
            Dispatchers.IO -> runtime.ioDispatcher()
            Dispatchers.Default -> runtime.computeDispatcher()
            Dispatchers.Main -> runtime.uiDispatcher()
            else -> runtime.computeDispatcher()
        }
        
        return kastraxDispatcher.withContext { block(CoroutineScope(coroutineContext)) }
    }
}
```

## 7. 测试策略

### 7.1 单元测试

创建专门的测试运行时实现，用于单元测试：

```kotlin
package ai.kastrax.core.coroutines.test

import ai.kastrax.core.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

/**
 * 用于测试的协程运行时实现
 */
class TestCoroutineRuntime(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : KastraxCoroutineRuntime {
    private val testScope = TestScope(testDispatcher)
    
    override fun getScope(owner: Any): KastraxCoroutineScope {
        return TestCoroutineScope(testScope)
    }
    
    override fun ioDispatcher(): KastraxDispatcher {
        return TestDispatcher(testDispatcher)
    }
    
    override fun computeDispatcher(): KastraxDispatcher {
        return TestDispatcher(testDispatcher)
    }
    
    override fun uiDispatcher(): KastraxDispatcher {
        return TestDispatcher(testDispatcher)
    }
    
    override fun <T> runBlocking(block: suspend () -> T): T {
        return testScope.runTest { block() }
    }
    
    override fun createCancellableScope(owner: Any): KastraxCoroutineScope {
        return getScope(owner)
    }
    
    /**
     * 推进虚拟时间
     */
    fun advanceTimeBy(delayTimeMillis: Long) {
        testScope.testScheduler.advanceTimeBy(delayTimeMillis)
    }
    
    /**
     * 运行所有待处理的协程直到完成
     */
    fun runCurrent() {
        testScope.testScheduler.runCurrent()
    }
}

// 其他测试相关实现类...
```

### 7.2 集成测试

为不同平台创建特定的集成测试：

```kotlin
// JVM集成测试
class JvmCoroutineRuntimeTest {
    @Test
    fun testJvmRuntime() {
        val runtime = JvmCoroutineRuntime()
        val scope = runtime.getScope(this)
        
        val result = runtime.runBlocking {
            scope.launch {
                // 测试代码
            }
            
            "Success"
        }
        
        assertEquals("Success", result)
    }
}

// IntelliJ IDEA插件集成测试
class IdeaCoroutineRuntimeTest : LightJavaCodeInsightFixtureTestCase() {
    fun testIdeaRuntime() {
        val runtime = IdeaCoroutineRuntime()
        val scope = runtime.getScope(project)
        
        val result = runtime.runBlocking {
            scope.launch {
                // 测试代码
            }
            
            "Success"
        }
        
        assertEquals("Success", result)
    }
}
```

## 8. 结论

通过实施这一全局多协程运行时抽象支持方案，kastrax将能够：

1. **适应多种运行环境**：无论是标准JVM、IntelliJ IDEA插件还是Android应用，都能使用统一的API
2. **遵循平台最佳实践**：在每个平台上使用推荐的协程实践，避免类加载器冲突和线程模型不兼容问题
3. **减少依赖冲突**：避免捆绑自己的协程库版本，使用宿主平台提供的协程支持
4. **提高代码质量**：通过抽象和封装，提高代码的可测试性和可维护性
5. **简化开发体验**：开发者可以使用统一的API，而不必关心底层平台差异

这一方案不仅解决了当前的协程冲突问题，还为kastrax提供了一个灵活、可扩展的协程抽象层，支持未来在更多平台上的部署和运行。
