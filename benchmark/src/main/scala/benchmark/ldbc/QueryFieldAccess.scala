/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package benchmark.ldbc

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import cats.effect.IO

import ldbc.core.*
import ldbc.query.builder.TableQuery

import benchmark.Model25

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class QueryFieldAccess:

  var table = Table[Model25]("model25")(
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
    column("p25", INT)
  )

  var query = TableQuery[IO, Model25](table)

  @Benchmark
  def accessC1 = query.c1

  @Benchmark
  def accessC2 = query.c2

  @Benchmark
  def accessC3 = query.c3

  @Benchmark
  def accessC4 = query.c4

  @Benchmark
  def accessC5 = query.c5

  @Benchmark
  def accessC6 = query.c6

  @Benchmark
  def accessC7 = query.c7

  @Benchmark
  def accessC8 = query.c8

  @Benchmark
  def accessC9 = query.c9

  @Benchmark
  def accessC10 = query.c10

  @Benchmark
  def accessC11 = query.c11

  @Benchmark
  def accessC12 = query.c12

  @Benchmark
  def accessC13 = query.c13

  @Benchmark
  def accessC14 = query.c14

  @Benchmark
  def accessC15 = query.c15

  @Benchmark
  def accessC16 = query.c16

  @Benchmark
  def accessC17 = query.c17

  @Benchmark
  def accessC18 = query.c18

  @Benchmark
  def accessC19 = query.c19

  @Benchmark
  def accessC20 = query.c20

  @Benchmark
  def accessC21 = query.c21

  @Benchmark
  def accessC22 = query.c22

  @Benchmark
  def accessC23 = query.c23

  @Benchmark
  def accessC24 = query.c24

  @Benchmark
  def accessC25 = query.c25
