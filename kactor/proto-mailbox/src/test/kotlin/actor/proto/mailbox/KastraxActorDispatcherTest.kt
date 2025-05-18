package actor.proto.mailbox

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineScope
import ai.kastrax.runtime.coroutines.KastraxJob
import ai.kastrax.runtime.coroutines.jvm.JvmCoroutineRuntime
import kotlinx.coroutines.delay
import org.awaitility.Awaitility
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class KastraxActorDispatcherTest {

    @Test
    fun testKastraxActorDispatcher() {
        // 创建kastrax运行时
        val runtime = JvmCoroutineRuntime()
        
        // 创建测试邮箱
        val mailbox = TestMailboxForKastrax()
        
        // 创建kastrax调度器
        val dispatcher = KastraxActorDispatcher(runtime)
        
        // 调度邮箱
        dispatcher.schedule(mailbox)
        
        // 等待邮箱运行完成
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertTrue(mailbox.completedRunning.get())
        }
    }
}

class TestMailboxForKastrax : Mailbox {
    val completedRunning = AtomicBoolean(false)
    
    override fun postSystemMessage(msg: Any) {
        // 不需要实现
    }
    
    override fun registerHandlers(invoker: MessageInvoker, dispatcher: Dispatcher) {
        // 不需要实现
    }
    
    override fun start() {
        // 不需要实现
    }
    
    override suspend fun run() {
        // 模拟处理时间
        delay(100)
        completedRunning.set(true)
    }
    
    override fun postUserMessage(msg: Any) {
        // 不需要实现
    }
}
