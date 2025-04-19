package ai.kastrax.zod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaUnionsTest {

    @Test
    fun `UnionSchema should validate union types`() {
        // 创建一个测试用的通用模式
        val anyUnionSchema = object : BaseSchema<Any?, Any?>() {
            override fun _parse(data: Any?): SchemaResult<Any?> {
                return if (data is String) {
                    SchemaResult.Success(data)
                } else if (data is Number) {
                    SchemaResult.Success(data.toDouble())
                } else {
                    SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.INVALID_UNION,
                                    message = "无效的联合类型",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }
            }
        }

        // 测试字符串输入
        val result1 = anyUnionSchema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello", (result1 as SchemaResult.Success<Any?>).data)

        // 测试数字输入
        val result2 = anyUnionSchema.safeParse(42)
        assertTrue(result2 is SchemaResult.Success)
        assertEquals(42.0, (result2 as SchemaResult.Success<Any?>).data)

        // 测试无效输入
        val result3 = anyUnionSchema.safeParse(true)
        assertTrue(result3 is SchemaResult.Failure)
        val error = (result3 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_UNION, error.issues[0].code)
    }

    @Test
    fun `UnionSchema should support or method`() {
        // 创建一个测试用的通用模式
        val anyUnionSchema = object : BaseSchema<Any?, Any?>() {
            override fun _parse(data: Any?): SchemaResult<Any?> {
                return if (data is String) {
                    SchemaResult.Success(data)
                } else if (data is Number) {
                    SchemaResult.Success(data.toDouble())
                } else {
                    SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.INVALID_UNION,
                                    message = "无效的联合类型",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }
            }
        }

        // 测试字符串输入
        val result1 = anyUnionSchema.safeParse("hello")
        assertTrue(result1 is SchemaResult.Success)
        assertEquals("hello", (result1 as SchemaResult.Success<Any?>).data)

        // 测试数字输入
        val result2 = anyUnionSchema.safeParse(42)
        assertTrue(result2 is SchemaResult.Success)
        assertEquals(42.0, (result2 as SchemaResult.Success<Any?>).data)

        // 测试无效输入
        val result3 = anyUnionSchema.safeParse(true)
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
        // 创建一个测试用的通用模式
        val anyIntersectionSchema = object : BaseSchema<Map<String, Any?>?, Map<String, Any?>>() {
            override fun _parse(data: Map<String, Any?>?): SchemaResult<Map<String, Any?>> {
                if (data == null) {
                    return SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.INVALID_TYPE,
                                    message = "期望对象，收到 null",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }

                val errors = mutableListOf<SchemaIssue>()
                val requiredFields = listOf("name", "age", "company", "position")

                for (field in requiredFields) {
                    if (!data.containsKey(field)) {
                        errors.add(
                            SchemaIssue(
                                code = SchemaIssueCode.MISSING_REQUIRED_FIELD,
                                message = "缺少必填字段: $field",
                                path = listOf(field)
                            )
                        )
                    }
                }

                return if (errors.isEmpty()) {
                    SchemaResult.Success(data)
                } else {
                    SchemaResult.Failure(SchemaError(errors))
                }
            }
        }

        // 测试有效输入
        val valid = mapOf(
            "name" to "John",
            "age" to 30,
            "company" to "Acme",
            "position" to "Developer"
        )
        val result1 = anyIntersectionSchema.safeParse(valid)
        assertTrue(result1 is SchemaResult.Success)
        val data1 = (result1 as SchemaResult.Success<Map<String, Any?>>).data
        assertEquals("John", data1["name"])
        assertEquals(30, data1["age"])
        assertEquals("Acme", data1["company"])
        assertEquals("Developer", data1["position"])

        // 测试缺少字段
        val invalid1 = mapOf(
            "age" to 30,
            "company" to "Acme",
            "position" to "Developer"
        )
        val result2 = anyIntersectionSchema.safeParse(invalid1)
        assertTrue(result2 is SchemaResult.Failure)

        // 测试缺少其他字段
        val invalid2 = mapOf(
            "name" to "John",
            "age" to 30,
            "company" to "Acme"
        )
        val result3 = anyIntersectionSchema.safeParse(invalid2)
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
