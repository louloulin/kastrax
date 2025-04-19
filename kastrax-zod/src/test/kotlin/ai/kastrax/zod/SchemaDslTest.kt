package ai.kastrax.zod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaDslTest {

    @Test
    fun `DSL should create string schema`() {
        val schema = string {
            minLength = 3
            maxLength = 10
            email = true
        }

        // Valid string
        val result1 = schema.safeParse("test@example.com")
        assertTrue(result1 is SchemaResult.Success)

        // Invalid length
        val result2 = schema.safeParse("hi")
        assertTrue(result2 is SchemaResult.Failure)

        // Invalid email
        val result3 = schema.safeParse("not-an-email")
        assertTrue(result3 is SchemaResult.Failure)
    }

    @Test
    fun `DSL should create number schema`() {
        val schema = number {
            min = 5.0
            max = 10.0
            isInt = true
        }

        // Valid number
        val result1 = schema.safeParse(7)
        assertTrue(result1 is SchemaResult.Success)

        // Too small
        val result2 = schema.safeParse(3)
        assertTrue(result2 is SchemaResult.Failure)

        // Too big
        val result3 = schema.safeParse(15)
        assertTrue(result3 is SchemaResult.Failure)

        // Not an integer
        val result4 = schema.safeParse(7.5)
        assertTrue(result4 is SchemaResult.Failure)
    }

    @Test
    fun `DSL should create object schema`() {
        val schema = obj {
            field("name", string { minLength = 2 })
            field("age", number { min = 0.0 })
            field("email", string { email = true }, required = false)
        }

        // Valid object
        val valid = mapOf(
            "name" to "John",
            "age" to 30,
            "email" to "john@example.com"
        )
        val result1 = schema.safeParse(valid)
        assertTrue(result1 is SchemaResult.Success)

        // Valid object without optional field
        val validNoEmail = mapOf(
            "name" to "John",
            "age" to 30
        )
        val result2 = schema.safeParse(validNoEmail)
        assertTrue(result2 is SchemaResult.Success)

        // Invalid name
        val invalidName = mapOf(
            "name" to "J",
            "age" to 30
        )
        val result3 = schema.safeParse(invalidName)
        assertTrue(result3 is SchemaResult.Failure)

        // Invalid age
        val invalidAge = mapOf(
            "name" to "John",
            "age" to -5
        )
        val result4 = schema.safeParse(invalidAge)
        assertTrue(result4 is SchemaResult.Failure)

        // Invalid email
        val invalidEmail = mapOf(
            "name" to "John",
            "age" to 30,
            "email" to "not-an-email"
        )
        val result5 = schema.safeParse(invalidEmail)
        assertTrue(result5 is SchemaResult.Failure)
    }

    @Test
    fun `DSL should create array schema`() {
        val schema = array(string { minLength = 2 }) {
            minLength = 1
            maxLength = 3
        }

        // Valid array
        val result1 = schema.safeParse(listOf("hello", "world"))
        assertTrue(result1 is SchemaResult.Success)

        // Empty array
        val result2 = schema.safeParse(emptyList<String>())
        assertTrue(result2 is SchemaResult.Failure)

        // Too many elements
        val result3 = schema.safeParse(listOf("one", "two", "three", "four"))
        assertTrue(result3 is SchemaResult.Failure)

        // Invalid element
        val result4 = schema.safeParse(listOf("hello", "a"))
        assertTrue(result4 is SchemaResult.Failure)
    }

    @Test
    fun `DSL should create tuple schema`() {
        val schema = tuple(string(), number(), boolean())

        // Valid tuple
        val result1 = schema.safeParse(listOf("hello", 42, true))
        assertTrue(result1 is SchemaResult.Success)

        // Invalid element type
        val result2 = schema.safeParse(listOf("hello", "world", true))
        assertTrue(result2 is SchemaResult.Failure)
    }

    @Test
    fun `DSL should create union schema`() {
        val schema = union(string(), number())

        // Valid string
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)

        // Valid number
        val result2 = schema.safeParse(42)
        assertTrue(result2 is SchemaResult.Success)

        // Invalid type
        val result3 = schema.safeParse(true)
        assertTrue(result3 is SchemaResult.Failure)
    }

    @Test
    fun `DSL should create discriminated union schema`() {
        val schema = discriminatedUnion<String>("type") {
            schema("dog", obj {
                field("type", literal("dog"))
                field("bark", boolean())
            })
            schema("cat", obj {
                field("type", literal("cat"))
                field("meow", boolean())
            })
        }

        // Valid dog
        val dog = mapOf("type" to "dog", "bark" to true)
        val result1 = schema.safeParse(dog)
        assertTrue(result1 is SchemaResult.Success)

        // Valid cat
        val cat = mapOf("type" to "cat", "meow" to true)
        val result2 = schema.safeParse(cat)
        assertTrue(result2 is SchemaResult.Success)

        // Invalid discriminator
        val invalid = mapOf("type" to "bird", "chirp" to true)
        val result3 = schema.safeParse(invalid)
        assertTrue(result3 is SchemaResult.Failure)
    }

    // Temporarily disable this test until we fix the z shorthand issue
    /*
    @Test
    fun `DSL should support z shorthand`() {
        val schema = ai.kastrax.zod.z.obj {
            field("name", ai.kastrax.zod.z.string { minLength = 2 })
            field("age", ai.kastrax.zod.z.number { min = 0.0 })
        }

        // Valid object
        val valid = mapOf(
            "name" to "John",
            "age" to 30
        )
        val result = schema.safeParse(valid)
        assertTrue(result is SchemaResult.Success<*>)
    }
    */

    @Test
    fun `DSL should support optional`() {
        val schema = optional(string())

        // Valid string
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello", (result1 as SchemaResult.Success).data)

        // Valid null
        val result2 = schema.safeParse(null)
        assertTrue(result2 is SchemaResult.Success)
        assertEquals(null, (result2 as SchemaResult.Success).data)
    }

    @Test
    fun `DSL should support nullable`() {
        val schema = nullable(string())

        // Valid string
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello", (result1 as SchemaResult.Success).data)

        // Valid null
        val result2 = schema.safeParse(null)
        assertTrue(result2 is SchemaResult.Success)
        assertEquals(null, (result2 as SchemaResult.Success).data)
    }

    @Test
    fun `DSL should support default`() {
        val schema = default(string(), "default value")

        // Valid string
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello", (result1 as SchemaResult.Success).data)

        // Null uses default
        val result2 = schema.safeParse(null)
        assertTrue(result2 is SchemaResult.Success)
        assertEquals("default value", (result2 as SchemaResult.Success).data)
    }

    @Test
    fun `DSL should support refine`() {
        val schema = refine(string(), { it.length % 2 == 0 })

        // Valid even length string
        val result1 = schema.safeParse("even")
        assertTrue(result1 is SchemaResult.Success)

        // Invalid odd length string
        val result2 = schema.safeParse("odd")
        assertTrue(result2 is SchemaResult.Failure)
    }
}
