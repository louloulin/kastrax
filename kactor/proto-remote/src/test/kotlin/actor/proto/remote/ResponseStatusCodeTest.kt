package actor.proto.remote

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ResponseStatusCodeTest {
    
    @Test
    fun `test status code to int32`() {
        assertEquals(0, ResponseStatusCode.OK.toInt32())
        assertEquals(1, ResponseStatusCode.UNAVAILABLE.toInt32())
        assertEquals(2, ResponseStatusCode.TIMEOUT.toInt32())
        assertEquals(3, ResponseStatusCode.PROCESS_NAME_ALREADY_EXIST.toInt32())
        assertEquals(4, ResponseStatusCode.ERROR.toInt32())
        assertEquals(5, ResponseStatusCode.DEAD_LETTER.toInt32())
    }
    
    @Test
    fun `test status code to string`() {
        assertEquals("OK", ResponseStatusCode.OK.toString())
        assertEquals("UNAVAILABLE", ResponseStatusCode.UNAVAILABLE.toString())
        assertEquals("TIMEOUT", ResponseStatusCode.TIMEOUT.toString())
        assertEquals("PROCESS_NAME_ALREADY_EXIST", ResponseStatusCode.PROCESS_NAME_ALREADY_EXIST.toString())
        assertEquals("ERROR", ResponseStatusCode.ERROR.toString())
        assertEquals("DEAD_LETTER", ResponseStatusCode.DEAD_LETTER.toString())
    }
    
    @Test
    fun `test status code to error`() {
        assertNull(ResponseStatusCode.OK.asError())
        assertEquals(ErrUnavailable, ResponseStatusCode.UNAVAILABLE.asError())
        assertEquals(ErrTimeout, ResponseStatusCode.TIMEOUT.asError())
        assertEquals(ErrProcessNameAlreadyExist, ResponseStatusCode.PROCESS_NAME_ALREADY_EXIST.asError())
        assertEquals(ErrUnknownError, ResponseStatusCode.ERROR.asError())
        assertEquals(ErrDeadLetter, ResponseStatusCode.DEAD_LETTER.asError())
    }
    
    @Test
    fun `test response error`() {
        val error = ResponseError(ResponseStatusCode.TIMEOUT)
        assertEquals("TIMEOUT", error.toString())
        assertEquals(ResponseStatusCode.TIMEOUT, error.code)
    }
    
    @Test
    fun `test predefined errors`() {
        assertEquals(ResponseStatusCode.UNAVAILABLE, ErrUnavailable.code)
        assertEquals(ResponseStatusCode.TIMEOUT, ErrTimeout.code)
        assertEquals(ResponseStatusCode.PROCESS_NAME_ALREADY_EXIST, ErrProcessNameAlreadyExist.code)
        assertEquals(ResponseStatusCode.ERROR, ErrUnknownError.code)
        assertEquals(ResponseStatusCode.DEAD_LETTER, ErrDeadLetter.code)
    }
}
