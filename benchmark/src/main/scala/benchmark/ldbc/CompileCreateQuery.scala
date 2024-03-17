/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.ldbc

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import benchmark.Compiler

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class CompileCreateQuery:

  @Param(Array("1", "5", "10", "20", "25"))
  var size: Int = 0

  var source: String = ""

  var compiler: Compiler = uninitialized

  @Setup(Level.Iteration)
  def setup(): Unit =
    compiler = new Compiler

    val columns = (1 to size).map(i => s"column(\"c$i\", INT)").mkString(",\n")

    source = s"""
         |import cats.effect.IO
         |import ldbc.core.*
         |import ldbc.query.builder.TableQuery
         |import benchmark.Model$size
         |val table = Table[Model$size](\"model$size\")(
         |  $columns
         |)
         |val tableQuery = TableQuery[IO, Model$size](table)
         |
         |val query = tableQuery.select(v => (${ (1 to size).map(i => s"v.c$i").mkString(", ") }))
         |  .limit(5000)
         |""".stripMargin

  @Benchmark
  def createQuery = compiler.compile(source)
