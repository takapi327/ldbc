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

    source = s"""
         |import ldbc.query.builder.Table
         |import benchmark.Model$size
         |val tableQuery = Table[Model$size]
         |
         |val query = tableQuery.select(v => (${ (1 to size).map(i => s"v.c$i").mkString(", ") }))
         |  .limit(5000)
         |""".stripMargin

  @Benchmark
  def createQuery = compiler.compile(source)
