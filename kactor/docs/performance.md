# Performance Testing in ProtoActor-Kotlin

ProtoActor-Kotlin includes a comprehensive performance testing framework to measure and optimize the performance of the actor system. This document describes the performance testing features and how to use them.

## Overview

The performance testing framework in ProtoActor-Kotlin provides the following features:

- **Local Benchmarks**: Measure the performance of local actor messaging, actor creation, and mailbox operations
- **Remote Benchmarks**: Measure the performance of remote actor messaging
- **Cluster Benchmarks**: Measure the performance of cluster grain operations
- **Comparison Tool**: Compare the performance of ProtoActor-Kotlin with ProtoActor-Go

## Running Benchmarks

### Local Benchmarks

To run the local benchmarks, use the following command:

```bash
./gradlew :proto-benchmarks:jmh
```

This will run all benchmarks and generate a results file in the `proto-benchmarks/build/reports/jmh` directory.

You can also run specific benchmarks:

```bash
./gradlew :proto-benchmarks:jmh --include=LocalMessageBenchmark
```

### Remote Benchmarks

To run the remote benchmarks, you need to start a remote benchmark server first:

```bash
./gradlew :proto-benchmarks:run --args="RemoteBenchmarkServer"
```

Then, in another terminal, run the remote benchmarks:

```bash
./gradlew :proto-benchmarks:jmh --include=RemoteMessageBenchmark
```

### Cluster Benchmarks

To run the cluster benchmarks, you need to start a cluster benchmark node first:

```bash
./gradlew :proto-benchmarks:run --args="ClusterBenchmarkNode"
```

Then, in another terminal, run the cluster benchmarks:

```bash
./gradlew :proto-benchmarks:jmh --include=ClusterGrainBenchmark
```

### Running All Benchmarks

You can run all benchmarks using the BenchmarkRunner:

```bash
./gradlew :proto-benchmarks:run --args="BenchmarkRunner"
```

This will run all benchmarks and generate a JSON results file.

## Comparing with ProtoActor-Go

To compare the performance of ProtoActor-Kotlin with ProtoActor-Go, you need to run the benchmarks for both implementations and then use the ComparisonTool:

```bash
./gradlew :proto-benchmarks:run --args="ComparisonTool kotlin-results.json go-results.json comparison.html"
```

This will generate an HTML report comparing the performance of the two implementations.

## Benchmark Descriptions

### Local Benchmarks

#### LocalMessageBenchmark

Measures the performance of local actor messaging:

- **pingPong**: Measures the round-trip time for a ping-pong message exchange between two actors
- **throughput**: Measures the throughput of sending a large number of messages to an actor

#### ActorCreationBenchmark

Measures the performance of actor creation:

- **createActors**: Measures the throughput of creating actors without names
- **createNamedActors**: Measures the throughput of creating actors with names

#### MailboxBenchmark

Measures the performance of different mailbox implementations:

- **unboundedMailbox**: Measures the throughput of the UnboundedMailbox implementation
- **defaultMailbox**: Measures the throughput of the DefaultMailbox implementation

### Remote Benchmarks

#### RemoteMessageBenchmark

Measures the performance of remote actor messaging:

- **pingPong**: Measures the round-trip time for a ping-pong message exchange between two remote actors
- **throughput**: Measures the throughput of sending a large number of messages to a remote actor

### Cluster Benchmarks

#### ClusterGrainBenchmark

Measures the performance of cluster grain operations:

- **grainCalls**: Measures the throughput of making calls to virtual actors in a cluster

## Performance Optimization Tips

Based on the benchmark results, here are some tips for optimizing the performance of your ProtoActor-Kotlin applications:

1. **Use local actors when possible**: Local actor messaging is much faster than remote messaging. Only use remote actors when necessary.

2. **Choose the right mailbox**: The DefaultMailbox is generally faster than the UnboundedMailbox, but the UnboundedMailbox can handle more messages.

3. **Batch messages**: Instead of sending many small messages, batch them into larger messages when possible.

4. **Minimize actor creation**: Actor creation is relatively expensive. Reuse actors when possible, especially for short-lived tasks.

5. **Use appropriate serialization**: Choose the most efficient serialization format for your messages.

6. **Optimize message handling**: Keep message handling code as efficient as possible, especially for high-throughput actors.

7. **Use appropriate cluster strategies**: Choose the right member strategy for your cluster based on your workload.

## Conclusion

The performance testing framework in ProtoActor-Kotlin provides a powerful tool for measuring and optimizing the performance of your actor system. By understanding the performance characteristics of different components, you can make informed decisions about how to design and implement your actor-based applications.
