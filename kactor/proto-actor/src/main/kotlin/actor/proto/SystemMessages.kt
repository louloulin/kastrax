package actor.proto

import actor.proto.mailbox.SystemMessage

/**
 * 重启消息
 */
val restartMessage: SystemMessage = Restart(Exception("Restarting"))
