package ai.kastrax.zod

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import java.util.regex.Pattern

class SchemaJsonExtensionsTest {

    @Test
    fun `StringSchema should convert to JSON Schema`() {
        val schema = StringSchema()
        schema.minLength = 3
        schema.maxLength = 10
        schema.pattern = Pattern.compile("^[a-z]+$")
        schema.email = true
        val describedSchema = schema.describe("A test string") as DescribedSchema<*, *>

        val jsonSchema = describedSchema.toJsonSchema()

        assertTrue(jsonSchema is JsonObject)
        assertEquals("string", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals(3, jsonSchema.jsonObject["minLength"]?.jsonPrimitive?.int)
        assertEquals(10, jsonSchema.jsonObject["maxLength"]?.jsonPrimitive?.int)
        assertEquals("^[a-z]+$", jsonSchema.jsonObject["pattern"]?.jsonPrimitive?.content)
        assertEquals("email", jsonSchema.jsonObject["format"]?.jsonPrimitive?.content)
        assertEquals("A test string", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)
    }

    @Test
    fun `NumberSchema should convert to JSON Schema`() {
        val schema = NumberSchema()
        schema.min = 0.0
        schema.max = 100.0
        schema.multipleOf = 5.0
        val describedSchema = schema.describe("A test number") as DescribedSchema<*, *>

        val jsonSchema = describedSchema.toJsonSchema()

        assertTrue(jsonSchema is JsonObject)
        assertEquals("number", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals(0.0, jsonSchema.jsonObject["minimum"]?.jsonPrimitive?.double)
        assertEquals(100.0, jsonSchema.jsonObject["maximum"]?.jsonPrimitive?.double)
        assertEquals(5.0, jsonSchema.jsonObject["multipleOf"]?.jsonPrimitive?.double)
        assertEquals("A test number", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)
    }

    @Test
    fun `BooleanSchema should convert to JSON Schema`() {
        val schema = BooleanSchema()
        val describedSchema = schema.describe("A test boolean") as DescribedSchema<*, *>

        val jsonSchema = describedSchema.toJsonSchema()

        assertTrue(jsonSchema is JsonObject)
        assertEquals("boolean", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals("A test boolean", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)
    }

    @Test
    fun `ObjectSchema should convert to JSON Schema`() {
        val stringSchema = StringSchema() as Schema<Any?, Any?>
        val numberSchema = NumberSchema() as Schema<Any?, Any?>
        val nameField = ObjectField("name", stringSchema, true)
        val ageField = ObjectField("age", numberSchema, false)
        val fields = mapOf("name" to nameField, "age" to ageField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields)
        val describedSchema = schema.describe("A test object") as DescribedSchema<*, *>

        val jsonSchema = describedSchema.toJsonSchema()

        assertTrue(jsonSchema is JsonObject)
        assertEquals("object", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)
        assertTrue(jsonSchema.jsonObject["properties"] is JsonObject)
        assertTrue(jsonSchema.jsonObject["properties"]?.jsonObject?.containsKey("name") == true)
        assertTrue(jsonSchema.jsonObject["properties"]?.jsonObject?.containsKey("age") == true)
        assertTrue(jsonSchema.jsonObject["required"] is JsonArray)
        assertEquals("name", jsonSchema.jsonObject["required"]?.jsonArray?.get(0)?.jsonPrimitive?.content)
        assertEquals("A test object", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)
    }

    @Test
    fun `ArraySchema should convert to JSON Schema`() {
        val schema = ArraySchema(StringSchema())
        schema.minLength = 1
        schema.maxLength = 5
        val describedSchema = schema.describe("A test array") as DescribedSchema<*, *>

        val jsonSchema = describedSchema.toJsonSchema()

        assertTrue(jsonSchema is JsonObject)
        assertEquals("array", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)
        assertTrue(jsonSchema.jsonObject["items"] is JsonObject)
        assertEquals("string", jsonSchema.jsonObject["items"]?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals(1, jsonSchema.jsonObject["minItems"]?.jsonPrimitive?.int)
        assertEquals(5, jsonSchema.jsonObject["maxItems"]?.jsonPrimitive?.int)
        assertEquals("A test array", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)
    }

    @Test
    fun `TupleSchema should convert to JSON Schema`() {
        val stringSchema = StringSchema() as Schema<Any?, Any?>
        val numberSchema = NumberSchema() as Schema<Any?, Any?>
        val booleanSchema = BooleanSchema() as Schema<Any?, Any?>
        val schemas = listOf(stringSchema, numberSchema, booleanSchema)
        val schema = TupleSchema(schemas)
        val describedSchema = schema.describe("A test tuple") as DescribedSchema<*, *>

        val jsonSchema = describedSchema.toJsonSchema()

        assertTrue(jsonSchema is JsonObject)
        assertEquals("array", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)
        assertTrue(jsonSchema.jsonObject["items"] is JsonArray)
        assertEquals(3, jsonSchema.jsonObject["items"]?.jsonArray?.size)
        assertEquals("string", jsonSchema.jsonObject["items"]?.jsonArray?.get(0)?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals("number", jsonSchema.jsonObject["items"]?.jsonArray?.get(1)?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals("boolean", jsonSchema.jsonObject["items"]?.jsonArray?.get(2)?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals(3, jsonSchema.jsonObject["minItems"]?.jsonPrimitive?.int)
        assertEquals(3, jsonSchema.jsonObject["maxItems"]?.jsonPrimitive?.int)
        assertEquals("A test tuple", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)
    }

    @Test
    fun `UnionSchema should convert to JSON Schema`() {
        val stringSchema = StringSchema() as Schema<Any?, Any?>
        val numberSchema = NumberSchema() as Schema<Any?, Any?>
        val schemas = listOf(stringSchema, numberSchema)
        val schema = UnionSchema(schemas)
        val describedSchema = schema.describe("A test union") as DescribedSchema<*, *>

        val jsonSchema = describedSchema.toJsonSchema()

        assertTrue(jsonSchema is JsonObject)
        assertTrue(jsonSchema.jsonObject["anyOf"] is JsonArray)
        assertEquals(2, jsonSchema.jsonObject["anyOf"]?.jsonArray?.size)
        assertEquals("string", jsonSchema.jsonObject["anyOf"]?.jsonArray?.get(0)?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals("number", jsonSchema.jsonObject["anyOf"]?.jsonArray?.get(1)?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals("A test union", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)
    }

    @Test
    fun `StringSchema should parse JSON`() {
        val schema = StringSchema()
        val json = JsonPrimitive("hello")

        val result = schema.parseJson(json)

        assertEquals("hello", result)
    }

    @Test
    fun `NumberSchema should parse JSON`() {
        val schema = NumberSchema()
        val json = JsonPrimitive(42)

        val result = schema.parseJson(json)

        assertEquals(42.0, result)
    }

    @Test
    fun `BooleanSchema should parse JSON`() {
        val schema = BooleanSchema()
        val json = JsonPrimitive(true)

        val result = schema.parseJson(json)

        assertEquals(true, result)
    }

    @Test
    fun `ObjectSchema should parse JSON`() {
        val stringSchema = StringSchema() as Schema<Any?, Any?>
        val numberSchema = NumberSchema() as Schema<Any?, Any?>
        val nameField = ObjectField("name", stringSchema, true)
        val ageField = ObjectField("age", numberSchema, false)
        val fields = mapOf("name" to nameField, "age" to ageField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields)

        val json = buildJsonObject {
            put("name", "John")
            put("age", 30)
        }

        val result = schema.parseJson(json)

        assertEquals("John", result["name"])
        assertEquals(30.0, result["age"])
    }

    @Test
    fun `ArraySchema should parse JSON`() {
        val schema = ArraySchema(StringSchema())

        val json = buildJsonArray {
            add("hello")
            add("world")
        }

        val result = schema.parseJson(json)

        assertEquals(2, result!!.size)
        assertEquals("hello", result[0])
        assertEquals("world", result[1])
    }

    @Test
    fun `TupleSchema should parse JSON`() {
        val stringSchema = StringSchema() as Schema<Any?, Any?>
        val numberSchema = NumberSchema() as Schema<Any?, Any?>
        val booleanSchema = BooleanSchema() as Schema<Any?, Any?>
        val schemas = listOf(stringSchema, numberSchema, booleanSchema)
        val schema = TupleSchema(schemas)

        val json = buildJsonArray {
            add("hello")
            add(42)
            add(true)
        }

        val result = schema.parseJson(json)

        assertEquals(3, result!!.size)
        assertEquals("hello", result[0])
        assertEquals(42.0, result[1])
        assertEquals(true, result[2])
    }

    @Test
    fun `UnionSchema should parse JSON`() {
        val stringSchema = StringSchema() as Schema<Any?, Any?>
        val numberSchema = NumberSchema() as Schema<Any?, Any?>
        val schemas = listOf(stringSchema, numberSchema)
        val schema = UnionSchema(schemas)

        val json1 = JsonPrimitive("hello")
        val result1 = schema.parseJson(json1)
        assertEquals("hello", result1)

        val json2 = JsonPrimitive(42)
        val result2 = schema.parseJson(json2)
        assertEquals(42.0, result2)
    }

    @Test
    fun `StringSchema should convert to JSON`() {
        val schema = StringSchema()
        val output = "hello"

        val json = schema.toJson(output)

        assertTrue(json is JsonPrimitive)
        assertEquals("hello", json.jsonPrimitive.content)
    }

    @Test
    fun `NumberSchema should convert to JSON`() {
        val schema = NumberSchema()
        val output = 42.0

        val json = schema.toJson(output)

        assertTrue(json is JsonPrimitive)
        assertEquals(42.0, json.jsonPrimitive.double)
    }

    @Test
    fun `BooleanSchema should convert to JSON`() {
        val schema = BooleanSchema()
        val output = true

        val json = schema.toJson(output)

        assertTrue(json is JsonPrimitive)
        assertEquals(true, json.jsonPrimitive.boolean)
    }

    @Test
    fun `ObjectSchema should convert to JSON`() {
        val stringSchema = StringSchema() as Schema<Any?, Any?>
        val numberSchema = NumberSchema() as Schema<Any?, Any?>
        val nameField = ObjectField("name", stringSchema, true)
        val ageField = ObjectField("age", numberSchema, false)
        val fields = mapOf("name" to nameField, "age" to ageField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields)

        val output = mapOf(
            "name" to "John",
            "age" to 30.0
        )

        val json = schema.toJson(output)

        assertTrue(json is JsonObject)
        assertEquals("John", json.jsonObject["name"]?.jsonPrimitive?.content)
        assertEquals(30.0, json.jsonObject["age"]?.jsonPrimitive?.double)
    }

    @Test
    fun `ArraySchema should convert to JSON`() {
        val stringSchema = StringSchema() as Schema<Any?, Any?>
        val schema = ArraySchema(stringSchema)

        val output = listOf("hello", "world")

        val json = schema.toJson(output)

        assertTrue(json is JsonArray)
        assertEquals(2, json.jsonArray.size)
        assertEquals("hello", json.jsonArray[0].jsonPrimitive.content)
        assertEquals("world", json.jsonArray[1].jsonPrimitive.content)
    }

    @Test
    fun `TupleSchema should convert to JSON`() {
        val stringSchema = StringSchema() as Schema<Any?, Any?>
        val numberSchema = NumberSchema() as Schema<Any?, Any?>
        val booleanSchema = BooleanSchema() as Schema<Any?, Any?>
        val schemas = listOf(stringSchema, numberSchema, booleanSchema)
        val schema = TupleSchema(schemas)

        val output = listOf("hello", 42.0, true)

        val json = schema.toJson(output)

        assertTrue(json is JsonArray)
        assertEquals(3, json.jsonArray.size)
        assertEquals("hello", json.jsonArray[0].jsonPrimitive.content)
        assertEquals(42.0, json.jsonArray[1].jsonPrimitive.double)
        assertEquals(true, json.jsonArray[2].jsonPrimitive.boolean)
    }
}
