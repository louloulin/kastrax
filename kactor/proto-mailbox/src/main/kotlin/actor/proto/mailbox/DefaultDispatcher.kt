package actor.proto.mailbox

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.coroutines.CoroutineContext


/**
 * 默认调度器实现
 *
 * @param context 协程上下文，默认为Dispatchers.Default
 * @param runtime 协程运行时，默认为null，表示使用kotlinx.coroutines
 * @param throughput 吞吐量，默认为300
 */
class DefaultDispatcher(
    context: CoroutineContext = Dispatchers.Default,
    private val runtime: KastraxCoroutineRuntime? = null,
    override var throughput: Int = 300
) : Dispatcher {
    // We could create a jobless GlobalScope root scope and replace the default dispatcher with our dispatcher
    // val scope : CoroutineScope = GlobalScope + context
    //Or we can create a scope from our dispatcher with a default job, and replace it with a supervisor job.
    //This way we get a normal scope that could be cancelled if needed
    private val scope : CoroutineScope = CoroutineScope(context) + SupervisorJob()

    override fun schedule(mailbox:Mailbox) {
        if (runtime != null) {
            // 使用kastrax-runtime
            val kastraxScope = runtime.getScope(this)
            kastraxScope.launch {
                mailbox.run()
            }
        } else {
            // 使用kotlinx.coroutines
            scope.launch {
                mailbox.run()
            }
        }
    }

    companion object {
        /**
         * 创建使用kastrax-runtime的DefaultDispatcher
         *
         * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
         * @param throughput 吞吐量，默认为300
         * @return DefaultDispatcher实例
         */
        fun withKastraxRuntime(
            runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime(),
            throughput: Int = 300
        ): DefaultDispatcher {
            return DefaultDispatcher(Dispatchers.Default, runtime, throughput)
        }
    }
}

