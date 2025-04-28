package ai.kastrax.a2a.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 认证配置
 */
data class AuthConfig(
    /**
     * 认证类型
     */
    val type: AuthType,

    /**
     * API 密钥（用于 API_KEY 认证）
     */
    val apiKey: String? = null,

    /**
     * JWT 密钥（用于 JWT 认证）
     */
    val jwtSecret: String? = null,

    /**
     * JWT 发行者（用于 JWT 认证）
     */
    val jwtIssuer: String? = null,

    /**
     * JWT 受众（用于 JWT 认证）
     */
    val jwtAudience: String? = null,

    /**
     * OAuth2 客户端 ID（用于 OAuth2 认证）
     */
    val oauth2ClientId: String? = null,

    /**
     * OAuth2 客户端密钥（用于 OAuth2 认证）
     */
    val oauth2ClientSecret: String? = null,

    /**
     * OAuth2 授权 URL（用于 OAuth2 认证）
     */
    val oauth2AuthUrl: String? = null,

    /**
     * OAuth2 令牌 URL（用于 OAuth2 认证）
     */
    val oauth2TokenUrl: String? = null
)

/**
 * 认证类型
 */
enum class AuthType {
    /**
     * 无认证
     */
    NONE,

    /**
     * API 密钥认证
     */
    API_KEY,

    /**
     * JWT 认证
     */
    JWT,

    /**
     * OAuth2 认证
     */
    OAUTH2
}

/**
 * 授权结果
 */
sealed class AuthResult {
    /**
     * 成功
     */
    data class Success(val principal: Principal) : AuthResult()

    /**
     * 失败
     */
    data class Failure(val reason: String) : AuthResult()
}

/**
 * 主体
 */
data class Principal(
    /**
     * 用户 ID
     */
    val userId: String,

    /**
     * 角色
     */
    val roles: List<String> = emptyList(),

    /**
     * 权限
     */
    val permissions: List<String> = emptyList()
)

/**
 * A2A 安全服务，用于认证和授权
 */
class A2ASecurityService(
    private val config: AuthConfig
) {
    /**
     * API 密钥映射
     */
    private val apiKeys = ConcurrentHashMap<String, Principal>()

    /**
     * JWT 算法
     */
    private val jwtAlgorithm = config.jwtSecret?.let { Algorithm.HMAC256(it) }

    /**
     * JWT 验证器
     */
    private val jwtVerifier = jwtAlgorithm?.let {
        JWT.require(it)
            .withIssuer(config.jwtIssuer ?: "a2a")
            .withAudience(config.jwtAudience ?: "a2a-api")
            .build()
    }

    /**
     * 注册 API 密钥
     */
    fun registerApiKey(apiKey: String, principal: Principal) {
        apiKeys[apiKey] = principal
    }

    /**
     * 验证 API 密钥
     */
    fun validateApiKey(apiKey: String): AuthResult {
        val principal = apiKeys[apiKey]
        return if (principal != null) {
            AuthResult.Success(principal)
        } else {
            AuthResult.Failure("Invalid API key")
        }
    }

    /**
     * 验证 JWT 令牌
     */
    fun validateJwt(token: String): AuthResult {
        if (jwtVerifier == null) {
            return AuthResult.Failure("JWT verification not configured")
        }

        return try {
            val jwt = jwtVerifier.verify(token)
            val userId = jwt.subject ?: return AuthResult.Failure("JWT subject is missing")
            val roles = jwt.getClaim("roles")?.asList(String::class.java) ?: emptyList()
            val permissions = jwt.getClaim("permissions")?.asList(String::class.java) ?: emptyList()

            AuthResult.Success(Principal(userId, roles, permissions))
        } catch (e: JWTVerificationException) {
            AuthResult.Failure("JWT verification failed: ${e.message}")
        }
    }

    /**
     * 生成 JWT 令牌
     */
    fun generateJwt(
        userId: String,
        roles: List<String> = emptyList(),
        permissions: List<String> = emptyList(),
        expirationSeconds: Long = 3600
    ): String? {
        if (jwtAlgorithm == null) {
            return null
        }

        return JWT.create()
            .withSubject(userId)
            .withIssuer(config.jwtIssuer ?: "a2a")
            .withAudience(config.jwtAudience ?: "a2a-api")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + expirationSeconds * 1000))
            .withClaim("roles", roles)
            .withClaim("permissions", permissions)
            .sign(jwtAlgorithm)
    }

    /**
     * 检查权限
     */
    fun checkPermission(principal: Principal, permission: String): Boolean {
        return principal.permissions.contains(permission) ||
                principal.permissions.contains("*")
    }

    /**
     * 检查角色
     */
    fun checkRole(principal: Principal, role: String): Boolean {
        return principal.roles.contains(role) ||
                principal.roles.contains("admin")
    }

    /**
     * 获取认证类型
     */
    fun getAuthType(): AuthType {
        return config.type
    }
}

