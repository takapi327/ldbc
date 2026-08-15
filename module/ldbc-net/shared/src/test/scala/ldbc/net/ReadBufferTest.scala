/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * Regression test for review finding B5: a read that has to wait for data must still receive at most
 * `n` bytes (like the immediate path), not the whole buffered chunk. Verifies both the parked and the
 * immediate paths honour `n`.
 */
class ReadBufferTest extends munit.FunSuite:

  test("a parked read receives at most n bytes and keeps the remainder buffered"):
    val buffer = new ReadBuffer
    var received: Option[Array[Byte]] = None
    buffer.read(3, { case Right(Some(a)) => received = Some(a); case other => fail(s"unexpected $other") })
    assert(received.isEmpty, "read should still be waiting")

    buffer.onData(Array[Byte](1, 2, 3, 4, 5))
    assert(received.isDefined, "waiter should be served once data arrives")
    assertEquals(received.get.toList, List[Byte](1, 2, 3), "must return only n bytes")

    var next: Option[Array[Byte]] = None
    buffer.read(10, { case Right(Some(a)) => next = Some(a); case _ => () })
    assertEquals(next.get.toList, List[Byte](4, 5), "the remainder stays buffered for the next read")

  test("an immediate read also honours n"):
    val buffer = new ReadBuffer
    buffer.onData(Array[Byte](1, 2, 3, 4))
    var got: Option[Array[Byte]] = None
    buffer.read(2, { case Right(Some(a)) => got = Some(a); case _ => () })
    assertEquals(got.get.toList, List[Byte](1, 2))

  test("EOF is delivered out of band as None, not as an empty array"):
    val buffer = new ReadBuffer
    var got: Option[Option[Array[Byte]]] = None
    buffer.onEof()
    buffer.read(4, { case Right(result) => got = Some(result); case Left(_) => () })
    assertEquals(got, Some(None), "end of stream must be None, distinguishable from data")

  test("read(0) completes with Some(empty), distinct from EOF, even while data is pending"):
    val buffer = new ReadBuffer
    buffer.onData(Array[Byte](1, 2, 3))
    var got: Option[Option[Array[Byte]]] = None
    buffer.read(0, { case Right(result) => got = Some(result); case Left(_) => () })
    assert(got.exists(_.exists(_.isEmpty)), s"read(0) must be Some(empty), got $got")
    var next: Option[Option[Array[Byte]]] = None
    buffer.read(10, { case Right(result) => next = Some(result); case Left(_) => () })
    assertEquals(next.flatten.map(_.toList), Some(List[Byte](1, 2, 3)), "pending data must remain readable")

  test("a read after onError surfaces the error"):
    val buffer = new ReadBuffer
    buffer.onError(new RuntimeException("socket error"))
    var got: Option[Either[Throwable, Option[Array[Byte]]]] = None
    buffer.read(4, r => got = Some(r))
    assertEquals(
      got.flatMap(_.left.toOption).map(_.getMessage),
      Some("socket error"),
      "an errored buffer must surface the error"
    )

  test("a parked read is woken by a later onError (does not hang)"):
    val buffer = new ReadBuffer
    var got: Option[Either[Throwable, Option[Array[Byte]]]] = None
    buffer.read(4, r => got = Some(r))
    assert(got.isEmpty, "read should be parked with no data yet")
    buffer.onError(new RuntimeException("socket error"))
    assertEquals(
      got.flatMap(_.left.toOption).map(_.getMessage),
      Some("socket error"),
      "a parked reader must be woken with the transport error, not left hanging"
    )

  test("a read after onClose fails fast rather than parking on a peer that never sends EOF"):
    val buffer = new ReadBuffer
    buffer.onClose()
    var got: Option[Either[Throwable, Option[Array[Byte]]]] = None
    buffer.read(4, r => got = Some(r))
    assert(got.exists(_.isLeft), s"a read after close must fail fast, got $got")

  test("read(0) after onClose still completes with Some(empty), not a failure"):
    val buffer = new ReadBuffer
    buffer.onClose()
    var got: Option[Either[Throwable, Option[Array[Byte]]]] = None
    buffer.read(0, r => got = Some(r))
    assert(got.exists(_.exists(_.exists(_.isEmpty))), s"read(0) after close must be Some(empty), got $got")

  test("a parked read is woken by a later onClose (does not hang)"):
    val buffer = new ReadBuffer
    var got: Option[Either[Throwable, Option[Array[Byte]]]] = None
    buffer.read(4, r => got = Some(r))
    assert(got.isEmpty, "read should be parked with no data yet")
    buffer.onClose()
    assert(got.exists(_.isLeft), "a parked reader must be woken when the socket is closed, not left hanging")
