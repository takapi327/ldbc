/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package benchmark.caseclass

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import benchmark.*

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class Creation:

  @Benchmark
  def createM1 = Model1(
    c1 = 1,
  )

  @Benchmark
  def createM5 = Model5(
    c1 = 1,
    c2 = 2,
    c3 = 3,
    c4 = 4,
    c5 = 5,
  )

  @Benchmark
  def createM10 = Model10(
    c1 = 1,
    c2 = 2,
    c3 = 3,
    c4 = 4,
    c5 = 5,
    c6 = 6,
    c7 = 7,
    c8 = 8,
    c9 = 9,
    c10 = 10,
  )

  @Benchmark
  def createM20 = Model20(
    c1 = 1,
    c2 = 2,
    c3 = 3,
    c4 = 4,
    c5 = 5,
    c6 = 6,
    c7 = 7,
    c8 = 8,
    c9 = 9,
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
  )

  @Benchmark
  def createM25 = Model25(
    c1 = 1,
    c2 = 2,
    c3 = 3,
    c4 = 4,
    c5 = 5,
    c6 = 6,
    c7 = 7,
    c8 = 8,
    c9 = 9,
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
    c25 = 25,
  )
