package ai.kastrax.runtime.coroutines.test

import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

@ExperimentalCoroutinesApi
class TestCoroutineRuntimeTest {

    @Test
    fun `test virtual time advances`() {
        val runtime = TestCoroutineRuntime()
        val scope = runtime.getScope(this)

        var executed = false

        scope.launch {
            delay(1000) // 1 second delay
            executed = true
        }

        // Time hasn't advanced yet
        runtime.runCurrent()
        assertTrue(!executed)

        // Advance time by 1 second
        runtime.advanceTimeBy(1000)
        runtime.runCurrent()

        assertTrue(executed)
    }

    @Test
    fun `test async with virtual time`() {
        val runtime = TestCoroutineRuntime()
        val scope = runtime.getScope(this)

        val deferred = scope.async {
            delay(1000) // 1 second delay
            "Result"
        }

        // Advance time by 1 second
        runtime.advanceTimeBy(1000)

        val result = runtime.runBlocking {
            deferred.await()
        }

        assertEquals("Result", result)
    }

    @Test
    fun `test flow with virtual time`() {
        val runtime = TestCoroutineRuntime()
        val flow = runtime.flow<Int> {
            emit(1)
            delay(1000) // 1 second delay
            emit(2)
            delay(1000) // 1 second delay
            emit(3)
        }

        val results = mutableListOf<Int>()

        val job = runtime.getScope(this).launch {
            flow.collect { value ->
                results.add(value)
            }
        }

        // First value should be emitted immediately
        runtime.runCurrent()
        assertEquals(listOf(1), results)

        // Second value after 1 second
        runtime.advanceTimeBy(1000)
        runtime.runCurrent()
        assertEquals(listOf(1, 2), results)

        // Third value after another second
        runtime.advanceTimeBy(1000)
        runtime.runCurrent()
        assertEquals(listOf(1, 2, 3), results)

        job.cancel()
    }
}
