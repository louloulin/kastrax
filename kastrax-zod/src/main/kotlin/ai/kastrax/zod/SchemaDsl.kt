package ai.kastrax.zod

import java.util.regex.Pattern

/**
 * 用于创建字符串模式的DSL。
 */
fun string(init: StringSchema.() -> Unit = {}): StringSchema {
    val schema = StringSchema()
    schema.init()
    return schema
}

/**
 * 用于创建数字模式的DSL。
 */
fun number(init: NumberSchema.() -> Unit = {}): NumberSchema {
    val schema = NumberSchema()
    schema.init()
    return schema
}

/**
 * 用于创建布尔模式的DSL。
 */
fun boolean(): BooleanSchema {
    return BooleanSchema()
}

/**
 * 用于创建枚举模式的DSL。
 */
fun <T : Any> enum(vararg values: T): EnumSchema<T> {
    return EnumSchema(values.toList())
}

/**
 * 用于创建字面值模式的DSL。
 */
fun <T : Any> literal(value: T): LiteralSchema<T> {
    return LiteralSchema(value)
}

/**
 * 用于创建null模式的DSL。
 */
fun nullSchema(): NullSchema {
    return NullSchema()
}

/**
 * 用于创建undefined模式的DSL。
 */
fun undefined(): UndefinedSchema {
    return UndefinedSchema()
}

/**
 * 用于创建any模式的DSL。
 */
fun any(): AnySchema {
    return AnySchema()
}

/**
 * 用于创建对象模式的DSL。
 */
fun obj(init: ObjectSchemaBuilder.() -> Unit): ObjectSchema<Map<String, Any?>, Map<String, Any?>> {
    val builder = ObjectSchemaBuilder()
    builder.init()
    return builder.build()
}

/**
 * 对象模式构建器，用于DSL。
 */
class ObjectSchemaBuilder {
    private val fields = mutableMapOf<String, ObjectField<String, Any?>>()
    private var strict = false
    private var catchall: Schema<*, *>? = null
    private var unknownKeys = ObjectSchema.UnknownKeysStrategy.STRIP

    /**
     * 添加字段。
     */
    fun <T : Any> field(key: String, schema: Schema<*, T>, required: Boolean = true) {
        @Suppress("UNCHECKED_CAST")
        fields[key] = ObjectField(key, schema as Schema<Any?, Any?>, required)
    }

    /**
     * 设置为严格模式。
     */
    fun strict() {
        strict = true
        unknownKeys = ObjectSchema.UnknownKeysStrategy.STRICT
    }

    /**
     * 设置为通过模式。
     */
    fun passthrough() {
        strict = false
        unknownKeys = ObjectSchema.UnknownKeysStrategy.PASSTHROUGH
    }

    /**
     * 设置为剥离模式。
     */
    fun strip() {
        strict = false
        unknownKeys = ObjectSchema.UnknownKeysStrategy.STRIP
    }

    /**
     * 设置catchall模式。
     */
    fun catchall(schema: Schema<*, *>) {
        catchall = schema
    }

    /**
     * 构建对象模式。
     */
    fun build(): ObjectSchema<Map<String, Any?>, Map<String, Any?>> {
        return ObjectSchema(fields, strict, catchall as Schema<Any?, Any?>?, unknownKeys)
    }
}

/**
 * 用于创建数组模式的DSL。
 */
fun <I, O> array(schema: Schema<I, O>, init: ArraySchema<I, O>.() -> Unit = {}): ArraySchema<I, O> {
    val arraySchema = ArraySchema(schema)
    arraySchema.init()
    return arraySchema
}

/**
 * 用于创建元组模式的DSL。
 */
fun tuple(vararg schemas: Schema<*, *>): TupleSchema {
    @Suppress("UNCHECKED_CAST")
    return TupleSchema(schemas.toList() as List<Schema<Any?, Any?>>)
}

/**
 * 用于创建联合模式的DSL。
 */
fun union(vararg schemas: Schema<*, *>): UnionSchema {
    @Suppress("UNCHECKED_CAST")
    return UnionSchema(schemas.toList() as List<Schema<Any?, Any?>>)
}

/**
 * 用于创建判别联合模式的DSL。
 */
fun <K : Any> discriminatedUnion(
    discriminator: String,
    init: DiscriminatedUnionBuilder<K>.() -> Unit
): DiscriminatedUnionSchema<K> {
    val builder = DiscriminatedUnionBuilder<K>(discriminator)
    builder.init()
    return builder.build()
}

/**
 * 判别联合模式构建器，用于DSL。
 */
class DiscriminatedUnionBuilder<K : Any>(private val discriminator: String) {
    private val schemas = mutableMapOf<K, Schema<Any?, Any?>>()

    /**
     * 添加模式。
     */
    fun schema(key: K, schema: Schema<*, *>) {
        @Suppress("UNCHECKED_CAST")
        schemas[key] = schema as Schema<Any?, Any?>
    }

    /**
     * 构建判别联合模式。
     */
    fun build(): DiscriminatedUnionSchema<K> {
        return DiscriminatedUnionSchema(discriminator, schemas)
    }
}

/**
 * 用于创建交叉模式的DSL。
 */
fun <I1, O1, I2, O2> intersection(
    left: Schema<I1, O1>,
    right: Schema<I2, O2>
): Schema<Any?, Any?> {
    return left.and(right)
}

/**
 * 用于创建可选模式的DSL。
 */
fun <I, O> optional(schema: Schema<I, O>): Schema<I?, O?> {
    return schema.optional()
}

/**
 * 用于创建可空模式的DSL。
 */
fun <I, O> nullable(schema: Schema<I, O>): Schema<I?, O?> {
    return schema.nullable()
}

