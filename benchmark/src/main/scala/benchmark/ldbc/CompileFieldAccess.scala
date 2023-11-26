/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package benchmark.ldbc

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import ldbc.core.*

import benchmark.{ Compiler, Model25 }

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
         |import benchmark.ldbc.CompileFieldAccess.table
         |object CompileFieldAccess {
         |  table.c$index
         |}
         |""".stripMargin

  @Benchmark
  def accessCN =
    compiler.compile(source)

object CompileFieldAccess:
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
