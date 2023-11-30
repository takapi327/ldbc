/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package benchmark.slick

import benchmark.Compiler
import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class CompileQuery:

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
