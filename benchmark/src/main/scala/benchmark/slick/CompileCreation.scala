/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package benchmark.slick

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import benchmark.Compiler

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class CompileCreation:

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
         |""".stripMargin

  @Benchmark
  def createTableN = compiler.compile(source)
