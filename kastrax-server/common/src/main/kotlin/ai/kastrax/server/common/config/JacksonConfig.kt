package ai.kastrax.server.common.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Jackson 配置类，用于注册自定义序列化器和反序列化器
 */
class JacksonConfig {
    /**
     * 创建 Jackson 模块，注册自定义序列化器和反序列化器
     */
    fun createModule(): SimpleModule {
        val module = SimpleModule()
        
        // 注册 JsonElement 序列化器和反序列化器
        module.addSerializer(JsonElement::class.java, JsonElementSerializer())
        module.addDeserializer(JsonElement::class.java, JsonElementDeserializer())
        
        // 注册 Instant 序列化器和反序列化器
        module.addSerializer(Instant::class.java, InstantSerializer())
        module.addDeserializer(Instant::class.java, InstantDeserializer())
        
        return module
    }
    
    /**
     * JsonElement 序列化器
     */
    class JsonElementSerializer : JsonSerializer<JsonElement>() {
        override fun serialize(value: JsonElement, gen: JsonGenerator, serializers: SerializerProvider) {
            when (value) {
                is JsonObject -> {
                    gen.writeStartObject()
                    value.entries.forEach { (key, element) ->
                        gen.writeFieldName(key)
                        serialize(element, gen, serializers)
                    }
                    gen.writeEndObject()
                }
                is JsonPrimitive -> {
                    when {
                        value.isString -> gen.writeString(value.content)
                        value.content == "true" -> gen.writeBoolean(true)
                        value.content == "false" -> gen.writeBoolean(false)
                        value.content.toDoubleOrNull() != null -> gen.writeNumber(value.content.toDouble())
                        value.content.toLongOrNull() != null -> gen.writeNumber(value.content.toLong())
                        else -> gen.writeString(value.content)
                    }
                }
                else -> gen.writeNull()
            }
        }
    }
    
    /**
     * JsonElement 反序列化器
     */
    class JsonElementDeserializer : JsonDeserializer<JsonElement>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): JsonElement {
            return when (p.currentToken) {
                com.fasterxml.jackson.core.JsonToken.START_OBJECT -> {
                    val map = mutableMapOf<String, JsonElement>()
                    while (p.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT) {
                        val key = p.currentName
                        p.nextToken()
                        map[key] = deserialize(p, ctxt)
                    }
                    JsonObject(map)
                }
                com.fasterxml.jackson.core.JsonToken.VALUE_STRING -> JsonPrimitive(p.valueAsString)
                com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT -> JsonPrimitive(p.valueAsLong.toString())
                com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_FLOAT -> JsonPrimitive(p.valueAsDouble.toString())
                com.fasterxml.jackson.core.JsonToken.VALUE_TRUE -> JsonPrimitive("true")
                com.fasterxml.jackson.core.JsonToken.VALUE_FALSE -> JsonPrimitive("false")
                com.fasterxml.jackson.core.JsonToken.VALUE_NULL -> JsonPrimitive("")
                else -> buildJsonObject {}
            }
        }
    }
    
    /**
     * Instant 序列化器
     */
    class InstantSerializer : JsonSerializer<Instant>() {
        override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeString(value.toString())
        }
    }
    
    /**
     * Instant 反序列化器
     */
    class InstantDeserializer : JsonDeserializer<Instant>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant {
            return Instant.parse(p.valueAsString)
        }
    }
}
