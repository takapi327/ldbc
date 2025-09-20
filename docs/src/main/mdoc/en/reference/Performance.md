{%
laika.title = Performance
laika.metadata.language = en
%}

# Performance

This document provides a detailed analysis of the performance characteristics between ldbc and jdbc, helping you understand when and why to choose ldbc based on technical considerations.

## Executive Summary

Benchmark results show that ldbc demonstrates approximately 1.8-2.1x higher throughput compared to jdbc. This advantage stems from Cats Effect's Fiber-based concurrency model and non-blocking I/O implementation. ldbc particularly excels in high-concurrency environments with superior scalability.

### Key Findings

1. **Performance**: ldbc is ~2x faster than jdbc (8-thread environment)
2. **Scalability**: ldbc achieves near-linear scaling with increased thread count
3. **Resource Efficiency**: Significantly reduced memory usage (Fiber: 500 bytes vs OS Thread: 1MB)
4. **Latency**: Maintains stable response times even under high load

## Benchmark Environment

### Hardware and Software Environment
- **JDK**: Amazon Corretto 21.0.6
- **JVM**: OpenJDK 64-Bit Server VM (21.0.6+7-LTS)
- **Memory**: 4GB (Heap size: -Xms4G -Xmx4G)
- **GC**: G1GC (-XX:+UseG1GC -XX:MaxGCPauseMillis=200)
- **MySQL**: Version 8.4.0 (Docker environment)
  - Port: 13306
  - User: ldbc
  - Password: password

### JMH Benchmark Configuration
```scala
@BenchmarkMode(Array(Mode.Throughput))      // Throughput measurement
@OutputTimeUnit(TimeUnit.SECONDS)           // Output in ops/s
@State(Scope.Benchmark)                     // Benchmark scope
@Fork(value = 1)                            // Fork count: 1
@Warmup(iterations = 5)                     // Warmup: 5 iterations
@Measurement(iterations = 10)               // Measurement: 10 iterations
@Threads(1)                                 // Thread count: varies 1-16
```

### Test Conditions
- **Connection Method**: No connection pooling (single connection reused)
- **Query Types**: 
  - Statement: Dynamic SQL execution
  - PreparedStatement: Parameterized queries
- **Data Sizes**: 500, 1000, 1500, 2000 rows
- **Target Table**: 16 columns (various data types)
  - Numeric types: Long, Short, Int, Float, Double, BigDecimal
  - String types: String (2 types)
  - Date/Time types: LocalDate, LocalTime, LocalDateTime (2 types)
  - Boolean type: Boolean

### ldbc-specific Configuration
```scala
MySQLDataSource
  .build[IO]("127.0.0.1", 13306, "ldbc")
  .setPassword("password")
  .setDatabase("benchmark")
  .setSSL(SSL.Trusted)
  // Default settings (no optimization)
  .setUseServerPrepStmts(false)
  .setUseCursorFetch(false)
```

### jdbc-specific Configuration
```scala
val ds = new MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(13306)
ds.setDatabaseName("benchmark")
ds.setUser("ldbc")
ds.setPassword("password")
ds.setUseSSL(true)
// Fixed-size thread pool
val executorService = Executors.newFixedThreadPool(
  Math.max(4, Runtime.getRuntime.availableProcessors())
)
```

## Performance Comparison by Thread Count

### Single Thread Environment

@:image(/img/connector/Select_Thread1.svg) {
alt = "Select Benchmark (1 Thread)"
}

In a single-threaded environment, the performance difference between ldbc and jdbc is relatively small as concurrency advantages are not utilized.

**Performance Ratio (ldbc/jdbc)**:
- 500 rows: 1.43x
- 1000 rows: 1.52x
- 1500 rows: 1.48x
- 2000 rows: 1.51x

### 2 Thread Environment

@:image(/img/connector/Select_Thread2.svg) {
alt = "Select Benchmark (2 Threads)"
}

Starting with 2 threads, ldbc's advantages become more apparent.

**Performance Ratio (ldbc/jdbc)**:
- 500 rows: 1.83x
- 1000 rows: 1.48x
- 1500 rows: 1.66x
- 2000 rows: 1.75x

### 4 Thread Environment

@:image(/img/connector/Select_Thread4.svg) {
alt = "Select Benchmark (4 Threads)"
}

