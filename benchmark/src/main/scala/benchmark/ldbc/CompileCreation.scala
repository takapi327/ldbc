/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package benchmark.ldbc

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

  var compiler: Compiler = null

  @Setup(Level.Iteration)
  def setup(): Unit =
    compiler = new Compiler

    val columns = (1 to size).map(i => s"column(\"c$i\", INT)").mkString(",\n")

    source =
      s"""
         |import ldbc.core.*
         |import benchmark.Model$size
         |val table = Table[Model$size](\"model$size\")(
         |  $columns
         |)
         |""".stripMargin

  @Benchmark
  def createTableN = compiler.compile(source)
