package ai.kastrax.datasource.api

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphQlConnectorTest {

    private lateinit var connector: GraphQlConnectorImpl
    private val mockEngine = MockEngine { request ->
        val content = request.body.toByteArray().decodeToString()
        val jsonContent = Json.parseToJsonElement(content).jsonObject
        
        val query = jsonContent["query"]?.jsonPrimitive?.content ?: ""
        val variables = jsonContent["variables"]?.jsonObject
        val operationName = jsonContent["operationName"]?.jsonPrimitive?.content
        
        val responseContent = when {
            query.contains("query IntrospectionQuery") -> {
                """
                {
                    "data": {
                        "__schema": {
                            "queryType": { "name": "Query" },
                            "mutationType": { "name": "Mutation" },
                            "subscriptionType": null,
                            "types": []
                        }
                    }
                }
                """
            }
            query.contains("query GetUser") -> {
                """
                {
                    "data": {
                        "user": {
                            "id": "1",
                            "name": "John Doe",
                            "email": "john@example.com"
                        }
                    }
                }
                """
            }
            query.contains("mutation CreateUser") -> {
                val name = variables?.get("name")?.jsonPrimitive?.content
                val email = variables?.get("email")?.jsonPrimitive?.content
                
                """
                {
                    "data": {
                        "createUser": {
                            "id": "2",
                            "name": "$name",
                            "email": "$email"
                        }
                    }
                }
                """
            }
            else -> {
                """
                {
                    "errors": [
                        {
                            "message": "Unknown query"
                        }
                    ]
                }
                """
            }
        }
        
        respond(
            content = ByteReadChannel(responseContent),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }

    @BeforeEach
    fun setup() {
        connector = GraphQlConnectorImpl(
            name = "test-graphql",
            baseUrl = "https://example.com/graphql",
            defaultHeaders = mapOf("Content-Type" to "application/json")
        )
        
        // Replace the HTTP client with our mock engine
        val field = ApiConnectorImpl::class.java.getDeclaredField("httpClient")
        field.isAccessible = true
        field.set(connector, io.ktor.client.HttpClient(mockEngine))
    }

    @Test
    fun `test query`() = runBlocking {
        val query = """
            query GetUser(${'$'}id: ID!) {
                user(id: ${'$'}id) {
                    id
                    name
                    email
                }
            }
        """.trimIndent()
        
        val variables = mapOf("id" to "1")
        val result = connector.query(query, variables, "GetUser")
        
        // Parse the result
        val jsonResult = Json.parseToJsonElement(result).jsonObject
        val data = jsonResult["data"]?.jsonObject
        val user = data?.get("user")?.jsonObject
        
        // Verify the result
        assertEquals("1", user?.get("id")?.jsonPrimitive?.content)
        assertEquals("John Doe", user?.get("name")?.jsonPrimitive?.content)
        assertEquals("john@example.com", user?.get("email")?.jsonPrimitive?.content)
    }

    @Test
    fun `test mutate`() = runBlocking {
        val mutation = """
            mutation CreateUser(${'$'}name: String!, ${'$'}email: String!) {
                createUser(name: ${'$'}name, email: ${'$'}email) {
                    id
                    name
                    email
                }
            }
        """.trimIndent()
        
        val variables = mapOf(
            "name" to "Jane Smith",
            "email" to "jane@example.com"
        )
        
        val result = connector.mutate(mutation, variables, "CreateUser")
        
        // Parse the result
        val jsonResult = Json.parseToJsonElement(result).jsonObject
        val data = jsonResult["data"]?.jsonObject
        val createUser = data?.get("createUser")?.jsonObject
        
        // Verify the result
        assertEquals("2", createUser?.get("id")?.jsonPrimitive?.content)
        assertEquals("Jane Smith", createUser?.get("name")?.jsonPrimitive?.content)
        assertEquals("jane@example.com", createUser?.get("email")?.jsonPrimitive?.content)
    }

    @Test
    fun `test getSchema`() = runBlocking {
        val schema = connector.getSchema()
        
        // Parse the result
        val jsonResult = Json.parseToJsonElement(schema).jsonObject
        val data = jsonResult["data"]?.jsonObject
        val schemaData = data?.get("__schema")?.jsonObject
        
        // Verify the result
        assertEquals("Query", schemaData?.get("queryType")?.jsonObject?.get("name")?.jsonPrimitive?.content)
        assertEquals("Mutation", schemaData?.get("mutationType")?.jsonObject?.get("name")?.jsonPrimitive?.content)
    }
}
