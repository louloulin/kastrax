package actor.proto.cluster

import actor.proto.Props

/**
 * Kind represents a type of virtual actor in the cluster.
 */
data class Kind(
    val name: String,
    val props: Props
)

/**
 * ActivatedKind represents an activated kind in the cluster.
 */
data class ActivatedKind(
    val kind: Kind,
    var count: Int = 0
) {
    /**
     * Increment the count of activated actors.
     */
    fun increment() {
        count++
    }
    
    /**
     * Decrement the count of activated actors.
     */
    fun decrement() {
        count--
    }
}
