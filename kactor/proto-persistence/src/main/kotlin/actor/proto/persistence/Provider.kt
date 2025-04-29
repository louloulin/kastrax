package actor.proto.persistence

/**
 * Provider is the abstraction used for persistence.
 */
interface Provider {
    /**
     * Get the provider state.
     * @return The provider state.
     */
    fun getState(): ProviderState
}

/**
 * ProviderState is an object containing the implementation for the provider.
 */
interface ProviderState : SnapshotStore, EventStore {
    /**
     * Restart the provider state.
     */
    fun restart()
    
    /**
     * Get the snapshot interval.
     * @return The snapshot interval.
     */
    fun getSnapshotInterval(): Int
}

/**
 * SnapshotStore is responsible for storing and retrieving snapshots.
 */
interface SnapshotStore {
    /**
     * Get a snapshot for an actor.
     * @param actorName The name of the actor.
     * @return The snapshot, event index, and whether the snapshot was found.
     */
    fun getSnapshot(actorName: String): Triple<Any?, Int, Boolean>
    
    /**
     * Persist a snapshot for an actor.
     * @param actorName The name of the actor.
     * @param snapshotIndex The index of the snapshot.
     * @param snapshot The snapshot to persist.
     */
    fun persistSnapshot(actorName: String, snapshotIndex: Int, snapshot: Any)
    
    /**
     * Delete snapshots for an actor.
     * @param actorName The name of the actor.
     * @param inclusiveToIndex The index up to which to delete snapshots (inclusive).
     */
    fun deleteSnapshots(actorName: String, inclusiveToIndex: Int)
}

/**
 * EventStore is responsible for storing and retrieving events.
 */
interface EventStore {
    /**
     * Get events for an actor.
     * @param actorName The name of the actor.
     * @param eventIndexStart The starting event index.
     * @param eventIndexEnd The ending event index (0 means all events).
     * @param callback The callback to invoke for each event.
     */
    fun getEvents(actorName: String, eventIndexStart: Int, eventIndexEnd: Int, callback: (Any) -> Unit)
    
    /**
     * Persist an event for an actor.
     * @param actorName The name of the actor.
     * @param eventIndex The index of the event.
     * @param event The event to persist.
     */
    fun persistEvent(actorName: String, eventIndex: Int, event: Any)
    
    /**
     * Delete events for an actor.
     * @param actorName The name of the actor.
     * @param inclusiveToIndex The index up to which to delete events (inclusive).
     */
    fun deleteEvents(actorName: String, inclusiveToIndex: Int)
}
