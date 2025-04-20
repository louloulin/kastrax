package ai.kastrax.zod

/**
 * 创建字符串输入模式。
 */
fun stringInput(description: String? = null): Schema<String?, String> {
    val schema = StringSchema()
    val nullableSchema = schema.nullable()
    return if (description != null) {
        nullableSchema.describe(description) as Schema<String?, String>
    } else {
        nullableSchema as Schema<String?, String>
    }
}

/**
 * 创建数字输入模式。
 */
fun numberInput(description: String? = null): Schema<Number?, Double> {
    val schema = NumberSchema()
    val nullableSchema = schema.nullable()
    return if (description != null) {
        nullableSchema.describe(description) as Schema<Number?, Double>
    } else {
        nullableSchema as Schema<Number?, Double>
    }
}

/**
 * 创建布尔输入模式。
 */
fun booleanInput(description: String? = null): Schema<Boolean?, Boolean> {
    val schema = BooleanSchema()
    val nullableSchema = schema.nullable()
    return if (description != null) {
        nullableSchema.describe(description) as Schema<Boolean?, Boolean>
    } else {
        nullableSchema as Schema<Boolean?, Boolean>
    }
}

/**
 * 创建对象输入模式。
 */
fun objectInput(description: String? = null, init: ObjectSchemaBuilder.() -> Unit): Schema<Map<String, Any?>?, Map<String, Any?>> {
    val builder = ObjectSchemaBuilder()
    builder.init()
    val schema = builder.build()
    val nullableSchema = schema.nullable()
    return if (description != null) {
        nullableSchema.describe(description) as Schema<Map<String, Any?>?, Map<String, Any?>>
    } else {
        nullableSchema as Schema<Map<String, Any?>?, Map<String, Any?>>
    }
}

/**
 * 创建数组输入模式。
 */
fun <I, O> arrayInput(elementSchema: Schema<I, O>, description: String? = null): Schema<List<I>?, List<O>> {
    val schema = ArraySchema(elementSchema)
    val nullableSchema = schema.nullable()
    return if (description != null) {
        nullableSchema.describe(description) as Schema<List<I>?, List<O>>
    } else {
        nullableSchema as Schema<List<I>?, List<O>>
    }
}

/**
 * 创建元组输入模式。
 */
fun tupleInput(vararg schemas: Schema<*, *>, description: String? = null): Schema<List<*>?, List<*>> {
    @Suppress("UNCHECKED_CAST")
    val schema = TupleSchema(schemas.toList() as List<Schema<Any?, Any?>>)
    val nullableSchema = schema.nullable()
    return if (description != null) {
        nullableSchema.describe(description) as Schema<List<*>?, List<*>>
    } else {
        nullableSchema as Schema<List<*>?, List<*>>
    }
}

/**
 * 创建联合输入模式。
 */
fun unionInput(vararg schemas: Schema<*, *>, description: String? = null): Schema<Any?, Any?> {
    @Suppress("UNCHECKED_CAST")
    val schema = UnionSchema(schemas.toList() as List<Schema<Any?, Any?>>)
    val nullableSchema = schema.nullable()
    return if (description != null) {
        nullableSchema.describe(description) as Schema<Any?, Any?>
    } else {
        nullableSchema as Schema<Any?, Any?>
    }
}

/**
 * 创建枚举输入模式。
 */
fun <T : Enum<T>> enumInput(enumClass: Class<T>, description: String? = null): Schema<String?, T> {
    val values = enumClass.enumConstants.toList()
    val schema = EnumSchema(values)
    val nullableSchema = schema.nullable()
    return if (description != null) {
        nullableSchema.describe(description) as Schema<String?, T>
    } else {
        nullableSchema as Schema<String?, T>
    }
}

/**
 * 创建字面量输入模式。
 */
fun <T : Any> literalInput(value: T, description: String? = null): Schema<Any?, T> {
    val schema = LiteralSchema(value)
    val nullableSchema = schema.nullable()
    return if (description != null) {
        nullableSchema.describe(description) as Schema<Any?, T>
    } else {
        nullableSchema as Schema<Any?, T>
    }
}



/**
 * 对象模式构建器的扩展函数，用于添加字符串字段。
 */
fun ObjectSchemaBuilder.stringField(
    key: String,
    description: String? = null,
    required: Boolean = true,
    init: (StringSchema.() -> Unit)? = null
) {
    val schema = StringSchema()
    if (init != null) {
        schema.init()
    }
    val finalSchema = if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
    field(key, finalSchema, required)
}

/**
 * 对象模式构建器的扩展函数，用于添加数字字段。
 */
