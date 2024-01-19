/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.doobie

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import doobie.*
import doobie.implicits.*

import benchmark.*

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class RuntimeCreateQuery:

  @Benchmark
  def createM1 = sql"SELECT c1 FROM model1".query[Int]

  @Benchmark
  def createM5 = sql"SELECT c1, c2, c3, c4, c5 FROM model5".query[(Int, Int, Int, Int, Int)]

  @Benchmark
  def createM10 = sql"SELECT c1, c2, c3, c4, c5, c6, c7, c8, c9, c10 FROM model10"
    .query[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)]

  @Benchmark
  def createM20 =
    sql"SELECT c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18, c19, c20 FROM model20"
      .query[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)]

  @Benchmark
  def createM25 =
    sql"SELECT c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18, c19, c20, c21, c22, c23, c24, c25 FROM model25"
      .query[
        (
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int
        )
      ]
