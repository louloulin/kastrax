package actor.proto.mailbox

/**
 * MessageBatch is a message that is sent to the actor and unpacks its payload in the mailbox.
 * This allows you to group messages together and send them as a single message
 * while processing them as individual messages.
 * This is used by the Cluster PubSub feature to send a batch of messages and then Ack to the entire batch.
 */
interface MessageBatch {
    /**
     * Gets the messages contained in this batch.
     * @return A list of messages in the batch.
     */
    fun getMessages(): List<Any>
}
