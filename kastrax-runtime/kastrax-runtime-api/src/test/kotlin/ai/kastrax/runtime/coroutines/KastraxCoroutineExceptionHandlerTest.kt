package ai.kastrax.runtime.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.EmptyCoroutineContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertTrue

class KastraxCoroutineExceptionHandlerTest {
    private lateinit var testException: RuntimeException
    private val exceptionHandled = AtomicBoolean(false)
    private val testHandler: (Throwable) -> Unit = { throwable ->
        if (throwable == testException) {
            exceptionHandled.set(true)
        }
    }

    @BeforeEach
    fun setup() {
        testException = RuntimeException("Test exception")
        exceptionHandled.set(false)
        KastraxCoroutineExceptionHandler.addHandler(testHandler)
    }

    @AfterEach
    fun teardown() {
        KastraxCoroutineExceptionHandler.removeHandler(testHandler)
    }

    @Test
    fun `test exception handler handles exceptions`() {
        // 直接调用处理方法
        KastraxCoroutineExceptionHandler.handleException(testException)

        // 验证异常被处理
        assertTrue(exceptionHandled.get())
    }

    @Test
    fun `test coroutine exception handler handles exceptions`() {
        // 直接调用异常处理器
        val handler = KastraxCoroutineExceptionHandler.createCoroutineExceptionHandler()
        handler.handleException(kotlin.coroutines.EmptyCoroutineContext, testException)

        // 验证异常被处理
        assertTrue(exceptionHandled.get())
    }

    @Test
    fun `test multiple handlers are called`() {
        val secondHandlerCalled = AtomicBoolean(false)
        val secondHandler: (Throwable) -> Unit = { throwable ->
            if (throwable == testException) {
                secondHandlerCalled.set(true)
            }
        }

        try {
            // 添加第二个处理器
            KastraxCoroutineExceptionHandler.addHandler(secondHandler)

            // 直接调用处理方法
            KastraxCoroutineExceptionHandler.handleException(testException)

            // 验证两个处理器都被调用
            assertTrue(exceptionHandled.get())
            assertTrue(secondHandlerCalled.get())
        } finally {
            // 移除第二个处理器
            KastraxCoroutineExceptionHandler.removeHandler(secondHandler)
        }
    }
}
