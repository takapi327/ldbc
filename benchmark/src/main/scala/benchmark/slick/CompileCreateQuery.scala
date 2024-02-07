/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark._slick

import benchmark.Compiler
import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class CompileCreateQuery:

  @Param(Array("1", "5", "10", "20", "25"))
  var size: Int = 0

  var source: String = ""

  var compiler: Compiler = _

  @Setup(Level.Iteration)
  def setup(): Unit =
    compiler = new Compiler

    val columns = (1 to size).map(i => s"def c$i = column[Int](\"c$i\")").mkString("\n  ")
    val *       = (1 to size).map(i => s"c$i").mkString(", ")

    source = s"""
         |import slick.jdbc.MySQLProfile.api.*
         |import benchmark.Model$size
         |class Model${ size }Table(tag: Tag) extends Table[Model$size](tag, "model$size"):
         |  $columns
         |  def * = (${ * }).mapTo[Model$size]
         |
         |val tableQuery = TableQuery[Model${ size }Table]
         |val query = tableQuery.map(v => (${ (1 to size).map(i => s"v.c$i").mkString(", ") })).take(5000)
         |""".stripMargin

  @Benchmark
  def createQuery = compiler.compile(source)
