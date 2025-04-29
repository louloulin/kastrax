package actor.proto.benchmarks.remote

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.remote.Remote
import actor.proto.remote.RemoteConfig
import actor.proto.send
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Benchmark for remote actor messaging.
 *
 * Note: This benchmark requires two separate JVM processes to run.
 * One process should run the server, and the other should run the benchmark.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
open class RemoteMessageBenchmark {

    private lateinit var system: ActorSystem
    private lateinit var remote: Remote
    private lateinit var pingActor: PID
    private lateinit var remotePID: PID

    @Setup(Level.Trial)
    fun setup() {
        system = ActorSystem("benchmark-client")

        // Configure remote
        val remoteConfig = RemoteConfig.create("localhost", 8090)
        remote = Remote.create(system, remoteConfig)
        remote.start("localhost", 8090)

        // Create ping actor
        val pingProps = fromProducer { PingActor() }
        pingActor = system.actorOf(pingProps)

        // Create remote PID
        remotePID = PID("localhost:8091", "echo")
    }

    @TearDown(Level.Trial)
    fun teardown() {
        system.stop(pingActor)
        remote.shutdown(true)
    }

    @Benchmark
    fun pingPong(bh: Blackhole) {
        val latch = CountDownLatch(1)
        system.send(pingActor, PingMessage(remotePID, latch))
        latch.await(5, TimeUnit.SECONDS)
        bh.consume(latch)
    }

    @Benchmark
    fun throughput(bh: Blackhole) {
        val count = 1000
        val latch = CountDownLatch(1)
        system.send(pingActor, ThroughputMessage(remotePID, count, latch))
        latch.await(30, TimeUnit.SECONDS)
        bh.consume(latch)
    }

    class PingActor : Actor {
        override suspend fun Context.receive(msg: Any) {
            when (msg) {
                is PingMessage -> {
                    send(msg.target, "ping")
                    msg.latch.countDown()
                }
                is ThroughputMessage -> {
                    val count = msg.count
                    val target = msg.target

                    for (i in 0 until count) {
                        send(target, "message-$i")
                    }

                    msg.latch.countDown()
                }
            }
        }
    }

    data class PingMessage(val target: PID, val latch: CountDownLatch)
    data class ThroughputMessage(val target: PID, val count: Int, val latch: CountDownLatch)
}

/**
 * Server for the remote benchmark.
 */
object RemoteBenchmarkServer {
    @JvmStatic
    fun main(args: Array<String>) {
        val system = ActorSystem("benchmark-server")

        // Configure remote
        val remoteConfig = RemoteConfig.create("localhost", 8091)
        val remote = Remote.create(system, remoteConfig)

        // Create echo actor
        val echoProps = fromProducer { EchoActor() }
        system.actorOf(echoProps, "echo")

        // Start remote
        remote.start("localhost", 8091)

        println("Remote benchmark server started at localhost:8091")

        // Keep the server running
        Thread.sleep(Long.MAX_VALUE)
    }

    class EchoActor : Actor {
        override suspend fun Context.receive(msg: Any) {
            // Just echo the message back
            sender?.let { send(it, msg) }
        }
    }
}
