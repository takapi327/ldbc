/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.ldbc

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import ldbc.query.builder.Table

import benchmark.*

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class RuntimeCreateQuery:

  @Benchmark
  def createM1 =
    Table[Model1].select(_.c1)

  @Benchmark
  def createM5 =
    Table[Model5].select(v => (v.c1, v.c2, v.c3, v.c4, v.c5))

  @Benchmark
  def createM10 =
    Table[Model10].select(v => (v.c1, v.c2, v.c3, v.c4, v.c5, v.c6, v.c7, v.c8, v.c9, v.c10))

  @Benchmark
  def createM20 =
    Table[Model20].select(v =>
      (
        v.c1,
        v.c2,
        v.c3,
        v.c4,
        v.c5,
        v.c6,
        v.c7,
        v.c8,
        v.c9,
        v.c10,
        v.c11,
        v.c12,
        v.c13,
        v.c14,
        v.c15,
        v.c16,
        v.c17,
        v.c18,
        v.c19,
        v.c20
      )
    )

  @Benchmark
  def createM25 =
    Table[Model25].select(v =>
      (
        v.c1,
        v.c2,
        v.c3,
        v.c4,
        v.c5,
        v.c6,
        v.c7,
        v.c8,
        v.c9,
        v.c10,
        v.c11,
        v.c12,
        v.c13,
        v.c14,
        v.c15,
        v.c16,
        v.c17,
        v.c18,
        v.c19,
        v.c20,
        v.c21,
        v.c22,
        v.c23,
        v.c24,
        v.c25
      )
    )
