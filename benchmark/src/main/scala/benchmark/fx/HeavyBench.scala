/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.fx

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.*

import org.openjdk.jmh.annotations.*

import cats.effect.unsafe.IORuntime
import cats.effect.IO

import ldbc.fx.Fx

import zio.{ Runtime as ZRuntime, Unsafe, ZIO }

/**
 * Checks whether the Fx-bridge overhead becomes pronounced when a single operation is heavy (CPU)
 * or time-consuming (~1ms latency). Expectation: no — the operation dominates, so the viaFx/native
 * ratio approaches 1.0.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class HeavyBench:

  private given IORuntime = IORuntime.global

  private val zrt = ZRuntime.default
  private def runZ[A](z: ZIO[Any, Throwable, A]): A =
    Unsafe.unsafe(implicit u => zrt.unsafe.run(z).getOrThrowFiberFailure())

  private def heavy(): Long =
    def fib(k: Int): Long = if k < 2 then k.toLong else fib(k - 1) + fib(k - 2)
    fib(30)

  @Benchmark def io_heavyCompute_native: Long = IO(heavy()).unsafeRunSync()
  @Benchmark def io_heavyCompute_viaFx:  Long = Bridges.toIO(Fx.delay(heavy())).unsafeRunSync()

  @Benchmark def zio_heavyCompute_native: Long = runZ(ZIO.attempt(heavy()))
  @Benchmark def zio_heavyCompute_viaFx:  Long = runZ(Bridges.toZIO(Fx.delay(heavy())))

  @Benchmark def io_sleep1ms_native: Unit = IO.sleep(1.milli).unsafeRunSync()
  @Benchmark def io_sleep1ms_viaFx:  Unit = Bridges.toIO(Fx.sleep(1.milli)).unsafeRunSync()

  @Benchmark def zio_sleep1ms_native: Unit = runZ(ZIO.sleep(zio.Duration.fromMillis(1)))
  @Benchmark def zio_sleep1ms_viaFx:  Unit = runZ(Bridges.toZIO(Fx.sleep(1.milli)))
