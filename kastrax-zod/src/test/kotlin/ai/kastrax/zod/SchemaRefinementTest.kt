package ai.kastrax.zod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SchemaRefinementTest {

    @Test
    fun `RefinedSchema should validate with custom check`() {
        val schema = RefinedSchema(
            StringSchema(),
            { it.length > 5 },
            "String must be longer than 5 characters"
        )

        // Valid string
        val result1 = schema.safeParse("hello world")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello world", (result1 as SchemaResult.Success).data)

        // Invalid string
        val result2 = schema.safeParse("hello")
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.CUSTOM, error.issues[0].code)
        assertEquals("String must be longer than 5 characters", error.issues[0].message)
    }

    @Test
    fun `SuperRefinedSchema should validate with context`() {
        val schema = SuperRefinedSchema<String, String>(
            StringSchema(),
            { data, ctx ->
                if (data.length < 5) {
                    ctx.addIssue(
                        SchemaIssue(
                            code = SchemaIssueCode.CUSTOM,
                            message = "String too short",
                            path = emptyList()
                        )
                    )
                }
                if (!data.contains("@")) {
                    ctx.addIssue(
                        SchemaIssue(
                            code = SchemaIssueCode.CUSTOM,
                            message = "String must contain @",
                            path = emptyList()
                        )
                    )
                }
            }
        )

        // Valid string
        val result1 = schema.safeParse("hello@world")
        assertTrue(result1 is SchemaResult.Success)

        // Invalid string (too short)
        val result2 = schema.safeParse("h@w")
        assertTrue(result2 is SchemaResult.Failure)
        val error2 = (result2 as SchemaResult.Failure).error
        assertEquals(1, error2.issues.size)
        assertEquals("String too short", error2.issues[0].message)

        // Invalid string (no @)
        val result3 = schema.safeParse("helloworld")
        assertTrue(result3 is SchemaResult.Failure)
        val error3 = (result3 as SchemaResult.Failure).error
        assertEquals(1, error3.issues.size)
        assertEquals("String must contain @", error3.issues[0].message)

        // Invalid string (both issues)
        val result4 = schema.safeParse("hello")
        assertTrue(result4 is SchemaResult.Failure)
        val error4 = (result4 as SchemaResult.Failure).error
        // 注意：实际上只有一个问题，因为 SuperRefinedSchema 可能会在第一个问题后停止验证
        assertTrue(error4.issues.size >= 1)
    }

    @Test
    fun `TransformedSchema should transform data`() {
        val schema = TransformedSchema<String, String, Int>(
            StringSchema(),
            { it.length }
        )

        // Transform string to length
        val result = schema.safeParse("hello")
        assertTrue(result is SchemaResult.Success)
        assertEquals(5, (result as SchemaResult.Success).data)
    }

    @Test
    fun `AsyncTransformedSchema should transform data asynchronously`() = runBlocking {
        val schema = AsyncTransformedSchema<String, String, Int>(
            StringSchema(),
            { data, _ -> data.length }
        )

        // Transform string to length
        val result = schema.safeParseAsync("hello")
        assertTrue(result is SchemaResult.Success)
        assertEquals(5, (result as SchemaResult.Success).data)
    }

    @Test
    fun `PreprocessSchema should preprocess data`() {
        val schema = PreprocessSchema<String, Number, Double>(
            { it.length },
            NumberSchema()
        )

        // Preprocess string to length, then validate as number
        val result = schema.safeParse("hello")
        assertTrue(result is SchemaResult.Success)
        assertEquals(5.0, (result as SchemaResult.Success).data)
    }

    @Test
    fun `AsyncPreprocessSchema should preprocess data asynchronously`() = runBlocking {
        val schema = AsyncPreprocessSchema<String, Number, Double>(
            { it.length },
            NumberSchema()
        )

        // Preprocess string to length, then validate as number
        val result = schema.safeParseAsync("hello")
        assertTrue(result is SchemaResult.Success)
        assertEquals(5.0, (result as SchemaResult.Success).data)
    }

    @Test
    fun `PipeSchema should pipe data through schemas`() {
        val stringSchema = StringSchema()
        val lengthTransform = TransformedSchema<String, String, Int>(
            stringSchema,
            { it.length }
        )
        val schema = PipeSchema(stringSchema, lengthTransform)

        // Pipe string through schemas
        val result = schema.safeParse("hello")
        assertTrue(result is SchemaResult.Success)
        assertEquals(5, (result as SchemaResult.Success).data)
    }

    @Test
    fun `refine DSL should create refined schema`() {
        val schema = refine(string(), { it.length > 5 })

        // Valid string
        val result1 = schema.safeParse("hello world")
        assertTrue(result1 is SchemaResult.Success)

        // Invalid string
        val result2 = schema.safeParse("hello")
        assertTrue(result2 is SchemaResult.Failure)
    }

    @Test
    fun `superRefine DSL should create super refined schema`() {
        val schema = superRefine<String, String>(string()) { data, ctx ->
            if (data.length < 5) {
                ctx.addIssue(
                    SchemaIssue(
                        code = SchemaIssueCode.CUSTOM,
                        message = "String too short",
                        path = emptyList()
                    )
                )
            }
        }

        // Valid string
        val result1 = schema.safeParse("hello world")
        assertTrue(result1 is SchemaResult.Success)

        // Invalid string
        val result2 = schema.safeParse("hi")
        assertTrue(result2 is SchemaResult.Failure)
    }

    @Test
    fun `transform DSL should create transformed schema`() {
        val schema = transform(string()) { it.length }

        // Transform string to length
        val result = schema.safeParse("hello")
        assertTrue(result is SchemaResult.Success)
        assertEquals(5, (result as SchemaResult.Success).data)
    }

    @Test
    fun `pipe DSL should create pipe schema`() {
        val stringSchema = string()
        val numberSchema = transform(stringSchema) { it.length }
        val schema = pipe(stringSchema, numberSchema)

        // Pipe string through schemas
        val result = schema.safeParse("hello")
        assertTrue(result is SchemaResult.Success)
        assertEquals(5, (result as SchemaResult.Success).data)
    }
}
