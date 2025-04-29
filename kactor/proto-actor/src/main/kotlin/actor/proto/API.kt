@file:JvmName("Actors")
@file:JvmMultifileClass

package actor.proto

import actor.proto.mailbox.SystemMessage
import java.time.Duration

@JvmSynthetic
fun fromProducer(producer: () -> Actor): Props = Props().withProducer(producer)

@JvmSynthetic
fun fromFunc(receive: suspend Context.(msg: Any) -> Unit): Props = fromProducer {
    object : Actor {
        override suspend fun Context.receive(msg: Any) = receive(this, msg)
    }
}

fun spawn(props: Props): PID {
    return ActorSystem.default().actorOf(props)
}

fun spawnPrefix(props: Props, prefix: String): PID {
    val name = prefix + ActorSystem.default().processRegistry().nextId()
    return spawnNamed(props, name)
}

fun spawnNamed(props: Props, name: String): PID = ActorSystem.default().actorOf(props, name)

fun stop(pid: PID) {
    ActorSystem.default().stop(pid)
}

fun sendSystemMessage(pid: PID, sys: SystemMessage) {
    val process: Process = pid.cachedProcess() ?: ActorSystem.default().processRegistry().get(pid)
    process.sendSystemMessage(pid, sys)
}

// 全局 send 函数已移至 ActorSystem.default().send

// 全局 request 函数已移至 ActorSystem.default().request

// 全局 requestAwait 函数已移至 ActorSystem.default().requestAsync

// 全局 poison 函数已移至 ActorSystem.default().poison
