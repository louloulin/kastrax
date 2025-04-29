package actor.proto.cluster.grain

import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.Props
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterConfig
import actor.proto.cluster.Kind
import actor.proto.cluster.providers.AutoManagedClusterProvider
import actor.proto.cluster.providers.DistributedHashIdentityLookup
import actor.proto.fromProducer
import actor.proto.persistence.Provider
import actor.proto.remote.RemoteConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GrainPersistenceTest {

    private lateinit var system: ActorSystem
    private lateinit var cluster: Cluster
    private lateinit var provider: Provider

    @BeforeEach
    fun setup() {
        system = ActorSystem("test")

        // Configure remote
        val remoteConfig = RemoteConfig.create("localhost", 0)

        // Configure cluster provider
        val clusterProvider = AutoManagedClusterProvider(
            port = 0,
            seedNodes = emptyList()
        )

        // Configure identity lookup
        val identityLookup = DistributedHashIdentityLookup()

        // Configure cluster
        val clusterConfig = ClusterConfig.create(
            name = "test-cluster",
            clusterProvider = clusterProvider,
            identityLookup = identityLookup,
            remoteConfig = remoteConfig
        )

        // Create cluster
        cluster = Cluster.create(system, clusterConfig)

        // Register the counter grain kind
        val props = fromProducer { CounterGrainActor() }
        cluster.registerKind(Kind("CounterGrain", props))

        // Start cluster as a member
        runBlocking {
            cluster.startMember()
        }

        // Create a mock persistence provider
        provider = object : Provider {}
    }

    @AfterEach
    fun teardown() {
        runBlocking {
            cluster.shutdown(true)
        }
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires proper implementation of persistence provider")
    fun `should persist and recover grain state`() = runBlocking {
        // Get a grain instance
        val grain = GrainFactory.getGrainWithPersistence(
            CounterGrain::class,
            "counter-1",
            cluster,
            provider
        )

        // Increment the counter a few times
        grain.increment()
        grain.increment()
        grain.increment()

        // Get the count
        val count = grain.getCount()
        assertEquals(3, count)

        // Create a new grain instance with the same identity
        val grain2 = GrainFactory.getGrainWithPersistence(
            CounterGrain::class,
            "counter-1",
            cluster,
            provider
        )

        // Get the count from the new instance
        val count2 = grain2.getCount()
        assertEquals(3, count2)
    }

    interface CounterGrain : Grain {
        suspend fun increment()
        suspend fun decrement()
        suspend fun getCount(): Int
    }

    class CounterGrainActor : GrainPersistentActor<Int>() {
        override fun initialState(): Int = 0

        override suspend fun handleCommand(context: Context, message: Any) {
            when (message) {
                is String -> {
                    when (message) {
                        "increment" -> {
                            val newState = getState() + 1
                            setState(newState)
                            // persistReceive is not available in this mock implementation
                            // Use setState instead
                            setState(newState)
                        }
                        "decrement" -> {
                            val newState = getState() - 1
                            setState(newState)
                            // persistReceive is not available in this mock implementation
                            // Use setState instead
                            setState(newState)
                        }
                        "getCount" -> {
                            context.sender?.let { context.send(it, getState()) }
                        }
                    }
                }
            }
        }
    }

    class GrainReference(
        cluster: Cluster,
        identity: String,
        kind: String
    ) : GrainBase(cluster, identity, kind), CounterGrain {

        override suspend fun increment() {
            send("increment")
        }

        override suspend fun decrement() {
            send("decrement")
        }

        override suspend fun getCount(): Int {
            return request("getCount", 1000)
        }
    }
}
