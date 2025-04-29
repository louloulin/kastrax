package actor.proto.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MurmurHash3Test {

    @Test
    fun `should generate consistent hash for same input`() {
        val input = "test string".toByteArray()
        val hash1 = MurmurHash3.hash32(input)
        val hash2 = MurmurHash3.hash32(input)

        assertEquals(hash1, hash2)
    }

    @Test
    fun `should generate different hash for different input`() {
        val input1 = "test string 1".toByteArray()
        val input2 = "test string 2".toByteArray()

        val hash1 = MurmurHash3.hash32(input1)
        val hash2 = MurmurHash3.hash32(input2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `should generate different hash for different seed`() {
        val input = "test string".toByteArray()
        val hash1 = MurmurHash3.hash32(input, 0)
        val hash2 = MurmurHash3.hash32(input, 1)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `should handle empty input`() {
        val input = ByteArray(0)
        val hash = MurmurHash3.hash32(input)

        // The hash of an empty array with seed 0 should be a specific value
        // This is a sanity check to ensure the implementation is correct
        // For MurmurHash3, the hash of an empty array with seed 0 is 0
        assertEquals(0, hash)
    }

    @Test
    fun `should handle inputs of different lengths`() {
        val inputs = listOf(
            ByteArray(1) { 1 },
            ByteArray(2) { 1 },
            ByteArray(3) { 1 },
            ByteArray(4) { 1 },
            ByteArray(5) { 1 }
        )

        val hashes = inputs.map { MurmurHash3.hash32(it) }

        // All hashes should be different
        assertEquals(inputs.size, hashes.toSet().size)
    }

    @Test
    fun `should distribute hashes evenly`() {
        val count = 10000
        val buckets = 10
        val bucketCounts = IntArray(buckets)

        for (i in 0 until count) {
            val input = "test string $i".toByteArray()
            val hash = MurmurHash3.hash32(input)
            val bucket = Math.abs(hash % buckets)
            bucketCounts[bucket]++
        }

        // Check that each bucket has roughly the same number of entries
        val expectedPerBucket = count / buckets
        val tolerance = expectedPerBucket * 0.2 // Allow 20% deviation

        for (i in 0 until buckets) {
            assertTrue(
                bucketCounts[i] > expectedPerBucket - tolerance &&
                bucketCounts[i] < expectedPerBucket + tolerance,
                "Bucket $i has ${bucketCounts[i]} entries, expected around $expectedPerBucket"
            )
        }
    }
}
