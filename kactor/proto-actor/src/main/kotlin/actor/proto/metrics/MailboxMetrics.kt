package actor.proto.metrics

import actor.proto.mailbox.MailboxStatistics

/**
 * Mailbox 度量统计实现
 * @param registry 度量注册表
 * @param mailboxName Mailbox 名称
 */
class MailboxMetrics(
    private val registry: MetricsRegistry,
    private val mailboxName: String
) : MailboxStatistics {
    
    override fun mailboxStarted() {
        registry.counter("mailbox.started", mapOf("mailbox" to mailboxName)).inc()
    }
    
    override fun messagePosted(message: Any) {
        registry.counter("mailbox.messages.posted", mapOf(
            "mailbox" to mailboxName,
            "message" to message.javaClass.simpleName
        )).inc()
    }
    
    override fun messageReceived(message: Any) {
        registry.counter("mailbox.messages.received", mapOf(
            "mailbox" to mailboxName,
            "message" to message.javaClass.simpleName
        )).inc()
    }
    
    override fun mailboxEmpty() {
        registry.counter("mailbox.empty", mapOf("mailbox" to mailboxName)).inc()
    }
    
    override fun messageDropped(msg: Any) {
        registry.counter("mailbox.messages.dropped", mapOf(
            "mailbox" to mailboxName,
            "message" to msg.javaClass.simpleName
        )).inc()
    }
}
