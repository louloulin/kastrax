package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory
import ai.kastrax.runtime.coroutines.jvm.JvmCoroutineRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.ContinuationInterceptor

/**
 * 测试协程运行时抽象层的正确性
 */
@ExperimentalCoroutinesApi
class RuntimeSwitchTest {

    @BeforeEach
    fun setup() {
        // 确保测试前重置运行时
        KastraxCoroutineGlobal.resetRuntime()
    }

    @AfterEach
    fun tearDown() {
        // 测试后重置运行时
        KastraxCoroutineGlobal.resetRuntime()
    }

    @Test
    fun `test runtime initialization and switching`() {
        // 初始化运行时
        val runtime = JvmCoroutineRuntime()
        KastraxCoroutineRuntimeFactory.setRuntime(runtime)

        // 验证运行时已设置
        val currentRuntime = KastraxCoroutineGlobal.getRuntime()
        assertNotNull(currentRuntime)

        // 验证运行时可以正常工作
        val result = KastraxCoroutineGlobal.runBlocking {
            val scope = KastraxCoroutineGlobal.getScope(this)
            val context = scope.coroutineContext[ContinuationInterceptor.Key]
            assertNotNull(context)
            "success"
        }

        assertEquals("success", result)
    }

    @Test
    fun `test different dispatchers`() {
        // 初始化运行时
        val runtime = JvmCoroutineRuntime()
        KastraxCoroutineRuntimeFactory.setRuntime(runtime)

        // 验证不同的调度器
        val ioDispatcher = KastraxCoroutineGlobal.ioDispatcher()
        val computeDispatcher = KastraxCoroutineGlobal.computeDispatcher()
        val uiDispatcher = KastraxCoroutineGlobal.uiDispatcher()

        assertNotNull(ioDispatcher)
        assertNotNull(computeDispatcher)
        assertNotNull(uiDispatcher)
    }
}
