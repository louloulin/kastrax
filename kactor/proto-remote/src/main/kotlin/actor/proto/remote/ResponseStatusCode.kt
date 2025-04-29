package actor.proto.remote

/**
 * Response status codes for remote communication
 */
enum class ResponseStatusCode {
    /**
     * Operation completed successfully
     */
    OK,
    
    /**
     * Target is unavailable
     */
    UNAVAILABLE,
    
    /**
     * Operation timed out
     */
    TIMEOUT,
    
    /**
     * Process name already exists
     */
    PROCESS_NAME_ALREADY_EXIST,
    
    /**
     * Generic error
     */
    ERROR,
    
    /**
     * Message was sent to deadletter
     */
    DEAD_LETTER;
    
    /**
     * Convert to int32 value
     */
    fun toInt32(): Int = ordinal
    
    /**
     * Convert to error
     */
    fun asError(): ResponseError? {
        return when (this) {
            OK -> null
            UNAVAILABLE -> ErrUnavailable
            TIMEOUT -> ErrTimeout
            PROCESS_NAME_ALREADY_EXIST -> ErrProcessNameAlreadyExist
            ERROR -> ErrUnknownError
            DEAD_LETTER -> ErrDeadLetter
        }
    }
}

/**
 * Response error class
 */
class ResponseError(val code: ResponseStatusCode) : Exception(code.toString()) {
    override fun toString(): String = code.toString()
}

// Predefined error instances
val ErrUnavailable = ResponseError(ResponseStatusCode.UNAVAILABLE)
val ErrTimeout = ResponseError(ResponseStatusCode.TIMEOUT)
val ErrProcessNameAlreadyExist = ResponseError(ResponseStatusCode.PROCESS_NAME_ALREADY_EXIST)
val ErrDeadLetter = ResponseError(ResponseStatusCode.DEAD_LETTER)
val ErrUnknownError = ResponseError(ResponseStatusCode.ERROR)
