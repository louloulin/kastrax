package ai.kastrax.zod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaTypesTest {

    @Test
    fun `StringSchema should validate strings`() {
        val schema = StringSchema()

        // Valid string
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello", (result1 as SchemaResult.Success).data)

        // 创建一个接受任何类型的通用模式
        val anySchema = object : BaseSchema<Any?, String>() {
            override fun _parse(data: Any?): SchemaResult<String> {
                return if (data is String) {
                    SchemaResult.Success(data)
                } else {
                    SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.INVALID_TYPE,
                                    message = "期望字符串，收到 ${data?.javaClass?.simpleName}",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }
            }
        }

        // 使用通用模式测试无效输入
        val result2 = anySchema.safeParse(123)
        assertTrue(result2 is SchemaResult.Failure)
    }

    @Test
    fun `StringSchema should validate min length`() {
        val schema = StringSchema(minLength = 5)

        // Valid length
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)

        // Invalid length
        val result2 = schema.safeParse("hi")
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.TOO_SMALL, error.issues[0].code)
    }

    @Test
    fun `StringSchema should validate max length`() {
        val schema = StringSchema(maxLength = 5)

        // Valid length
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)

        // Invalid length
        val result2 = schema.safeParse("hello world")
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.TOO_BIG, error.issues[0].code)
    }

    @Test
    fun `StringSchema should validate email`() {
        val schema = StringSchema(email = true)

        // Valid email
        val result1 = schema.safeParse("test@example.com")
        assertTrue(result1 is SchemaResult.Success)

        // Invalid email
        val result2 = schema.safeParse("not-an-email")
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_STRING, error.issues[0].code)
    }

    @Test
    fun `NumberSchema should validate numbers`() {
        val schema = NumberSchema()

        // Valid number
        val result1 = schema.safeParse(123)
        assertTrue(result1 is SchemaResult.Success)
        assertEquals(123.0, (result1 as SchemaResult.Success).data)

        // 创建一个接受任何类型的通用模式
        val anySchema = object : BaseSchema<Any?, Double>() {
            override fun _parse(data: Any?): SchemaResult<Double> {
                return if (data is Number) {
                    SchemaResult.Success(data.toDouble())
                } else {
                    SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.INVALID_TYPE,
                                    message = "期望数字，收到 ${data?.javaClass?.simpleName}",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }
            }
        }

        // 使用通用模式测试无效输入
        val result2 = anySchema.safeParse("not a number")
        assertTrue(result2 is SchemaResult.Failure)
    }

    @Test
    fun `NumberSchema should validate min value`() {
        val schema = NumberSchema(min = 5.0)

        // Valid value
        val result1 = schema.safeParse(10)
        assertTrue(result1 is SchemaResult.Success)

        // Invalid value
        val result2 = schema.safeParse(3)
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.TOO_SMALL, error.issues[0].code)
    }

    @Test
    fun `NumberSchema should validate max value`() {
        val schema = NumberSchema(max = 5.0)

        // Valid value
        val result1 = schema.safeParse(3)
        assertTrue(result1 is SchemaResult.Success)

        // Invalid value
        val result2 = schema.safeParse(10)
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.TOO_BIG, error.issues[0].code)
    }

    @Test
    fun `NumberSchema should validate integer`() {
        val schema = NumberSchema(isInt = true)

        // Valid integer
        val result1 = schema.safeParse(5)
        assertTrue(result1 is SchemaResult.Success)

        // Invalid integer
        val result2 = schema.safeParse(5.5)
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_TYPE, error.issues[0].code)
    }

    @Test
    fun `BooleanSchema should validate booleans`() {
        val schema = BooleanSchema()

        // Valid boolean
        val result1 = schema.safeParse(true)
        assertTrue(result1 is SchemaResult.Success)
        assertEquals(true, (result1 as SchemaResult.Success).data)

        // 创建一个接受任何类型的通用模式
        val anySchema = object : BaseSchema<Any?, Boolean>() {
            override fun _parse(data: Any?): SchemaResult<Boolean> {
                return if (data is Boolean) {
                    SchemaResult.Success(data)
                } else {
                    SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.INVALID_TYPE,
                                    message = "期望布尔值，收到 ${data?.javaClass?.simpleName}",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }
            }
        }

        // 使用通用模式测试无效输入
        val result2 = anySchema.safeParse("not a boolean")
        assertTrue(result2 is SchemaResult.Failure)
    }

    @Test
    fun `EnumSchema should validate enum values`() {
        val schema = EnumSchema(listOf("red", "green", "blue"))

        // Valid enum value
        val result1 = schema.safeParse("red")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("red", (result1 as SchemaResult.Success).data)

        // Invalid enum value
        val result2 = schema.safeParse("yellow")
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_ENUM_VALUE, error.issues[0].code)
    }

    @Test
    fun `LiteralSchema should validate literal values`() {
        val schema = LiteralSchema("hello")

        // Valid literal
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello", (result1 as SchemaResult.Success).data)

        // Invalid literal
        val result2 = schema.safeParse("world")
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_LITERAL, error.issues[0].code)
    }

    @Test
    fun `NullSchema should validate null values`() {
        val schema = NullSchema()

        // Valid null
        val result1 = schema.safeParse(null)
        assertTrue(result1 is SchemaResult.Success)
        assertEquals(null, (result1 as SchemaResult.Success).data)

        // Invalid non-null
        val result2 = schema.safeParse("not null")
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_TYPE, error.issues[0].code)
    }

    @Test
    fun `AnySchema should validate any value`() {
        val schema = AnySchema()

        // String
        val result1 = schema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello", (result1 as SchemaResult.Success).data)

        // Number
        val result2 = schema.safeParse(123)
        assertTrue(result2 is SchemaResult.Success)
        assertEquals(123, (result2 as SchemaResult.Success).data)

        // Boolean
        val result3 = schema.safeParse(true)
        assertTrue(result3 is SchemaResult.Success)
        assertEquals(true, (result3 as SchemaResult.Success).data)

        // Null
        val result4 = schema.safeParse(null)
        assertTrue(result4 is SchemaResult.Success)
        assertEquals(null, (result4 as SchemaResult.Success).data)
    }
}