fun ObjectSchemaBuilder.numberField(
    key: String,
    description: String? = null,
    required: Boolean = true,
    init: (NumberSchema.() -> Unit)? = null
) {
    val schema = NumberSchema()
    if (init != null) {
        schema.init()
    }
    val finalSchema = if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
    field(key, finalSchema, required)
}

/**
 * 对象模式构建器的扩展函数，用于添加布尔字段。
 */
fun ObjectSchemaBuilder.booleanField(
    key: String,
    description: String? = null,
    required: Boolean = true,
    init: (BooleanSchema.() -> Unit)? = null
) {
    val schema = BooleanSchema()
    if (init != null) {
        schema.init()
    }
    val finalSchema = if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
    field(key, finalSchema, required)
}

/**
 * 对象模式构建器的扩展函数，用于添加对象字段。
 */
fun ObjectSchemaBuilder.objectField(
    key: String,
    description: String? = null,
    required: Boolean = true,
    init: ObjectSchemaBuilder.() -> Unit
) {
    val builder = ObjectSchemaBuilder()
    builder.init()
    val schema = builder.build()
    val finalSchema = if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
    field(key, finalSchema, required)
}

/**
 * 对象模式构建器的扩展函数，用于添加数组字段。
 */
fun <I, O> ObjectSchemaBuilder.arrayField(
    key: String,
    elementSchema: Schema<I, O>,
    description: String? = null,
    required: Boolean = true,
    init: (ArraySchema<I, O>.() -> Unit)? = null
) {
    val schema = ArraySchema(elementSchema)
    if (init != null) {
        schema.init()
    }
    val finalSchema = if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
    field(key, finalSchema, required)
}

/**
 * 对象模式构建器的扩展函数，用于添加枚举字段。
 */
fun <T : Enum<T>> ObjectSchemaBuilder.enumField(
    key: String,
    enumClass: Class<T>,
    description: String? = null,
    required: Boolean = true
) {
    val values = enumClass.enumConstants.toList()
    val schema = EnumSchema(values)
    val finalSchema = if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
    field(key, finalSchema, required)
}

/**
 * 对象模式构建器的扩展函数，用于添加字面量字段。
 */
fun <T : Any> ObjectSchemaBuilder.literalField(
    key: String,
    value: T,
    description: String? = null,
    required: Boolean = true
) {
    val schema = LiteralSchema(value)
    val finalSchema = if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
    field(key, finalSchema, required)
}

/**
 * 创建字符串输出模式。
 */
fun stringOutput(description: String? = null): Schema<String, String> {
    val schema = StringSchema()
    return if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
}

/**
 * 创建数字输出模式。
 */
fun numberOutput(description: String? = null): Schema<Double, Double> {
    val schema = NumberSchema()
    return if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
}

/**
 * 创建布尔输出模式。
 */
fun booleanOutput(description: String? = null): Schema<Boolean, Boolean> {
    val schema = BooleanSchema()
    return if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
}

/**
 * 创建对象输出模式。
 */
fun objectOutput(description: String? = null, init: ObjectSchemaBuilder.() -> Unit): Schema<Map<String, Any?>, Map<String, Any?>> {
    val builder = ObjectSchemaBuilder()
    builder.init()
    val schema = builder.build()
    return if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
}

/**
 * 创建数组输出模式。
 */
fun <I, O> arrayOutput(elementSchema: Schema<I, O>, description: String? = null): Schema<List<I>, List<O>> {
    val schema = ArraySchema(elementSchema)
    return if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
}

/**
 * 创建元组输出模式。
 */
fun tupleOutput(vararg schemas: Schema<*, *>, description: String? = null): Schema<List<*>, List<*>> {
    @Suppress("UNCHECKED_CAST")
    val schema = TupleSchema(schemas.toList() as List<Schema<Any?, Any?>>)
    return if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
}

/**
 * 创建联合输出模式。
 */
fun unionOutput(vararg schemas: Schema<*, *>, description: String? = null): Schema<Any?, Any?> {
    @Suppress("UNCHECKED_CAST")
    val schema = UnionSchema(schemas.toList() as List<Schema<Any?, Any?>>)
    return if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
}

/**
 * 创建枚举输出模式。
 */
fun <T : Enum<T>> enumOutput(enumClass: Class<T>, description: String? = null): Schema<T, T> {
    val values = enumClass.enumConstants.toList()
    val schema = EnumSchema(values)
    return if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
}

/**
 * 创建字面量输出模式。
 */
fun <T : Any> literalOutput(value: T, description: String? = null): Schema<T, T> {
    val schema = LiteralSchema(value)
    return if (description != null) {
        schema.describe(description)
    } else {
        schema
    }
}