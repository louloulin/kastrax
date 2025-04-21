package ai.kastrax.datasource.api

import ai.kastrax.datasource.common.DataSourceType
import ai.kastrax.datasource.common.GraphQlConnector
import ai.kastrax.datasource.common.AuthType
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * GraphQL 连接器实现类，提供了与 GraphQL API 交互的功能。
 */
class GraphQlConnectorImpl(
    name: String,
    override val baseUrl: String,
    override val defaultHeaders: Map<String, String> = emptyMap(),
    private val authType: AuthType = AuthType.NONE,
    private val authToken: String = "",
    private val username: String = "",
    private val password: String = ""
) : ApiConnectorImpl(name), GraphQlConnector {

    /**
     * 执行 GraphQL 查询。
     */
    override suspend fun query(
        query: String,
        variables: Map<String, Any>,
        operationName: String?,
        headers: Map<String, String>
    ): String {
        logger.debug { "Executing GraphQL query: $query" }

        val requestBody = buildJsonObject {
            put("query", JsonPrimitive(query))

            if (variables.isNotEmpty()) {
                put("variables", JsonObject(variables.mapValues { (_, value) ->
                    when (value) {
                        is String -> JsonPrimitive(value)
                        is Number -> JsonPrimitive(value)
                        is Boolean -> JsonPrimitive(value)
                        is JsonElement -> value
                        else -> JsonPrimitive(value.toString())
                    }
                }))
            }

            if (operationName != null) {
                put("operationName", JsonPrimitive(operationName))
            }
        }.toString()

        return post(
            path = "",
            body = requestBody,
            headers = headers + mapOf("Content-Type" to "application/json")
        )
    }

    /**
     * 执行 GraphQL 变更。
     */
    override suspend fun mutate(
        mutation: String,
        variables: Map<String, Any>,
        operationName: String?,
        headers: Map<String, String>
    ): String {
        logger.debug { "Executing GraphQL mutation: $mutation" }

        // GraphQL 不区分查询和变更的请求方式，都是 POST 请求
        return query(
            query = mutation,
            variables = variables,
            operationName = operationName,
            headers = headers
        )
    }

    /**
     * 获取 GraphQL Schema。
     */
    override suspend fun getSchema(headers: Map<String, String>): String {
        logger.debug { "Getting GraphQL schema" }

        val introspectionQuery = """
            query IntrospectionQuery {
              __schema {
                queryType { name }
                mutationType { name }
                subscriptionType { name }
                types {
                  ...FullType
                }
                directives {
                  name
                  description
                  locations
                  args {
                    ...InputValue
                  }
                }
              }
            }

            fragment FullType on __Type {
              kind
              name
              description
              fields(includeDeprecated: true) {
                name
                description
                args {
                  ...InputValue
                }
                type {
                  ...TypeRef
                }
                isDeprecated
                deprecationReason
              }
              inputFields {
                ...InputValue
              }
              interfaces {
                ...TypeRef
              }
              enumValues(includeDeprecated: true) {
                name
                description
                isDeprecated
                deprecationReason
              }
              possibleTypes {
                ...TypeRef
              }
            }

            fragment InputValue on __InputValue {
              name
              description
              type { ...TypeRef }
              defaultValue
            }

            fragment TypeRef on __Type {
              kind
              name
              ofType {
                kind
                name
                ofType {
                  kind
                  name
                  ofType {
                    kind
                    name
                    ofType {
                      kind
                      name
                      ofType {
                        kind
                        name
                        ofType {
                          kind
                          name
                          ofType {
                            kind
                            name
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        return query(
            query = introspectionQuery,
            headers = headers
        )
    }

    /**
     * 添加认证信息到请求头。
     */
    private fun appendAuth(headers: HeadersBuilder) {
        when (authType) {
            AuthType.BEARER -> {
                if (authToken.isNotEmpty()) {
                    headers.append(HttpHeaders.Authorization, "Bearer $authToken")
                }
            }
            AuthType.BASIC -> {
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    val credentials = "$username:$password"
                    val encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
                    headers.append(HttpHeaders.Authorization, "Basic $encodedCredentials")
                }
            }
            AuthType.NONE -> {
                // 不添加认证信息
            }
        }
    }
}
