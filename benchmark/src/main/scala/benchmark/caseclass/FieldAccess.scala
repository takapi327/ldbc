/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package benchmark.caseclass

import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit

import benchmark.Model25

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class FieldAccess:

  var model = Model25(
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

  @Benchmark
  def accessC1 = model.c1

  @Benchmark
  def accessC2 = model.c2

  @Benchmark
  def accessC3 = model.c3

  @Benchmark
  def accessC4 = model.c4

  @Benchmark
  def accessC5 = model.c5

  @Benchmark
  def accessC6 = model.c6

  @Benchmark
  def accessC7 = model.c7

  @Benchmark
  def accessC8 = model.c8

  @Benchmark
  def accessC9 = model.c9

  @Benchmark
  def accessC10 = model.c10

  @Benchmark
  def accessC11 = model.c11

  @Benchmark
  def accessC12 = model.c12

  @Benchmark
  def accessC13 = model.c13

  @Benchmark
  def accessC14 = model.c14

  @Benchmark
  def accessC15 = model.c15

  @Benchmark
  def accessC16 = model.c16

  @Benchmark
  def accessC17 = model.c17

  @Benchmark
  def accessC18 = model.c18

  @Benchmark
  def accessC19 = model.c19

  @Benchmark
  def accessC20 = model.c20

  @Benchmark
  def accessC21 = model.c21

  @Benchmark
  def accessC22 = model.c22

  @Benchmark
  def accessC23 = model.c23

  @Benchmark
  def accessC24 = model.c24

  @Benchmark
  def accessC25 = model.c25
