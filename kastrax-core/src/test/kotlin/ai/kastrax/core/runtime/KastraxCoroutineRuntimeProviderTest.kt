package ai.kastrax.core.runtime

import ai.kastrax.runtime.coroutines.jvm.JvmCoroutineRuntime
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class KastraxCoroutineRuntimeProviderTest {

    @Test
    fun `test getRuntime returns default runtime`() {
        // 重置运行时
        KastraxCoroutineRuntimeProvider.resetRuntime()
        
        // 获取运行时
        val runtime = KastraxCoroutineRuntimeProvider.getRuntime()
        
        // 验证运行时不为空
        assertNotNull(runtime)
    }
    
    @Test
    fun `test setRuntime and getRuntime`() {
        // 创建自定义运行时
        val customRuntime = JvmCoroutineRuntime()
        
        // 设置运行时
        KastraxCoroutineRuntimeProvider.setRuntime(customRuntime)
        
        // 获取运行时
        val runtime = KastraxCoroutineRuntimeProvider.getRuntime()
        
        // 验证运行时是自定义运行时
        assertEquals(customRuntime, runtime)
        
        // 重置运行时
        KastraxCoroutineRuntimeProvider.resetRuntime()
    }
    
    @Test
    fun `test withIO executes in IO dispatcher`() {
        // 重置运行时
        KastraxCoroutineRuntimeProvider.resetRuntime()
        
        // 在IO调度器上执行代码块
        runBlockingCoroutine {
            val result = withIO {
                // 模拟IO操作
                delay(100)
                "IO result"
            }
            
            // 验证结果
            assertEquals("IO result", result)
        }
    }
    
    @Test
    fun `test launchCoroutine launches a new coroutine`() {
        // 重置运行时
        KastraxCoroutineRuntimeProvider.resetRuntime()
        
        // 创建计数器
        val counter = AtomicInteger(0)
        
        // 启动协程
        val job = launchCoroutine(this) {
            // 模拟操作
            delay(100)
            counter.incrementAndGet()
        }
        
        // 等待协程完成
        runBlockingCoroutine {
            job.join()
        }
        
        // 验证计数器值
        assertEquals(1, counter.get())
    }
}
