/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.fx

import java.util.concurrent.TimeUnit

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.*

import org.openjdk.jmh.annotations.*

import cats.effect.unsafe.IORuntime
import cats.effect.IO

import ldbc.fx.Fx

import zio.{ Runtime as ZRuntime, Unsafe, ZIO }

/**
 * Checks how the Fx-bridge overhead behaves as the flatMap chain grows (N = 100 / 1000 / 10000).
 * The bridge is one wrap per run regardless of N, so the per-step interpreter cost is what scales;
 * this verifies the viaFx/native ratio stays stable rather than growing.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class ScaleBench:

  @Param(Array("100", "1000", "10000"))
  var n: Int = 0

  private given IORuntime        = IORuntime.global
  private given ExecutionContext = ExecutionContext.global

  private val zrt = ZRuntime.default
  private def runZ[A](z: ZIO[Any, Throwable, A]): A =
    Unsafe.unsafe(implicit u => zrt.unsafe.run(z).getOrThrowFiberFailure())

  private def fxChain: Fx[Int] =
    var f = Fx.pure(0); var i = 0
    while i < n do { f = f.flatMap(x => Fx.pure(x + 1)); i += 1 }
    f
  private def ioChain: IO[Int] =
    var f = IO.pure(0); var i = 0
    while i < n do { f = f.flatMap(x => IO.pure(x + 1)); i += 1 }
    f
  private def zioChain: ZIO[Any, Throwable, Int] =
    var f: ZIO[Any, Throwable, Int] = ZIO.succeed(0); var i = 0
    while i < n do { f = f.flatMap(x => ZIO.succeed(x + 1)); i += 1 }
    f
  private def futureChain: Future[Int] =
    var f = Future.successful(0); var i = 0
    while i < n do { f = f.flatMap(x => Future.successful(x + 1)); i += 1 }
    f

  @Benchmark def io_native:     Int = ioChain.unsafeRunSync()
  @Benchmark def io_viaFx:      Int = Bridges.toIO(fxChain).unsafeRunSync()
  @Benchmark def zio_native:    Int = runZ(zioChain)
  @Benchmark def zio_viaFx:     Int = runZ(Bridges.toZIO(fxChain))
  @Benchmark def future_native: Int = Await.result(futureChain, 60.seconds)
  @Benchmark def future_viaFx:  Int = Await.result(Bridges.toFuture(fxChain), 60.seconds)
