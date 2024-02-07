/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark._slick

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import slick.jdbc.MySQLProfile.api.*

import benchmark.*

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class RuntimeCreate:

  @Benchmark
  def createM1 =
    class Model1Table(tag: Tag) extends Table[Model1](tag, "model1"):
      def c1 = column[Int]("c1")
      def *  = (c1).mapTo[Model1]
    TableQuery[Model1Table]

  @Benchmark
  def createM5 =
    class Model5Table(tag: Tag) extends Table[Model5](tag, "model5"):
      def c1 = column[Int]("c1")
      def c2 = column[Int]("c2")
      def c3 = column[Int]("c3")
      def c4 = column[Int]("c4")
      def c5 = column[Int]("c5")
      def *  = (c1, c2, c3, c4, c5).mapTo[Model5]
    TableQuery[Model5Table]

  @Benchmark
  def createM10 =
    class Model10Table(tag: Tag) extends Table[Model10](tag, "model10"):
      def c1  = column[Int]("c1")
      def c2  = column[Int]("c2")
      def c3  = column[Int]("c3")
      def c4  = column[Int]("c4")
      def c5  = column[Int]("c5")
      def c6  = column[Int]("c6")
      def c7  = column[Int]("c7")
      def c8  = column[Int]("c8")
      def c9  = column[Int]("c9")
      def c10 = column[Int]("c10")
      def *   = (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10).mapTo[Model10]
    TableQuery[Model10Table]

  @Benchmark
  def createM20 =
    class Model20Table(tag: Tag) extends Table[Model20](tag, "model20"):
      def c1  = column[Int]("c1")
      def c2  = column[Int]("c2")
      def c3  = column[Int]("c3")
      def c4  = column[Int]("c4")
      def c5  = column[Int]("c5")
      def c6  = column[Int]("c6")
      def c7  = column[Int]("c7")
      def c8  = column[Int]("c8")
      def c9  = column[Int]("c9")
      def c10 = column[Int]("c10")
      def c11 = column[Int]("c11")
      def c12 = column[Int]("c12")
      def c13 = column[Int]("c13")
      def c14 = column[Int]("c14")
      def c15 = column[Int]("c15")
      def c16 = column[Int]("c16")
      def c17 = column[Int]("c17")
      def c18 = column[Int]("c18")
      def c19 = column[Int]("c19")
      def c20 = column[Int]("c20")
      def * = (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18, c19, c20).mapTo[Model20]
    TableQuery[Model20Table]
