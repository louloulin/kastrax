package actor.proto.cluster

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.cluster.consensus.Consensus
import actor.proto.cluster.consensus.ConsensusCheckBuilder
import actor.proto.cluster.consensus.ConsensusManager
import actor.proto.cluster.informer.Informer
import actor.proto.remote.Remote
import actor.proto.remote.RemoteConfig
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Cluster represents a cluster of actor systems.
 * It manages membership, gossip, and virtual actors.
 */
class Cluster(
    val actorSystem: ActorSystem,
    val config: ClusterConfig
) {
    private val kinds = mutableMapOf<String, Kind>()
    private val activatedKinds = mutableMapOf<String, ActivatedKind>()

    lateinit var memberList: MemberList
    lateinit var pidCache: PidCache
    lateinit var identityLookup: IdentityLookup
    lateinit var gossip: Gossiper
    lateinit var pubSub: PubSub
    lateinit var remote: Remote
    lateinit var informer: Informer
    lateinit var consensusManager: ConsensusManager

    /**
     * Start the cluster as a member.
     */
    suspend fun startMember(): Boolean {
        logger.info { "Starting Proto.Actor cluster member at ${actorSystem.address}" }

        // Initialize components
        remote = Remote.create(actorSystem, config.remoteConfig)
        pidCache = PidCache()
        memberList = MemberList(this)

        // Initialize identity lookup
        identityLookup = config.identityLookup
        identityLookup.setup(this, getClusterKinds(), false)

        // Initialize gossip
        gossip = Gossiper(this)

        // Initialize pubsub
        pubSub = PubSub(this)

        // Initialize informer
        informer = config.informer
        informer.initialize(this)

        // Initialize consensus manager
        consensusManager = ConsensusManager(this)
        consensusManager.start()

        // Initialize kinds
        initKinds()

        // Start remote
        remote.start(config.remoteConfig.hostname, config.remoteConfig.port)

        // Start cluster provider
        return config.clusterProvider.startMember(this)
    }

    /**
     * Start the cluster as a client.
     */
    suspend fun startClient(): Boolean {
        logger.info { "Starting Proto.Actor cluster client at ${actorSystem.address}" }

        // Initialize components
        remote = Remote.create(actorSystem, config.remoteConfig)
        pidCache = PidCache()
        memberList = MemberList(this)

        // Initialize identity lookup
        identityLookup = config.identityLookup
        identityLookup.setup(this, getClusterKinds(), true)

        // Initialize gossip
        gossip = Gossiper(this)

        // Initialize pubsub
        pubSub = PubSub(this)

        // Initialize informer
        informer = config.informer
        informer.initialize(this)

        // Start remote
        remote.start(config.remoteConfig.hostname, config.remoteConfig.port)

        // Start cluster provider
        return config.clusterProvider.startClient(this)
    }

    /**
     * Shutdown the cluster.
     * @param graceful Whether to shutdown gracefully.
     */
    suspend fun shutdown(graceful: Boolean): Boolean {
        logger.info { "Shutting down Proto.Actor cluster at ${actorSystem.address}" }

        // Shutdown cluster provider
        val result = config.clusterProvider.shutdown(graceful)

        // Shutdown informer
        informer.shutdown()

        // Shutdown consensus manager
        consensusManager.stop()

        // Shutdown remote
        if (graceful) {
            remote.shutdown(true)
        } else {
            remote.shutdown(false)
        }

        return result
    }

    /**
     * Register a kind with the cluster.
     * @param kind The kind to register.
     */
    fun registerKind(kind: Kind) {
        kinds[kind.name] = kind
    }

    /**
     * Get a kind by name.
     * @param name The name of the kind.
     * @return The kind, or null if not found.
     */
    fun getKind(name: String): Kind? {
        return kinds[name]
    }

    /**
     * Get all registered kinds.
     * @return A map of kind names to kinds.
     */
    fun getClusterKinds(): Map<String, Kind> {
        return kinds.toMap()
    }

    /**
     * Register a consensus check.
     * @param key The key of the consensus check.
     * @param builder The consensus check builder.
     * @param buildAll Whether to build all consensus checks.
     * @return The consensus handler.
     */
    fun registerConsensusCheck(key: String, builder: ConsensusCheckBuilder, buildAll: Boolean = true): Consensus {
        val check = if (buildAll) {
            actor.proto.cluster.consensus.AllConsensusCheck(builder.buildChecks())
        } else {
            actor.proto.cluster.consensus.AnyConsensusCheck(builder.buildChecks())
        }
        return consensusManager.registerCheck(key, check)
    }

    /**
     * Get a consensus handler.
     * @param key The key of the consensus handler.
     * @return The consensus handler, or null if not found.
     */
    fun getConsensus(key: String): Consensus? {
        return consensusManager.getConsensus(key)
    }

    /**
     * Get a virtual actor by identity and kind.
     * @param identity The identity of the actor.
     * @param kind The kind of the actor.
     * @return The PID of the actor.
     */
    suspend fun get(identity: String, kind: String): PID {
        val clusterIdentity = ClusterIdentity(identity, kind)
        return identityLookup.lookup(clusterIdentity)
    }

    /**
     * Get a virtual actor by cluster identity.
     * @param clusterIdentity The cluster identity of the actor.
     * @return The PID of the actor.
     */
    suspend fun get(clusterIdentity: ClusterIdentity): PID {
        return identityLookup.lookup(clusterIdentity)
    }

    /**
     * Initialize the registered kinds.
     */
    private fun initKinds() {
        for ((name, kind) in kinds) {
            val activatedKind = ActivatedKind(kind)
            activatedKinds[name] = activatedKind
        }
    }

    companion object {
        /**
         * Create a new cluster.
         * @param actorSystem The actor system to use.
         * @param config The cluster configuration.
         * @return A new cluster.
         */
        fun create(actorSystem: ActorSystem, config: ClusterConfig): Cluster {
            return Cluster(actorSystem, config)
        }
    }
}
