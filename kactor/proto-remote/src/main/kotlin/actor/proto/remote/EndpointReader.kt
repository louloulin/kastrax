package actor.proto.remote

import actor.proto.MessageEnvelope
import actor.proto.PID
import actor.proto.ProcessRegistry
import actor.proto.Terminated
import actor.proto.diagnostics.MatchType
import actor.proto.mailbox.SystemMessage
import actor.proto.request
import actor.proto.send
import actor.proto.sendSystemMessage
import io.grpc.stub.StreamObserver
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
class EndpointReader(private val remote: Remote? = null) : RemotingGrpc.RemotingImplBase() {
    override fun connect(request: RemoteProtos.ConnectRequest, responseObserver: StreamObserver<RemoteProtos.ConnectResponse>) {
        val response = RemoteProtos.ConnectResponse.newBuilder()
            .setMemberId(ProcessRegistry.address)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun receive(responseObserver: StreamObserver<RemoteProtos.Unit>): StreamObserver<RemoteProtos.MessageBatch> {
        return object : StreamObserver<RemoteProtos.MessageBatch> {
            override fun onCompleted() = responseObserver.onCompleted()
            override fun onError(err: Throwable): Unit = logger.error("Stream observer exception",err)
            override fun onNext(batch: RemoteProtos.MessageBatch) {
                receiveBatch(batch)
            }
        }
    }

    fun receiveBatch(batch: RemoteProtos.MessageBatch) {
        val typeNames = batch.typeNamesList
        val targetNames = batch.targetNamesList
        val senders = batch.sendersList

        for (envelope in batch.envelopesList) {
            val targetName = targetNames[envelope.target]
            val typeName = typeNames[envelope.typeId]
            val message = Serialization.deserialize(typeName, envelope.messageData, envelope.serializerId)

            // Create PID from target name
            val target = PID(ProcessRegistry.address, targetName)

            val sender = if (envelope.sender >= 0 && envelope.sender < senders.size) {
                senders[envelope.sender]
            } else null

            when (message) {
                is Terminated -> {
                    // Convert ActorProtos.PID to Protos.PID
                    val who = PID(message.who.address, message.who.id)
                    send(Remote.endpointManagerPid, RemoteTerminate(target, who))
                }
                is SystemMessage -> sendSystemMessage(target, message)
                else -> {
                    if (sender != null) {
                        // Convert ActorProtos.PID to Protos.PID
                        val senderPid = PID(sender.address, sender.id)
                        request(target, message, senderPid)
                    } else {
                        send(target, message)
                    }
                }
            }
        }
    }




}


