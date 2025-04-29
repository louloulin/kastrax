package actor.proto.benchmarks.local

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
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
import java.util.concurrent.TimeUnit

/**
 * Benchmark for actor creation.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
open class ActorCreationBenchmark {
    
    private lateinit var system: ActorSystem
    private lateinit var props: Props
    private val actors = mutableListOf<PID>()
    
    @Param("1000")
    var actorCount: Int = 0
    
    @Setup(Level.Trial)
    fun setup() {
        system = ActorSystem("benchmark")
        props = fromProducer { EmptyActor() }
    }
    
    @TearDown(Level.Iteration)
    fun teardownIteration() {
        actors.forEach { system.stop(it) }
        actors.clear()
    }
    
    @TearDown(Level.Trial)
    fun teardown() {
        // Clean up
    }
    
    @Benchmark
    fun createActors(bh: Blackhole) {
        for (i in 0 until actorCount) {
            val pid = system.actorOf(props)
            actors.add(pid)
            bh.consume(pid)
        }
    }
    
    @Benchmark
    fun createNamedActors(bh: Blackhole) {
        for (i in 0 until actorCount) {
            val pid = system.actorOf(props, "actor-$i")
            actors.add(pid)
            bh.consume(pid)
        }
    }
    
    class EmptyActor : Actor {
        override suspend fun Context.receive(msg: Any) {
            // Do nothing
        }
    }
}
