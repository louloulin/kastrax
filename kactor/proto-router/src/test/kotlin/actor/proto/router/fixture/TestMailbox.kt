package actor.proto.router.fixture

import actor.proto.mailbox.Dispatcher
import actor.proto.mailbox.Mailbox
import actor.proto.mailbox.MessageInvoker
import actor.proto.mailbox.SystemMessage
import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal

/**
 * 测试用邮箱实现
 * 修复了两个问题：
 * 1. 保存dispatcher参数，避免NullPointerException
 * 2. 使用KastraxCoroutineGlobal.runBlocking而不是kotlinx.coroutines.runBlocking
 */
class TestMailbox : Mailbox {
    private lateinit var _invoker: MessageInvoker
    private lateinit var _dispatcher: Dispatcher
    private val userMessages: MutableList<Any> = mutableListOf()
    private val systemMessages: MutableList<Any> = mutableListOf()

    override fun postUserMessage(msg: Any) {
        userMessages.add(msg)
        KastraxCoroutineGlobal.runBlocking { _invoker.invokeUserMessage(msg) }
    }

    override suspend fun run() {
        // 实现空方法
    }

    override fun postSystemMessage(msg: Any) {
        systemMessages.add(msg)
        KastraxCoroutineGlobal.runBlocking {
            if (msg is SystemMessage) {
                _invoker.invokeSystemMessage(msg)
            }
        }
    }

    override fun registerHandlers(invoker: MessageInvoker, dispatcher: Dispatcher) {
        _invoker = invoker
        _dispatcher = dispatcher
    }

    override fun start() {
        // 实现空方法
    }
}

