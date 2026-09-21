/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import java.io.IOException

import ldbc.sql.{ SQLException, SQLTransientConnectionException }

import ldbc.fx.{ Fx, FxSuite }
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.mysql.exception.EofException
import ldbc.net.{ RawBackedSocket, RawSocket, Socket }

class TransportErrorsTest extends FxSuite:

  private val raw: RawSocket = new RawSocket:
    override def read(n:      Int, cb:         Either[Throwable, Option[Array[Byte]]] => Unit) = ldbc.net.Canceler.noop
    override def write(bytes: Array[Byte], cb: Either[Throwable, Unit] => Unit) = ldbc.net.Canceler.noop
    override def close(): Unit = ()

  private def failing(error: Throwable): Socket[Fx] = new Socket[Fx]:
    override def read(n:      Int):         Fx[Option[Array[Byte]]] = Fx.raiseError(error)
    override def write(bytes: Array[Byte]): Fx[Unit]                = Fx.raiseError(error)
    override def close():                   Fx[Unit]                = Fx.raiseError(error)

  private def failingBacked(error: Throwable): Socket[Fx] =
    new Socket[Fx] with RawBackedSocket:
      override def underlying:                RawSocket               = raw
      override def read(n:      Int):         Fx[Option[Array[Byte]]] = Fx.raiseError(error)
      override def write(bytes: Array[Byte]): Fx[Unit]                = Fx.raiseError(error)
      override def close():                   Fx[Unit]                = Fx.raiseError(error)

  private def connecting(socket: Socket[Fx]): Socket[Fx] = TransportErrors.phased(socket).socket

  private def established(socket: Socket[Fx]): Socket[Fx] =
    val phased = TransportErrors.phased(socket)
    phased.markEstablished()
    phased.socket

  private def caught(socket: Socket[Fx]): Fx[SQLException] =
    interceptFx[SQLException](socket.read(4).void)

  private def caughtWriting(socket: Socket[Fx]): Fx[SQLException] =
    interceptFx[SQLException](socket.write(Array[Byte](1)).void)

  private def caughtClosing(socket: Socket[Fx]): Fx[SQLException] =
    interceptFx[SQLException](socket.close().void)

  test("a transport failure becomes a transient connection exception"):
    val cause = new IOException("socket closed")
    caught(established(failing(cause))).map { error =>
      assert(error.isInstanceOf[SQLTransientConnectionException], s"unexpected type: ${ error.getClass }")
      assertEquals(error.getSQLState, "08S01")
      assertEquals(error.getCause, cause)
    }

  test("the connecting phase reports a different state"):
    caught(connecting(failing(new IOException("connection refused")))).map { error =>
      assertEquals(error.getSQLState, "08001")
    }

  test("an existing SQL exception passes through untouched"):
    val eof = EofException(4, 1)
    caught(established(failing(eof))).map(error => assertEquals(error, eof))

  test("the same wrapper reports a lost link once the connection is established"):
    val cause  = new IOException("socket closed")
    val phased = TransportErrors.phased(failing(cause))
    for
      before <- caught(phased.socket)
      _ = phased.markEstablished()
      after <- caught(phased.socket)
    yield
      assertEquals(before.getSQLState, "08001")
      assertEquals(after.getSQLState, "08S01")
      assertEquals(after.getCause, cause)

  test("the raw socket stays reachable for the TLS layer"):
    connecting(failingBacked(new IOException("boom"))) match
      case backed: RawBackedSocket => assertEquals(backed.underlying, raw)
      case other                   => fail(s"RawBackedSocket was dropped by the decorator: $other")

  test("a plain socket does not gain the marker"):
    assert(!connecting(failing(new IOException("boom"))).isInstanceOf[RawBackedSocket])

  test("a write failure is translated the same way"):
    val cause = new IOException("broken pipe")
    caughtWriting(established(failing(cause))).map { error =>
      assert(error.isInstanceOf[SQLTransientConnectionException], s"unexpected type: ${ error.getClass }")
      assertEquals(error.getSQLState, "08S01")
      assertEquals(error.getCause, cause)
    }

  test("a close failure is translated the same way"):
    val cause = new IOException("already closed")
    caughtClosing(connecting(failing(cause))).map { error =>
      assert(error.isInstanceOf[SQLTransientConnectionException], s"unexpected type: ${ error.getClass }")
      assertEquals(error.getSQLState, "08001")
      assertEquals(error.getCause, cause)
    }

  test("an existing SQL exception passes through write and close too"):
    val eof = EofException(4, 1)
    for
      onWrite <- caughtWriting(established(failing(eof)))
      onClose <- caughtClosing(established(failing(eof)))
    yield
      assertEquals(onWrite, eof)
      assertEquals(onClose, eof)
