package actor.proto.util

/**
 * Utility class for generating efficient IDs
 */
object IdGenerator {
    private const val digits = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ~+"
    
    /**
     * Convert a uint64 value to a base64 encoded ID string
     * This produces shorter IDs than simple decimal representation
     * 
     * @param value The uint64 value to convert
     * @return A base64 encoded ID string
     */
    fun uint64ToId(value: ULong): String {
        val buf = CharArray(13)
        var i = 13
        var remaining = value
        
        // base is power of 2: use shifts and masks instead of / and %
        while (remaining >= 64u) {
            i--
            buf[i] = digits[(remaining and 0x3Fu).toInt()]
            remaining = remaining shr 6
        }
        
        // remaining < base
        i--
        buf[i] = digits[remaining.toInt()]
        i--
        buf[i] = '$'
        
        return String(buf, i, 13 - i)
    }
}
