/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.collection.mutable.ListBuffer

import cats.syntax.all.*

import cats.effect.*

import ldbc.connector.data.*
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.packet.RequestPacket
import ldbc.connector.net.Protocol
import ldbc.connector.util.Version

class StreamingResultSetTest extends FTestPlatform:

  // Mock implementation for testing StreamingResultSet behavior
  class MockStreamingResultSet[F[_]: Async](
    testStatementId:        Long,
    testColumns:            Vector[ColumnDefinitionPacket],
    testRecords:            Vector[ResultSetRowPacket],
    testServerVariables:    Map[String, String],
    testVersion:            Version,
    testIsClosed:           Ref[F, Boolean],
    testFetchSize:          Ref[F, Int],
    testUseCursorFetch:     Boolean = true,
    testUseServerPrepStmts: Boolean = true
  ) {
    // Parameters are stored but not directly used in mock
    val _ = (
      testColumns,
      testRecords,
      testServerVariables,
      testVersion,
      testIsClosed,
      testUseCursorFetch,
      testUseServerPrepStmts
    )
    // Track protocol interactions
    val sentPackets           = ListBuffer.empty[RequestPacket]
    val mockResponses         = ListBuffer.empty[Vector[BinaryProtocolResultSetRowPacket]]
    var responseIndex         = 0
    var resetSequenceIdCalled = 0
    var closeStmtCalled       = false

    // Create a mock protocol
    val mockProtocol: Protocol[F] = null.asInstanceOf[Protocol[F]]

    // We'll create our own implementation that mimics StreamingResultSet behavior
    var isCompleteAllFetch: Boolean                    = false
    var rows:               Vector[ResultSetRowPacket] = Vector.empty
    var currentCursor:      Int                        = 0
    var currentRow:         Option[ResultSetRowPacket] = None

    def addMockResponse(response: Vector[BinaryProtocolResultSetRowPacket]): Unit = {
      mockResponses += response
    }

    def reset(): Unit = {
      sentPackets.clear()
      mockResponses.clear()
      responseIndex         = 0
      resetSequenceIdCalled = 0
      closeStmtCalled       = false
      isCompleteAllFetch    = false
      rows                  = Vector.empty
      currentCursor         = 0
      currentRow            = None
    }

    // Simulate fetchRow
    def fetchRow(size: Int): F[Unit] = Async[F].delay {
      resetSequenceIdCalled += 1
      sentPackets += ComStmtFetchPacket(testStatementId, size)

      if responseIndex < mockResponses.length then {
        val resultSetRow = mockResponses(responseIndex)
        rows               = resultSetRow
        currentCursor      = 0
        currentRow         = None
        isCompleteAllFetch = resultSetRow.length < size
        responseIndex += 1
      } else {
        rows               = Vector.empty
        isCompleteAllFetch = true
      }
    }

    // Simulate closeStmt
    def closeStmt(): F[Boolean] = Async[F].delay {
      sentPackets += ComStmtClosePacket(testStatementId)
      closeStmtCalled = true
      false
    }

    // Simulate next() behavior from StreamingResultSet
    def next(): F[Boolean] = testFetchSize.get.flatMap { size =>
      if isCompleteAllFetch && currentCursor >= rows.length then {
        resetSequenceIdCalled += 1
        closeStmt()
      } else if rows.isEmpty then {
        fetchRow(size) *> next()
      } else if currentCursor >= rows.length then {
        fetchRow(size) *> next()
      } else {
        Async[F].delay {
          currentCursor = currentCursor + 1
          currentRow    = rows.lift(currentCursor - 1)
          true
        }
      }
    }

    // Simulate getString
    def getString(columnIndex: Int): F[String] = Async[F].delay {
      currentRow match {
        case Some(row) => row.values(columnIndex - 1).getOrElse("")
        case None      => ""
      }
    }
  }

  // Helper to create test columns
  def createTestColumns: Vector[ColumnDefinitionPacket] = Vector(
    ColumnDefinition41Packet(
      catalog      = "def",
      schema       = "test_db",
      table        = "test_table",
      orgTable     = "test_table",
      name         = "id",
      orgName      = "id",
      length       = 12,
      characterSet = 33,
      columnLength = 11,
      columnType   = ColumnDataType.MYSQL_TYPE_LONG,
      flags        = Seq.empty,
      decimals     = 0
    ),
    ColumnDefinition41Packet(
      catalog      = "def",
      schema       = "test_db",
      table        = "test_table",
      orgTable     = "test_table",
      name         = "name",
      orgName      = "name",
      length       = 12,
      characterSet = 33,
      columnLength = 255,
      columnType   = ColumnDataType.MYSQL_TYPE_VARCHAR,
      flags        = Seq.empty,
      decimals     = 0
    )
  )

  // Helper to create binary protocol result set rows
  def createBinaryRows(values: Vector[(String, String)]): Vector[BinaryProtocolResultSetRowPacket] = {
    values.map {
      case (id, name) =>
        BinaryProtocolResultSetRowPacket(Array(Some(id), Some(name)))
    }
  }

  test("next() should fetch rows based on fetch size") {
    for
      isClosed  <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](2) // Fetch 2 rows at a time

      mockResultSet = new MockStreamingResultSet[IO](
                        testStatementId        = 123L,
                        testColumns            = createTestColumns,
                        testRecords            = Vector.empty,
                        testServerVariables    = Map.empty,
                        testVersion            = Version(8, 0, 0),
                        testIsClosed           = isClosed,
                        testFetchSize          = fetchSize,
                        testUseCursorFetch     = true,
                        testUseServerPrepStmts = true
                      )

      // Prepare mock responses
      _ = mockResultSet.addMockResponse(createBinaryRows(Vector(("1", "Alice"), ("2", "Bob"))))
      _ = mockResultSet.addMockResponse(createBinaryRows(Vector(("3", "Charlie"))))

      // First next() should trigger fetch
      hasNext1 <- mockResultSet.next()
      value1   <- mockResultSet.getString(2)

      // Second next() should use cached row
      hasNext2 <- mockResultSet.next()
      value2   <- mockResultSet.getString(2)

      // Third next() should trigger another fetch
      hasNext3 <- mockResultSet.next()
      value3   <- mockResultSet.getString(2)

      // Fourth next() should return false (no more rows)
      hasNext4 <- mockResultSet.next()

      // Verify protocol interactions
      sentPackets  = mockResultSet.sentPackets.toList
      fetchPackets = sentPackets.collect { case p: ComStmtFetchPacket => p }
    yield {
      assertEquals(hasNext1, true)
      assertEquals(value1, "Alice")
      assertEquals(hasNext2, true)
      assertEquals(value2, "Bob")
      assertEquals(hasNext3, true)
      assertEquals(value3, "Charlie")
      assertEquals(hasNext4, false)

      // Should have 2 fetch packets and 1 close packet
      assertEquals(fetchPackets.length, 2)
      assertEquals(fetchPackets(0).statementId, 123L)
      assertEquals(fetchPackets(0).numRows, 2)
      assertEquals(fetchPackets(1).statementId, 123L)
      assertEquals(fetchPackets(1).numRows, 2)

      // Should have close packet at the end
      assert(sentPackets.last.isInstanceOf[ComStmtClosePacket], "Should have close packet at the end")
    }
  }

  test("next() with empty initial records should fetch immediately") {
    for
      isClosed  <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](5)

      mockResultSet = new MockStreamingResultSet[IO](
                        testStatementId        = 456L,
                        testColumns            = createTestColumns,
                        testRecords            = Vector.empty,
                        testServerVariables    = Map.empty,
                        testVersion            = Version(8, 0, 0),
                        testIsClosed           = isClosed,
                        testFetchSize          = fetchSize,
                        testUseCursorFetch     = true,
                        testUseServerPrepStmts = true
                      )

      _ = mockResultSet.addMockResponse(createBinaryRows(Vector(("1", "Test"))))

      hasNext <- mockResultSet.next()
      value   <- mockResultSet.getString(2)
    yield {
      assertEquals(hasNext, true)
      assertEquals(value, "Test")
      assertEquals(mockResultSet.resetSequenceIdCalled, 1)
    }
  }

  test("streaming completes when fewer rows returned than fetch size") {
    for
      isClosed  <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](10) // Request 10 rows

      mockResultSet = new MockStreamingResultSet[IO](
                        testStatementId        = 789L,
                        testColumns            = createTestColumns,
                        testRecords            = Vector.empty,
                        testServerVariables    = Map.empty,
                        testVersion            = Version(8, 0, 0),
                        testIsClosed           = isClosed,
                        testFetchSize          = fetchSize,
                        testUseCursorFetch     = true,
                        testUseServerPrepStmts = true
                      )

      // Return only 3 rows - indicating end of results
      _ = mockResultSet.addMockResponse(createBinaryRows(Vector(("1", "A"), ("2", "B"), ("3", "C"))))

      // Read all rows
      _       <- mockResultSet.next()
      _       <- mockResultSet.next()
      _       <- mockResultSet.next()
      hasNext <- mockResultSet.next() // Should close statement and return false

      sentPackets = mockResultSet.sentPackets.toList
    yield {
      assertEquals(hasNext, false)

      // Should have ComStmtClosePacket
      assert(sentPackets.exists(_.isInstanceOf[ComStmtClosePacket]))

      val closePacket = sentPackets.collect { case p: ComStmtClosePacket => p }.head
      assertEquals(closePacket.statementId, 789L)
    }
  }

  test("multiple fetch cycles with varying response sizes") {
    for
      isClosed  <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](3)

      mockResultSet = new MockStreamingResultSet[IO](
                        testStatementId        = 222L,
                        testColumns            = createTestColumns,
                        testRecords            = Vector.empty,
                        testServerVariables    = Map.empty,
                        testVersion            = Version(8, 0, 0),
                        testIsClosed           = isClosed,
                        testFetchSize          = fetchSize,
                        testUseCursorFetch     = true,
                        testUseServerPrepStmts = true
                      )

      // Add multiple responses of different sizes
      _ = mockResultSet.addMockResponse(createBinaryRows(Vector(("1", "A"), ("2", "B"), ("3", "C"))))
      _ = mockResultSet.addMockResponse(createBinaryRows(Vector(("4", "D"), ("5", "E")))) // Less than fetch size

      results <- (1 to 6).toList.traverse { _ =>
                   mockResultSet.next().flatMap { hasNext =>
                     if hasNext then mockResultSet.getString(2).map(Some(_))
                     else IO.pure(None)
                   }
                 }
    yield {
      assertEquals(results.flatten, List("A", "B", "C", "D", "E"))

      // Verify protocol reset was called appropriately
      assert(mockResultSet.resetSequenceIdCalled > 0)

      // Verify fetch packets
      val fetchPackets = mockResultSet.sentPackets.collect { case p: ComStmtFetchPacket => p }
      assertEquals(fetchPackets.length, 2)
      assert(fetchPackets.forall(_.numRows == 3))
    }
  }

  test("changing fetch size during iteration") {
    for
      isClosed  <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](2)

      mockResultSet = new MockStreamingResultSet[IO](
                        testStatementId        = 333L,
                        testColumns            = createTestColumns,
                        testRecords            = Vector.empty,
                        testServerVariables    = Map.empty,
                        testVersion            = Version(8, 0, 0),
                        testIsClosed           = isClosed,
                        testFetchSize          = fetchSize,
                        testUseCursorFetch     = true,
                        testUseServerPrepStmts = true
                      )

      _ = mockResultSet.addMockResponse(createBinaryRows(Vector(("1", "A"), ("2", "B"))))
      _ = mockResultSet.addMockResponse(createBinaryRows(Vector(("3", "C"), ("4", "D"), ("5", "E"))))

      // Read first batch
      _ <- mockResultSet.next()
      _ <- mockResultSet.next()

      // Change fetch size
      _ <- fetchSize.set(5)

      // Continue reading - should use new fetch size
      _ <- mockResultSet.next()
      _ <- mockResultSet.next()
      _ <- mockResultSet.next()

      fetchPackets = mockResultSet.sentPackets.collect { case p: ComStmtFetchPacket => p }
    yield {
      assertEquals(fetchPackets.length, 2)
      assertEquals(fetchPackets(0).numRows, 2)
      assertEquals(fetchPackets(1).numRows, 5)
    }
  }

  test("empty result set handling") {
    for
      isClosed  <- Ref.of[IO, Boolean](false)
      fetchSize <- Ref.of[IO, Int](10)

      mockResultSet = new MockStreamingResultSet[IO](
                        testStatementId        = 555L,
                        testColumns            = createTestColumns,
                        testRecords            = Vector.empty,
                        testServerVariables    = Map.empty,
                        testVersion            = Version(8, 0, 0),
                        testIsClosed           = isClosed,
                        testFetchSize          = fetchSize,
                        testUseCursorFetch     = true,
                        testUseServerPrepStmts = true
                      )

      // Return empty response
      _ = mockResultSet.addMockResponse(Vector.empty)

      hasNext <- mockResultSet.next()
    yield {
      assertEquals(hasNext, false)

      // Should still send close packet
      val closePackets = mockResultSet.sentPackets.collect { case p: ComStmtClosePacket => p }
      assertEquals(closePackets.length, 1)
    }
  }
