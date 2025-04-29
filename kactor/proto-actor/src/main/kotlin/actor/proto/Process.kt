package actor.proto

import actor.proto.mailbox.Mailbox
import actor.proto.mailbox.SystemMessage

abstract class Process {
    abstract fun sendUserMessage(pid: PID, message: Any)
    open fun stop(pid: PID) = sendSystemMessage(pid, StopInstance)

    abstract fun sendSystemMessage(pid: PID, message: SystemMessage)

    /**
     * 检查进程是否活跃
     * @return 如果进程活跃则返回 true，否则返回 false
     */
    open fun isAlive(): Boolean = true
}


open class LocalProcess(private val mailbox: Mailbox) : Process() {
    private var isDead: Boolean = false
    override fun sendUserMessage(pid: PID, message: Any) {
        mailbox.postUserMessage(message)
    }

    override fun sendSystemMessage(pid: PID, message: SystemMessage) {
        mailbox.postSystemMessage(message)
    }

    override fun stop(pid: PID) {
        super.stop(pid)
        markAsDead()
    }

    /**
     * 标记进程为死亡
     */
    fun markAsDead() {
        isDead = true
    }

    /**
     * 检查进程是否已死亡
     * @return 如果进程已死亡则为 true，否则为 false
     */
    fun isDead(): Boolean = isDead

    /**
     * 检查进程是否活跃
     * @return 如果进程活跃则返回 true，否则返回 false
     */
    override fun isAlive(): Boolean = !isDead
}