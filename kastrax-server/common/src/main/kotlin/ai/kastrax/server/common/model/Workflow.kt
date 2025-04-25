package ai.kastrax.server.common.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.Contextual

/**
 * 工作流模型
 */
@Serializable
data class Workflow(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val nodes: List<Node> = emptyList(),
    val edges: List<Edge> = emptyList(),
    val metadata: JsonObject = JsonObject(emptyMap()),
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
)

/**
 * 节点模型
 */
@Serializable
data class Node(
    val id: String,
    val type: String,
    val label: String,
    val position: Position,
    val data: JsonObject = JsonObject(emptyMap()),
    val style: JsonObject = JsonObject(emptyMap())
)

/**
 * 边模型
 */
@Serializable
data class Edge(
    val id: String,
    val source: String,
    val target: String,
    val label: String,
    val data: JsonObject = JsonObject(emptyMap()),
    val style: JsonObject = JsonObject(emptyMap())
)

/**
 * 位置模型
 */
@Serializable
data class Position(
    val x: Double,
    val y: Double
)

/**
 * Instant 序列化器
 */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}