/**
 * 用于创建可空可选模式的DSL。
 */
fun <I, O> nullish(schema: Schema<I, O>): Schema<I?, O?> {
    return schema.nullish()
}

/**
 * 用于创建带有默认值的模式的DSL。
 */
fun <I, O> default(schema: Schema<I, O>, defaultValue: O): Schema<I?, O> {
    return schema.default(defaultValue)
}

/**
 * 用于创建带有描述的模式的DSL。
 */
fun <I, O> describe(schema: Schema<I, O>, description: String): Schema<I, O> {
    return schema.describe(description)
}

/**
 * 用于创建带有细化的模式的DSL。
 */
fun <I, O> refine(
    schema: Schema<I, O>,
    check: (data: O) -> Boolean,
    message: String? = null
): Schema<I, O> {
    return schema.refine(check, message)
}

/**
 * 用于创建带有超级细化的模式的DSL。
 */
fun <I, O> superRefine(
    schema: Schema<I, O>,
    refinement: (data: O, ctx: RefinementContext) -> Any?
): Schema<I, O> {
    return SuperRefinedSchema(schema, refinement)
}

/**
 * 用于创建带有转换的模式的DSL。
 */
fun <I, O, U> transform(
    schema: Schema<I, O>,
    transform: (data: O) -> U
): Schema<I, U> {
    return schema.transform(transform)
}

/**
 * 用于创建带有异步转换的模式的DSL。
 */
fun <I, O, U> transformAsync(
    schema: Schema<I, O>,
    transform: suspend (data: O) -> U
): Schema<I, U> {
    return AsyncTransformedSchema(schema) { data, _ -> transform(data) }
}

/**
 * 用于创建带有预处理的模式的DSL。
 */
fun <I, T, O> preprocess(
    preprocess: (data: I) -> T,
    schema: Schema<T, O>
): Schema<I, O> {
    return PreprocessSchema(preprocess, schema)
}

/**
 * 用于创建带有异步预处理的模式的DSL。
 */
fun <I, T, O> preprocessAsync(
    preprocess: suspend (data: I) -> T,
    schema: Schema<T, O>
): Schema<I, O> {
    return AsyncPreprocessSchema(preprocess, schema)
}

/**
 * 用于创建带有管道的模式的DSL。
 */
fun <I, O, U> pipe(
    input: Schema<I, O>,
    output: Schema<O, U>
): Schema<I, U> {
    return input.pipe(output)
}

/**
 * 用于创建字符串模式的简写。
 */
val z = object {
    fun string(init: StringSchema.() -> Unit = {}) = ai.kastrax.zod.string(init)
    fun number(init: NumberSchema.() -> Unit = {}) = ai.kastrax.zod.number(init)
    fun boolean() = ai.kastrax.zod.boolean()
    fun <T : Any> enum(vararg values: T) = ai.kastrax.zod.enum(*values)
    fun <T : Any> literal(value: T) = ai.kastrax.zod.literal(value)
    fun nullSchema() = ai.kastrax.zod.nullSchema()
    fun undefined() = ai.kastrax.zod.undefined()
    fun any() = ai.kastrax.zod.any()
    fun obj(init: ObjectSchemaBuilder.() -> Unit) = ai.kastrax.zod.obj(init)
    fun <I, O> array(schema: Schema<I, O>, init: ArraySchema<I, O>.() -> Unit = {}) = ai.kastrax.zod.array(schema, init)
    fun tuple(vararg schemas: Schema<*, *>) = ai.kastrax.zod.tuple(*schemas)
    fun union(vararg schemas: Schema<*, *>) = ai.kastrax.zod.union(*schemas)
    fun <K : Any> discriminatedUnion(discriminator: String, init: DiscriminatedUnionBuilder<K>.() -> Unit) = ai.kastrax.zod.discriminatedUnion(discriminator, init)
    fun <I1, O1, I2, O2> intersection(left: Schema<I1, O1>, right: Schema<I2, O2>) = ai.kastrax.zod.intersection(left, right)
    fun <I, O> optional(schema: Schema<I, O>) = ai.kastrax.zod.optional(schema)
    fun <I, O> nullable(schema: Schema<I, O>) = ai.kastrax.zod.nullable(schema)
    fun <I, O> nullish(schema: Schema<I, O>) = ai.kastrax.zod.nullish(schema)
    fun <I, O> default(schema: Schema<I, O>, defaultValue: O) = ai.kastrax.zod.default(schema, defaultValue)
    fun <I, O> describe(schema: Schema<I, O>, description: String) = ai.kastrax.zod.describe(schema, description)
    fun <I, O> refine(schema: Schema<I, O>, check: (data: O) -> Boolean, message: String? = null) = ai.kastrax.zod.refine(schema, check, message)
    fun <I, O> superRefine(schema: Schema<I, O>, refinement: (data: O, ctx: RefinementContext) -> Any?) = ai.kastrax.zod.superRefine(schema, refinement)
    fun <I, O, U> transform(schema: Schema<I, O>, transform: (data: O) -> U) = ai.kastrax.zod.transform(schema, transform)
    fun <I, O, U> transformAsync(schema: Schema<I, O>, transform: suspend (data: O) -> U) = ai.kastrax.zod.transformAsync(schema, transform)
    fun <I, T, O> preprocess(preprocess: (data: I) -> T, schema: Schema<T, O>) = ai.kastrax.zod.preprocess(preprocess, schema)
    fun <I, T, O> preprocessAsync(preprocess: suspend (data: I) -> T, schema: Schema<T, O>) = ai.kastrax.zod.preprocessAsync(preprocess, schema)
    fun <I, O, U> pipe(input: Schema<I, O>, output: Schema<O, U>) = ai.kastrax.zod.pipe(input, output)
}
