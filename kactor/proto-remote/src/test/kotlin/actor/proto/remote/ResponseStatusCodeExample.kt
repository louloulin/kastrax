package actor.proto.remote

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.spawnNamed
import kotlinx.coroutines.runBlocking

/**
 * Example demonstrating how to use Response Status Codes
 */
class ResponseStatusCodeExample {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            // Example of handling different response status codes
            println("Response Status Code Example")
            println("============================")
            
            // Print all available status codes
            println("Available status codes:")
            ResponseStatusCode.values().forEach { 
                println("- ${it.name}: ${it.toInt32()}")
            }
            println()
            
            // Example of converting status codes to errors
            println("Status codes as errors:")
            ResponseStatusCode.values().forEach { 
                val error = it.asError()
                if (error != null) {
                    println("- ${it.name} -> ${error}")
                } else {
                    println("- ${it.name} -> No error (success)")
                }
            }
            println()
            
            // Example of handling errors in a try-catch block
            println("Error handling example:")
            try {
                // Simulate an error
                throw ResponseError(ResponseStatusCode.TIMEOUT)
            } catch (e: ResponseError) {
                when (e.code) {
                    ResponseStatusCode.UNAVAILABLE -> println("Service unavailable, retry later")
                    ResponseStatusCode.TIMEOUT -> println("Operation timed out, consider increasing timeout")
                    ResponseStatusCode.PROCESS_NAME_ALREADY_EXIST -> println("Process name already exists, use a different name")
                    ResponseStatusCode.DEAD_LETTER -> println("Message sent to dead letter")
                    ResponseStatusCode.ERROR -> println("Unknown error occurred")
                    else -> println("Unexpected error: ${e.code}")
                }
            }
        }
    }
}

/**
 * Example actor that demonstrates error handling with response status codes
 */
class ErrorHandlingActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        try {
            when (msg) {
                is String -> {
                    when (msg) {
                        "timeout" -> throw ResponseError(ResponseStatusCode.TIMEOUT)
                        "unavailable" -> throw ResponseError(ResponseStatusCode.UNAVAILABLE)
                        "exists" -> throw ResponseError(ResponseStatusCode.PROCESS_NAME_ALREADY_EXIST)
                        "error" -> throw ResponseError(ResponseStatusCode.ERROR)
                        "deadletter" -> throw ResponseError(ResponseStatusCode.DEAD_LETTER)
                        else -> respond("Success: $msg")
                    }
                }
                else -> respond("Unknown message type")
            }
        } catch (e: ResponseError) {
            // Handle the error and respond with appropriate message
            val errorMessage = when (e.code) {
                ResponseStatusCode.UNAVAILABLE -> "Service unavailable"
                ResponseStatusCode.TIMEOUT -> "Operation timed out"
                ResponseStatusCode.PROCESS_NAME_ALREADY_EXIST -> "Process name already exists"
                ResponseStatusCode.DEAD_LETTER -> "Message sent to dead letter"
                ResponseStatusCode.ERROR -> "Unknown error occurred"
                else -> "Unexpected error: ${e.code}"
            }
            respond("Error: $errorMessage")
        }
    }
}
