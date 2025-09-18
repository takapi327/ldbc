/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import cats.effect.*

import ldbc.sql.ResultSet

import ldbc.connector.data.*
import ldbc.connector.net.Protocol
import ldbc.connector.net.packet.response.*
import ldbc.connector.util.Version

class SharedResultSetTest extends FTestPlatform:

  // Test implementation of SharedResultSet
  class TestSharedResultSet[F[_]](
    mockProtocol:             Protocol[F],
    val columns:              Vector[ColumnDefinitionPacket],
    val records:              Vector[ResultSetRowPacket],
    val serverVariables:      Map[String, String],
    val version:              Version,
    val isClosed:             Ref[F, Boolean],
    val fetchSize:            Ref[F, Int],
    val useCursorFetch:       Boolean = false,
    val useServerPrepStmts:   Boolean = false,
    val resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
    val resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY,
    val statement:            Option[String] = None
  )(using F: Async[F]) extends SharedResultSet[F]:
    // Mock protocol for testing
    val protocol: Protocol[F] = mockProtocol
    
    // Implement the missing abstract method
    override def next(): F[Boolean] = F.delay {
      if currentCursor < records.length then
        currentCursor = currentCursor + 1
        currentRow = records.lift(currentCursor - 1)
        true
      else
        false
    }

  // Helper to create test data
  def createTestColumns: Vector[ColumnDefinitionPacket] = Vector(
    ColumnDefinition41Packet(
      catalog = "def",
      schema = "test_db",
      table = "test_table",
      orgTable = "test_table",
      name = "id",
      orgName = "id",
      length = 12,
      characterSet = 33,
      columnLength = 11,
      columnType = ColumnDataType.MYSQL_TYPE_LONG,
      flags = Seq.empty,
      decimals = 0
    ),
    ColumnDefinition41Packet(
      catalog = "def",
      schema = "test_db",
      table = "test_table",
      orgTable = "test_table",
      name = "name",
      orgName = "name",
      length = 12,
      characterSet = 33,
      columnLength = 255,
      columnType = ColumnDataType.MYSQL_TYPE_VARCHAR,
      flags = Seq.empty,
      decimals = 0
    ),
    ColumnDefinition41Packet(
      catalog = "def",
      schema = "test_db",
      table = "test_table",
      orgTable = "test_table",
      name = "age",
      orgName = "age",
      length = 12,
      characterSet = 33,
      columnLength = 11,
      columnType = ColumnDataType.MYSQL_TYPE_LONG,
      flags = Seq.empty,
      decimals = 0
    ),
    ColumnDefinition41Packet(
      catalog = "def",
      schema = "test_db",
      table = "test_table",
      orgTable = "test_table",
      name = "active",
      orgName = "active",
      length = 12,
      characterSet = 33,
      columnLength = 1,
      columnType = ColumnDataType.MYSQL_TYPE_TINY,
      flags = Seq.empty,
      decimals = 0
    ),
    ColumnDefinition41Packet(
      catalog = "def",
      schema = "test_db",
      table = "test_table",
      orgTable = "test_table",
      name = "created_at",
      orgName = "created_at",
      length = 12,
      characterSet = 33,
      columnLength = 19,
      columnType = ColumnDataType.MYSQL_TYPE_TIMESTAMP,
      flags = Seq.empty,
      decimals = 0
    )
  )

  def createTestRecords: Vector[ResultSetRowPacket] = Vector(
    ResultSetRowPacket(Array(Some("1"), Some("Alice"), Some("25"), Some("1"), Some("2023-01-01 10:00:00"))),
    ResultSetRowPacket(Array(Some("2"), Some("Bob"), None, Some("0"), Some("2023-01-02 11:00:00"))),
    ResultSetRowPacket(Array(Some("3"), Some("Charlie"), Some("30"), Some("true"), Some("2023-01-03 12:00:00")))
  )

  def createEmptyRecords: Vector[ResultSetRowPacket] = Vector.empty

  // Since Protocol requires many complex dependencies, we'll use null for testing
  // as SharedResultSet doesn't actually use the protocol in most methods we're testing
  def createMockProtocol[F[_]]: Protocol[F] = null.asInstanceOf[Protocol[F]]

  test("getString retrieves string value by index") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      _ <- rs.next()
      value <- rs.getString(2)
    yield assertEquals(value, "Alice")
  }

  test("getInt retrieves integer value by index") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      _ <- rs.next()
      value <- rs.getInt(1)
    yield assertEquals(value, 1)
  }

  test("getBoolean handles various boolean representations") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      // First row - "1" should be true
      _ <- rs.next()
      bool1 <- rs.getBoolean(4)
      // Second row - "0" should be false
      _ <- rs.next()
      bool2 <- rs.getBoolean(4)
      // Third row - "true" should be true
      _ <- rs.next()
      bool3 <- rs.getBoolean(4)
    yield {
      assertEquals(bool1, true)
      assertEquals(bool2, false)
      assertEquals(bool3, true)
    }
  }

  test("getTimestamp parses datetime correctly") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      _ <- rs.next()
      timestamp <- rs.getTimestamp(5)
    yield assertEquals(timestamp, LocalDateTime.of(2023, 1, 1, 10, 0, 0))
  }

  test("wasNull returns true after reading null value") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      _ <- rs.next()
      _ <- rs.next() // Move to second row which has null age
      age <- rs.getInt(3)
      wasNull <- rs.wasNull()
    yield {
      assertEquals(age, 0) // Default value for null
      assertEquals(wasNull, true)
    }
  }

  test("getString by column name") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      _ <- rs.next()
      value <- rs.getString("name")
    yield assertEquals(value, "Alice")
  }

  test("getInt by column name with case-insensitive matching") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      _ <- rs.next()
      value1 <- rs.getInt("ID")
      value2 <- rs.getInt("id")
      value3 <- rs.getInt("Id")
    yield {
      assertEquals(value1, 1)
      assertEquals(value2, 1)
      assertEquals(value3, 1)
    }
  }

  test("findByName fails for non-existent column") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      _ <- rs.next()
      result <- rs.getString("nonexistent").attempt
    yield assert(result.isLeft)
  }

  test("navigation methods for TYPE_FORWARD_ONLY") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize,
        resultSetType = ResultSet.TYPE_FORWARD_ONLY
      )
      // These should fail for TYPE_FORWARD_ONLY
      firstResult <- rs.first().attempt
      lastResult <- rs.last().attempt
      absoluteResult <- rs.absolute(2).attempt
      relativeResult <- rs.relative(1).attempt
      previousResult <- rs.previous().attempt
      beforeFirstResult <- rs.beforeFirst().attempt
      afterLastResult <- rs.afterLast().attempt
    yield {
      assert(firstResult.isLeft)
      assert(lastResult.isLeft)
      assert(absoluteResult.isLeft)
      assert(relativeResult.isLeft)
      assert(previousResult.isLeft)
      assert(beforeFirstResult.isLeft)
      assert(afterLastResult.isLeft)
    }
  }

  test("navigation methods for TYPE_SCROLL_INSENSITIVE") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize,
        resultSetType = ResultSet.TYPE_SCROLL_INSENSITIVE
      )
      // Test first()
      firstResult <- rs.first()
      firstValue <- rs.getString(2)
      // Test last()
      lastResult <- rs.last()
      lastValue <- rs.getString(2)
      // Test absolute positioning
      absoluteResult <- rs.absolute(2)
      absoluteValue <- rs.getString(2)
      // Test relative positioning
      relativeResult <- rs.relative(-1)
      relativeValue <- rs.getString(2)
      // Test previous()
      previousResult <- rs.previous()
      // Test getRow()
      currentRow <- rs.getRow()
    yield {
      assertEquals(firstResult, true)
      assertEquals(firstValue, "Alice")
      assertEquals(lastResult, true)
      assertEquals(lastValue, "Charlie")
      assertEquals(absoluteResult, true)
      assertEquals(absoluteValue, "Bob")
      assertEquals(relativeResult, true)
      assertEquals(relativeValue, "Alice")
      assertEquals(previousResult, false) // Should be before first now
      assertEquals(currentRow, 0)
    }
  }

  test("isBeforeFirst, isFirst, isLast, isAfterLast") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      beforeFirst1 <- rs.isBeforeFirst()
      isFirst1 <- rs.isFirst()
      // Move to first row
      _ <- rs.next()
      beforeFirst2 <- rs.isBeforeFirst()
      isFirst2 <- rs.isFirst()
      isLast1 <- rs.isLast()
      // Move to last row
      _ <- rs.next()
      _ <- rs.next()
      isLast2 <- rs.isLast()
      isAfterLast1 <- rs.isAfterLast()
      // Move past last row
      _ <- rs.next()
      isAfterLast2 <- rs.isAfterLast()
    yield {
      assertEquals(beforeFirst1, true)
      assertEquals(isFirst1, false)
      assertEquals(beforeFirst2, false)
      assertEquals(isFirst2, true)
      assertEquals(isLast1, false)
      assertEquals(isLast2, true)
      assertEquals(isAfterLast1, false)
      assertEquals(isAfterLast2, false) // next() doesn't move cursor past last
    }
  }

  test("absolute with positive and negative positions") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize,
        resultSetType = ResultSet.TYPE_SCROLL_INSENSITIVE
      )
      // Positive position
      result1 <- rs.absolute(2)
      value1 <- rs.getString(2)
      // Negative position (from end)
      result2 <- rs.absolute(-1)
      value2 <- rs.getString(2)
      // Out of bounds
      result3 <- rs.absolute(10)
      // Zero position
      result4 <- rs.absolute(0)
    yield {
      assertEquals(result1, true)
      assertEquals(value1, "Bob")
      assertEquals(result2, true)
      assertEquals(value2, "Charlie")
      assertEquals(result3, false)
      assertEquals(result4, false)
    }
  }

  test("close and isClosed behavior") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      closedBefore <- isClosed.get
      _ <- rs.close()
      closedAfter <- isClosed.get
      // Try to use after closing - checkClosed is called in getMetaData
      result <- rs.getMetaData().attempt
    yield {
      assertEquals(closedBefore, false)
      assertEquals(closedAfter, true)
      assert(result.isLeft)
    }
  }

  test("getMetaData returns correct metadata") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      metadata <- rs.getMetaData()
      columnCount = metadata.getColumnCount()
      columnName1 = metadata.getColumnName(1)
      columnName2 = metadata.getColumnName(2)
    yield {
      assertEquals(columnCount, 5)
      assertEquals(columnName1, "id")
      assertEquals(columnName2, "name")
    }
  }

  test("getType and getConcurrency return correct values") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize,
        resultSetType = ResultSet.TYPE_SCROLL_SENSITIVE,
        resultSetConcurrency = ResultSet.CONCUR_UPDATABLE
      )
      rsType <- rs.getType()
      rsConcurrency <- rs.getConcurrency()
    yield {
      assertEquals(rsType, ResultSet.TYPE_SCROLL_SENSITIVE)
      assertEquals(rsConcurrency, ResultSet.CONCUR_UPDATABLE)
    }
  }

  test("hasRows and rowLength") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed1 <- Ref.of[IO, Boolean](false)
      isClosed2 <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs1 = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed1,
        fetchSize
      )
      rs2 = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createEmptyRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed2,
        fetchSize
      )
      hasRows1 <- rs1.hasRows()
      rowLength1 <- rs1.rowLength()
      hasRows2 <- rs2.hasRows()
      rowLength2 <- rs2.rowLength()
    yield {
      assertEquals(hasRows1, true)
      assertEquals(rowLength1, 3)
      assertEquals(hasRows2, false)
      assertEquals(rowLength2, 0)
    }
  }

  test("getByte handles character and numeric values") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      // Create custom records with byte values
      byteRecords = Vector(
        ResultSetRowPacket(Array(Some("65"), Some("A"), Some("-128"), Some("127")))
      )
      byteColumns = Vector(
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "numeric_byte",
          orgName = "numeric_byte",
      length = 12,
          characterSet = 33,
          columnLength = 4,
          columnType = ColumnDataType.MYSQL_TYPE_TINY,
          flags = Seq.empty,
          decimals = 0
        ),
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "char_byte",
          orgName = "char_byte",
      length = 12,
          characterSet = 33,
          columnLength = 1,
          columnType = ColumnDataType.MYSQL_TYPE_STRING,
          flags = Seq.empty,
          decimals = 0
        ),
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "min_byte",
          orgName = "min_byte",
      length = 12,
          characterSet = 33,
          columnLength = 4,
          columnType = ColumnDataType.MYSQL_TYPE_TINY,
          flags = Seq.empty,
          decimals = 0
        ),
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "max_byte",
          orgName = "max_byte",
      length = 12,
          characterSet = 33,
          columnLength = 4,
          columnType = ColumnDataType.MYSQL_TYPE_TINY,
          flags = Seq.empty,
          decimals = 0
        )
      )
      rs = new TestSharedResultSet[IO](
        protocol,
        byteColumns,
        byteRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      _ <- rs.next()
      numericByte <- rs.getByte(1)
      charByte <- rs.getByte(2)
      minByte <- rs.getByte(3)
      maxByte <- rs.getByte(4)
    yield {
      assertEquals(numericByte, 65.toByte)
      assertEquals(charByte, 65.toByte) // 'A' as byte
      assertEquals(minByte, -128.toByte)
      assertEquals(maxByte, 127.toByte)
    }
  }

  test("error when column index out of bounds") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      rs = new TestSharedResultSet[IO](
        protocol,
        createTestColumns,
        createTestRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      _ <- rs.next()
      result1 <- rs.getString(0).attempt
      result2 <- rs.getString(10).attempt
    yield {
      assert(result1.isLeft)
      assert(result2.isLeft)
    }
  }

  test("all data type getters with column names") {
    for
      protocol <- IO(createMockProtocol[IO])
      isClosed <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](0)
      // Create records with various data types
      dataRecords = Vector(
        ResultSetRowPacket(Array(
          Some("123"),           // short
          Some("456789"),        // long
          Some("3.14"),          // float
          Some("2.71828"),       // double
          Some("Hello"),         // bytes
          Some("2023-05-15"),    // date
          Some("14:30:25"),      // time
          Some("1234.5678")      // bigdecimal
        ))
      )
      dataColumns = Vector(
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "short_col",
          orgName = "short_col",
      length = 12,
          characterSet = 33,
          columnLength = 6,
          columnType = ColumnDataType.MYSQL_TYPE_SHORT,
          flags = Seq.empty,
          decimals = 0
        ),
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "long_col",
          orgName = "long_col",
      length = 12,
          characterSet = 33,
          columnLength = 20,
          columnType = ColumnDataType.MYSQL_TYPE_LONGLONG,
          flags = Seq.empty,
          decimals = 0
        ),
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "float_col",
          orgName = "float_col",
      length = 12,
          characterSet = 33,
          columnLength = 12,
          columnType = ColumnDataType.MYSQL_TYPE_FLOAT,
          flags = Seq.empty,
          decimals = 2
        ),
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "double_col",
          orgName = "double_col",
      length = 12,
          characterSet = 33,
          columnLength = 22,
          columnType = ColumnDataType.MYSQL_TYPE_DOUBLE,
          flags = Seq.empty,
          decimals = 5
        ),
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "bytes_col",
          orgName = "bytes_col",
      length = 12,
          characterSet = 33,
          columnLength = 255,
          columnType = ColumnDataType.MYSQL_TYPE_VAR_STRING,
          flags = Seq.empty,
          decimals = 0
        ),
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "date_col",
          orgName = "date_col",
      length = 12,
          characterSet = 33,
          columnLength = 10,
          columnType = ColumnDataType.MYSQL_TYPE_DATE,
          flags = Seq.empty,
          decimals = 0
        ),
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "time_col",
          orgName = "time_col",
      length = 12,
          characterSet = 33,
          columnLength = 8,
          columnType = ColumnDataType.MYSQL_TYPE_TIME,
          flags = Seq.empty,
          decimals = 0
        ),
        ColumnDefinition41Packet(
          catalog = "def",
          schema = "test_db",
          table = "test_table",
          orgTable = "test_table",
          name = "bigdecimal_col",
          orgName = "bigdecimal_col",
      length = 12,
          characterSet = 33,
          columnLength = 10,
          columnType = ColumnDataType.MYSQL_TYPE_NEWDECIMAL,
          flags = Seq.empty,
          decimals = 4
        )
      )
      rs = new TestSharedResultSet[IO](
        protocol,
        dataColumns,
        dataRecords,
        Map.empty,
        Version(8, 0, 0),
        isClosed,
        fetchSize
      )
      _ <- rs.next()
      shortVal <- rs.getShort("short_col")
      longVal <- rs.getLong("long_col")
      floatVal <- rs.getFloat("float_col")
      doubleVal <- rs.getDouble("double_col")
      bytesVal <- rs.getBytes("bytes_col")
      dateVal <- rs.getDate("date_col")
      timeVal <- rs.getTime("time_col")
      bigDecimalVal <- rs.getBigDecimal("bigdecimal_col")
    yield {
      assertEquals(shortVal, 123.toShort)
      assertEquals(longVal, 456789L)
      assertEquals(floatVal, 3.14f)
      assertEquals(doubleVal, 2.71828)
      assertEquals(new String(bytesVal, "UTF-8"), "Hello")
      assertEquals(dateVal, LocalDate.of(2023, 5, 15))
      assertEquals(timeVal, LocalTime.of(14, 30, 25))
      assertEquals(bigDecimalVal, BigDecimal("1234.5678"))
    }
  }