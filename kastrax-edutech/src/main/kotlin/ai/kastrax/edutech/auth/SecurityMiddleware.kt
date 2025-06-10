package ai.kastrax.edutech.auth

import kotlinx.serialization.Serializable

/**
 * 安全中间件 - 实现ed2.md第一阶段Week 3-4安全策略实施
 * 
 * 提供请求认证、权限验证和多租户隔离
 */
class SecurityMiddleware(
    private val authService: AuthService
) {
    /**
     * 认证请求
     *
     * @param request 请求信息
     * @return 认证结果
     */
    fun authenticateRequest(request: AuthRequest): AuthResult {
        val token = extractTokenFromRequest(request)
            ?: return AuthResult.Failure("Missing authentication token")
        
        val claims = authService.validateToken(token)
            ?: return AuthResult.Failure("Invalid or expired token")
        
        val userId = claims.subject
        val roles = authService.getRolesFromToken(token)
        val tenantId = authService.getTenantIdFromToken(token)
        
        return AuthResult.Success(
            AuthenticatedUser(
                userId = userId,
                roles = roles,
                tenantId = tenantId,
                token = token
            )
        )
    }
    
    /**
     * 验证权限
     *
     * @param user 认证用户
     * @param requiredPermission 所需权限
     * @param resourceTenantId 资源租户ID (可选)
     * @return 是否有权限
     */
    fun authorizeRequest(
        user: AuthenticatedUser,
        requiredPermission: Permission,
        resourceTenantId: String? = null
    ): Boolean {
        // 检查基本权限
        if (!authService.hasPermission(user.roles, requiredPermission)) {
            return false
        }
        
        // 检查租户权限
        return authService.hasTenantAccess(user.tenantId, resourceTenantId, user.roles)
    }
    
    /**
     * 从请求中提取令牌
     *
     * @param request 请求信息
     * @return JWT令牌
     */
    private fun extractTokenFromRequest(request: AuthRequest): String? {
        val authHeader = request.headers["Authorization"] ?: return null
        
        return if (authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else {
            null
        }
    }
}

/**
 * 认证请求
 */
@Serializable
data class AuthRequest(
    val path: String,
    val method: String,
    val headers: Map<String, String>,
    val body: String? = null
)

/**
 * 认证结果
 */
sealed class AuthResult {
    data class Success(val user: AuthenticatedUser) : AuthResult()
    data class Failure(val reason: String) : AuthResult()
}

/**
 * 认证用户
 */
@Serializable
data class AuthenticatedUser(
    val userId: String,
    val roles: List<Role>,
    val tenantId: String? = null,
    val token: String
)

/**
 * 安全策略配置
 */
@Serializable
data class SecurityConfig(
    val enableMultiTenant: Boolean = true,
    val tokenValidityMinutes: Long = 1440, // 24小时
    val refreshTokenValidityDays: Long = 30,
    val maxLoginAttempts: Int = 5,
    val lockoutDurationMinutes: Long = 30,
    val passwordPolicy: PasswordPolicy = PasswordPolicy()
)

/**
 * 密码策略
 */
@Serializable
data class PasswordPolicy(
    val minLength: Int = 8,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireNumbers: Boolean = true,
    val requireSpecialChars: Boolean = true,
    val maxAge: Long = 90 // 天
)

/**
 * 安全审计日志
 */
@Serializable
data class SecurityAuditLog(
    val timestamp: Long,
    val userId: String?,
    val action: SecurityAction,
    val resource: String?,
    val result: SecurityResult,
    val ipAddress: String?,
    val userAgent: String?,
    val details: Map<String, String> = emptyMap()
)

/**
 * 安全操作
 */
enum class SecurityAction {
    LOGIN,
    LOGOUT,
    TOKEN_REFRESH,
    PERMISSION_CHECK,
    RESOURCE_ACCESS,
    PASSWORD_CHANGE,
    ACCOUNT_LOCK,
    ACCOUNT_UNLOCK
}

/**
 * 安全结果
 */
enum class SecurityResult {
    SUCCESS,
    FAILURE,
    DENIED
}

/**
 * 安全审计服务
 */
class SecurityAuditService {
    private val auditLogs = mutableListOf<SecurityAuditLog>()
    
    /**
     * 记录安全事件
     *
     * @param userId 用户ID
     * @param action 安全操作
     * @param resource 资源
     * @param result 结果
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param details 详细信息
     */
    fun logSecurityEvent(
        userId: String?,
        action: SecurityAction,
        resource: String? = null,
        result: SecurityResult,
        ipAddress: String? = null,
        userAgent: String? = null,
        details: Map<String, String> = emptyMap()
    ) {
        val log = SecurityAuditLog(
            timestamp = System.currentTimeMillis(),
            userId = userId,
            action = action,
            resource = resource,
            result = result,
            ipAddress = ipAddress,
            userAgent = userAgent,
            details = details
        )
        
        auditLogs.add(log)
        
        // 在实际实现中，这里应该将日志写入持久化存储
        println("Security Audit: $log")
    }
    
    /**
     * 获取用户的安全日志
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 安全日志列表
     */
    fun getUserSecurityLogs(userId: String, limit: Int = 100): List<SecurityAuditLog> {
        return auditLogs
            .filter { it.userId == userId }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * 获取失败的登录尝试
     *
     * @param userId 用户ID
     * @param timeWindowMinutes 时间窗口(分钟)
     * @return 失败次数
     */
    fun getFailedLoginAttempts(userId: String, timeWindowMinutes: Long): Int {
        val cutoffTime = System.currentTimeMillis() - (timeWindowMinutes * 60 * 1000)
        
        return auditLogs.count { log ->
            log.userId == userId &&
            log.action == SecurityAction.LOGIN &&
            log.result == SecurityResult.FAILURE &&
            log.timestamp > cutoffTime
        }
    }
}

/**
 * 多租户安全管理器
 */
class MultiTenantSecurityManager {
    private val tenantConfigs = mutableMapOf<String, SecurityConfig>()
    
    /**
     * 设置租户安全配置
     *
     * @param tenantId 租户ID
     * @param config 安全配置
     */
    fun setTenantSecurityConfig(tenantId: String, config: SecurityConfig) {
        tenantConfigs[tenantId] = config
    }
    
    /**
     * 获取租户安全配置
     *
     * @param tenantId 租户ID
     * @return 安全配置
     */
    fun getTenantSecurityConfig(tenantId: String): SecurityConfig {
        return tenantConfigs[tenantId] ?: SecurityConfig()
    }
    
    /**
     * 验证租户资源访问权限
     *
     * @param userTenantId 用户租户ID
     * @param resourceTenantId 资源租户ID
     * @param userRoles 用户角色
     * @return 是否有权限
     */
    fun validateTenantAccess(
        userTenantId: String?,
        resourceTenantId: String?,
        userRoles: List<Role>
    ): Boolean {
        // 系统管理员可以访问所有租户资源
        if (userRoles.contains(Role.SYSTEM_ADMIN)) {
            return true
        }
        
        // 如果资源没有租户ID，则允许访问
        if (resourceTenantId == null) {
            return true
        }
        
        // 用户必须属于资源所在的租户
        return userTenantId == resourceTenantId
    }
}
