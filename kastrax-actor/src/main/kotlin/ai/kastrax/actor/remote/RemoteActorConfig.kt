package ai.kastrax.actor.remote

import actor.proto.remote.RemoteConfig as KactorRemoteConfig

/**
 * 远程 Actor 系统配置
 *
 * @property hostname 主机地址，默认为 "0.0.0.0"
 * @property port 端口号，默认为 8090
 * @property advertisedHostname 对外公布的主机名，默认为 "localhost"
 * @property advertisedPort 对外公布的端口，默认为 port
 */
data class RemoteActorConfig(
    val hostname: String = "0.0.0.0",
    val port: Int = 8090,
    val advertisedHostname: String = "localhost",
    val advertisedPort: Int = port
) {
    /**
     * 转换为 kactor 的 RemoteConfig
     */
    fun toKactorRemoteConfig(): KactorRemoteConfig {
        return KactorRemoteConfig(
            hostname = hostname,
            port = port,
            advertisedHostname = advertisedHostname,
            advertisedPort = advertisedPort
        )
    }
}
