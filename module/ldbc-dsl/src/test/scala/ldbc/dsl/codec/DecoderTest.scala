/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import munit.CatsEffectSuite

import ldbc.sql.{ ResultSet, ResultSetMetaData }
import scala.reflect.Enum
import cats.syntax.all.*

class DecoderTest extends CatsEffectSuite:

  // Mock ResultSet for testing with minimum implementations needed
  private class MockResultSet extends ResultSet:
    private var nullFlag = false
    private var stringValue = ""
    private var longValue = 0L

    def setStringValue(value: String): Unit = stringValue = value
    def setLongValue(value: Long): Unit = longValue = value
    def setNull(isNull: Boolean): Unit = nullFlag = isNull

    override def getString(columnIndex: Int): String = stringValue
    override def getString(columnLabel: String): String = stringValue
    
    override def getLong(columnIndex: Int): Long = longValue
    override def getLong(columnLabel: String): Long = longValue
    
    override def wasNull(): Boolean = nullFlag
    
    // Implement the minimum required methods with default values
    override def absolute(row: Int): Boolean = false
    override def afterLast(): Unit = {}
    override def beforeFirst(): Unit = {}
    override def close(): Unit = {}
    override def first(): Boolean = false
    override def getBigDecimal(columnIndex: Int): BigDecimal = BigDecimal(0)
    override def getBigDecimal(columnLabel: String): BigDecimal = BigDecimal(0)
    override def getBoolean(columnIndex: Int): Boolean = false
    override def getBoolean(columnLabel: String): Boolean = false
    override def getByte(columnIndex: Int): Byte = 0
    override def getByte(columnLabel: String): Byte = 0
    override def getBytes(columnIndex: Int): Array[Byte] = Array.empty
    override def getBytes(columnLabel: String): Array[Byte] = Array.empty
    override def getConcurrency(): Int = 0
    override def getDate(columnIndex: Int): java.time.LocalDate = java.time.LocalDate.now
    override def getDate(columnLabel: String): java.time.LocalDate = java.time.LocalDate.now
    override def getDouble(columnIndex: Int): Double = 0.0
    override def getDouble(columnLabel: String): Double = 0.0
    override def getFloat(columnIndex: Int): Float = 0.0f
    override def getFloat(columnLabel: String): Float = 0.0f
    override def getInt(columnIndex: Int): Int = 0
    override def getInt(columnLabel: String): Int = 0
    override def getMetaData(): ResultSetMetaData = null
    override def getRow(): Int = 0
    override def getShort(columnIndex: Int): Short = 0
    override def getShort(columnLabel: String): Short = 0
    override def getTime(columnIndex: Int): java.time.LocalTime = java.time.LocalTime.now
    override def getTime(columnLabel: String): java.time.LocalTime = java.time.LocalTime.now
    override def getTimestamp(columnIndex: Int): java.time.LocalDateTime = java.time.LocalDateTime.now
    override def getTimestamp(columnLabel: String): java.time.LocalDateTime = java.time.LocalDateTime.now
    override def getType(): Int = 0
    override def isAfterLast(): Boolean = false
    override def isBeforeFirst(): Boolean = false
    override def isFirst(): Boolean = false
    override def isLast(): Boolean = false
    override def last(): Boolean = false
    override def next(): Boolean = false
    override def previous(): Boolean = false
    override def relative(rows: Int): Boolean = false

  // Create simple string and long decoders for testing
  private val stringDecoder = new Decoder[String]:
    override def decode(resultSet: ResultSet, index: Int): Either[Decoder.Error, String] =
      Right(resultSet.getString(index))

  private val longDecoder = new Decoder[Long]:
    override def decode(resultSet: ResultSet, index: Int): Either[Decoder.Error, Long] =
      Right(resultSet.getLong(index))

  test("Decoder map should transform the output") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setLongValue(123L)
    
    val stringifiedLong = longDecoder.map(_.toString)
    assertEquals(stringifiedLong.decode(mockResultSet, 1), Right("123"))
  }

  test("Decoder emap should transform with possible failures") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("123")
    
    val successDecoder = stringDecoder.emap(s => Right(s.toInt))
    val failureDecoder = stringDecoder.emap(s => Left(s"Invalid value: $s"))
    
    assertEquals(successDecoder.decode(mockResultSet, 1), Right(123))
    assertEquals(
      failureDecoder.decode(mockResultSet, 1).left.map(_.message),
      Left("Invalid value: 123")
    )
  }

  test("Decoder product should combine two decoders") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("test")
    mockResultSet.setLongValue(123L)
    
    val tupleDecoder = stringDecoder.product(longDecoder)
    assertEquals(tupleDecoder.decode(mockResultSet, 1), Right(("test", 123L)))
  }

  test("Decoder opt should handle null values") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("test")
    
    // Test non-null case
    mockResultSet.setNull(false)
    val optDecoder = stringDecoder.opt
    assertEquals(optDecoder.decode(mockResultSet, 1), Right(Some("test")))
    
    // Test null case
    mockResultSet.setNull(true)
    assertEquals(optDecoder.decode(mockResultSet, 1), Right(None))
  }

  test("Decoder derived should work with case classes") {
    case class User(id: Long, name: String)
    given Decoder[User] = Decoder.derived[User]
    
    val mockResultSet = new MockResultSet()
    mockResultSet.setLongValue(1L)
    mockResultSet.setStringValue("test")
    
    val userDecoder = Decoder[User]
    // Note: This test is somewhat artificial since our mock doesn't properly handle multiple columns
    // In a real implementation, we'd need to test with a more sophisticated mock
    assertEquals(
      userDecoder.decode(mockResultSet, 1).map(_.id),
      Right(1L)
    )
  }

  test("Decoder should compose with Applicative") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("42")
    
    val pureDecoder = cats.Applicative[Decoder].pure(123)
    assertEquals(pureDecoder.decode(mockResultSet, 1), Right(123))
    
    val mappedDecoder = pureDecoder.map(_ + 1)
    assertEquals(mappedDecoder.decode(mockResultSet, 1), Right(124))
  }

  test("Decoder for tuples should work with predefined instances") {
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("test")
    mockResultSet.setLongValue(123L)
    
    val tupleDecoder = Decoder[(String, Long)]
    assertEquals(tupleDecoder.decode(mockResultSet, 1), Right(("test", 123L)))
  }

  // If your codebase includes enums, test the enum decoder
  test("Decoder should work with enums") {
    enum TestEnum:
      case First, Second, Third
    
    val mockResultSet = new MockResultSet()
    mockResultSet.setStringValue("First")
    
    given Decoder[TestEnum] = Decoder.derivedEnum[TestEnum]
    val enumDecoder = Decoder[TestEnum]
    
    assertEquals(enumDecoder.decode(mockResultSet, 1).map(_.toString), Right("First"))
  }
