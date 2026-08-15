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

import cats.effect.IO
import cats.effect.unsafe.IORuntime

import zio.{ Runtime as ZRuntime, Unsafe, ZIO }

import ldbc.fx.Fx

/**
 * Compares "run via the Fx bridge" (`toIO` / `toZIO` / `toFuture`) against native cats-effect / ZIO /
 * Future, to measure the conversion overhead. Two shapes: a chain of N flatMaps over pure values
 * (interpreter/bridge cost) and a single `async` completion (async-bridge cost).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class BridgeBench:

  val N = 100

  private given IORuntime        = IORuntime.global
  private given ExecutionContext = ExecutionContext.global

  private val zrt = ZRuntime.default
  private def runZ[A](z: ZIO[Any, Throwable, A]): A =
    Unsafe.unsafe(implicit u => zrt.unsafe.run(z).getOrThrowFiberFailure())

  private def fxChain: Fx[Int] =
    var f = Fx.pure(0); var i = 0
    while i < N do { f = f.flatMap(x => Fx.pure(x + 1)); i += 1 }
    f
  private def ioChain: IO[Int] =
    var f = IO.pure(0); var i = 0
    while i < N do { f = f.flatMap(x => IO.pure(x + 1)); i += 1 }
    f
  private def zioChain: ZIO[Any, Throwable, Int] =
    var f: ZIO[Any, Throwable, Int] = ZIO.succeed(0); var i = 0
    while i < N do { f = f.flatMap(x => ZIO.succeed(x + 1)); i += 1 }
    f
  private def futureChain: Future[Int] =
    var f = Future.successful(0); var i = 0
    while i < N do { f = f.flatMap(x => Future.successful(x + 1)); i += 1 }
    f

  @Benchmark def io_syncChain_native: Int = ioChain.unsafeRunSync()
  @Benchmark def io_syncChain_viaFx: Int  = Bridges.toIO(fxChain).unsafeRunSync()

  @Benchmark def zio_syncChain_native: Int = runZ(zioChain)
  @Benchmark def zio_syncChain_viaFx: Int  = runZ(Bridges.toZIO(fxChain))

  @Benchmark def future_syncChain_native: Int = Await.result(futureChain, 10.seconds)
  @Benchmark def future_syncChain_viaFx: Int  = Await.result(Bridges.toFuture(fxChain), 10.seconds)

  @Benchmark def io_asyncOnce_native: Int = IO.async_[Int](cb => cb(Right(1))).unsafeRunSync()
  @Benchmark def io_asyncOnce_viaFx: Int  =
    Bridges.toIO(Fx.async[Int] { cb => cb(Right(1)); Fx.Canceler.noop }).unsafeRunSync()

  @Benchmark def zio_asyncOnce_native: Int = runZ(ZIO.async[Any, Throwable, Int](k => k(ZIO.succeed(1))))
  @Benchmark def zio_asyncOnce_viaFx: Int  =
    runZ(Bridges.toZIO(Fx.async[Int] { cb => cb(Right(1)); Fx.Canceler.noop }))
