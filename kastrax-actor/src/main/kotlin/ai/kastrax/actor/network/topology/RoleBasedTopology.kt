package ai.kastrax.actor.network.topology

import ai.kastrax.actor.network.AgentRelation
import ai.kastrax.actor.network.NetworkTopology
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于角色的网络拓扑，为 Agent 分配特定角色并管理角色之间的关系
 *
 * 基于角色的网络拓扑为 Agent 分配特定角色，并根据角色定义 Agent 之间的关系，
 * 适用于需要明确职责分工的场景，如团队协作、组织管理等
 */
class RoleBasedTopology {
    private val topology = NetworkTopology()

    /**
     * 角色定义，包含角色名称和描述
     */
    data class Role(
        val name: String,
        val description: String,
        val capabilities: Set<String> = emptySet(),
        val metadata: Map<String, String> = emptyMap()
    )

    /**
     * 角色关系定义，描述两个角色之间的关系
     */
    data class RoleRelation(
        val sourceRole: String,
        val targetRole: String,
        val relation: AgentRelation,
        val description: String = "",
        val metadata: Map<String, String> = emptyMap()
    )

    /**
     * 角色映射，记录每个节点的角色
     */
    private val nodeRoles = ConcurrentHashMap<String, String>()

    /**
     * 角色定义映射，记录所有已定义的角色
     */
    private val roles = ConcurrentHashMap<String, Role>()

    /**
     * 角色关系映射，记录角色之间的关系
     */
    private val roleRelations = mutableListOf<RoleRelation>()

    /**
     * 添加角色定义
     *
     * @param role 角色定义
     */
    fun addRole(role: Role) {
        roles[role.name] = role
    }

    /**
     * 移除角色定义
     *
     * @param roleName 角色名称
     */
    fun removeRole(roleName: String) {
        roles.remove(roleName)

        // 移除与该角色相关的所有关系
        roleRelations.removeIf { it.sourceRole == roleName || it.targetRole == roleName }

        // 移除分配了该角色的节点的角色
        nodeRoles.entries.removeIf { it.value == roleName }
    }

    /**
     * 添加角色关系
     *
     * @param roleRelation 角色关系定义
     * @return 是否成功添加
     */
    fun addRoleRelation(roleRelation: RoleRelation): Boolean {
        // 检查角色是否存在
        if (!roles.containsKey(roleRelation.sourceRole) || !roles.containsKey(roleRelation.targetRole)) {
            return false
        }

        roleRelations.add(roleRelation)
        return true
    }

    /**
     * 移除角色关系
     *
     * @param sourceRole 源角色名称
     * @param targetRole 目标角色名称
     * @return 是否成功移除
     */
    fun removeRoleRelation(sourceRole: String, targetRole: String): Boolean {
        val initialSize = roleRelations.size
        roleRelations.removeIf { it.sourceRole == sourceRole && it.targetRole == targetRole }
        return roleRelations.size < initialSize
    }

    /**
     * 为节点分配角色
     *
     * @param nodeId 节点 ID
     * @param roleName 角色名称
     * @return 是否成功分配
     */
    fun assignRole(nodeId: String, roleName: String): Boolean {
        // 检查角色是否存在
        if (!roles.containsKey(roleName)) {
            return false
        }

        // 检查节点是否存在
        if (!containsNode(nodeId)) {
            topology.addNode(nodeId)
        }

        // 分配角色
        nodeRoles[nodeId] = roleName

        // 更新节点之间的关系
        updateNodeRelations()

        return true
    }

    /**
     * 移除节点的角色
     *
     * @param nodeId 节点 ID
     * @return 是否成功移除
     */
    fun removeNodeRole(nodeId: String): Boolean {
        return nodeRoles.remove(nodeId) != null
    }

    /**
     * 获取节点的角色
     *
     * @param nodeId 节点 ID
     * @return 角色名称，如果没有分配角色则返回 null
     */
    fun getNodeRole(nodeId: String): String? {
        return nodeRoles[nodeId]
    }

    /**
     * 获取具有特定角色的所有节点
     *
     * @param roleName 角色名称
     * @return 节点 ID 列表
     */
    fun getNodesByRole(roleName: String): List<String> {
        return nodeRoles.entries
            .filter { it.value == roleName }
            .map { it.key }
    }

    /**
     * 获取角色定义
     *
     * @param roleName 角色名称
     * @return 角色定义，如果不存在则返回 null
     */
    fun getRole(roleName: String): Role? {
        return roles[roleName]
    }

    /**
     * 获取所有角色定义
     *
     * @return 角色定义映射
     */
    fun getAllRoles(): Map<String, Role> {
        return roles.toMap()
    }

    /**
     * 获取所有角色关系
     *
     * @return 角色关系列表
     */
    fun getAllRoleRelations(): List<RoleRelation> {
        return roleRelations.toList()
    }

    /**
     * 获取特定角色之间的关系
     *
     * @param sourceRole 源角色名称
     * @param targetRole 目标角色名称
     * @return 角色关系列表
     */
    fun getRoleRelations(sourceRole: String, targetRole: String): List<RoleRelation> {
        return roleRelations.filter { it.sourceRole == sourceRole && it.targetRole == targetRole }
    }