/**
 * 配置 Ktor 认证
 */
fun Application.configureA2AAuth(securityService: A2ASecurityService, config: AuthConfig) {
    install(Authentication) {
        when (config.type) {
            AuthType.API_KEY -> {
                bearer("auth-bearer") {
                    authenticate { tokenCredential ->
                        when (val result = securityService.validateApiKey(tokenCredential.token)) {
                            is AuthResult.Success -> JWTPrincipal(createJwtClaims(result.principal))
                            is AuthResult.Failure -> null
                        }
                    }
                }
            }
            AuthType.JWT -> {
                jwt("auth-jwt") {
                    realm = "A2A API"
                    verifier(
                        JWT.require(Algorithm.HMAC256(config.jwtSecret ?: ""))
                            .withIssuer(config.jwtIssuer ?: "a2a")
                            .withAudience(config.jwtAudience ?: "a2a-api")
                            .build()
                    )
                    validate { credential ->
                        if (credential.payload.audience.contains(config.jwtAudience ?: "a2a-api")) {
                            JWTPrincipal(credential.payload)
                        } else {
                            null
                        }
                    }
                    challenge { _, _ ->
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or expired JWT token")
                    }
                }
            }
            AuthType.OAUTH2 -> {
                // OAuth2 认证配置
                // 这里只是一个示例，实际实现需要根据具体的 OAuth2 提供商进行配置
                // OAuth2 认证配置
                // 这里只是一个示例，实际实现需要根据具体的 OAuth2 提供商进行配置
                // 由于实现复杂度较高，这里暂时注释掉
                /*
                oauth("auth-oauth") {
                    urlProvider = { "http://localhost:8080/callback" }
                    providerLookup = {
                        OAuthServerSettings.OAuth2ServerSettings(
                            name = "oauth2",
                            authorizeUrl = config.oauth2AuthUrl ?: "",
                            accessTokenUrl = config.oauth2TokenUrl ?: "",
                            clientId = config.oauth2ClientId ?: "",
                            clientSecret = config.oauth2ClientSecret ?: "",
                            defaultScopes = listOf("profile", "email")
                        )
                    }
                    client = HttpClient {
                        // 配置 HTTP 客户端
                    }
                }
                */
            }
            AuthType.NONE -> {
                // 无认证
            }
        }
    }
}

/**
 * 创建 JWT 声明
 */
private fun createJwtClaims(principal: Principal): DecodedJWT {
    val token = JWT.create()
        .withSubject(principal.userId)
        .withClaim("roles", principal.roles)
        .withClaim("permissions", principal.permissions)
        .sign(Algorithm.HMAC256("dummy-secret"))

    return JWT.decode(token)
}

/**
 * 安全 DSL 构建器
 */
class SecurityBuilder {
    /**
     * 认证类型
     */
    var type: AuthType = AuthType.NONE

    /**
     * API 密钥
     */
    var apiKey: String? = null

    /**
     * JWT 密钥
     */
    var jwtSecret: String? = null

    /**
     * JWT 发行者
     */
    var jwtIssuer: String? = null

    /**
     * JWT 受众
     */
    var jwtAudience: String? = null

    /**
     * OAuth2 客户端 ID
     */
    var oauth2ClientId: String? = null

    /**
     * OAuth2 客户端密钥
     */
    var oauth2ClientSecret: String? = null

    /**
     * OAuth2 授权 URL
     */
    var oauth2AuthUrl: String? = null

    /**
     * OAuth2 令牌 URL
     */
    var oauth2TokenUrl: String? = null

    /**
     * 构建认证配置
     */
    fun build(): AuthConfig {
        return AuthConfig(
            type = type,
            apiKey = apiKey,
            jwtSecret = jwtSecret,
            jwtIssuer = jwtIssuer,
            jwtAudience = jwtAudience,
            oauth2ClientId = oauth2ClientId,
            oauth2ClientSecret = oauth2ClientSecret,
            oauth2AuthUrl = oauth2AuthUrl,
            oauth2TokenUrl = oauth2TokenUrl
        )
    }
}

/**
 * 创建安全服务的 DSL 函数
 */
fun security(init: SecurityBuilder.() -> Unit): A2ASecurityService {
    val builder = SecurityBuilder()
    builder.init()
    val config = builder.build()
    return A2ASecurityService(config)
}
