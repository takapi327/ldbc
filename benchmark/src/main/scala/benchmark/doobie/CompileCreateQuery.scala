/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package benchmark.doobie

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import benchmark.Compiler

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

    source = s"""
         |import doobie.*
         |import doobie.implicits.*
         |
         |val len = 5000
         |val query = sql"SELECT ${ (1 to size).map(i => s"c$i").mkString(", ") } FROM model$size LIMIT $$len"
         |  .query[(${ (1 to size).map(_ => "Int").mkString(", ") })]
         |""".stripMargin

  @Benchmark
  def createQuery = compiler.compile(source)
