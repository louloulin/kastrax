[![Build Status](https://travis-ci.org/AsynkronIT/protoactor-kotlin.svg?branch=master)](https://travis-ci.org/AsynkronIT/protoactor-kotlin)
[![Download](https://api.bintray.com/packages/asynkronit/protoactor-kotlin/proto-actor/images/download.svg)](https://bintray.com/asynkronit/protoactor-kotlin/proto-actor/_latestVersion)
[![Coverage Status](https://codecov.io/gh/AsynkronIT/protoactor-kotlin/branch/master/graph/badge.svg)](https://codecov.io/gh/AsynkronIT/protoactor-kotlin)
![stability-experimental](https://img.shields.io/badge/stability-experimental-orange.svg)


# Proto.Actor Kotlin
Ultra-fast, distributed, cross-platform actors.
This is the Kotlin repository for [Proto.Actor](http://proto.actor/).

## Stability
It's used in production but doesn't have the same adoption and stability as the [C#](https://github.com/AsynkronIT/protoactor-dotnet) and [Go](https://github.com/AsynkronIT/protoactor-go) implementations.

## Compatibility with ProtoActor-Go
This implementation is now compatible with ProtoActor-Go, allowing for cross-language communication between Kotlin and Go actors. The protocol buffer definitions have been updated to match the Go implementation, and the remote communication protocol has been aligned to ensure interoperability.

## How to build
```
./gradlew build
```

## Native Image Support
ProtoActor-Kotlin now supports GraalVM Native Image compilation, which allows you to compile your actor system into a native executable for improved startup time and reduced memory footprint.

### Requirements
- GraalVM CE 17.0.9 or later
- Native Image tool installed (`gu install native-image`)

### Building a Native Image
We provide a convenient script to build native images:
```
./build-native.sh
```

This script will:
1. Check if GraalVM is installed
2. Run the application with the GraalVM agent to generate configuration files
3. Compile the native image
4. Run the generated native executable

You can also use Gradle directly:
```
# Run with agent to generate configuration
./gradlew -Pagent=standard :native-example:run

# Copy generated configuration
./gradlew :native-example:metadataCopy

# Compile native image
./gradlew :native-example:nativeCompile

# Run the native executable
./native-example/build/native/nativeCompile/proto-actor-native
```

## Design principles

**Minimalistic API** - The API should be small and easy to use. Avoid enterprisey containers and configurations.

**Build on existing technologies** - There are already a lot of great technologies for e.g. networking and clustering.
Build on those instead of reinventing them. E.g. gRPC streams for networking, Consul for clustering.

**Pass data, not objects** - Serialization is an explicit concern - don't try to hide it. Protobuf all the way.

**Be fast** - Do not trade performance for magic API trickery.

Inprocess Ping-Pong results:
```
Dispatcher		Elapsed		Msg/sec
300			273		116885925
400			217		147426522
500			150		213037390
600			85		375979638
700			87		364621820
800			83		381552772 <-- 380+ mil msg/sec
```

## Modules

Dependencies

![Package dependencies](docs/diagrams/proto-actor-packages.png)

## Getting started
The best place currently for learning how to use Proto.Actor is the [examples](https://github.com/AsynkronIT/protoactor-kotlin/tree/master/examples).

For information about the compatibility implementation, see the following documentation:
- [Implementation Documentation](docs/implementation.md)
- [Remote Communication](docs/remote.md)
- [Native Image Support](docs/native-image.md)
- [Simplified Native Image Building](docs/simple-native.md)


### Hello world
`build.gradle.kts`
```
repositories {
    jcenter()
}

dependencies {
	implementation("actor.proto:proto-actor:latest.release")
}
```

`App.kt`
```
import actor.proto.*

fun main() {
	val prop = fromFunc { msg ->
		when (msg) {
			is Started -> println("Started")
			is String -> {
				println("Hello $msg")
				stop(self)
			}
			is Stopping -> println("Stopping")
			is Stopped -> println("Stopped")
			else -> println("Unknown message $msg")
		}
	}

	val pid = spawn(prop)
	send(pid, "Proto.Actor")
	readLine()
}
```

## Release management
Stable release are published to https://bintray.com/asynkronit/protoactor-kotlin and linked to jcenter.
Anyone of the repositories below will do.
```
repositories {
   	maven("https://dl.bintray.com/asynkronit/protoactor-kotlin")
}
```
```
repositories {
   	jcenter()
}
```


### Snapshot
Commits on the master branch are deployed as snapshots to
https://oss.jfrog.org/artifactory/oss-snapshot-local/actor/proto/ and can be consumed by adding the following configuration to your gradle file:

```
repositories {
    repositories {
        maven { url 'http://oss.jfrog.org/artifactory/oss-snapshot-local' }
    }
}

dependencies {
    compile 'actor.proto:proto-actor:0.1.0-SNAPSHOT'
}
```

### Publishing a new version
When a tag is created e.g. `v0.1.0` Travis will build and publish the packages to Bintray.

### Support

Many thanks to [JetBrains](https://www.jetbrains.com) for support!

Also thanks to [ej-technologies.com for their Java profiler - JProfiler](https://www.ej-technologies.com/products/jprofiler/overview.html)

