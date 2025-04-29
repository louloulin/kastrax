package actor.proto.cluster

import actor.proto.cluster.informer.DefaultInformer
import actor.proto.cluster.informer.Informer
import actor.proto.remote.RemoteConfig
import java.time.Duration

/**
 * ClusterConfig contains configuration for a cluster.
 */
data class ClusterConfig(
    val name: String,
    val clusterProvider: ClusterProvider,
    val identityLookup: IdentityLookup,
    val remoteConfig: RemoteConfig,
    val requestTimeout: Duration = Duration.ofSeconds(5),
    val gossipInterval: Duration = Duration.ofMillis(300),
    val gossipRequestTimeout: Duration = Duration.ofMillis(500),
    val gossipFanOut: Int = 3,
    val gossipMaxSend: Int = 50,
    val heartbeatExpiration: Duration = Duration.ofSeconds(20),
    val memberStrategyBuilder: (Cluster, String) -> MemberStrategy = { _, _ -> RoundRobinMemberStrategy() },
    val informer: Informer = DefaultInformer()
) {
    companion object {
        /**
         * Create a new cluster configuration.
         * @param name The name of the cluster.
         * @param clusterProvider The cluster provider to use.
         * @param identityLookup The identity lookup to use.
         * @param remoteConfig The remote configuration to use.
         * @param options Additional configuration options.
         * @return A new cluster configuration.
         */
        fun create(
            name: String,
            clusterProvider: ClusterProvider,
            identityLookup: IdentityLookup,
            remoteConfig: RemoteConfig,
            vararg options: ClusterConfigOption
        ): ClusterConfig {
            val config = ClusterConfig(
                name = name,
                clusterProvider = clusterProvider,
                identityLookup = identityLookup,
                remoteConfig = remoteConfig
            )

            return options.fold(config) { acc, option ->
                option.apply(acc)
            }
        }
    }
}

/**
 * ClusterConfigOption is an option for configuring a cluster.
 */
interface ClusterConfigOption {
    /**
     * Apply the option to the configuration.
     * @param config The configuration to apply the option to.
     * @return The updated configuration.
     */
    fun apply(config: ClusterConfig): ClusterConfig
}

/**
 * WithRequestTimeout sets the request timeout for the cluster.
 * @param timeout The request timeout.
 */
class WithRequestTimeout(private val timeout: Duration) : ClusterConfigOption {
    override fun apply(config: ClusterConfig): ClusterConfig {
        return config.copy(requestTimeout = timeout)
    }
}

/**
 * WithGossipInterval sets the gossip interval for the cluster.
 * @param interval The gossip interval.
 */
class WithGossipInterval(private val interval: Duration) : ClusterConfigOption {
    override fun apply(config: ClusterConfig): ClusterConfig {
        return config.copy(gossipInterval = interval)
    }
}

/**
 * WithGossipRequestTimeout sets the gossip request timeout for the cluster.
 * @param timeout The gossip request timeout.
 */
class WithGossipRequestTimeout(private val timeout: Duration) : ClusterConfigOption {
    override fun apply(config: ClusterConfig): ClusterConfig {
        return config.copy(gossipRequestTimeout = timeout)
    }
}

/**
 * WithGossipFanOut sets the gossip fan-out for the cluster.
 * @param fanOut The gossip fan-out.
 */
class WithGossipFanOut(private val fanOut: Int) : ClusterConfigOption {
    override fun apply(config: ClusterConfig): ClusterConfig {
        return config.copy(gossipFanOut = fanOut)
    }
}

/**
 * WithGossipMaxSend sets the maximum number of gossip messages to send.
 * @param maxSend The maximum number of gossip messages to send.
 */
class WithGossipMaxSend(private val maxSend: Int) : ClusterConfigOption {
    override fun apply(config: ClusterConfig): ClusterConfig {
        return config.copy(gossipMaxSend = maxSend)
    }
}

/**
 * WithHeartbeatExpiration sets the heartbeat expiration for the cluster.
 * @param expiration The heartbeat expiration.
 */
class WithHeartbeatExpiration(private val expiration: Duration) : ClusterConfigOption {
    override fun apply(config: ClusterConfig): ClusterConfig {
        return config.copy(heartbeatExpiration = expiration)
    }
}

/**
 * WithMemberStrategyBuilder sets the member strategy builder for the cluster.
 * @param builder The member strategy builder.
 */
class WithMemberStrategyBuilder(private val builder: (Cluster, String) -> MemberStrategy) : ClusterConfigOption {
    override fun apply(config: ClusterConfig): ClusterConfig {
        return config.copy(memberStrategyBuilder = builder)
    }
}

/**
 * WithInformer sets the informer for the cluster.
 * @param informer The informer to use.
 */
class WithInformer(private val informer: Informer) : ClusterConfigOption {
    override fun apply(config: ClusterConfig): ClusterConfig {
        return config.copy(informer = informer)
    }
}
