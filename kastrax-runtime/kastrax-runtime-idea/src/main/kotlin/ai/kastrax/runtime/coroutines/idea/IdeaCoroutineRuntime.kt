package ai.kastrax.runtime.coroutines.idea

import ai.kastrax.runtime.coroutines.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.coroutines.runBlockingCancellable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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
    
    override fun <T> flow(block: suspend FlowCollector<T>.() -> Unit): KastraxFlow<T> {
        return IdeaFlow(kotlinx.coroutines.flow.flow { 
            val collector = object : FlowCollector<T> {
                override suspend fun emit(value: T) {
                    emit(value)
                }
            }
            block(collector)
        })
    }
    
    override fun <T> sharedFlow(replay: Int, extraBufferCapacity: Int): KastraxSharedFlow<T> {
        val flow = MutableSharedFlow<T>(replay = replay, extraBufferCapacity = extraBufferCapacity)
        return IdeaSharedFlow(flow)
    }
}
