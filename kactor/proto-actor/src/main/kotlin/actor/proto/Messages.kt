package actor.proto

import actor.proto.mailbox.SystemMessage

interface AutoReceiveMessage

//map Proto Messages
typealias PoisonPill = Protos.PoisonPill

typealias Terminated = Protos.Terminated

fun Terminated(who: PID, addressTerminated: Boolean): Terminated {
    val t = Protos.Terminated.newBuilder()
    t.who = who
    t.addressTerminated = addressTerminated
    return t.build()
}
typealias Watch = Protos.Watch

fun Watch(watcher: PID): Watch {
    val w = Watch.newBuilder()
    w.watcher = watcher
    return w.build()
}
typealias Unwatch = Protos.Unwatch

fun Unwatch(watcher: PID): Unwatch {
    val w = Unwatch.newBuilder()
    w.watcher = watcher
    return w.build()
}
typealias Stop = Protos.Stop

internal val StopInstance: Stop = Stop.newBuilder().build()

object ReceiveTimeout : SystemMessage
object Stopped : AutoReceiveMessage
object Started : SystemMessage
object Restarting
object Stopping : AutoReceiveMessage
object NullMessage

// Failure 类已移至 Failure.kt
data class Restart(val reason: Exception) : SystemMessage
data class Continuation(val action: suspend () -> Unit, val message: Any) : SystemMessage
interface NotInfluenceReceiveTimeout

