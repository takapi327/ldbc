{%
laika.title = Fx
laika.metadata.language = en
%}

# Fx

## Overview

`ldbc-fx` provides `Fx`, a small effect type with no dependency on an external effect library, together with its execution substrate `FxRuntime`.

In 0.9.x the driver, network layer and connection pool are written against the `ldbc-effect` type classes (`Async ⊂ Temporal ⊂ Concurrent`), so they run natively on any effect that has a `Concurrent` instance. `Fx` is one such implementation.

@:callout(info)

**Most users do not need this page**

If you use Cats Effect (`IO`) or ZIO (`Task`), `Fx` never appears. It is relevant in exactly two cases:

- **You use `scala.concurrent.Future`**: `Future` has neither cancellation nor fork, so it cannot satisfy `Concurrent`. `ldbc-future` therefore runs the driver on `Fx` internally and bridges to `Future` exactly once per run. Even then you never write `Fx` yourself
- **You use no effect library at all**: you can work with `Fx` directly

@:@

## Using it as the Future backend

With `ldbc-future`, `Fx` appears only as the type parameter of `MySQLDataSource`.

```scala
import scala.concurrent.Future

import ldbc.dsl.*
import ldbc.fx.{ Fx, concurrentFx }
import ldbc.future.Connector
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL

val datasource = MySQLDataSource
  .build[Fx]("127.0.0.1", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .setSSL(SSL.Trusted)

val connector = Connector.fromDataSource(datasource)

val cities: Future[List[String]] =
  sql"SELECT Name FROM city".query[String].to[List].readOnly(connector)
```

`Future` has no host runtime and no reason to swap one, so the `Fx` program on this path always runs on the platform default `FxRuntime.global`.

## The effect type `Fx`

`Fx` is a lazy, cancelable, stack-safe asynchronous effect. Nothing runs until you call `unsafeRun`.

### Construction

| Constructor | Meaning |
|---|---|
| `Fx.pure(a)` | An already computed value |
| `Fx.unit` | Yields `()` |
| `Fx.raiseError(t)` | Fails |
| `Fx.delay(thunk)` | Suspends a synchronous side effect. Runs on the calling thread |
| `Fx.blocking(thunk)` | Runs a blocking thunk on a dedicated pool (inline on JS) |
| `Fx.interruptible(thunk)` | Like `blocking`, but the thread is `Thread.interrupt()`ed on cancellation |
| `Fx.async(k)` | Bridges a callback API into `Fx`. `k` registers the completion callback and returns a `Canceler` |
| `Fx.never` | Never completes |
| `Fx.sleep(d)` | Completes after the given delay |

The difference between `blocking` and `interruptible` is whether the thunk itself is interrupted on cancellation. `blocking` is cancelable at its boundary — the result is discarded and the continuation does not run — but the thunk still runs to completion.

### Composition

```scala
import ldbc.fx.syntax.*

val program: Fx[Int] =
  Fx.delay(1)
    .flatMap(n => Fx.delay(n + 1))
    .map(_ * 10)
    .handleErrorWith(_ => Fx.pure(0))
```

Importing `ldbc.fx.syntax.*` gives you `>>` `*>` `<*` `void` `as` `flatTap` `attempt` `handleError` `guarantee` `onError` `start` `timeout`. For collections there are `traverse` `traverse_` `filterA` `parTraverse` `parTraverse_` `parTraverseN`, and for tuples `parTupled`.

### Resources and cancellation

```scala
Fx.bracket(acquire)(use)(release)
```

`release` runs on success, failure and cancellation alike. `acquire` is masked, so a cancellation during acquisition cannot leak the resource.

| API | Meaning |
|---|---|
| `Fx.bracket(acquire)(use)(release)` | Acquire, use, release |
| `Fx.uncancelable(body)` | Defers cancellation for the duration of `body`. There is no partial `poll` |
| `fa.onCancel(fin)` | Runs `fin` **on cancellation only** — not on success or failure |
| `fa.guarantee(fin)` | Runs `fin` on success, failure and cancellation |

### Running

```scala
val canceler: Fx.Canceler = program.unsafeRun {
  case Right(value) => println(value)
  case Left(error)  => error.printStackTrace()
}
```

`unsafeRun` is the boundary where side effects actually happen. The returned `Canceler` requests interruption.

`unsafeRunCancelable` returns a `CancelToken` instead of a `Canceler`. Its `cancel: Fx[Unit]` completes only **after the finalizers on the cancellation path have actually finished** — use it when you need to wait for a rollback or a resource release to complete.

## Concurrency primitives

