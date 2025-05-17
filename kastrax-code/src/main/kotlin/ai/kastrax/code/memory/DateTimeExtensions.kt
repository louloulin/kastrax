package ai.kastrax.code.memory

import kotlinx.datetime.Instant
import java.time.Instant as JavaInstant

/**
 * 将kotlinx.datetime.Instant转换为java.time.Instant
 *
 * @return java.time.Instant
 */
fun Instant.toJavaInstant(): JavaInstant {
    return JavaInstant.ofEpochSecond(this.epochSeconds, this.nanosecondsOfSecond.toLong())
}
