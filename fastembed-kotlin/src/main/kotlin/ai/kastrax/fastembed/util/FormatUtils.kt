package ai.kastrax.fastembed.util

/**
 * Format a float to a specified number of decimal places.
 */
fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
