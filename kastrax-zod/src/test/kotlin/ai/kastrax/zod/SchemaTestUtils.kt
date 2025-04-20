package ai.kastrax.zod

import java.util.regex.Pattern

/**
 * Unsafe cast extension function for Schema
 * This is used to work around type inference issues in tests
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T, reified R> Schema<*, *>.unsafeCast(): Schema<T, R> = this as Schema<T, R>

/**
 * Extension function to create a Pattern from a String
 * This is used to work around type inference issues in tests
 */
fun String.toPattern(): Pattern = Pattern.compile(this)

/**
 * Test helper function to create a schema with a description
 * This is used to work around the private schema property in DescribedSchema
 */
fun <I, O> createSchemaWithDescription(schema: Schema<I, O>, description: String): Schema<I, O> {
    return schema.describe(description)
}

/**
 * Test helper function to create a StringSchema with a description
 */
fun createStringSchema(description: String): Schema<String, String> {
    return StringSchema().describe(description)
}

/**
 * Test helper function to create a NumberSchema with a description
 */
fun createNumberSchema(description: String): Schema<Number, Double> {
    return NumberSchema().describe(description)
}

/**
 * Test helper function to create a BooleanSchema with a description
 */
fun createBooleanSchema(description: String): Schema<Boolean, Boolean> {
    return BooleanSchema().describe(description)
}
