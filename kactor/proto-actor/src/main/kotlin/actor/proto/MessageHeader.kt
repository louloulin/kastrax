package actor.proto

/**
 * MessageHeader provides a way to add metadata to messages.
 * It's used for passing context information between actors.
 */
class MessageHeader {
    private val headers: MutableMap<String, String> = mutableMapOf()

    /**
     * Set a header value
     * @param key The header key
     * @param value The header value
     */
    fun set(key: String, value: String) {
        headers[key] = value
    }

    /**
     * Get a header value
     * @param key The header key
     * @return The header value or null if not found
     */
    fun get(key: String): String? = headers[key]

    /**
     * Get a header value or default
     * @param key The header key
     * @param default The default value
     * @return The header value or default if not found
     */
    fun getOrDefault(key: String, default: String): String = headers.getOrDefault(key, default)

    /**
     * Remove a header
     * @param key The header key
     */
    fun remove(key: String) {
        headers.remove(key)
    }

    /**
     * Clear all headers
     */
    fun clear() {
        headers.clear()
    }

    /**
     * Get all header keys
     * @return The list of header keys
     */
    fun keys(): List<String> = headers.keys.toList()

    /**
     * Get the number of headers
     * @return The number of headers
     */
    fun length(): Int = headers.size

    /**
     * Convert to a map
     * @return The headers as a map
     */
    fun toMap(): Map<String, String> = headers.toMap()

    companion object {
        /**
         * Empty message header
         */
        val EMPTY = MessageHeader()
    }
}