At 4 threads, ldbc's scalability becomes pronounced.

**Performance Ratio (ldbc/jdbc)**:
- 500 rows: 1.89x
- 1000 rows: 1.82x
- 1500 rows: 1.87x
- 2000 rows: 1.93x

### 8 Thread Environment

@:image(/img/connector/Select_Thread8.svg) {
alt = "Select Benchmark (8 Threads)"
}

At 8 threads, ldbc shows its highest performance advantage.

**Performance Ratio (ldbc/jdbc)**:
- 500 rows: 1.76x
- 1000 rows: 2.01x
- 1500 rows: 1.92x
- 2000 rows: 2.09x

### 16 Thread Environment

@:image(/img/connector/Select_Thread16.svg) {
alt = "Select Benchmark (16 Threads)"
}

Even at 16 threads, ldbc maintains stable high performance.

**Performance Ratio (ldbc/jdbc)**:
- 500 rows: 1.95x
- 1000 rows: 2.03x
- 1500 rows: 1.98x
- 2000 rows: 2.12x

## Technical Analysis

### Cats Effect Performance Characteristics

Cats Effect 3 is optimized for long-lived backend network applications:

#### Optimization Targets
- **Physical threads**: Environments with 8+ threads
- **Processing type**: Network socket I/O-focused processing
- **Runtime duration**: Applications running for hours or more

#### Performance Metrics
- **`flatMap` operation**: 
  - Older Intel CPUs: ~7 nanoseconds
  - Modern AMD CPUs: ~3 nanoseconds
- **Bottlenecks**: Scheduling and I/O rather than computation

#### Userspace Scheduler
1. **Work-stealing algorithm**: Designed for throughput over pure responsiveness
2. **Fine-grained preemption**: More flexible task switching than Kotlin coroutines
3. **Stack usage**: Constant memory usage through coroutine interpreter

#### Comparison with Other Runtimes
- **Project Loom (Virtual Threads)**: Cats Effect currently outperforms
- **Threadless suspension**: Supports safe resource management and asynchronous interruption

### Concurrency Model Differences

#### ldbc (Cats Effect 3)

ldbc adopts Cats Effect 3's Fiber-based concurrency model:

```scala
// Non-blocking I/O operations
for {
  statement <- connection.prepareStatement(sql)
  _         <- statement.setInt(1, id)
  resultSet <- statement.executeQuery()
  result    <- resultSet.decode[User]
} yield result
```

**Characteristics**:
- **Fibers (Green Threads)**: Lightweight user-space threads
  - Memory usage: ~300-500 bytes per Fiber
  - Context switching: Completes in user space (no kernel calls required)
  - CPU cache efficiency: High cache hit rate due to thread affinity

- **Work-Stealing Thread Pool**:
  - Per-CPU-core work queues (avoiding global contention)
  - Dynamic load balancing
  - Automatic yield insertion to prevent CPU starvation

#### jdbc (Traditional Thread Model)

jdbc uses traditional OS threads with blocking I/O:

```scala
// Blocking I/O operations
Sync[F].blocking {
  val statement = connection.prepareStatement(sql)
  statement.setInt(1, id)
  val resultSet = statement.executeQuery()
  // Thread blocks here
}
```

**Characteristics**:
- **OS Threads**: Native threads
  - Memory usage: ~1MB per thread
  - Context switching: Requires kernel calls
  - Fixed-size thread pool

### Network I/O Implementation

#### ldbc - Non-blocking Socket

```scala
// Non-blocking reads using fs2 Socket
socket.read(8192).flatMap { chunk =>
  // Efficient chunk-based processing
  processChunk(chunk)
}
```

- **Zero-copy optimization**: Efficient buffer management using BitVector
- **Streaming**: Efficient processing of large result sets
- **Timeout control**: Fine-grained timeout configuration

#### jdbc - Blocking Socket

```scala
// Traditional blocking I/O
val bytes = inputStream.read(buffer)
// Thread blocks until I/O completes
```

- **Buffering**: Entire result set loaded into memory
- **Thread blocking**: Thread unavailable during I/O wait

### Memory Efficiency and GC Pressure

#### ldbc Memory Management

