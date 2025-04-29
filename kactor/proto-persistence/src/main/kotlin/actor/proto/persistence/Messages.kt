package actor.proto.persistence

/**
 * Replay is a message sent to an actor to replay events.
 */
object Replay

/**
 * ReplayComplete is a message sent to an actor when event replay is complete.
 */
object ReplayComplete

/**
 * OfferSnapshot is a message sent to an actor to offer a snapshot.
 */
data class OfferSnapshot(val snapshot: Any)

/**
 * RequestSnapshot is a message sent to an actor to request a snapshot.
 */
object RequestSnapshot
