package actor.proto.remote

import actor.proto.PID
import actor.proto.diagnostics.MatchType
import actor.proto.diagnostics.ProcessInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

/**
 * DiagnosticsClient provides methods for accessing diagnostics information from remote systems.
 */
class DiagnosticsClient(private val address: String) {

    companion object {
        /**
         * Create a new DiagnosticsClient for the given address.
         * @param address The address of the remote system
         * @return A new DiagnosticsClient
         */
        fun create(address: String): DiagnosticsClient {
            return DiagnosticsClient(address)
        }
    }
}
