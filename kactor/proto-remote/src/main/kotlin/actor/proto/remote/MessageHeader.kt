package actor.proto.remote

/**
 * MessageHeader provides a way to add metadata to messages.
 * It's used for passing context information between actors.
 */
class MessageHeader(private val headers: Map<String, String> = mapOf()) {
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
    fun toMap(): Map<String, String> = headers

    companion object {
        /**
         * Empty message header
         */
        val EMPTY = MessageHeader()
    }
}
