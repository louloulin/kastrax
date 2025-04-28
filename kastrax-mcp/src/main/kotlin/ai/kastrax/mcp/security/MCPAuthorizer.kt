package ai.kastrax.mcp.security

import ai.kastrax.mcp.protocol.Resource
import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.Prompt

/**
 * MCP 授权器接口，用于控制对 MCP 资源和工具的访问。
 */
interface MCPAuthorizer {
    /**
     * 检查是否有权限访问资源
     *
     * @param clientId 客户端 ID
     * @param resource 资源
     * @return 是否有权限
     */
    fun canAccessResource(clientId: String, resource: Resource): Boolean

    /**
     * 检查是否有权限使用工具
     *
     * @param clientId 客户端 ID
     * @param tool 工具
     * @return 是否有权限
     */
    fun canUseTool(clientId: String, tool: Tool): Boolean

    /**
     * 检查是否有权限使用提示
     *
     * @param clientId 客户端 ID
     * @param prompt 提示
     * @return 是否有权限
     */
    fun canUsePrompt(clientId: String, prompt: Prompt): Boolean

    /**
     * 授予资源访问权限
     *
     * @param clientId 客户端 ID
     * @param resourceId 资源 ID
     * @return 是否授予成功
     */
    fun grantResourceAccess(clientId: String, resourceId: String): Boolean

    /**
     * 授予工具使用权限
     *
     * @param clientId 客户端 ID
     * @param toolId 工具 ID
     * @return 是否授予成功
     */
    fun grantToolAccess(clientId: String, toolId: String): Boolean

    /**
     * 授予提示使用权限
     *
     * @param clientId 客户端 ID
     * @param promptId 提示 ID
     * @return 是否授予成功
     */
    fun grantPromptAccess(clientId: String, promptId: String): Boolean

    /**
     * 撤销资源访问权限
     *
     * @param clientId 客户端 ID
     * @param resourceId 资源 ID
     * @return 是否撤销成功
     */
    fun revokeResourceAccess(clientId: String, resourceId: String): Boolean

    /**
     * 撤销工具使用权限
     *
     * @param clientId 客户端 ID
     * @param toolId 工具 ID
     * @return 是否撤销成功
     */
    fun revokeToolAccess(clientId: String, toolId: String): Boolean

    /**
     * 撤销提示使用权限
     *
     * @param clientId 客户端 ID
     * @param promptId 提示 ID
     * @return 是否撤销成功
     */
    fun revokePromptAccess(clientId: String, promptId: String): Boolean

    /**
     * 检查客户端是否有指定权限
     *
     * @param clientId 客户端 ID
     * @param permission 权限
     * @return 是否有权限
     */
    fun hasPermission(clientId: String, permission: String): Boolean

    /**
     * 授予权限
     *
     * @param clientId 客户端 ID
     * @param permission 权限
     * @return 是否授予成功
     */
    fun grantPermission(clientId: String, permission: String): Boolean

    /**
     * 撤销权限
     *
     * @param clientId 客户端 ID
     * @param permission 权限
     * @return 是否撤销成功
     */
    fun revokePermission(clientId: String, permission: String): Boolean
}

/**
 * 基于内存的 MCP 授权器实现
 */
class InMemoryMCPAuthorizer : MCPAuthorizer {
    private val resourceAccess = mutableMapOf<String, MutableSet<String>>() // clientId -> resourceIds
    private val toolAccess = mutableMapOf<String, MutableSet<String>>() // clientId -> toolIds
    private val promptAccess = mutableMapOf<String, MutableSet<String>>() // clientId -> promptIds
    private val permissions = mutableMapOf<String, MutableSet<String>>() // clientId -> permissions

    override fun canAccessResource(clientId: String, resource: Resource): Boolean {
        // 检查是否有全局资源访问权限
        if (hasPermission(clientId, "resources:read:all")) {
            return true
        }

        // 检查是否有特定资源访问权限
        if (hasPermission(clientId, "resources:read:${resource.id}")) {
            return true
        }

        // 检查资源访问列表
        val allowedResources = resourceAccess[clientId] ?: return false
        return allowedResources.contains(resource.id)
    }

    override fun canUseTool(clientId: String, tool: Tool): Boolean {
        // 检查是否有全局工具使用权限
        if (hasPermission(clientId, "tools:use:all")) {
            return true
        }

        // 检查是否有特定工具使用权限
        if (hasPermission(clientId, "tools:use:${tool.id}")) {
            return true
        }

        // 检查工具访问列表
        val allowedTools = toolAccess[clientId] ?: return false
        return allowedTools.contains(tool.id)
    }

    override fun canUsePrompt(clientId: String, prompt: Prompt): Boolean {
        // 检查是否有全局提示使用权限
        if (hasPermission(clientId, "prompts:use:all")) {
            return true
        }

        // 检查是否有特定提示使用权限
        if (hasPermission(clientId, "prompts:use:${prompt.id}")) {
            return true
        }

        // 检查提示访问列表
        val allowedPrompts = promptAccess[clientId] ?: return false
        return allowedPrompts.contains(prompt.id)
    }

    override fun grantResourceAccess(clientId: String, resourceId: String): Boolean {
        val allowedResources = resourceAccess.getOrPut(clientId) { mutableSetOf() }
        allowedResources.add(resourceId)
        return true
    }

    override fun grantToolAccess(clientId: String, toolId: String): Boolean {
        val allowedTools = toolAccess.getOrPut(clientId) { mutableSetOf() }
        allowedTools.add(toolId)
        return true
    }

    override fun grantPromptAccess(clientId: String, promptId: String): Boolean {
        val allowedPrompts = promptAccess.getOrPut(clientId) { mutableSetOf() }
        allowedPrompts.add(promptId)
        return true
    }

    override fun revokeResourceAccess(clientId: String, resourceId: String): Boolean {
        val allowedResources = resourceAccess[clientId] ?: return false
        return allowedResources.remove(resourceId)
    }

    override fun revokeToolAccess(clientId: String, toolId: String): Boolean {
        val allowedTools = toolAccess[clientId] ?: return false
        return allowedTools.remove(toolId)
    }

    override fun revokePromptAccess(clientId: String, promptId: String): Boolean {
        val allowedPrompts = promptAccess[clientId] ?: return false
        return allowedPrompts.remove(promptId)
    }

    override fun hasPermission(clientId: String, permission: String): Boolean {
        val clientPermissions = permissions[clientId] ?: return false
        
        // 检查是否有完全匹配的权限
        if (clientPermissions.contains(permission)) {
            return true
        }
        
        // 检查是否有通配符权限
        val parts = permission.split(":")
        if (parts.size >= 2) {
            val resourceType = parts[0]
            val action = parts[1]
            
            // 检查 resourceType:action:* 权限
            if (clientPermissions.contains("$resourceType:$action:*")) {
                return true
            }
            
            // 检查 resourceType:* 权限
            if (clientPermissions.contains("$resourceType:*")) {
                return true
            }
            
            // 检查 * 权限（超级管理员）
            if (clientPermissions.contains("*")) {
                return true
            }
        }
        
        return false
    }

    override fun grantPermission(clientId: String, permission: String): Boolean {
        val clientPermissions = permissions.getOrPut(clientId) { mutableSetOf() }
        clientPermissions.add(permission)
        return true
    }

    override fun revokePermission(clientId: String, permission: String): Boolean {
        val clientPermissions = permissions[clientId] ?: return false
        return clientPermissions.remove(permission)
    }
}
