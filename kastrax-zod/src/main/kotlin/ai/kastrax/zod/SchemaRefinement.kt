package ai.kastrax.zod

/**
 * 表示验证上下文，用于在超级细化中添加问题。
 *
 * @property addIssue 添加问题的函数
 */
class RefinementContext(
    val addIssue: (issue: SchemaIssue) -> Unit
)

/**
 * 为 Schema 添加细化和转换功能。
 */

/**
 * 超级细化模式，提供更灵活的验证逻辑。
 *
 * @property schema 基础模式
 * @property refinement 细化函数
 */
class SuperRefinedSchema<I, O>(
    private val schema: Schema<I, O>,
    private val refinement: (data: O, ctx: RefinementContext) -> Any?
) : BaseSchema<I, O>() {

    companion object {
        /**
         * 特殊标记，用于提前返回。
         */
        val NEVER = Any()
    }

    override fun _parse(data: I): SchemaResult<O> {
        val result = schema.safeParse(data)

        return when (result) {
            is SchemaResult.Success -> {
                val issues = mutableListOf<SchemaIssue>()
                val ctx = RefinementContext { issue -> issues.add(issue) }

                val refinementResult = refinement(result.data, ctx)

                if (issues.isEmpty()) {
                    result
                } else {
                    SchemaResult.Failure(SchemaError(issues))
                }
            }
            is SchemaResult.Failure -> result
        }
    }

    override suspend fun _parseAsync(data: I): SchemaResult<O> {
        val result = schema.safeParseAsync(data)

        return when (result) {
            is SchemaResult.Success -> {
                val issues = mutableListOf<SchemaIssue>()
                val ctx = RefinementContext { issue -> issues.add(issue) }

                val refinementResult = refinement(result.data, ctx)

                if (issues.isEmpty()) {
                    result
                } else {
                    SchemaResult.Failure(SchemaError(issues))
                }
            }
            is SchemaResult.Failure -> result
        }
    }
}

/**
 * 异步转换模式，用于异步转换数据。
 *
 * @property schema 基础模式
 * @property transform 异步转换函数
 */
class AsyncTransformedSchema<I, O, U>(
    private val schema: Schema<I, O>,
    private val transform: suspend (data: O, ctx: RefinementContext?) -> U
) : BaseSchema<I, U>() {

    override fun _parse(data: I): SchemaResult<U> {
        throw UnsupportedOperationException("异步转换必须使用 parseAsync 方法")
    }

    override suspend fun _parseAsync(data: I): SchemaResult<U> {
        val result = schema.safeParseAsync(data)

        return when (result) {
            is SchemaResult.Success -> {
                try {
                    val issues = mutableListOf<SchemaIssue>()
                    val ctx = RefinementContext { issue -> issues.add(issue) }

                    val transformed = transform(result.data, ctx)

                    if (issues.isEmpty()) {
                        SchemaResult.Success(transformed)
                    } else {
                        SchemaResult.Failure(SchemaError(issues))
                    }
                } catch (e: Exception) {
                    SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.CUSTOM,
                                    message = "异步转换失败: ${e.message}",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }
            }
            is SchemaResult.Failure -> SchemaResult.Failure(result.error)
        }
    }
}

/**
 * 预处理模式，用于在验证前预处理数据。
 *
 * @property preprocess 预处理函数
 * @property schema 基础模式
 */
class PreprocessSchema<I, T, O>(
    private val preprocess: (data: I) -> T,
    private val schema: Schema<T, O>
) : BaseSchema<I, O>() {

    override fun _parse(data: I): SchemaResult<O> {
        val preprocessed = try {
            preprocess(data)
        } catch (e: Exception) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.CUSTOM,
                            message = "预处理失败: ${e.message}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        return schema.safeParse(preprocessed)
    }

    override suspend fun _parseAsync(data: I): SchemaResult<O> {
        val preprocessed = try {
            preprocess(data)
        } catch (e: Exception) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.CUSTOM,
                            message = "预处理失败: ${e.message}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        return schema.safeParseAsync(preprocessed)
    }
}

/**
 * 异步预处理模式，用于在验证前异步预处理数据。
 *
 * @property preprocess 异步预处理函数
 * @property schema 基础模式
 */
class AsyncPreprocessSchema<I, T, O>(
    private val preprocess: suspend (data: I) -> T,
    private val schema: Schema<T, O>
) : BaseSchema<I, O>() {

    override fun _parse(data: I): SchemaResult<O> {
        throw UnsupportedOperationException("异步预处理必须使用 parseAsync 方法")
    }

    override suspend fun _parseAsync(data: I): SchemaResult<O> {
        val preprocessed = try {
            preprocess(data)
        } catch (e: Exception) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.CUSTOM,
                            message = "异步预处理失败: ${e.message}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        return schema.safeParseAsync(preprocessed)
    }
}
