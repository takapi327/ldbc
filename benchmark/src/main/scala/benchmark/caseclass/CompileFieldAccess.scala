/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package benchmark.caseclass

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import benchmark.{ Model25, Compiler }

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class CompileFieldAccess:
  @Param(Array("1", "2", "4", "8", "10", "12", "14", "16", "18", "20", "22", "24"))
  var index: Int = 0

  var source: String = ""

  var compiler: Compiler = null

  @Setup(Level.Iteration)
  def setup(): Unit =
    compiler = new Compiler

    source = s"""
         |import benchmark.caseclass.CompileFieldAccess.model
         |object CompileFieldAccess {
         |  model.c$index
         |}
         |""".stripMargin

  @Benchmark
  def accessCN =
    compiler.compile(source)

object CompileFieldAccess:
  val model = Model25(
    c1  = 1,
    c2  = 2,
    c3  = 3,
    c4  = 4,
    c5  = 5,
    c6  = 6,
    c7  = 7,
    c8  = 8,
    c9  = 9,
    c10 = 10,
    c11 = 11,
    c12 = 12,
    c13 = 13,
    c14 = 14,
    c15 = 15,
    c16 = 16,
    c17 = 17,
    c18 = 18,
    c19 = 19,
    c20 = 20,
    c21 = 21,
    c22 = 22,
    c23 = 23,
    c24 = 24,
    c25 = 25
  )
