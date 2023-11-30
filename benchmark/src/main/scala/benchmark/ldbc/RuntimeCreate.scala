/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package benchmark.ldbc

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import ldbc.core.*

import benchmark.*

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class Creation:

  @Benchmark
  def createM1 = Table[Model1]("model1")(
    column("c1", INT)
  )

  @Benchmark
  def createM5 = Table[Model5]("model5")(
    column("c1", INT),
    column("c2", INT),
    column("c3", INT),
    column("c4", INT),
    column("c5", INT)
  )

  @Benchmark
  def createM10 = Table[Model10]("model10")(
    column("c1", INT),
    column("c2", INT),
    column("c3", INT),
    column("c4", INT),
    column("c5", INT),
    column("c6", INT),
    column("c7", INT),
    column("c8", INT),
    column("c9", INT),
    column("c10", INT)
  )

  @Benchmark
  def createM20 = Table[Model20]("model20")(
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
    column("c20", INT)
  )

  @Benchmark
  def createM25 = Table[Model25]("model25")(
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
