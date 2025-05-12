package ai.kastrax.codebase.actor

// Import kactor types
import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.ActorSystem

// Import kactor functions
import actor.proto.fromProducer
import actor.proto.spawn
import actor.proto.spawnNamed
import actor.proto.stop
import actor.proto.send
import actor.proto.request
import actor.proto.requestAwait

// Note: The 'ask' function is not directly available in kactor, it's a suspend function
// that needs to be called from a coroutine context
