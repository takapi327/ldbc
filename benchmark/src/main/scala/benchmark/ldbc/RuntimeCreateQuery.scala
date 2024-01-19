/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.ldbc

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import cats.effect.IO

import ldbc.core.*
import ldbc.query.builder.TableQuery

import benchmark.*

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class RuntimeCreateQuery:

  @Benchmark
  def createM1 =
    val table = Table[Model1]("model1")(
      column("c1", INT)
    )
    TableQuery[IO, Model1](table).select(_.c1)

  @Benchmark
  def createM5 =
    val table = Table[Model5]("model5")(
      column("c1", INT),
      column("c2", INT),
      column("c3", INT),
      column("c4", INT),
      column("c5", INT)
    )
    TableQuery[IO, Model5](table).select(v => (v.c1, v.c2, v.c3, v.c4, v.c5))

  @Benchmark
  def createM10 =
    val table = Table[Model10]("model10")(
      column("c1", INT),
      column("c2", INT),
      column("c3", INT),
      column("c4", INT),
      column("c5", INT),
      column("c6", INT),
      column("c7", INT),
      column("c8", INT),
      column("c9", INT),
      column("c10", INT)
    )
    TableQuery[IO, Model10](table).select(v => (v.c1, v.c2, v.c3, v.c4, v.c5, v.c6, v.c7, v.c8, v.c9, v.c10))

  @Benchmark
  def createM20 =
    val table = Table[Model20]("model20")(
      column("c1", INT),
      column("c2", INT),
      column("c3", INT),
      column("c4", INT),
      column("c5", INT),
      column("c6", INT),
      column("c7", INT),
      column("c8", INT),
      column("c9", INT),
      column("c10", INT),
      column("c11", INT),
      column("c12", INT),
      column("c13", INT),
      column("c14", INT),
      column("c15", INT),
      column("c16", INT),
      column("c17", INT),
      column("c18", INT),
      column("c19", INT),
      column("c20", INT)
    )
    TableQuery[IO, Model20](table).select(v =>
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
    val table = Table[Model25]("model25")(
      column("c1", INT),
      column("c2", INT),
      column("c3", INT),
      column("c4", INT),
      column("c5", INT),
      column("c6", INT),
      column("c7", INT),
      column("c8", INT),
      column("c9", INT),
      column("c10", INT),
      column("c11", INT),
      column("c12", INT),
      column("c13", INT),
      column("c14", INT),
      column("c15", INT),
      column("c16", INT),
      column("c17", INT),
      column("c18", INT),
      column("c19", INT),
      column("c20", INT),
      column("c21", INT),
      column("c22", INT),
      column("c23", INT),
      column("c24", INT),
      column("c25", INT)
    )
    TableQuery[IO, Model25](table).select(v =>
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
