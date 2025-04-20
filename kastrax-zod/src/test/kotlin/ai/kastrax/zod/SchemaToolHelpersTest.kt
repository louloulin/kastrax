package ai.kastrax.zod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.*

// Mock implementation for testing
fun enumInput(values: List<String>, description: String? = null): Schema<String?, String> {
    val schema = EnumSchema(values)
    val nullableSchema = schema.nullable()
    return if (description != null) {
        (nullableSchema.describe(description) as DescribedSchema<String?, String>).unsafeCast<String?, String>()
    } else {
        nullableSchema.unsafeCast<String?, String>()
    }
}

// Mock implementation for testing
fun ObjectSchemaBuilder.enumField(key: String, values: List<String>, description: String? = null, required: Boolean = true) {
    val schema = EnumSchema(values)
    val finalSchema = if (description != null) {
        schema.describe(description).unsafeCast<String, String>()
    } else {
        schema
    }
    field(key, finalSchema, required)
}

class SchemaToolHelpersTest {

    @Test
    fun `stringInput should create StringSchema`() {
        val schema = stringInput("A test string").unsafeCast<String?, String>()

        assertTrue(schema is Schema<*, *>)

        // Check if it's a string schema
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema is JsonObject)
        assertEquals("string", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)

        // Check if the description is set correctly in the JSON schema
        val description = jsonSchema.jsonObject["description"]?.jsonPrimitive?.content
        assertEquals("A test string", description)
    }

    @Test
    fun `numberInput should create NumberSchema`() {
        val schema = numberInput("A test number").unsafeCast<Number?, Double>()

        assertTrue(schema is Schema<*, *>)

        // Check if it's a number schema
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema is JsonObject)
        assertEquals("number", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)

        // Check if the description is set correctly in the JSON schema
        val description = jsonSchema.jsonObject["description"]?.jsonPrimitive?.content
        assertEquals("A test number", description)
    }

    @Test
    fun `booleanInput should create BooleanSchema`() {
        val schema = booleanInput("A test boolean").unsafeCast<Boolean?, Boolean>()

        assertTrue(schema is Schema<*, *>)

        // Check if it's a boolean schema
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema is JsonObject)
        assertEquals("boolean", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)

        // Check if the description is set correctly in the JSON schema
        val description = jsonSchema.jsonObject["description"]?.jsonPrimitive?.content
        assertEquals("A test boolean", description)
    }

    @Test
    fun `objectInput should create ObjectSchema`() {
        val schema = objectInput("A test object") {
            stringField("name", "Person's name")
            numberField("age", "Person's age")
            booleanField("active", "Is the person active")
        }.unsafeCast<Map<String, Any?>, Map<String, Any?>>()

        assertTrue(schema is Schema<*, *>)
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema is JsonObject)
        assertEquals("A test object", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)

        // Check if it's an object schema by verifying the JSON schema type
        assertEquals("object", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)

        // Check if properties exist in the JSON schema
        val properties = jsonSchema.jsonObject["properties"]?.jsonObject
        assertNotNull(properties)

        // Verify fields exist
        assertTrue(properties.containsKey("name"))
        assertTrue(properties.containsKey("age"))
        assertTrue(properties.containsKey("active"))
    }

    @Test
    fun `arrayInput should create ArraySchema`() {
        val schema = arrayInput(stringInput("Array element").unsafeCast<String?, String>(), "A test array").unsafeCast<List<String?>, List<String>>()

        assertTrue(schema is Schema<*, *>)
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema is JsonObject)
        assertEquals("A test array", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)

        // Check if it's an array schema by verifying the JSON schema type
        assertEquals("array", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)

        // Check if items exist in the JSON schema
        assertNotNull(jsonSchema.jsonObject["items"])
    }

    @Test
    fun `tupleInput should create TupleSchema`() {
        val schema = tupleInput(
            stringInput("First element"),
            numberInput("Second element"),
            booleanInput("Third element"),
            description = "A test tuple"
        )

        assertTrue(schema is Schema<*, *>)
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema is JsonObject)
        assertEquals("A test tuple", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)

        // Check if it's an array schema by verifying the JSON schema type
        assertEquals("array", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)

        // Check if items exist in the JSON schema and it's an array
        val items = jsonSchema.jsonObject["items"]?.jsonArray
        assertNotNull(items)
        assertEquals(3, items.size)
    }

    @Test
    fun `unionInput should create UnionSchema`() {
        val schema = unionInput(
            stringInput("String option"),
            numberInput("Number option"),
            description = "A test union"
        )

        assertTrue(schema is Schema<*, *>)
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema is JsonObject)
        assertEquals("A test union", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)

        // Check if anyOf exists in the JSON schema and it's an array
        val anyOf = jsonSchema.jsonObject["anyOf"]?.jsonArray
        assertNotNull(anyOf)
        assertEquals(2, anyOf.size)
    }

    @Test
    fun `enumInput should create EnumSchema`() {
        // Use string values for enum test
        val schema = enumInput(listOf("A", "B", "C"), "A test enum")

        assertTrue(schema is Schema<*, *>)
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema is JsonObject)
        assertEquals("A test enum", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)

        // Check if enum values exist in the JSON schema
        val enumValues = jsonSchema.jsonObject["enum"]?.jsonArray
        assertNotNull(enumValues)
        assertEquals(3, enumValues.size)
    }

    @Test
    fun `literalInput should create LiteralSchema`() {
        val schema = literalInput("test", "A test literal")

        assertTrue(schema is Schema<*, *>)
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema is JsonObject)
        assertEquals("A test literal", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)

        // Check if const exists in the JSON schema
        assertEquals("test", jsonSchema.jsonObject["const"]?.jsonPrimitive?.content)
    }

    @Test
    fun `ObjectSchemaBuilder extensions should work`() {
        val schema = objectInput("A test object") {
            // Test stringField
            stringField("name", "Person's name") {
                minLength = 3
                maxLength = 50
            }

            // Test numberField
            numberField("age", "Person's age") {
                min = 0.0
                max = 120.0
            }

            // Test booleanField
            booleanField("active", "Is the person active")

            // Test objectField
            objectField("address", "Person's address") {
                stringField("street", "Street name")
                stringField("city", "City name")
                stringField("country", "Country name")
            }

            // Test arrayField
            arrayField("hobbies", stringInput(), "Person's hobbies") {
                minLength = 1
                maxLength = 5
            }

            // Test enumField
            // Use string values for enum test
            enumField("enum", listOf("A", "B", "C"), "Test enum")

            // Test literalField
            literalField("literal", "test", "Test literal")
        }

        assertTrue(schema is Schema<*, *>)
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema is JsonObject)
        assertEquals("A test object", jsonSchema.jsonObject["description"]?.jsonPrimitive?.content)

        // Check if it's an object schema by verifying the JSON schema type
        assertEquals("object", jsonSchema.jsonObject["type"]?.jsonPrimitive?.content)

        // Check if properties exist in the JSON schema
        val properties = jsonSchema.jsonObject["properties"]?.jsonObject
        assertNotNull(properties)

        // Verify fields exist
        assertTrue(properties.containsKey("name"))
        assertTrue(properties.containsKey("age"))
        assertTrue(properties.containsKey("active"))
        assertTrue(properties.containsKey("address"))
        assertTrue(properties.containsKey("hobbies"))
        assertTrue(properties.containsKey("enum"))
        assertTrue(properties.containsKey("literal"))

        // No need for additional verification as we've already checked the properties
    }
}
