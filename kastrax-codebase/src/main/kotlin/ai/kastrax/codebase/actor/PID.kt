package ai.kastrax.codebase.actor

/**
 * 进程 ID
 *
 * 用于标识 Actor
 *
 * @property id ID
 */
data class PID(val id: String) {
    override fun toString(): String = id
}
