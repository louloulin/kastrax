package actor.proto.remote

data class RemoteConfig(
        val hostname: String,
        val port: Int,
        val endpointWriterBatchSize: Int = 1000,
        val advertisedHostname: String? = null,
        val advertisedPort: Int? = null,
        val idleTimeout: Long? = null,
        val keepAliveTime: Long? = null,
        val keepAliveTimeout: Long? = null,
        val keepAliveWithoutCalls : Boolean? = null,
        val usePlainText : Boolean = true,
        val enableBlocklist: Boolean = true,
        val blocklistMaxFailures: Int = 3,
        val blocklistDuration: java.time.Duration = java.time.Duration.ofMinutes(5),
        val blocklistCleanupInterval: java.time.Duration = java.time.Duration.ofMinutes(1)
) {
    companion object {
        fun create(hostname: String, port: Int): RemoteConfig {
            return RemoteConfig(hostname, port)
        }
    }
}
