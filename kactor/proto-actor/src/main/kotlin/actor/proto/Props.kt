package actor.proto

import actor.proto.mailbox.Dispatcher
import actor.proto.mailbox.Mailbox
import actor.proto.mailbox.newUnboundedMailbox
import actor.proto.middleware.ContextDecorator
import actor.proto.middleware.SpawnMiddleware

typealias Receive = suspend (Context) -> Unit
typealias Send = suspend (SenderContext, PID, MessageEnvelope) -> Unit

typealias ReceiveMiddleware = (Receive) -> Receive
typealias SenderMiddleware = (Send) -> Send

data class Props(
        val spawner: (name: String, props: Props, parent: PID?) -> PID = ::defaultSpawner,
        val producer: (() -> Actor)? = null,
        val mailboxProducer: () -> Mailbox = { newUnboundedMailbox() },
        val supervisorStrategy: SupervisorStrategy = Supervision.defaultStrategy,
        val dispatcher: Dispatcher = actor.proto.mailbox.Dispatchers.DEFAULT_DISPATCHER,
        val receiveMiddleware: List<ReceiveMiddleware> = listOf(),
        val senderMiddleware: List<SenderMiddleware> = listOf(),
        val spawnMiddleware: List<SpawnMiddleware> = listOf(),
        val contextDecorator: List<ContextDecorator> = listOf()
) {
    fun withChildSupervisorStrategy(supervisorStrategy: SupervisorStrategy): Props = copy(supervisorStrategy = supervisorStrategy)
    fun withMailbox(mailboxProducer: () -> Mailbox): Props = copy(mailboxProducer = mailboxProducer)
    fun withDispatcher(dispatcher: Dispatcher): Props = copy(dispatcher = dispatcher)
}

internal fun Props.spawn(name: String, parent: PID?): PID = spawner(name, this, parent)


fun Props.withProducer(producer: () -> Actor): Props = copy(producer = producer)
fun Props.withSpawner(spawner: (String, Props, PID?) -> PID): Props = copy(spawner = spawner)
fun Props.withSenderMiddleware(vararg middleware: SenderMiddleware): Props = copy(senderMiddleware = middleware.toList())
fun Props.withReceiveMiddleware(vararg middleware: ReceiveMiddleware): Props = copy(receiveMiddleware = middleware.toList())
fun Props.withSpawnMiddleware(vararg middleware: SpawnMiddleware): Props = copy(spawnMiddleware = middleware.toList())
fun Props.withContextDecorator(vararg decorator: ContextDecorator): Props = copy(contextDecorator = decorator.toList())

fun defaultSpawner(name: String, props: Props, parent: PID?): PID {
    val system = ActorSystem.default()
    val mailbox = props.mailboxProducer()
    val dispatcher = props.dispatcher
    val process = LocalProcess(mailbox)
    val self = system.processRegistry().put(name, process)
    val ctx = ActorContext(props.producer!!, self, props.supervisorStrategy, props.receiveMiddleware, props.senderMiddleware, parent)
    mailbox.registerHandlers(ctx, dispatcher)
    mailbox.postSystemMessage(Started)
    mailbox.start()
    return self
}


