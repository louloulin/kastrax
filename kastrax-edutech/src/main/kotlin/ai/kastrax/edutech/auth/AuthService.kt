package ai.kastrax.edutech.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.security.Key
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * 认证服务 - 实现ed2.md第一阶段Week 3-4用户认证和授权系统
 * 
 * 提供JWT令牌管理、角色权限控制和多租户支持
 */
class AuthService(
    private val secretKey: Key = Keys.secretKeyFor(SignatureAlgorithm.HS512),
    private val tokenValidity: kotlin.time.Duration = 1.days,
    private val refreshTokenValidity: kotlin.time.Duration = 30.days
) {
    /**
     * 生成JWT令牌
     *
     * @param userId 用户ID
     * @param roles 用户角色列表
     * @param tenantId 租户ID (多租户支持)
     * @param additionalClaims 额外的声明信息
     * @return 认证令牌
     */
    fun generateToken(
        userId: String,
        roles: List<Role>,
        tenantId: String? = null,
        additionalClaims: Map<String, Any> = emptyMap()
    ): AuthToken {
        val now = Clock.System.now()
        val expiration = now.plus(tokenValidity)
        val refreshExpiration = now.plus(refreshTokenValidity)
        
        val claims = mutableMapOf<String, Any>(
            "sub" to userId,
            "roles" to roles.map { it.name },
            "created" to now.toEpochMilliseconds()
        )
        
        // 添加租户信息 (多租户支持)
        tenantId?.let { claims["tenantId"] = it }
        
        // 添加额外声明
        claims.putAll(additionalClaims)
        
        // 生成访问令牌
        val accessToken = Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(Date(now.toEpochMilliseconds()))
            .setExpiration(Date(expiration.toEpochMilliseconds()))
            .signWith(secretKey)
            .compact()
        
        // 生成刷新令牌
        val refreshToken = Jwts.builder()
            .setSubject(userId)
            .claim("tokenType", "refresh")
            .setIssuedAt(Date(now.toEpochMilliseconds()))
            .setExpiration(Date(refreshExpiration.toEpochMilliseconds()))
            .signWith(secretKey)
            .compact()
        
        return AuthToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = tokenValidity.inWholeSeconds
        )
    }
    
    /**
     * 验证令牌
     *
     * @param token JWT令牌
     * @return 令牌声明信息
     */
    fun validateToken(token: String): Claims? {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 从令牌中获取用户ID
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    fun getUserIdFromToken(token: String): String? {
        return validateToken(token)?.subject
    }
    
    /**
     * 从令牌中获取用户角色
     *
     * @param token JWT令牌
     * @return 用户角色列表
     */
    @Suppress("UNCHECKED_CAST")
    fun getRolesFromToken(token: String): List<Role> {
        val claims = validateToken(token) ?: return emptyList()
        val roleNames = claims["roles"] as? List<String> ?: return emptyList()
        return roleNames.mapNotNull { roleName ->
            try {
                Role.valueOf(roleName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
    
    /**
     * 从令牌中获取租户ID
     *
     * @param token JWT令牌
     * @return 租户ID
     */
    fun getTenantIdFromToken(token: String): String? {
        return validateToken(token)?.get("tenantId") as? String
    }
    
    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的认证令牌
     */
    fun refreshToken(refreshToken: String): AuthToken? {
        val claims = validateToken(refreshToken) ?: return null
        
        // 验证是否为刷新令牌
        if (claims.get("tokenType") as? String != "refresh") {
            return null
        }

        val userId = claims.subject
        val roles = getRolesFromToken(refreshToken)
        val tenantId = getTenantIdFromToken(refreshToken)
        
        return generateToken(userId, roles, tenantId)
    }
    
    /**
     * 检查用户是否有权限执行操作
     *
     * @param userRoles 用户角色列表
     * @param requiredPermission 所需权限
     * @return 是否有权限
     */
    fun hasPermission(userRoles: List<Role>, requiredPermission: Permission): Boolean {
        return userRoles.any { role ->
            role.permissions.contains(requiredPermission)
        }
    }
    
    /**
     * 检查用户是否有权限访问租户资源
     *
     * @param userTenantId 用户租户ID
     * @param resourceTenantId 资源租户ID
     * @param userRoles 用户角色列表
     * @return 是否有权限
     */
    fun hasTenantAccess(userTenantId: String?, resourceTenantId: String?, userRoles: List<Role>): Boolean {
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

/**
 * 认证令牌
 */
@Serializable
data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long
)

/**
 * 用户角色
 */
enum class Role(val displayName: String, val permissions: Set<Permission>) {
    STUDENT("学生", setOf(
        Permission.VIEW_COURSE,
        Permission.SUBMIT_ASSIGNMENT,
        Permission.VIEW_GRADE,
        Permission.VIEW_PROFILE
    )),
    
    TEACHER("教师", setOf(
        Permission.VIEW_COURSE,
        Permission.CREATE_COURSE,
        Permission.EDIT_COURSE,
        Permission.GRADE_ASSIGNMENT,
        Permission.VIEW_STUDENT_PROGRESS,
        Permission.VIEW_PROFILE,
        Permission.EDIT_PROFILE
    )),
    
    ADMIN("管理员", setOf(
        Permission.VIEW_COURSE,
        Permission.CREATE_COURSE,
        Permission.EDIT_COURSE,
        Permission.DELETE_COURSE,
        Permission.MANAGE_USERS,
        Permission.VIEW_ANALYTICS,
        Permission.VIEW_PROFILE,
        Permission.EDIT_PROFILE
    )),
    
    SYSTEM_ADMIN("系统管理员", Permission.values().toSet())
}

/**
 * 权限
 */
enum class Permission {
    VIEW_COURSE,
    CREATE_COURSE,
    EDIT_COURSE,
    DELETE_COURSE,
    
    SUBMIT_ASSIGNMENT,
    GRADE_ASSIGNMENT,
    
    VIEW_GRADE,
    VIEW_STUDENT_PROGRESS,
    
    MANAGE_USERS,
    VIEW_ANALYTICS,
    
    VIEW_PROFILE,
    EDIT_PROFILE,
    
    MANAGE_SYSTEM
}
