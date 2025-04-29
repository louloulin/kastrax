package actor.proto.benchmarks.local

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.mailbox.DefaultMailbox
import actor.proto.mailbox.newUnboundedMailbox
import actor.proto.send
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Benchmark for mailbox performance.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
open class MailboxBenchmark {

    private lateinit var system: ActorSystem
    private lateinit var counterActor: PID

    @Param("1000", "10000", "100000")
    var messageCount: Int = 0

    @Setup(Level.Trial)
    fun setup() {
        system = ActorSystem("benchmark")
    }

    @TearDown(Level.Iteration)
    fun teardownIteration() {
        system.stop(counterActor)
    }

    @TearDown(Level.Trial)
    fun teardown() {
        // Clean up
    }

    @Benchmark
    fun unboundedMailbox(bh: Blackhole) {
        val props = fromProducer { CounterActor() }
            .withMailbox { newUnboundedMailbox() }

        counterActor = system.actorOf(props)

        val latch = CountDownLatch(1)
        system.send(counterActor, StartMessage(messageCount, latch))
        latch.await(30, TimeUnit.SECONDS)
        bh.consume(latch)
    }

    @Benchmark
    fun defaultMailbox(bh: Blackhole) {
        val props = fromProducer { CounterActor() }
            .withMailbox { DefaultMailbox(java.util.concurrent.ConcurrentLinkedQueue(), java.util.concurrent.ConcurrentLinkedQueue()) }

        counterActor = system.actorOf(props)

        val latch = CountDownLatch(1)
        system.send(counterActor, StartMessage(messageCount, latch))
        latch.await(30, TimeUnit.SECONDS)
        bh.consume(latch)
    }

    class CounterActor : Actor {
        private var count = 0
        private var target = 0
        private var latch: CountDownLatch? = null

        override suspend fun Context.receive(msg: Any) {
            when (msg) {
                is StartMessage -> {
                    target = msg.count
                    latch = msg.latch

                    // Send all messages to self
                    for (i in 0 until target) {
                        send(self, IncrementMessage)
                    }
                }
                is IncrementMessage -> {
                    count++
                    if (count >= target) {
                        latch?.countDown()
                    }
                }
            }
        }
    }

    data class StartMessage(val count: Int, val latch: CountDownLatch)
    object IncrementMessage

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Run the benchmark using JMH
            org.openjdk.jmh.Main.main(args)
        }
    }
}