| Type | Construction | Purpose |
|---|---|---|
| `Ref[A]` | `Ref.of(a)` / `Ref.unsafe(a)` | Atomically updatable mutable reference. `get` `set` `update` `modify` and more |
| `Deferred[A]` | `Deferred[A]` | A write-once asynchronous value. `get` suspends until it is completed |
| `Semaphore` | `Semaphore(permits)` | FIFO counting semaphore. Wrap work in `withPermit` |
| `Mutex` | `Mutex.create` | Mutual exclusion. Wrap work in `surround` |
| `Resource[A]` | `Resource.make` / `eval` / `pure` | An acquire/release pair. Consume with `use`; nesting via `flatMap` releases in LIFO order |
| `Fiber[A]` | `fa.start` | Concurrent execution. `join` yields an `Outcome`; `joinWithNever` / `joinWith` / `cancel` are also available |

The `Outcome` returned by `Fiber#join` is one of `Succeeded(a)` / `Errored(e)` / `Canceled`.

## FxRuntime

`FxRuntime` is the execution substrate for `Fx`. It decides where the run loop schedules work.

```scala
trait FxRuntime:
  def executeCompute(task: () => Unit): Unit
  def executeBlocking(task: () => Unit): Unit
  def executeInterruptible(task: () => Unit): Fx.Canceler
  def scheduleOnce(delayNanos: Long, task: () => Unit): Fx.Canceler

  def autoCedeThreshold: Int = FxRuntime.defaultAutoCedeThreshold
  def finalizerTimeout: FiniteDuration = FxRuntime.defaultFinalizerTimeout
```

### Platform default runtimes

`FxRuntime.global` is provided per platform. Every pool is created lazily, so you pay nothing for the ones you do not use.

| Platform | compute | blocking | scheduler |
|---|---|---|---|
| JVM / Native | Fixed pool (core count, minimum 2) `fx-compute` | Cached pool `fx-blocking` | Single thread `fx-scheduler` |
| Scala.js | Yields to the event loop via `setTimeout(0)` | Runs inline (there is nowhere to offload to) | `setTimeout` |

All threads are daemon threads.

### Which runtime a program runs on

`unsafeRun` takes `using rt: FxRuntime = FxRuntime.current`. `FxRuntime.current` is the runtime in effect on that thread, or `global` if there is none. A nested `unsafeRun` started during interpretation inherits the outer runtime.

To choose one explicitly:

```scala
program.unsafeRun(callback)(using myRuntime)
```

### Tuning

`autoCedeThreshold` and `finalizerTimeout` have default implementations, so you only override the ones you need.

```scala
final class LatencySensitiveRuntime(delegate: FxRuntime) extends FxRuntime:
  export delegate.{ executeCompute, executeBlocking, executeInterruptible, scheduleOnce }

  override def autoCedeThreshold: Int            = 64
  override def finalizerTimeout:  FiniteDuration = 5.seconds

val runtime: FxRuntime = new LatencySensitiveRuntime(FxRuntime.global)

program.unsafeRun(callback)(using runtime)
```

The `export` targets the constructor parameter `delegate` rather than `FxRuntime.global` because `export` requires an immutable path, and `FxRuntime.global` is a `def`.

**`autoCedeThreshold`** — how many consecutive synchronous steps the run loop takes before moving the continuation onto `executeCompute` and releasing the thread. The default is 1024.

This is what stops a long synchronous chain from monopolising an I/O poller or selector thread that resumes continuations inline. Lower it for runtimes whose threads are latency sensitive; raise it for compute-only runtimes.

@:callout(warning)

**It must be at least `FxRuntime.minAutoCedeThreshold` (= 2).**

At 1 or below the cede condition holds before a single step has run, and the run loop keeps re-scheduling itself without making progress. The run loop clamps to this lower bound, so an out-of-range value will not hang — it just cedes very often — but it will not do what you intended either.

@:@

**`finalizerTimeout`** — the upper bound for a single release on the cancellation path. The default is 30 seconds.

It keeps `CancelToken.cancel` from hanging forever when a release never completes — a rollback to a dead connection, for example. The right value depends on what the programs on that runtime release, which is why this is a per-runtime value rather than a process-wide one.

@:callout(warning)

**It must be positive.**

A value of zero or less aborts every release before it runs. Errors from a release are swallowed, so resources that should have been released are silently left unreleased. Note that "wait indefinitely" cannot be expressed: this bound exists precisely to keep `cancel` from hanging.

@:@

## Relationship to other effects

`ldbc.fx.concurrentFx` is the `ldbc.effect.Concurrent[Fx]` instance. It is what lets the driver, network layer and pool — all written against `F: Concurrent` — run on `Fx` unchanged.

The bridge between `Fx` and cats (`cats.MonadError[Fx, Throwable]`) lives in `ldbc-future`'s `FxInstances`. `ldbc-fx` itself does not depend on cats: the dependency is pushed to the consumer so that the DB-agnostic core stays effect-agnostic.
