/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import scala.reflect.Enum

import cats.syntax.all.*

import cats.effect.IO

import munit.CatsEffectSuite

import ldbc.sql.{ ResultSet, ResultSetMetaData }
import ldbc.dsl.free.ResultSetIO
import ldbc.dsl.free.ResultSetIO.*
import ldbc.dsl.exception.DecodeFailureException

class DecoderTest extends CatsEffectSuite:

  // Mock ResultSet for testing with minimum implementations needed
  private class MockResultSet extends ResultSet[IO]:
    private var nullFlag    = false
    private var stringValue = ""
    private var longValue   = 0L

    def setStringValue(value: String):  Unit = stringValue = value
    def setLongValue(value:   Long):    Unit = longValue   = value
    def setNull(isNull:       Boolean): Unit = nullFlag    = isNull

    override def getString(columnIndex: Int):    IO[String] = IO(stringValue)
    override def getString(columnLabel: String): IO[String] = IO(stringValue)

    override def getLong(columnIndex: Int):    IO[Long] = IO(longValue)
    override def getLong(columnLabel: String): IO[Long] = IO(longValue)

    override def wasNull(): IO[Boolean] = IO(nullFlag)

    // Implement the minimum required methods with default values
    override def absolute(row:              Int):    IO[Boolean]                 = IO(false)
    override def afterLast():                        IO[Unit]                    = IO.unit
    override def beforeFirst():                      IO[Unit]                    = IO.unit
    override def close():                            IO[Unit]                    = IO.unit
    override def first():                            IO[Boolean]                 = IO(false)
    override def getBigDecimal(columnIndex: Int):    IO[BigDecimal]              = IO(BigDecimal(0))
    override def getBigDecimal(columnLabel: String): IO[BigDecimal]              = IO(BigDecimal(0))
    override def getBoolean(columnIndex:    Int):    IO[Boolean]                 = IO(false)
    override def getBoolean(columnLabel:    String): IO[Boolean]                 = IO(false)
    override def getByte(columnIndex:       Int):    IO[Byte]                    = IO(0)
    override def getByte(columnLabel:       String): IO[Byte]                    = IO(0)
    override def getBytes(columnIndex:      Int):    IO[Array[Byte]]             = IO(Array.empty)
    override def getBytes(columnLabel:      String): IO[Array[Byte]]             = IO(Array.empty)
    override def getConcurrency():                   IO[Int]                     = IO(0)
    override def getDate(columnIndex:       Int):    IO[java.time.LocalDate]     = IO(java.time.LocalDate.now)
    override def getDate(columnLabel:       String): IO[java.time.LocalDate]     = IO(java.time.LocalDate.now)
    override def getDouble(columnIndex:     Int):    IO[Double]                  = IO(0.0)
    override def getDouble(columnLabel:     String): IO[Double]                  = IO(0.0)
    override def getFloat(columnIndex:      Int):    IO[Float]                   = IO(0.0f)
    override def getFloat(columnLabel:      String): IO[Float]                   = IO(0.0f)
    override def getInt(columnIndex:        Int):    IO[Int]                     = IO(0)
    override def getInt(columnLabel:        String): IO[Int]                     = IO(0)
    override def getMetaData():                      IO[ResultSetMetaData]       = IO(null)
    override def getRow():                           IO[Int]                     = IO(0)
    override def getShort(columnIndex:      Int):    IO[Short]                   = IO(0)
    override def getShort(columnLabel:      String): IO[Short]                   = IO(0)
    override def getTime(columnIndex:       Int):    IO[java.time.LocalTime]     = IO(java.time.LocalTime.now)
    override def getTime(columnLabel:       String): IO[java.time.LocalTime]     = IO(java.time.LocalTime.now)
    override def getTimestamp(columnIndex:  Int):    IO[java.time.LocalDateTime] = IO(java.time.LocalDateTime.now)
    override def getTimestamp(columnLabel:  String): IO[java.time.LocalDateTime] = IO(java.time.LocalDateTime.now)
    override def getType():                          IO[Int]                     = IO(0)
    override def isAfterLast():                      IO[Boolean]                 = IO(false)
    override def isBeforeFirst():                    IO[Boolean]                 = IO(false)
    override def isFirst():                          IO[Boolean]                 = IO(false)
    override def isLast():                           IO[Boolean]                 = IO(false)
    override def last():                             IO[Boolean]                 = IO(false)
    override def next():                             IO[Boolean]                 = IO(false)
    override def previous():                         IO[Boolean]                 = IO(false)
    override def relative(rows:             Int):    IO[Boolean]                 = IO(false)

  // Create simple string and long decoders for testing
  private val stringDecoder = new Decoder[String]:
    override def decode(index: Int, statement: String): ResultSetIO[String] =
      ResultSetIO.getString(index)

  private val longDecoder = new Decoder[Long]:
    override def decode(index: Int, statement: String): ResultSetIO[Long] =
      ResultSetIO.getLong(index)

  test("Decoder map should transform the output") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setLongValue(123L)

    val stringifiedLong = longDecoder.map(_.toString)
    assertIO(
      stringifiedLong.decode(1, "empty statement").foldMap(mockResultSet.interpreter),
      "123"
    )
  }

  test("Decoder emap should transform with possible failures") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("123")

    val successDecoder = stringDecoder.emap(s => Right(s.toInt))
    assertIO(
      successDecoder.decode(1, "empty statement").foldMap(mockResultSet.interpreter),
      123
    )

    val failureDecoder = stringDecoder.emap(s => Left(s"Invalid value: $s"))
    interceptIO[DecodeFailureException](
      failureDecoder.decode(1, "empty statement").foldMap(mockResultSet.interpreter)
    )
  }

  test("Decoder product should combine two decoders") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("test")
    mockResultSet.setLongValue(123L)

    val tupleDecoder = stringDecoder.product(longDecoder)
    assertIO(
      tupleDecoder.decode(1, "empty statement").foldMap(mockResultSet.interpreter),
      ("test", 123L)
    )
  }

  test("Decoder opt should handle null values") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("test")

    // Test non-null case
    mockResultSet.setNull(false)
    val optDecoder = stringDecoder.opt
    assertIO(
      optDecoder.decode(1, "empty statement").foldMap(mockResultSet.interpreter),
      Some("test")
    )

    // Test null case
    mockResultSet.setNull(true)
    mockResultSet.setStringValue(null)
    assertIO(
      optDecoder.decode(1, "empty statement").foldMap(mockResultSet.interpreter),
      None
    )
  }

  test("Decoder derived should work with case classes") {
    case class User(id: Long, name: String)
    given Decoder[User] = Decoder.derived[User]

    val mockResultSet = new MockResultSet()
    mockResultSet.setLongValue(1L)
    mockResultSet.setStringValue("test")

    val userDecoder = Decoder[User]
    assertIO(
      userDecoder.decode(1, "empty statement").foldMap(mockResultSet.interpreter).map(_.id),
      1L
    )
  }

  test("Decoder should compose with Applicative") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("42")

    val pureDecoder = cats.Applicative[Decoder].pure(123)
    assertIO(
      pureDecoder.decode(1, "empty statement").foldMap(mockResultSet.interpreter),
      123
    )

    val mappedDecoder = pureDecoder.map(_ + 1)
    assertIO(
      mappedDecoder.decode(1, "empty statement").foldMap(mockResultSet.interpreter),
      124
    )
  }

  test("Decoder for tuples should work with predefined instances") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("test")
    mockResultSet.setLongValue(123L)

    val tupleDecoder = Decoder[(String, Long)]
    assertIO(
      tupleDecoder.decode(1, "empty statement").foldMap(mockResultSet.interpreter),
      ("test", 123L)
    )
  }

  // If your codebase includes enums, test the enum decoder
  test("Decoder should work with enums") {
    enum TestEnum:
      case First, Second, Third

    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("First")

    given Decoder[TestEnum] = Decoder.derivedEnum[TestEnum]
    val enumDecoder         = Decoder[TestEnum]

    assertIO(
      enumDecoder.decode(1, "empty statement").foldMap(mockResultSet.interpreter).map(_.toString),
      "First"
    )
  }
