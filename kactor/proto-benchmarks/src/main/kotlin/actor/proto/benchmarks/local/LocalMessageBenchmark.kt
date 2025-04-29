package actor.proto.benchmarks.local

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
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
 * Benchmark for local actor messaging.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
open class LocalMessageBenchmark {
    
    private lateinit var system: ActorSystem
    private lateinit var echoActor: PID
    private lateinit var pingActor: PID
    
    @Setup(Level.Trial)
    fun setup() {
        system = ActorSystem("benchmark")
        
        // Create echo actor
        val echoProps = fromProducer { EchoActor() }
        echoActor = system.actorOf(echoProps)
        
        // Create ping actor
        val pingProps = fromProducer { PingActor(echoActor) }
        pingActor = system.actorOf(pingProps)
    }
    
    @TearDown(Level.Trial)
    fun teardown() {
        system.stop(pingActor)
        system.stop(echoActor)
    }
    
    @Benchmark
    fun pingPong(bh: Blackhole) {
        val latch = CountDownLatch(1)
        system.send(pingActor, PingMessage(latch))
        latch.await(5, TimeUnit.SECONDS)
        bh.consume(latch)
    }
    
    @Benchmark
    fun throughput(bh: Blackhole) {
        val count = 1000
        val latch = CountDownLatch(1)
        system.send(pingActor, ThroughputMessage(count, latch))
        latch.await(5, TimeUnit.SECONDS)
        bh.consume(latch)
    }
    
    class EchoActor : Actor {
        override suspend fun Context.receive(msg: Any) {
            when (msg) {
                is PingMessage -> {
                    sender?.let { send(it, PongMessage()) }
                }
                is String -> {
                    sender?.let { send(it, msg) }
                }
            }
        }
    }
    
    class PingActor(private val echoActor: PID) : Actor {
        override suspend fun Context.receive(msg: Any) {
            when (msg) {
                is PingMessage -> {
                    send(echoActor, msg)
                }
                is PongMessage -> {
                    (message as? PingMessage)?.latch?.countDown()
                }
                is ThroughputMessage -> {
                    val count = msg.count
                    val latch = msg.latch
                    
                    for (i in 0 until count) {
                        send(echoActor, "message-$i")
                    }
                    
                    latch.countDown()
                }
            }
        }
    }
    
    data class PingMessage(val latch: CountDownLatch)
    class PongMessage
    data class ThroughputMessage(val count: Int, val latch: CountDownLatch)
}
