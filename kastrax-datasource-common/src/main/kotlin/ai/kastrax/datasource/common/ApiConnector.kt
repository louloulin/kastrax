package ai.kastrax.datasource.common

/**
 * API 连接器接口，定义了 API 连接器的通用操作。
 */
interface ApiConnector : DataSource {
    /**
     * 发送 GET 请求。
     *
     * @param path 请求路径。
     * @param params 查询参数。
     * @param headers 请求头。
     * @return 响应内容。
     */
    suspend fun get(
        path: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): String

    /**
     * 发送 POST 请求。
     *
     * @param path 请求路径。
     * @param body 请求体。
     * @param headers 请求头。
     * @return 响应内容。
     */
    suspend fun post(path: String, body: String, headers: Map<String, String> = emptyMap()): String

    /**
     * 发送 PUT 请求。
     *
     * @param path 请求路径。
     * @param body 请求体。
     * @param headers 请求头。
     * @return 响应内容。
     */
    suspend fun put(path: String, body: String, headers: Map<String, String> = emptyMap()): String

    /**
     * 发送 DELETE 请求。
     *
     * @param path 请求路径。
     * @param headers 请求头。
     * @return 响应内容。
     */
    suspend fun delete(path: String, headers: Map<String, String> = emptyMap()): String

    /**
     * 发送 PATCH 请求。
     *
     * @param path 请求路径。
     * @param body 请求体。
     * @param headers 请求头。
     * @return 响应内容。
     */
    suspend fun patch(path: String, body: String, headers: Map<String, String> = emptyMap()): String

    /**
     * API 基础 URL。
     */
    val baseUrl: String

    /**
     * 默认请求头。
     */
    val defaultHeaders: Map<String, String>
}
