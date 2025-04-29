package actor.proto.remote

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.RestartStatistics
import actor.proto.Started
import actor.proto.Supervisor
import actor.proto.SupervisorStrategy
import actor.proto.fromProducer
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}
class EndpointManager(private val config: RemoteConfig) : Actor, SupervisorStrategy {
    companion object {
        private val channels = ConcurrentHashMap<String, ManagedChannel>()

        /**
         * Get or create a gRPC channel for the given address
         * @param address The address to connect to
         * @return The gRPC channel
         */
        fun getChannel(address: String): ManagedChannel {
            return channels.getOrPut(address) {
                val parts = address.split(":")
                val host = parts[0]
                val port = parts[1].toInt()

                ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .build()
            }
        }
    }


    private val _connections: HashMap<String, Endpoint> = HashMap()
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> logger.info("Started EndpointManager")
            is EndpointTerminatedEvent ->  ensureConnected(msg.address).watcher.let { send(it,msg) }
            is RemoteTerminate -> ensureConnected(msg.watchee.address).watcher.let {send(it,msg) }
            is RemoteWatch -> ensureConnected(msg.watchee.address).watcher.let { send(it,msg) }
            is RemoteUnwatch -> ensureConnected(msg.watchee.address).watcher.let { send(it,msg) }
            is RemoteDeliver -> ensureConnected(msg.target.address).writer.let { send(it,msg) }
            else -> {
            }
        }
    }

    override fun handleFailure(actorSystem: ActorSystem, supervisor: Supervisor, child: PID, restartStatistics: RestartStatistics, reason: Any, message: Any?) {
        supervisor.restartChildren(child)
    }

    private fun Context.ensureConnected(address: String): Endpoint = _connections.getOrPut(address, {
        val writer: PID = spawnWriter(address)
        val watcher: PID = spawnWatcher(address)
        Endpoint(writer, watcher)
    })

    private fun Context.spawnWriter(address: String): PID {
        val writerProps: Props = fromProducer { EndpointWriter(address, config) }.withMailbox { EndpointWriterMailbox(config.endpointWriterBatchSize) }
        val writer: PID = spawnChild(writerProps)
        return writer
    }

    private fun Context.spawnWatcher(address: String): PID {
        val watcherProps: Props = fromProducer { EndpointWatcher(address) }
        val watcher: PID = spawnChild(watcherProps)
        return watcher
    }
}
