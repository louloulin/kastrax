package actor.proto.remote

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import actor.proto.ProcessRegistry
import actor.proto.Props
import actor.proto.spawnNamed


class Activator : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is RemoteProtos.ActorPidRequest -> {
                try {
                    val props: Props = Remote.getKnownKind(msg.kind)
                    val name: String = when {
                        msg.name.isEmpty() -> msg.name
                        else -> ProcessRegistry.nextId()
                    }

                    // Check if process name already exists
                    if (!msg.name.isEmpty() && ProcessRegistry.get(msg.name) != null) {
                        val res = ActorPidResponse(PID("", ""), ResponseStatusCode.PROCESS_NAME_ALREADY_EXIST)
                        respond(res)
                        return
                    }

                    val pid: PID = spawnNamed(props, name)
                    val res = ActorPidResponse(pid, ResponseStatusCode.OK)
                    respond(res)
                } catch (e: Exception) {
                    // Handle errors
                    val res = ActorPidResponse(PID("", ""), ResponseStatusCode.ERROR)
                    respond(res)
                }
            }
            else -> {
            }
        }
    }
}

