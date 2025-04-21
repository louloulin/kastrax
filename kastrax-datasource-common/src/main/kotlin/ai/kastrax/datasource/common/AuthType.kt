package ai.kastrax.datasource.common

/**
 * 认证类型枚举。
 */
enum class AuthType {
    /**
     * 无认证。
     */
    NONE,
    
    /**
     * Bearer 令牌认证。
     */
    BEARER,
    
    /**
     * 基本认证（用户名/密码）。
     */
    BASIC
}
