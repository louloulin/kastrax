package actor.proto.cluster.informer

import actor.proto.cluster.Cluster
import actor.proto.cluster.Member
import actor.proto.cluster.MemberStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * KubernetesInformer 是基于 Kubernetes API 的 Informer 实现
 * 它通过 Kubernetes API 监控 Pod 的状态，并将其映射为集群成员
 */
class KubernetesInformer(
    private val namespace: String,
    private val labelSelector: String,
    private val pollInterval: Duration = Duration.ofSeconds(5),
    private val serviceAccountToken: String? = null
) : Informer {
    private val lock = ReentrantReadWriteLock()
    private val members = ConcurrentHashMap<String, Member>()
    private lateinit var cluster: Cluster
    private val scope = CoroutineScope(Dispatchers.Default)
    private var pollJob: Job? = null

    /**
     * 初始化Informer
     * @param cluster 集群实例
     */
    override fun initialize(cluster: Cluster) {
        this.cluster = cluster
        startPolling()
        logger.info { "KubernetesInformer initialized for cluster ${cluster.config.name} in namespace $namespace" }
    }

    /**
     * 获取集群成员列表
     * @return 集群成员列表
     */
    override fun getMembers(): List<Member> {
        return lock.read { members.values.toList() }
    }

    /**
     * 获取集群成员
     * @param id 成员ID
     * @return 成员信息，如果不存在则返回null
     */
    override fun getMember(id: String): Member? {
        return lock.read { members[id] }
    }

    /**
     * 添加集群成员
     * @param member 要添加的成员
     */
    override fun addMember(member: Member) {
        lock.write {
            val oldMember = members[member.id]
            members[member.id] = member

            // 发布成员加入事件
            if (oldMember == null) {
                val event = MemberJoinedEvent(member)
                cluster.actorSystem.eventStream().publish(event)
                logger.info { "Member joined: ${member.id} (${member.host}:${member.port})" }
            } else if (oldMember.status != member.status) {
                // 发布状态变化事件
                val event = MemberStatusChangedEvent(
                    memberId = member.id,
                    oldStatus = oldMember.status,
                    newStatus = member.status
                )
                cluster.actorSystem.eventStream().publish(event)
                logger.info { "Member status changed: ${member.id} ${oldMember.status} -> ${member.status}" }
            }
        }
    }

    /**
     * 移除集群成员
     * @param id 要移除的成员ID
     */
    override fun removeMember(id: String) {
        lock.write {
            val oldMember = members.remove(id)

            // 发布成员离开事件
            if (oldMember != null) {
                val event = MemberLeftEvent(id)
                cluster.actorSystem.eventStream().publish(event)
                logger.info { "Member left: $id" }
            }
        }
    }

    /**
     * 更新成员状态
     * @param id 成员ID
     * @param status 新的状态
     */
    override fun updateMemberStatus(id: String, status: MemberStatus) {
        lock.write {
            val member = members[id] ?: return@write
            val oldStatus = member.status
            
            if (oldStatus != status) {
                members[id] = member.copy(status = status)
                
                // 发布状态变化事件
                val event = MemberStatusChangedEvent(
                    memberId = id,
                    oldStatus = oldStatus,
                    newStatus = status
                )
                cluster.actorSystem.eventStream().publish(event)
                logger.info { "Member status changed: $id $oldStatus -> $status" }
            }
        }
    }

    /**
     * 关闭Informer
     */
    override fun shutdown() {
        pollJob?.cancel()
        lock.write {
            members.clear()
        }
        logger.info { "KubernetesInformer shutdown" }
    }

    /**
     * 开始轮询 Kubernetes API
     */
    private fun startPolling() {
        pollJob = scope.launch {
            while (true) {
                try {
                    pollKubernetesAPI()
                } catch (e: Exception) {
                    logger.error(e) { "Error polling Kubernetes API" }
                }
                delay(pollInterval.toMillis())
            }
        }
    }

    /**
     * 轮询 Kubernetes API 获取 Pod 信息
     */
    private fun pollKubernetesAPI() {
        val apiUrl = "https://kubernetes.default.svc/api/v1/namespaces/$namespace/pods?labelSelector=$labelSelector"
        val token = serviceAccountToken ?: readServiceAccountToken()
        
        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val pods = parsePods(response)
            updateMembers(pods)
        } else {
            logger.error { "Failed to poll Kubernetes API: $responseCode" }
        }
    }

    /**
     * 读取服务账号令牌
     * @return 服务账号令牌
     */
    private fun readServiceAccountToken(): String {
        return try {
            val tokenFile = "/var/run/secrets/kubernetes.io/serviceaccount/token"
            java.io.File(tokenFile).readText().trim()
        } catch (e: Exception) {
            logger.error(e) { "Failed to read service account token" }
            ""
        }
    }

    /**
     * 解析 Pod 信息
     * @param json Pod 信息的 JSON 字符串
     * @return Pod 信息列表
     */
    private fun parsePods(json: String): List<KubernetesPod> {
        // 这里应该使用 JSON 解析库解析 Pod 信息
        // 为了简化示例，这里返回一个空列表
        return emptyList()
    }

    /**
     * 更新成员列表
     * @param pods Pod 信息列表
     */
    private fun updateMembers(pods: List<KubernetesPod>) {
        val currentMembers = mutableMapOf<String, Member>()
        
        // 将 Pod 信息转换为成员信息
        for (pod in pods) {
            if (pod.status == "Running") {
                val member = Member(
                    id = pod.name,
                    host = pod.ip,
                    port = pod.port,
                    status = MemberStatus.ALIVE
                )
                currentMembers[pod.name] = member
            }
        }
        
        // 更新成员列表
        lock.write {
            // 找出已经不存在的成员
            val removedMembers = members.keys - currentMembers.keys
            
            // 移除不存在的成员
            for (id in removedMembers) {
                removeMember(id)
            }
            
            // 添加或更新成员
            for ((_, member) in currentMembers) {
                addMember(member)
            }
        }
    }
}

/**
 * KubernetesPod 表示 Kubernetes Pod 信息
 * @param name Pod 名称
 * @param ip Pod IP 地址
 * @param port Pod 端口
 * @param status Pod 状态
 */
data class KubernetesPod(
    val name: String,
    val ip: String,
    val port: Int,
    val status: String
)
