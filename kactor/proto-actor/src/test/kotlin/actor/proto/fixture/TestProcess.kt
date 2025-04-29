package actor.proto.fixture

import actor.proto.PID
import actor.proto.mailbox.SystemMessage
import actor.proto.mailbox.newUnboundedMailbox

class TestProcess : actor.proto.LocalProcess(newUnboundedMailbox()) {
    override fun sendUserMessage(pid: PID, message: Any) {
    }

    override fun sendSystemMessage(pid: PID, message: SystemMessage) {
    }
}