1. **Pre-allocated buffers**: Reusable buffers for result rows
2. **Streaming processing**: On-demand data fetching
3. **Immutable data structures**: Efficient memory usage through structural sharing

#### jdbc Memory Management

1. **Bulk loading**: Entire result set held in memory
2. **Intermediate objects**: Boxing/unboxing overhead
3. **GC pressure**: Frequent GC due to temporary objects

## Usage Recommendations by Scenario

### When to Choose ldbc

1. **High-Concurrency Applications**
   - Web applications (high traffic)
   - Microservices
   - Real-time data processing

2. **Resource-Constrained Environments**
   - Container environments (Kubernetes, etc.)
   - Serverless environments
   - Memory-limited environments

3. **Scalability Focus**
   - Expected future load increases
   - Need for elastic scaling
   - Cloud-native applications

4. **Functional Programming**
   - Pure functional architecture
   - Type safety emphasis
   - Composability focus

### When to Choose jdbc

1. **Legacy System Integration**
   - Existing jdbc codebase
   - Third-party library dependencies
   - High migration costs

2. **Simple CRUD Operations**
   - Low concurrency
   - Batch processing
   - Administrative tools

3. **Special jdbc Features**
   - Vendor-specific extensions
   - Special driver requirements

## Performance Tuning

### Cats Effect Best Practices

#### 1. Understanding Your Workload
- **I/O-bound tasks**: Cats Effect excels with non-blocking I/O
- **CPU-bound tasks**: Consider delegation to dedicated thread pools
- **Measurement importance**: Measure performance in your specific application context

#### 2. Optimizing IO Operations
```scala
// Leverage fine-grained IO composition
val optimized = for {
  data <- fetchData()           // Non-blocking I/O
  _    <- IO.cede              // Explicit cooperative yield
  processed <- processData(data) // CPU-intensive processing
  _    <- saveResult(processed) // Non-blocking I/O
} yield processed

// Execute CPU-bound tasks on dedicated pool
val cpuBound = IO.blocking {
  // Heavy computation
}.evalOn(cpuBoundExecutor)
```

#### 3. Resource Management
```scala
// Safe resource management with Resource
val dataSource = Resource.make(
  createDataSource()  // Acquire
)(_.close())         // Release

// Guaranteed resource cleanup
dataSource.use { ds =>
  // Process with datasource
}
```

### ldbc Optimization Settings

```scala
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "user")
  .setPassword("password")
  .setDatabase("db")
  // Performance settings
  .setUseServerPrepStmts(true)      // Server-side prepared statements
  .setUseCursorFetch(true)           // Cursor-based fetching
  .setFetchSize(1000)                // Fetch size
  .setSocketOptions(List(
    SocketOption.noDelay(true),      // TCP_NODELAY
    SocketOption.keepAlive(true)     // Keep-alive
  ))
  .setReadTimeout(30.seconds)        // Read timeout
```

### Thread Pool Configuration

```scala
// Cats Effect 3 runtime configuration
object Main extends IOApp {
  override def computeWorkerThreadCount: Int = 
    math.max(4, Runtime.getRuntime.availableProcessors())
    
  override def run(args: List[String]): IO[ExitCode] = {
    // Application logic
  }
}
```

## Conclusion

ldbc is an excellent choice particularly when these conditions are met:

1. **High Concurrency**: Need to handle many concurrent connections
2. **Scalability**: Require flexible scaling based on load
3. **Resource Efficiency**: Need to minimize memory usage
4. **Type Safety**: Value compile-time type checking
5. **Functional Programming**: Adopting pure functional architecture

As benchmark results demonstrate, ldbc achieves approximately 2x the throughput of jdbc in environments with 8+ threads, while maintaining excellent scalability with minimal performance degradation at higher thread counts.

### Benefits from Cats Effect Optimization

The Cats Effect 3 runtime that ldbc leverages is optimized for:

- **Long-running applications**: Shows its true value in applications running for hours or more
- **Network I/O-centric**: Perfect for non-blocking I/O operations like database access
- **Multi-core utilization**: Best performance in environments with 8+ threads

These characteristics perfectly align with ldbc's use case as a database access library, making ldbc a powerful choice for modern cloud-native applications and high-traffic web services.

**Key Principle**: "Performance is always relative to your target use-case and assumptions" - we recommend measuring in your actual application context.