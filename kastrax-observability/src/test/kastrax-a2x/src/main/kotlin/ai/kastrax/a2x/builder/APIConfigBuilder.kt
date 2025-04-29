package ai.kastrax.a2x.builder

import ai.kastrax.a2x.adapter.APIConfig
import ai.kastrax.a2x.adapter.APIEndpoint
import ai.kastrax.a2x.adapter.APIParameter
import ai.kastrax.a2x.model.Authentication
import ai.kastrax.a2x.model.AuthenticationType
import java.util.UUID

/**
 * API 配置构建器
 */
class APIConfigBuilder {
    /**
     * API ID
     */
    var id: String = UUID.randomUUID().toString()
    
    /**
     * API 名称
     */
    var name: String = ""
    
    /**
     * API 描述
     */
    var description: String = ""
    
    /**
     * 基础 URL
     */
    var baseUrl: String = ""
    
    /**
     * 端点列表
     */
    private val endpoints = mutableListOf<APIEndpoint>()
    
    /**
     * 默认请求头
     */
    private val defaultHeaders = mutableMapOf<String, String>()
    
    /**
     * 认证信息
     */
    private var authentication: Authentication? = null
    
    /**
     * 超时时间（毫秒）
     */
    var timeout: Long = 30000
    
    /**
     * 添加端点
     */
    fun endpoint(init: EndpointBuilder.() -> Unit) {
        val builder = EndpointBuilder()
        builder.init()
        endpoints.add(builder.build())
    }
    
    /**
     * 添加默认请求头
     */
    fun header(name: String, value: String) {
        defaultHeaders[name] = value
    }
    
    /**
     * 配置 API 密钥认证
     */
    fun apiKeyAuth(keyName: String, keyValue: String, keyLocation: String = "header") {
        authentication = Authentication(
            type = AuthenticationType.API_KEY,
            metadata = mapOf(
                "key_name" to keyName,
                "key_value" to keyValue,
                "key_location" to keyLocation
            )
        )
    }
    
    /**
     * 配置 Bearer 令牌认证
     */
    fun bearerAuth(token: String) {
        authentication = Authentication(
            type = AuthenticationType.BEARER,
            metadata = mapOf(
                "token" to token
            )
        )
    }
    
    /**
     * 配置基本认证
     */
    fun basicAuth(username: String, password: String) {
        authentication = Authentication(
            type = AuthenticationType.BASIC,
            metadata = mapOf(
                "username" to username,
                "password" to password
            )
        )
    }
    
    /**
     * 构建 API 配置
     */
    fun build(): APIConfig {
        require(name.isNotBlank()) { "API name is required" }
        require(baseUrl.isNotBlank()) { "Base URL is required" }
        
        return APIConfig(
            id = id,
            name = name,
            description = description,
            baseUrl = baseUrl,
            endpoints = endpoints.toList(),
            defaultHeaders = defaultHeaders.toMap(),
            authentication = authentication,
            timeout = timeout
        )
    }
    
    /**
     * 端点构建器
     */
    class EndpointBuilder {
        /**
         * 端点 ID
         */
        var id: String = UUID.randomUUID().toString()
        
        /**
         * 端点名称
         */
        var name: String = ""
        
        /**
         * 端点描述
         */
        var description: String = ""
        
        /**
         * HTTP 方法
         */
        var method: String = "GET"
        
        /**
         * 端点路径
         */
        var path: String = ""
        
        /**
         * 参数列表
         */
        private val parameters = mutableListOf<APIParameter>()
        
        /**
         * 添加查询参数
         */
        fun queryParam(name: String, type: String, description: String, required: Boolean = false) {
            parameters.add(
                APIParameter(
                    name = name,
                    type = type,
                    description = description,
                    required = required,
                    location = "query"
                )
            )
        }
        
        /**
         * 添加路径参数
         */
        fun pathParam(name: String, type: String, description: String) {
            parameters.add(
                APIParameter(
                    name = name,
                    type = type,
                    description = description,
                    required = true,
                    location = "path"
                )
            )
        }
        
        /**
         * 添加请求体参数
         */
        fun bodyParam(name: String, type: String, description: String, required: Boolean = false) {
            parameters.add(
                APIParameter(
                    name = name,
                    type = type,
                    description = description,
                    required = required,
                    location = "body"
                )
            )
        }
        
        /**
         * 添加请求头参数
         */
        fun headerParam(name: String, type: String, description: String, required: Boolean = false) {
            parameters.add(
                APIParameter(
                    name = name,
                    type = type,
                    description = description,
                    required = required,
                    location = "header"
                )
            )
        }
        
        /**
         * 构建端点
         */
        fun build(): APIEndpoint {
            require(name.isNotBlank()) { "Endpoint name is required" }
            require(path.isNotBlank()) { "Endpoint path is required" }
            
            return APIEndpoint(
                id = id,
                name = name,
                description = description,
                method = method,
                path = path,
                parameters = parameters.toList()
            )
        }
    }
}
