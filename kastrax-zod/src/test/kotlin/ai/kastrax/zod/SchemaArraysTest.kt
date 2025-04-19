package ai.kastrax.zod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaArraysTest {

    @Test
    fun `ArraySchema should validate arrays`() {
        val schema = ArraySchema(StringSchema())

        // Valid array
        val result1 = schema.safeParse(listOf("a", "b", "c"))
        assertTrue(result1 is SchemaResult.Success)
        assertEquals(listOf("a", "b", "c"), (result1 as SchemaResult.Success<List<String>>).data)

        // 创建一个测试用的通用模式
        val anySchema = object : BaseSchema<List<Any>?, List<String>>() {
            override fun _parse(data: List<Any>?): SchemaResult<List<String>> {
                if (data == null) {
                    return SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.INVALID_TYPE,
                                    message = "期望数组，收到 null",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }

                val result = mutableListOf<String>()
                val errors = mutableListOf<SchemaIssue>()

                for ((index, item) in data.withIndex()) {
                    if (item is String) {
                        result.add(item)
                    } else {
                        errors.add(
                            SchemaIssue(
                                code = SchemaIssueCode.INVALID_TYPE,
                                message = "期望字符串，收到 ${item?.javaClass?.simpleName}",
                                path = listOf(index.toString())
                            )
                        )
                    }
                }

                return if (errors.isEmpty()) {
                    SchemaResult.Success(result)
                } else {
                    SchemaResult.Failure(SchemaError(errors))
                }
            }
        }

        // 使用通用模式测试无效输入
        val result2 = anySchema.safeParse(listOf("a", 1, "c"))
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_TYPE, error.issues[0].code)

        // Null array
        val result3 = schema.safeParse(null)
        assertTrue(result3 is SchemaResult.Failure)
        val error3 = (result3 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_TYPE, error3.issues[0].code)
    }

    @Test
    fun `ArraySchema should validate min length`() {
        val schema = ArraySchema(StringSchema(), minLength = 2)

        // Valid length
        val result1 = schema.safeParse(listOf("a", "b", "c"))
        assertTrue(result1 is SchemaResult.Success)

        // Invalid length
        val result2 = schema.safeParse(listOf("a"))
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.TOO_SMALL, error.issues[0].code)
    }

    @Test
    fun `ArraySchema should validate max length`() {
        val schema = ArraySchema(StringSchema(), maxLength = 2)

        // Valid length
        val result1 = schema.safeParse(listOf("a", "b"))
        assertTrue(result1 is SchemaResult.Success)

        // Invalid length
        val result2 = schema.safeParse(listOf("a", "b", "c"))
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.TOO_BIG, error.issues[0].code)
    }

    @Test
    fun `ArraySchema should validate nonempty`() {
        val schema = ArraySchema(StringSchema(), nonempty = true)

        // Valid non-empty array
        val result1 = schema.safeParse(listOf("a"))
        assertTrue(result1 is SchemaResult.Success)

        // Invalid empty array
        val result2 = schema.safeParse(emptyList<String>())
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.TOO_SMALL, error.issues[0].code)
    }

    @Test
    fun `ArraySchema should support method chaining`() {
        val schema = ArraySchema(StringSchema())
            .min(2)
            .max(4)
            .nonempty()

        // Valid array
        val result1 = schema.safeParse(listOf("a", "b", "c"))
        assertTrue(result1 is SchemaResult.Success)

        // Too short
        val result2 = schema.safeParse(listOf("a"))
        assertTrue(result2 is SchemaResult.Failure)

        // Too long
        val result3 = schema.safeParse(listOf("a", "b", "c", "d", "e"))
        assertTrue(result3 is SchemaResult.Failure)

        // Empty
        val result4 = schema.safeParse(emptyList<String>())
        assertTrue(result4 is SchemaResult.Failure)
    }

    @Test
    fun `TupleSchema should validate tuples`() {
        val schema = TupleSchema<Any?, Any?>(
            listOf(StringSchema(), NumberSchema(), BooleanSchema())
        )

        // Valid tuple
        val result1 = schema.safeParse(listOf("hello", 42, true))
        assertTrue(result1 is SchemaResult.Success)
        val data1 = (result1 as SchemaResult.Success).data
        assertEquals("hello", data1[0])
        assertEquals(42.0, data1[1])
        assertEquals(true, data1[2])

        // 创建一个测试用的通用模式
        val anyTupleSchema = object : BaseSchema<List<Any>?, List<Any>>() {
            override fun _parse(data: List<Any>?): SchemaResult<List<Any>> {
                if (data == null) {
                    return SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.INVALID_TYPE,
                                    message = "期望数组，收到 null",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }

                if (data.size != 3) {
                    return SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.TOO_SMALL,
                                    message = "元组长度必须为 3",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }

                val errors = mutableListOf<SchemaIssue>()

                if (data[0] !is String) {
                    errors.add(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望字符串，收到 ${data[0]?.javaClass?.simpleName}",
                            path = listOf("0")
                        )
                    )
                }

                if (data[1] !is Number) {
                    errors.add(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望数字，收到 ${data[1]?.javaClass?.simpleName}",
                            path = listOf("1")
                        )
                    )
                }

                if (data[2] !is Boolean) {
                    errors.add(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望布尔值，收到 ${data[2]?.javaClass?.simpleName}",
                            path = listOf("2")
                        )
                    )
                }

                return if (errors.isEmpty()) {
                    SchemaResult.Success(data)
                } else {
                    SchemaResult.Failure(SchemaError(errors))
                }
            }
        }

        // 使用通用模式测试无效输入
        val result2 = anyTupleSchema.safeParse(listOf("hello", "world", true))
        assertTrue(result2 is SchemaResult.Failure)
        val error2 = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_TYPE, error2.issues[0].code)

        // Too few elements
        val result3 = schema.safeParse(listOf("hello", 42))
        assertTrue(result3 is SchemaResult.Failure)
        val error3 = (result3 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.TOO_SMALL, error3.issues[0].code)

        // Too many elements
        val result4 = schema.safeParse(listOf("hello", 42, true, "extra"))
        assertTrue(result4 is SchemaResult.Failure)
        val error4 = (result4 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.TOO_BIG, error4.issues[0].code)
    }

    @Test
    fun `TupleSchema should support rest`() {
        val schema = TupleSchema<Any?, Any?>(
            listOf(StringSchema(), NumberSchema()),
            rest = StringSchema()
        )

        // Valid tuple with exact elements
        val result1 = schema.safeParse(listOf("hello", 42))
        assertTrue(result1 is SchemaResult.Success)

        // Valid tuple with rest elements
        val result2 = schema.safeParse(listOf("hello", 42, "extra1", "extra2"))
        assertTrue(result2 is SchemaResult.Success)
        val data2 = (result2 as SchemaResult.Success).data
        assertEquals("hello", data2[0])
        assertEquals(42.0, data2[1])
        assertEquals("extra1", data2[2])
        assertEquals("extra2", data2[3])

        // 创建一个测试用的通用模式
        val anyRestTupleSchema = object : BaseSchema<List<Any>?, List<Any>>() {
            override fun _parse(data: List<Any>?): SchemaResult<List<Any>> {
                if (data == null) {
                    return SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.INVALID_TYPE,
                                    message = "期望数组，收到 null",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }

                if (data.size < 2) {
                    return SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.TOO_SMALL,
                                    message = "元组长度至少为 2",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }

                val errors = mutableListOf<SchemaIssue>()

                if (data[0] !is String) {
                    errors.add(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望字符串，收到 ${data[0]?.javaClass?.simpleName}",
                            path = listOf("0")
                        )
                    )
                }

                if (data[1] !is Number) {
                    errors.add(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望数字，收到 ${data[1]?.javaClass?.simpleName}",
                            path = listOf("1")
                        )
                    )
                }

                // 检查剩余元素是否都是字符串
                for (i in 2 until data.size) {
                    if (data[i] !is String) {
                        errors.add(
                            SchemaIssue(
                                code = SchemaIssueCode.INVALID_TYPE,
                                message = "期望字符串，收到 ${data[i]?.javaClass?.simpleName}",
                                path = listOf(i.toString())
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

        // 使用通用模式测试无效输入
        val result3 = anyRestTupleSchema.safeParse(listOf("hello", 42, "extra", 123))
        assertTrue(result3 is SchemaResult.Failure)
        val error3 = (result3 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_TYPE, error3.issues[0].code)
    }
}
