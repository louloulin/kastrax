package actor.proto.remote

import actor.proto.PID
import com.google.protobuf.ByteString

data class EndpointTerminatedEvent(var address: String)
data class RemoteTerminate(val watcher: PID, val watchee: PID)
data class RemoteWatch(val watcher: PID, val watchee: PID)
data class RemoteUnwatch(val watcher: PID, val watchee: PID)
data class RemoteDeliver(val message: Any, val target: PID, val sender: PID?, val serializerId: Int)
data class JsonMessage(val typeName: String, val json: String)

fun ActorPidRequest(kind: String, name: String): RemoteProtos.ActorPidRequest {
    val builder = RemoteProtos.ActorPidRequest.newBuilder()
    builder.kind = kind
    builder.name = name
    return builder.build()
}

fun MessageEnvelope(bytes: ByteString, sender: PID?, targetId: Int, typeId: Int, serializerId: Int): RemoteProtos.MessageEnvelope {
    val builder = RemoteProtos.MessageEnvelope.newBuilder()
    builder.messageData = bytes
    if (sender != null) {
        builder.sender = 0 // TODO: Fix this
    }
    builder.target = targetId
    builder.typeId = typeId
    builder.serializerId = serializerId
    return builder.build()
}

fun ConnectRequest(): RemoteProtos.ConnectRequest {
    val builder = RemoteProtos.ConnectRequest.newBuilder()
    return builder.build()
}


fun ActorPidResponse(pid: PID, statusCode: ResponseStatusCode = ResponseStatusCode.OK): RemoteProtos.ActorPidResponse {
    val builder = RemoteProtos.ActorPidResponse.newBuilder()
    val protoPid = actor.proto.ActorProtos.PID.newBuilder()
        .setAddress(pid.address)
        .setId(pid.id)
        .build()
    builder.pid = protoPid
    builder.statusCode = RemoteProtos.ResponseStatusCode.valueOf(statusCode.name)
    return builder.build()
}

