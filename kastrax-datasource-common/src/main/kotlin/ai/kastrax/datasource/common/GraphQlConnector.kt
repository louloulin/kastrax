package ai.kastrax.datasource.common

/**
 * GraphQL 连接器接口，定义了 GraphQL 连接器的通用操作。
 */
interface GraphQlConnector : ApiConnector {
    /**
     * 执行 GraphQL 查询。
     *
     * @param query GraphQL 查询语句。
     * @param variables 查询变量，使用 Map 格式。
     * @param operationName 操作名称，可选。
     * @param headers 请求头，可选。
     * @return 查询结果，以 JSON 字符串形式返回。
     */
    suspend fun query(
        query: String,
        variables: Map<String, Any> = emptyMap(),
        operationName: String? = null,
        headers: Map<String, String> = emptyMap()
    ): String

    /**
     * 执行 GraphQL 变更。
     *
     * @param mutation GraphQL 变更语句。
     * @param variables 变更变量，使用 Map 格式。
     * @param operationName 操作名称，可选。
     * @param headers 请求头，可选。
     * @return 变更结果，以 JSON 字符串形式返回。
     */
    suspend fun mutate(
        mutation: String,
        variables: Map<String, Any> = emptyMap(),
        operationName: String? = null,
        headers: Map<String, String> = emptyMap()
    ): String

    /**
     * 获取 GraphQL Schema。
     *
     * @param headers 请求头，可选。
     * @return GraphQL Schema，以 SDL 字符串形式返回。
     */
    suspend fun getSchema(headers: Map<String, String> = emptyMap()): String
}
