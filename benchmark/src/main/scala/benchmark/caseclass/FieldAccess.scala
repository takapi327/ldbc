/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package benchmark.caseclass

import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class FieldAccess:

  var model = Model(
    p1 = 1,
    p2 = 2,
    p3 = 3,
    p4 = 4,
    p5 = 5,
    p6 = 6,
    p7 = 7,
    p8 = 8,
    p9 = 9,
    p10 = 10,
    p11 = 11,
    p12 = 12,
    p13 = 13,
    p14 = 14,
    p15 = 15,
    p16 = 16,
    p17 = 17,
    p18 = 18,
    p19 = 19,
    p20 = 20,
    p21 = 21,
    p22 = 22,
    p23 = 23,
    p24 = 24,
    p25 = 25,
  )

  @Benchmark
  def accessP1 = model.p1

  @Benchmark
  def accessP2 = model.p2

  @Benchmark
  def accessP3 = model.p3

  @Benchmark
  def accessP4 = model.p4

  @Benchmark
  def accessP5 = model.p5

  @Benchmark
  def accessP6 = model.p6

  @Benchmark
  def accessP7 = model.p7

  @Benchmark
  def accessP8 = model.p8

  @Benchmark
  def accessP9 = model.p9

  @Benchmark
  def accessP10 = model.p10

  @Benchmark
  def accessP11 = model.p11

  @Benchmark
  def accessP12 = model.p12

  @Benchmark
  def accessP13 = model.p13

  @Benchmark
  def accessP14 = model.p14

  @Benchmark
  def accessP15 = model.p15

  @Benchmark
  def accessP16 = model.p16

  @Benchmark
  def accessP17 = model.p17

  @Benchmark
  def accessP18 = model.p18

  @Benchmark
  def accessP19 = model.p19

  @Benchmark
  def accessP20 = model.p20

  @Benchmark
  def accessP21 = model.p21

  @Benchmark
  def accessP22 = model.p22

  @Benchmark
  def accessP23 = model.p23

  @Benchmark
  def accessP24 = model.p24

  @Benchmark
  def accessP25 = model.p25
