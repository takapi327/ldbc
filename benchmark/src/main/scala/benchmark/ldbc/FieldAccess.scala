/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package benchmark.ldbc

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import ldbc.core.*

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class FieldAccess:

  var table = Table[Table25]("table25")(
    column("p1", INT),
    column("p2", INT),
    column("p3", INT),
    column("p4", INT),
    column("p5", INT),
    column("p6", INT),
    column("p7", INT),
    column("p8", INT),
    column("p9", INT),
    column("p10", INT),
    column("p11", INT),
    column("p12", INT),
    column("p13", INT),
    column("p14", INT),
    column("p15", INT),
    column("p16", INT),
    column("p17", INT),
    column("p18", INT),
    column("p19", INT),
    column("p20", INT),
    column("p21", INT),
    column("p22", INT),
    column("p23", INT),
    column("p24", INT),
    column("p25", INT),
  )

  @Benchmark
  def accessC1 = table.c1

  @Benchmark
  def accessC2 = table.c2

  @Benchmark
  def accessC3 = table.c3

  @Benchmark
  def accessC4 = table.c4

  @Benchmark
  def accessC5 = table.c5

  @Benchmark
  def accessC6 = table.c6

  @Benchmark
  def accessC7 = table.c7

  @Benchmark
  def accessC8 = table.c8

  @Benchmark
  def accessC9 = table.c9

  @Benchmark
  def accessC10 = table.c10

  @Benchmark
  def accessC11 = table.c11

  @Benchmark
  def accessC12 = table.c12

  @Benchmark
  def accessC13 = table.c13

  @Benchmark
  def accessC14 = table.c14

  @Benchmark
  def accessC15 = table.c15

  @Benchmark
  def accessC16 = table.c16

  @Benchmark
  def accessC17 = table.c17

  @Benchmark
  def accessC18 = table.c18

  @Benchmark
  def accessC19 = table.c19

  @Benchmark
  def accessC20 = table.c20

  @Benchmark
  def accessC21 = table.c21

  @Benchmark
  def accessC22 = table.c22

  @Benchmark
  def accessC23 = table.c23

  @Benchmark
  def accessC24 = table.c24

  @Benchmark
  def accessC25 = table.c25
