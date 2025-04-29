rootProject.name = "protoactor-kotlin"

include("proto-mailbox")
include("proto-actor")
include("proto-router")
include("proto-remote")
include("proto-cluster")
include("proto-persistence")
include("proto-stream")
include("proto-benchmarks")
include("proto-plugin")
include("proto-plugin-examples")
include("proto-cluster-libp2p")
include("examples")
include("native-example")
include("simple-native")
include("benchmark-native")

// 如果您想直接使用 jvm-libp2p 源码，取消下面的注释
// includeBuild("jvm-libp2p") {
//     dependencySubstitution {
//         substitute(module("io.libp2p:jvm-libp2p-minimal")).using(project(":jvm-libp2p-minimal"))
//         substitute(module("io.libp2p:jvm-libp2p-core")).using(project(":jvm-libp2p-core"))
//         substitute(module("io.libp2p:jvm-libp2p-crypto")).using(project(":jvm-libp2p-crypto"))
//         substitute(module("io.libp2p:jvm-libp2p-discovery")).using(project(":jvm-libp2p-discovery"))
//         substitute(module("io.libp2p:jvm-libp2p-protocol")).using(project(":jvm-libp2p-protocol"))
//         substitute(module("io.libp2p:jvm-libp2p-pubsub")).using(project(":jvm-libp2p-pubsub"))
//     }
// }
