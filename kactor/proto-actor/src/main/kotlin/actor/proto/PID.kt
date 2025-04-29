package actor.proto

import actor.proto.mailbox.SystemMessage

typealias PID = Protos.PID

fun PID(address: String, id: String): PID {
    val p = PID.newBuilder()
    p.address = address
    p.id = id
    return p.build()
}

fun PID.isLocal(): Boolean = address == ProcessRegistry.noHost || address == ProcessRegistry.address
fun PID.isLocal(registry: ProcessRegistryImpl): Boolean = address == registry.address || address == ProcessRegistryImpl.noHost

internal fun PID.cachedProcess(): Process? {
    if (cachedProcess_ == null) {
        cachedProcess_ = ActorSystem.default().processRegistry().get(this)
    }
    return cachedProcess_
}

internal fun PID.cachedProcess(registry: ProcessRegistryImpl): Process? {
    if (cachedProcess_ == null) {
        cachedProcess_ = registry.get(this)
    }
    return cachedProcess_
}

fun PID.toShortString(): String {
    return "$address/$id"
}

/**
 * 发送系统消息给指定的 PID
 * @param actorSystem Actor 系统
 * @param message 要发送的系统消息
 */
fun PID.sendSystemMessage(actorSystem: ActorSystem, message: SystemMessage) {
    val process = actorSystem.processRegistry().get(this)
    process.sendSystemMessage(this, message)
}

/**
 * 获取与此 PID 关联的 Actor 系统
 * @return Actor 系统
 */
fun PID.actorSystem(): ActorSystem {
    return ActorSystem.default()
}
