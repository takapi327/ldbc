/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.caseclass

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import benchmark.Compiler

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class CompileCreate:

  @Param(Array("1", "5", "10", "20", "25"))
  var size: Int = 0

  var source: String = ""

  var compiler: Compiler = uninitialized

  @Setup(Level.Iteration)
  def setup(): Unit =
    compiler = new Compiler

    val properties = (1 to size).map(i => s"c$i: Int").mkString(",\n")

    source = s"""
         |case class Table$size(
         |  $properties
         |)
         |""".stripMargin

  @Benchmark
  def createCaseClassN = compiler.compile(source)
