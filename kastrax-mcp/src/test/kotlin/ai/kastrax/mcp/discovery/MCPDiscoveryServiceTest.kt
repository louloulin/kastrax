package ai.kastrax.mcp.discovery

import ai.kastrax.mcp.protocol.ServerCapabilities
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URL

class MCPDiscoveryServiceTest {
    private lateinit var discoveryService: MCPDiscoveryService

    @BeforeEach
    fun setUp() {
        // 创建发现服务
        discoveryService = mcpDiscoveryService {
            // 添加测试注册表
            registry {
                name("Test Registry")
                description("A test registry for MCP servers")
                homepage("https://example.com/test-registry")
            }
        }

        // 获取测试注册表
        val registry = discoveryService.getRegistry("Test Registry")

        // 添加测试服务器
        registry?.registerServer(
            MCPRegistryEntry(
                id = "test-server-1",
                name = "Test Server 1",
                description = "A test MCP server",
                version = "1.0.0",
                publisher = PublisherInfo(
                    id = "test-publisher",
                    name = "Test Publisher",
                    url = "https://example.com/test-publisher"
                ),
                isOfficial = true,
                sourceUrl = "https://github.com/test/test-server",
                capabilities = ServerCapabilities(
                    resources = true,
                    tools = true,
                    prompts = false
                ),
                schemas = listOf(
                    ServerSchema(
                        command = "npx",
                        args = listOf("tsx", "test-server.ts"),
                        env = mapOf(
                            "API_KEY" to EnvVarSchema(
                                description = "API key for the test server",
                                required = true
                            )
                        )
                    )
                )
            )
        )

        registry?.registerServer(
            MCPRegistryEntry(
                id = "test-server-2",
                name = "Test Server 2",
                description = "Another test MCP server",
                version = "1.0.0",
                capabilities = ServerCapabilities(
                    resources = false,
                    tools = true,
                    prompts = true
                ),
                schemas = listOf(
                    ServerSchema(
                        command = "npx",
                        args = listOf("tsx", "test-server-2.ts")
                    )
                )
            )
        )
    }

    @Test
    fun `test list registries`() {
        // 获取所有注册表
        val registries = discoveryService.listRegistries()

        // 验证结果
        assertEquals(1, registries.size)
        assertEquals("Test Registry", registries[0].name)
        assertEquals("A test registry for MCP servers", registries[0].description)
        assertEquals("https://example.com/test-registry", registries[0].homepage)
    }

    @Test
    fun `test discover servers`() {
        // 发现所有服务器
        val servers = discoveryService.discoverServers()

        // 验证结果
        assertEquals(2, servers.size)

        // 验证第一个服务器
        val server1 = servers.find { it.id == "test-server-1" }
        assertNotNull(server1)
        assertEquals("Test Server 1", server1?.name)
        assertEquals("A test MCP server", server1?.description)
        assertEquals("1.0.0", server1?.version)
        assertTrue(server1?.capabilities?.resources ?: false)
        assertTrue(server1?.capabilities?.tools ?: false)
        assertFalse(server1?.capabilities?.prompts ?: true)

        // 验证第二个服务器
        val server2 = servers.find { it.id == "test-server-2" }
        assertNotNull(server2)
        assertEquals("Test Server 2", server2?.name)
        assertEquals("Another test MCP server", server2?.description)
        assertEquals("1.0.0", server2?.version)
        assertFalse(server2?.capabilities?.resources ?: true)
        assertTrue(server2?.capabilities?.tools ?: false)
        assertTrue(server2?.capabilities?.prompts ?: false)
    }

    @Test
    fun `test discover servers by query`() {
        // 使用查询发现服务器
        val servers = discoveryService.discoverServers("Another")

        // 验证结果
        assertEquals(1, servers.size)
        assertEquals("test-server-2", servers[0].id)
        assertEquals("Test Server 2", servers[0].name)
        assertEquals("Another test MCP server", servers[0].description)
    }

    @Test
    fun `test discover servers by capabilities`() {
        // 根据能力发现服务器
        val servers = discoveryService.discoverServersByCapabilities(listOf("resources", "tools"))

        // 验证结果
        assertEquals(1, servers.size)
        assertEquals("test-server-1", servers[0].id)
        assertEquals("Test Server 1", servers[0].name)
    }

    @Test
    fun `test observe server changes`() {
        // 获取测试注册表
        val registry = discoveryService.getRegistry("Test Registry")
        assertNotNull(registry)

        // 添加新服务器
        val newServer = MCPRegistryEntry(
            id = "test-server-3",
            name = "Test Server 3",
            description = "A third test MCP server",
            version = "1.0.0",
            capabilities = ServerCapabilities(
                resources = true,
                tools = true,
                prompts = true
            ),
            schemas = listOf(
                ServerSchema(
                    command = "npx",
                    args = listOf("tsx", "test-server-3.ts")
                )
            )
        )

        // 注册服务器
        registry?.registerServer(newServer)

        // 发布事件
        (discoveryService as MCPDiscoveryServiceImpl).publishServerChangeEvent(
            ServerChangeEvent.ServerAdded(newServer, registry!!)
        )

        // 验证服务器已添加
        val servers = discoveryService.discoverServers()
        assertEquals(3, servers.size)
        val addedServer = servers.find { it.id == "test-server-3" }
        assertNotNull(addedServer)
        assertEquals("Test Server 3", addedServer?.name)
    }
}