    /**
     * 更新节点之间的关系
     *
     * 根据角色关系定义更新节点之间的关系
     */
    fun updateNodeRelations() {
        // 清除所有现有的边
        val allNodes = topology.getAllNodes()
        for (i in 0 until allNodes.size - 1) {
            for (j in i + 1 until allNodes.size) {
                topology.removeEdge(allNodes[i], allNodes[j])
            }
        }

        // 根据角色关系添加边
        for (roleRelation in roleRelations) {
            val sourceNodes = getNodesByRole(roleRelation.sourceRole)
            val targetNodes = getNodesByRole(roleRelation.targetRole)

            for (sourceNode in sourceNodes) {
                for (targetNode in targetNodes) {
                    if (sourceNode != targetNode) {
                        topology.addEdge(sourceNode, targetNode, roleRelation.relation)
                    }
                }
            }
        }
    }

    /**
     * 检查节点是否具有特定能力
     *
     * @param nodeId 节点 ID
     * @param capability 能力名称
     * @return 是否具有该能力
     */
    fun hasCapability(nodeId: String, capability: String): Boolean {
        val roleName = getNodeRole(nodeId) ?: return false
        val role = getRole(roleName) ?: return false
        return capability in role.capabilities
    }

    // 委托方法
    fun containsNode(nodeId: String): Boolean = topology.getAllNodes().contains(nodeId)
    fun addNode(nodeId: String) = topology.addNode(nodeId)
    fun removeNode(nodeId: String) = topology.removeNode(nodeId)
    fun addEdge(fromId: String, toId: String, relation: AgentRelation) = topology.addEdge(fromId, toId, relation)
    fun removeEdge(fromId: String, toId: String) = topology.removeEdge(fromId, toId)
    fun getConnectedNodes(nodeId: String) = topology.getConnectedNodes(nodeId)
    fun getNodesByRelation(nodeId: String, relation: AgentRelation) = topology.getNodesByRelation(nodeId, relation)
    fun getRelation(fromId: String, toId: String) = topology.getRelation(fromId, toId)
    fun getAllNodes() = topology.getAllNodes()
    fun getAllEdges() = topology.getAllEdges()
    fun clear() = topology.clear()

    /**
     * 获取具有特定能力的所有节点
     *
     * @param capability 能力名称
     * @return 节点 ID 列表
     */
    fun getNodesByCapability(capability: String): List<String> {
        return nodeRoles.entries
            .filter { entry ->
                val role = roles[entry.value]
                role?.capabilities?.contains(capability) == true
            }
            .map { it.key }
    }

    /**
     * 创建团队结构
     *
     * 创建一个包含领导者和成员的团队结构
     *
     * @param leaderRole 领导者角色名称
     * @param memberRoles 成员角色名称列表
     * @return 是否成功创建
     */
    fun createTeamStructure(leaderRole: String, memberRoles: List<String>): Boolean {
        // 检查所有角色是否存在
        if (!roles.containsKey(leaderRole) || memberRoles.any { !roles.containsKey(it) }) {
            return false
        }

        // 创建领导者与成员之间的关系
        for (memberRole in memberRoles) {
            addRoleRelation(
                RoleRelation(
                    sourceRole = leaderRole,
                    targetRole = memberRole,
                    relation = AgentRelation.MASTER,
                    description = "领导关系"
                )
            )
        }

        // 创建成员之间的对等关系
        for (i in 0 until memberRoles.size - 1) {
            for (j in i + 1 until memberRoles.size) {
                addRoleRelation(
                    RoleRelation(
                        sourceRole = memberRoles[i],
                        targetRole = memberRoles[j],
                        relation = AgentRelation.PEER,
                        description = "团队成员关系"
                    )
                )
            }
        }

        // 更新节点关系
        updateNodeRelations()

        return true
    }

    /**
     * 创建监督结构
     *
     * 创建一个包含监督者和被监督者的结构
     *
     * @param supervisorRole 监督者角色名称
     * @param supervisedRoles 被监督者角色名称列表
     * @return 是否成功创建
     */
    fun createSupervisorStructure(supervisorRole: String, supervisedRoles: List<String>): Boolean {
        // 检查所有角色是否存在
        if (!roles.containsKey(supervisorRole) || supervisedRoles.any { !roles.containsKey(it) }) {
            return false
        }

        // 创建监督关系
        for (supervisedRole in supervisedRoles) {
            addRoleRelation(
                RoleRelation(
                    sourceRole = supervisorRole,
                    targetRole = supervisedRole,
                    relation = AgentRelation.SUPERVISOR,
                    description = "监督关系"
                )
            )
        }

        // 更新节点关系
        updateNodeRelations()

        return true
    }

    /**
     * 创建协作结构
     *
     * 创建一个所有角色平等协作的结构
     *
     * @param collaboratorRoles 协作者角色名称列表
     * @return 是否成功创建
     */
    fun createCollaborativeStructure(collaboratorRoles: List<String>): Boolean {
        // 检查所有角色是否存在
        if (collaboratorRoles.any { !roles.containsKey(it) }) {
            return false
        }

        // 创建协作关系
        for (i in 0 until collaboratorRoles.size - 1) {
            for (j in i + 1 until collaboratorRoles.size) {
                addRoleRelation(
                    RoleRelation(
                        sourceRole = collaboratorRoles[i],
                        targetRole = collaboratorRoles[j],
                        relation = AgentRelation.COLLABORATOR,
                        description = "协作关系"
                    )
                )
            }
        }

        // 更新节点关系
        updateNodeRelations()

        return true
    }
}
