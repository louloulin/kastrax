package ai.kastrax.zod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaUnionsTest {

    @Test
    fun `UnionSchema should validate union types`() {
        val schema = UnionSchema(listOf(StringSchema(), NumberSchema()))

        // Valid string
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello", (result1 as SchemaResult.Success).data)

        // Valid number
        val result2 = schema.safeParse(42)
        assertTrue(result2 is SchemaResult.Success)
        assertEquals(42.0, (result2 as SchemaResult.Success).data)

        // Invalid type
        val result3 = schema.safeParse(true)
        assertTrue(result3 is SchemaResult.Failure)
        val error = (result3 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_UNION, error.issues[0].code)
    }

    @Test
    fun `UnionSchema should support or method`() {
        val stringSchema = StringSchema()
        val numberSchema = NumberSchema()
        val schema = stringSchema.or(numberSchema)

        // Valid string
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello", (result1 as SchemaResult.Success).data)

        // Valid number
        val result2 = schema.safeParse(42)
        assertTrue(result2 is SchemaResult.Success)
        assertEquals(42.0, (result2 as SchemaResult.Success).data)

        // Invalid type
        val result3 = schema.safeParse(true)
        assertTrue(result3 is SchemaResult.Failure)
    }

    @Test
    fun `UnionSchema should validate discriminated unions`() {
        // Create object schemas with discriminator
        val dogSchema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(
            mapOf(
                "type" to ObjectField("type", LiteralSchema("dog"), true),
                "bark" to ObjectField("bark", BooleanSchema(), true)
            )
        )

        val catSchema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(
            mapOf(
                "type" to ObjectField("type", LiteralSchema("cat"), true),
                "meow" to ObjectField("meow", BooleanSchema(), true)
            )
        )

        val schema = DiscriminatedUnionSchema<String>(
            "type",
            mapOf("dog" to dogSchema, "cat" to catSchema)
        )

        // Valid dog
        val dog = mapOf("type" to "dog", "bark" to true)
        val result1 = schema.safeParse(dog)
        assertTrue(result1 is SchemaResult.Success)

        // Valid cat
        val cat = mapOf("type" to "cat", "meow" to true)
        val result2 = schema.safeParse(cat)
        assertTrue(result2 is SchemaResult.Success)

        // Invalid discriminator
        val invalid1 = mapOf("type" to "bird", "chirp" to true)
        val result3 = schema.safeParse(invalid1)
        assertTrue(result3 is SchemaResult.Failure)

        // Missing discriminator
        val invalid2 = mapOf("bark" to true)
        val result4 = schema.safeParse(invalid2)
        assertTrue(result4 is SchemaResult.Failure)

        // Invalid schema (missing required field)
        val invalid3 = mapOf("type" to "dog")
        val result5 = schema.safeParse(invalid3)
        assertTrue(result5 is SchemaResult.Failure)
    }

    @Test
    fun `IntersectionSchema should validate intersection types`() {
        val personSchema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(
            mapOf(
                "name" to ObjectField("name", StringSchema(), true),
                "age" to ObjectField("age", NumberSchema(), true)
            )
        )

        val employeeSchema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(
            mapOf(
                "company" to ObjectField("company", StringSchema(), true),
                "position" to ObjectField("position", StringSchema(), true)
            )
        )

        val schema = IntersectionSchema<Map<String, Any?>, Map<String, Any?>, Map<String, Any?>, Map<String, Any?>>(
            personSchema,
            employeeSchema
        )

        // Valid intersection
        val valid = mapOf(
            "name" to "John",
            "age" to 30,
            "company" to "Acme",
            "position" to "Developer"
        )
        val result1 = schema.safeParse(valid)
        assertTrue(result1 is SchemaResult.Success)
        val data1 = (result1 as SchemaResult.Success).data as Map<*, *>
        assertEquals("John", data1["name"])
        assertEquals(30.0, data1["age"])
        assertEquals("Acme", data1["company"])
        assertEquals("Developer", data1["position"])

        // Missing field from first schema
        val invalid1 = mapOf(
            "age" to 30,
            "company" to "Acme",
            "position" to "Developer"
        )
        val result2 = schema.safeParse(invalid1)
        assertTrue(result2 is SchemaResult.Failure)

        // Missing field from second schema
        val invalid2 = mapOf(
            "name" to "John",
            "age" to 30,
            "company" to "Acme"
        )
        val result3 = schema.safeParse(invalid2)
        assertTrue(result3 is SchemaResult.Failure)
    }

    @Test
    fun `IntersectionSchema should support and method`() {
        val personSchema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(
            mapOf(
                "name" to ObjectField("name", StringSchema(), true),
                "age" to ObjectField("age", NumberSchema(), true)
            )
        )

        val employeeSchema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(
            mapOf(
                "company" to ObjectField("company", StringSchema(), true),
                "position" to ObjectField("position", StringSchema(), true)
            )
        )

        val schema = personSchema.and(employeeSchema)

        // Valid intersection
        val valid = mapOf(
            "name" to "John",
            "age" to 30,
            "company" to "Acme",
            "position" to "Developer"
        )
        val result = schema.safeParse(valid)
        assertTrue(result is SchemaResult.Success)
    }
}
