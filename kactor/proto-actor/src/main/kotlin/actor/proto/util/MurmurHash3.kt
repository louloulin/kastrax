package actor.proto.util

/**
 * MurmurHash3 implementation for Kotlin
 * Based on the implementation from https://github.com/aappleby/smhasher
 */
object MurmurHash3 {
    /**
     * Compute a 32-bit hash value for the given byte array
     * @param data The byte array to hash
     * @param seed The seed value (default: 0)
     * @return The 32-bit hash value
     */
    fun hash32(data: ByteArray, seed: Int = 0): Int {
        val length = data.size
        var h1 = seed
        val c1 = 0xcc9e2d51.toInt()
        val c2 = 0x1b873593.toInt()
        
        // Body
        val nblocks = length / 4
        for (i in 0 until nblocks) {
            val i4 = i * 4
            var k1 = ((data[i4].toInt() and 0xFF)) or
                    ((data[i4 + 1].toInt() and 0xFF) shl 8) or
                    ((data[i4 + 2].toInt() and 0xFF) shl 16) or
                    ((data[i4 + 3].toInt() and 0xFF) shl 24)
            
            k1 *= c1
            k1 = (k1 shl 15) or (k1 ushr 17)
            k1 *= c2
            
            h1 = h1 xor k1
            h1 = (h1 shl 13) or (h1 ushr 19)
            h1 = h1 * 5 + 0xe6546b64.toInt()
        }
        
        // Tail
        var k1 = 0
        val tailStart = nblocks * 4
        when (length and 3) {
            3 -> {
                k1 = k1 xor ((data[tailStart + 2].toInt() and 0xFF) shl 16)
                k1 = k1 xor ((data[tailStart + 1].toInt() and 0xFF) shl 8)
                k1 = k1 xor (data[tailStart].toInt() and 0xFF)
                k1 *= c1
                k1 = (k1 shl 15) or (k1 ushr 17)
                k1 *= c2
                h1 = h1 xor k1
            }
            2 -> {
                k1 = k1 xor ((data[tailStart + 1].toInt() and 0xFF) shl 8)
                k1 = k1 xor (data[tailStart].toInt() and 0xFF)
                k1 *= c1
                k1 = (k1 shl 15) or (k1 ushr 17)
                k1 *= c2
                h1 = h1 xor k1
            }
            1 -> {
                k1 = k1 xor (data[tailStart].toInt() and 0xFF)
                k1 *= c1
                k1 = (k1 shl 15) or (k1 ushr 17)
                k1 *= c2
                h1 = h1 xor k1
            }
        }
        
        // Finalization
        h1 = h1 xor length
        h1 = h1 xor (h1 ushr 16)
        h1 *= 0x85ebca6b.toInt()
        h1 = h1 xor (h1 ushr 13)
        h1 *= 0xc2b2ae35.toInt()
        h1 = h1 xor (h1 ushr 16)
        
        return h1
    }
}
