package ai.kastrax.examples

import ai.kastrax.zod.Schema

/**
 * Unsafe cast extension function for Schema
 * This is used to work around type inference issues
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T, reified R> Schema<*, *>.unsafeCast(): Schema<T, R> = this as Schema<T, R>
