package actor.proto.benchmarks.cluster

// 暂时禁用 ClusterGrainBenchmark 类，等待 proto-cluster 模块修复
/*
import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.Props
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterConfig
import actor.proto.cluster.Kind
import actor.proto.cluster.grain.Grain
import actor.proto.cluster.grain.GrainBase
import actor.proto.cluster.grain.GrainFactory
import actor.proto.cluster.providers.AutoManagedClusterProvider
import actor.proto.cluster.providers.DistributedHashIdentityLookup
import actor.proto.fromProducer
import actor.proto.remote.RemoteConfig
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
*/

/**
 * Benchmark for cluster grain messaging.
 *
 * Note: This benchmark requires a cluster to be set up.
 * You can use the ClusterBenchmarkNode class to start cluster nodes.
 */
// 暂时禁用 ClusterGrainBenchmark 类，等待 proto-cluster 模块修复
/*
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
open class ClusterGrainBenchmark {

    private lateinit var system: ActorSystem
    private lateinit var cluster: Cluster

    @Param("100")
    var grainCount: Int = 0

    @Param("10")
    var messagesPerGrain: Int = 0

    @Setup(Level.Trial)
    fun setup() {
        system = ActorSystem("benchmark-client")

        // Configure remote
        val remoteConfig = RemoteConfig.create("localhost", 0)

        // Configure cluster provider
        val clusterProvider = AutoManagedClusterProvider(
            port = 0,
            seedNodes = listOf("localhost:8090")
        )

        // Configure identity lookup
        val identityLookup = DistributedHashIdentityLookup()

        // Configure cluster
        val clusterConfig = ClusterConfig.create(
            name = "benchmark-cluster",
            clusterProvider = clusterProvider,
            identityLookup = identityLookup,
            remoteConfig = remoteConfig
        )

        // Create cluster
        cluster = Cluster.create(system, clusterConfig)

        // Start cluster as a client
        kotlinx.coroutines.runBlocking {
            cluster.startClient()
        }
    }

    @TearDown(Level.Trial)
    fun teardown() {
        kotlinx.coroutines.runBlocking {
            cluster.shutdown(true)
        }
    }

    @Benchmark
    fun grainCalls(bh: Blackhole) {
        val latch = CountDownLatch(grainCount)

        for (i in 0 until grainCount) {
            val grain = GrainFactory.getGrain(TestGrain::class, "grain-$i", cluster)

            kotlinx.coroutines.runBlocking {
                for (j in 0 until messagesPerGrain) {
                    grain.increment()
                }

                val count = grain.getCount()
                bh.consume(count)

                latch.countDown()
            }
        }

        latch.await(30, TimeUnit.SECONDS)
    }

    interface TestGrain : Grain {
        suspend fun increment()
        suspend fun getCount(): Int
    }

    class TestGrainActor : Actor {
        private var count = 0

        override suspend fun Context.receive(msg: Any) {
            when (msg) {
                is String -> {
                    when (msg) {
                        "increment" -> count++
                        "getCount" -> sender?.let { send(it, count) }
                    }
                }
            }
        }
    }

    class TestGrainReference(
        cluster: Cluster,
        identity: String,
        kind: String
    ) : GrainBase(cluster, identity, kind), TestGrain {

        override suspend fun increment() {
            send("increment")
        }

        override suspend fun getCount(): Int {
            return request("getCount", 1000)
        }
    }
}
*/

/**
 * Node for the cluster benchmark.
 */
/*
object ClusterBenchmarkNode {
    @JvmStatic
    fun main(args: Array<String>) {
        val system = ActorSystem("benchmark-node")

        // Configure remote
        val remoteConfig = RemoteConfig.create("localhost", 8090)

        // Configure cluster provider
        val clusterProvider = AutoManagedClusterProvider(
            port = 8090,
            seedNodes = listOf("localhost:8090")
        )

        // Configure identity lookup
        val identityLookup = DistributedHashIdentityLookup()

        // Configure cluster
        val clusterConfig = ClusterConfig.create(
            name = "benchmark-cluster",
            clusterProvider = clusterProvider,
            identityLookup = identityLookup,
            remoteConfig = remoteConfig
        )

        // Create cluster
        val cluster = Cluster.create(system, clusterConfig)

        // Register the test grain kind
        val props = fromProducer { ClusterGrainBenchmark.TestGrainActor() }
        cluster.registerKind(Kind("TestGrain", props))

        // Start cluster as a member
        kotlinx.coroutines.runBlocking {
            cluster.startMember()
        }

        println("Cluster benchmark node started at localhost:8090")

        // Keep the node running
        Thread.sleep(Long.MAX_VALUE)
    }
}
*/