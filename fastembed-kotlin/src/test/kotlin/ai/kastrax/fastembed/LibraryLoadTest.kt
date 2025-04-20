package ai.kastrax.fastembed

import kotlin.test.Test
import kotlin.test.assertNotNull

class LibraryLoadTest {
    
    @Test
    fun testLibraryLoads() {
        // This test just verifies that the native library loads correctly
        // If the library fails to load, the test will fail with an UnsatisfiedLinkError
        val instance = TextEmbeddingNative
        assertNotNull(instance)
    }
}
