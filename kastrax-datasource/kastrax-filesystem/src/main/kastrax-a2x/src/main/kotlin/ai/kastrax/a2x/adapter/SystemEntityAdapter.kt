package ai.kastrax.a2x.adapter

import ai.kastrax.a2x.A2X
import ai.kastrax.a2x.EntityAdapter
import ai.kastrax.a2x.entity.Entity
import ai.kastrax.a2x.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.lang.management.ManagementFactory
import java.util.UUID

/**
 * 系统实体适配器，将系统适配为 A2X 实体
 */
class SystemEntityAdapter : EntityAdapter {
    /**
     * A2X 实例
     */
    private val a2x = A2X.getInstance()

    /**
     * 适配系统为 A2X 实体
     */
    override fun adapt(obj: Any): Entity {
        // 这里的 obj 参数被忽略，因为我们总是创建一个表示当前系统的实体
        return SystemEntityImpl()
    }

    /**
     * 系统实体实现
     */
    private inner class SystemEntityImpl(
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) : Entity {
        /**
         * 实体卡片
         */
        private val entityCard: EntityCard by lazy {
            val osName = System.getProperty("os.name")
            val osVersion = System.getProperty("os.version")
            val osArch = System.getProperty("os.arch")
            
            EntityCard(
                id = "system-${UUID.randomUUID()}",
                name = "System ($osName)",
                description = "System entity representing the local operating system",
                version = osVersion,
                type = EntityType.SYSTEM,
                endpoint = "local://system",
                capabilities = listOf(
                    Capability(
                        id = "system_info",
                        name = "System Information",
                        description = "Get system information",
                        parameters = emptyList(),
                        returnType = "json"
                    ),
                    Capability(
                        id = "file_operations",
                        name = "File Operations",
                        description = "Perform file operations",
                        parameters = listOf(
                            Parameter(
                                name = "operation",
                                type = "string",
                                description = "Operation to perform (read, write, list, exists, delete)",
                                required = true
                            ),
                            Parameter(
                                name = "path",
                                type = "string",
                                description = "File or directory path",
                                required = true
                            ),
                            Parameter(
                                name = "content",
                                type = "string",
                                description = "Content to write (for write operation)",
                                required = false
                            )
                        ),
                        returnType = "json"
                    ),
                    Capability(
                        id = "process_operations",
                        name = "Process Operations",
                        description = "Perform process operations",
                        parameters = listOf(
                            Parameter(
                                name = "operation",
                                type = "string",
                                description = "Operation to perform (list, start, kill)",
                                required = true
                            ),
                            Parameter(
                                name = "command",
                                type = "string",
                                description = "Command to execute (for start operation)",
                                required = false
                            ),
                            Parameter(
                                name = "pid",
                                type = "number",
                                description = "Process ID (for kill operation)",
                                required = false
                            )
                        ),
                        returnType = "json"
                    )
                ),
                authentication = Authentication(
                    type = AuthenticationType.NONE
                ),
                metadata = mapOf(
                    "os.name" to osName,
                    "os.version" to osVersion,
                    "os.arch" to osArch,
                    "java.version" to System.getProperty("java.version")
                )
            )
        }

        override fun getEntityCard(): EntityCard = entityCard

        override fun getCapabilities(): List<Capability> = entityCard.capabilities

        override suspend fun invoke(request: InvokeRequest): InvokeResponse {
            return when (request.capabilityId) {
                "system_info" -> getSystemInfo(request)
                "file_operations" -> performFileOperation(request)
                "process_operations" -> performProcessOperation(request)
                else -> InvokeResponse(
                    id = request.id,
                    source = EntityReference(
                        id = entityCard.id,
                        type = EntityType.SYSTEM
                    ),
                    target = request.source,
                    result = JsonPrimitive("Capability not supported: ${request.capabilityId}")
                )
            }
        }

        override suspend fun query(request: QueryRequest): QueryResponse {
            return when (request.queryType) {
                "system_status" -> getSystemStatus(request)
                else -> QueryResponse(
                    id = request.id,
                    source = EntityReference(
                        id = entityCard.id,
                        type = EntityType.SYSTEM
                    ),
                    target = request.source,
                    result = JsonPrimitive("Query type not supported: ${request.queryType}")
                )
            }
        }

        override suspend fun processMessage(message: A2XMessage): A2XMessage {
            return when (message) {
                is CapabilityRequest -> handleCapabilityRequest(message)
                is InvokeRequest -> invoke(message)
                is QueryRequest -> query(message)
                else -> ErrorMessage(
                    id = message.id,
                    source = EntityReference(
                        id = entityCard.id,
                        type = EntityType.SYSTEM
                    ),
                    target = message.source,
                    code = "unsupported_message_type",
                    message = "Unsupported message type: ${message.type}"
                )
            }
        }

        override suspend fun sendEvent(event: EventMessage) {
            // 系统实体不处理事件
        }

        override fun subscribeToEvents(eventTypes: List<String>): Flow<EventMessage> {
            // 订阅 A2X 事件流，过滤出目标是当前实体的事件
            return a2x.eventFlow.filter { event ->
                event.target.id == entityCard.id || event.target.id == "*"
            }.filter { event ->
                eventTypes.isEmpty() || eventTypes.contains(event.eventType)
            }
        }

        override fun start() {
            // 系统实体不需要启动
        }

        override fun stop() {
            // 系统实体不需要停止
        }

        /**
         * 处理能力请求
         */
        private fun handleCapabilityRequest(request: CapabilityRequest): CapabilityResponse {
            val capabilities = if (request.capabilityId != null) {
                entityCard.capabilities.filter { it.id == request.capabilityId }
            } else {
                entityCard.capabilities
            }
            
            return CapabilityResponse(
                id = request.id,
                source = EntityReference(
                    id = entityCard.id,
                    type = EntityType.SYSTEM
                ),
                target = request.source,
                capabilities = capabilities,
                metadata = request.metadata
            )
        }

        /**
         * 获取系统信息
         */
        private fun getSystemInfo(request: InvokeRequest): InvokeResponse {
            val runtime = Runtime.getRuntime()
            val mb = 1024 * 1024
            
            val info = mapOf(
                "os" to mapOf(
                    "name" to System.getProperty("os.name"),
                    "version" to System.getProperty("os.version"),
                    "arch" to System.getProperty("os.arch")
                ),
                "java" to mapOf(
                    "version" to System.getProperty("java.version"),
                    "vendor" to System.getProperty("java.vendor")
                ),
                "memory" to mapOf(
                    "total" to (runtime.totalMemory() / mb),
                    "free" to (runtime.freeMemory() / mb),
                    "max" to (runtime.maxMemory() / mb)
                ),
                "processors" to runtime.availableProcessors()
            )
            
            return InvokeResponse(
                id = request.id,
                source = EntityReference(
                    id = entityCard.id,
                    type = EntityType.SYSTEM
                ),
                target = request.source,
                result = kotlinx.serialization.json.buildJsonObject {
                    info.forEach { (key, value) ->
                        when (value) {
                            is Map<*, *> -> {
                                put(key, kotlinx.serialization.json.buildJsonObject {
                                    value.forEach { (k, v) ->
                                        put(k.toString(), JsonPrimitive(v.toString()))
                                    }
                                })
                            }
                            else -> put(key, JsonPrimitive(value.toString()))
                        }
                    }
                }
            )
        }

        /**
         * 执行文件操作
         */
        private fun performFileOperation(request: InvokeRequest): InvokeResponse {
            val operation = request.parameters["operation"]?.toString()?.replace("\"", "") ?: ""
            val path = request.parameters["path"]?.toString()?.replace("\"", "") ?: ""
            val content = request.parameters["content"]?.toString()?.replace("\"", "")
            
            val file = File(path)
            
            val result = when (operation) {
                "read" -> {
                    if (file.exists() && file.isFile) {
                        JsonPrimitive(file.readText())
                    } else {
                        JsonPrimitive("File not found: $path")
                    }
                }
                "write" -> {
                    if (content != null) {
                        file.writeText(content)
                        JsonPrimitive("File written successfully: $path")
                    } else {
                        JsonPrimitive("Content parameter is required for write operation")
                    }
                }
                "list" -> {
                    if (file.exists() && file.isDirectory) {
                        val files = file.listFiles()?.map { it.name } ?: emptyList()
                        kotlinx.serialization.json.buildJsonArray {
                            files.forEach { add(JsonPrimitive(it)) }
                        }
                    } else {
                        JsonPrimitive("Directory not found: $path")
                    }
                }
                "exists" -> {
                    JsonPrimitive(file.exists())
                }
                "delete" -> {
                    if (file.exists()) {
                        val deleted = file.delete()
                        JsonPrimitive("File deletion ${if (deleted) "successful" else "failed"}: $path")
                    } else {
                        JsonPrimitive("File not found: $path")
                    }
                }
                else -> {
                    JsonPrimitive("Unsupported operation: $operation")
                }
            }
            
            return InvokeResponse(
                id = request.id,
                source = EntityReference(
                    id = entityCard.id,
                    type = EntityType.SYSTEM
                ),
                target = request.source,
                result = result
            )
        }

        /**
         * 执行进程操作
         */
        private fun performProcessOperation(request: InvokeRequest): InvokeResponse {
            val operation = request.parameters["operation"]?.toString()?.replace("\"", "") ?: ""
            val command = request.parameters["command"]?.toString()?.replace("\"", "")
            val pid = request.parameters["pid"]?.toString()?.toIntOrNull()
            
            val result = when (operation) {
                "list" -> {
                    val processes = ManagementFactory.getRuntimeMXBean().name.split("@")[0]
                    kotlinx.serialization.json.buildJsonArray {
                        add(JsonPrimitive(processes))
                    }
                }
                "start" -> {
                    if (command != null) {
                        val process = Runtime.getRuntime().exec(command)
                        JsonPrimitive("Process started with PID: ${process.pid()}")
                    } else {
                        JsonPrimitive("Command parameter is required for start operation")
                    }
                }
                "kill" -> {
                    if (pid != null) {
                        try {
                            val process = ProcessHandle.of(pid.toLong()).orElse(null)
                            if (process != null) {
                                val killed = process.destroy()
                                JsonPrimitive("Process kill ${if (killed) "successful" else "failed"}: $pid")
                            } else {
                                JsonPrimitive("Process not found: $pid")
                            }
                        } catch (e: Exception) {
                            JsonPrimitive("Error killing process: ${e.message}")
                        }
                    } else {
                        JsonPrimitive("PID parameter is required for kill operation")
                    }
                }
                else -> {
                    JsonPrimitive("Unsupported operation: $operation")
                }
            }
            
            return InvokeResponse(
                id = request.id,
                source = EntityReference(
                    id = entityCard.id,
                    type = EntityType.SYSTEM
                ),
                target = request.source,
                result = result
            )
        }

        /**
         * 获取系统状态
         */
        private fun getSystemStatus(request: QueryRequest): QueryResponse {
            val runtime = Runtime.getRuntime()
            val mb = 1024 * 1024
            
            val status = mapOf(
                "memory" to mapOf(
                    "total" to (runtime.totalMemory() / mb),
                    "free" to (runtime.freeMemory() / mb),
                    "used" to ((runtime.totalMemory() - runtime.freeMemory()) / mb),
                    "max" to (runtime.maxMemory() / mb)
                ),
                "processors" to mapOf(
                    "available" to runtime.availableProcessors(),
                    "load" to ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
                ),
                "uptime" to ManagementFactory.getRuntimeMXBean().uptime
            )
            
            return QueryResponse(
                id = request.id,
                source = EntityReference(
                    id = entityCard.id,
                    type = EntityType.SYSTEM
                ),
                target = request.source,
                result = kotlinx.serialization.json.buildJsonObject {
                    status.forEach { (key, value) ->
                        when (value) {
                            is Map<*, *> -> {
                                put(key, kotlinx.serialization.json.buildJsonObject {
                                    value.forEach { (k, v) ->
                                        put(k.toString(), JsonPrimitive(v.toString()))
                                    }
                                })
                            }
                            else -> put(key, JsonPrimitive(value.toString()))
                        }
                    }
                }
            )
        }
    }
}
