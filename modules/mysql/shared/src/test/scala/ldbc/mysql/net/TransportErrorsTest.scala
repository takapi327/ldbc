/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import java.io.IOException

import ldbc.sql.{ SQLException, SQLTransientConnectionException }

import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.{ Fx, FxSuite }
import ldbc.mysql.exception.EofException
import ldbc.net.{ RawBackedSocket, RawSocket, Socket }

class TransportErrorsTest extends FxSuite:

  private val raw: RawSocket = new RawSocket:
    override def read(n: Int, cb: Either[Throwable, Option[Array[Byte]]] => Unit) = ldbc.net.Canceler.noop
    override def write(bytes: Array[Byte], cb: Either[Throwable, Unit] => Unit)   = ldbc.net.Canceler.noop
    override def close(): Unit                                                    = ()

  private def failing(error: Throwable): Socket[Fx] = new Socket[Fx]:
    override def read(n: Int): Fx[Option[Array[Byte]]] = Fx.raiseError(error)
    override def write(bytes: Array[Byte]): Fx[Unit]   = Fx.raiseError(error)
    override def close():                   Fx[Unit]   = Fx.raiseError(error)

  private def failingBacked(error: Throwable): Socket[Fx] =
    new Socket[Fx] with RawBackedSocket:
      override def underlying:   RawSocket              = raw
      override def read(n: Int): Fx[Option[Array[Byte]]] = Fx.raiseError(error)
      override def write(bytes: Array[Byte]): Fx[Unit]   = Fx.raiseError(error)
      override def close():                   Fx[Unit]   = Fx.raiseError(error)

  private def caught(socket: Socket[Fx]): Fx[SQLException] =
    interceptFx[SQLException](socket.read(4).void)

  test("a transport failure becomes a transient connection exception"):
    val cause = new IOException("socket closed")
    caught(TransportErrors.established(failing(cause))).map { error =>
      assert(error.isInstanceOf[SQLTransientConnectionException], s"unexpected type: ${ error.getClass }")
      assertEquals(error.getSQLState, "08S01")
      assertEquals(error.getCause, cause)
    }

  test("the connecting phase reports a different state"):
    caught(TransportErrors.connecting(failing(new IOException("connection refused")))).map { error =>
      assertEquals(error.getSQLState, "08001")
    }

  test("an existing SQL exception passes through untouched"):
    val eof = EofException(4, 1)
    caught(TransportErrors.established(failing(eof))).map(error => assertEquals(error, eof))

  test("wrapping twice does not stack another layer"):
    val cause = new IOException("socket closed")
    caught(TransportErrors.established(TransportErrors.connecting(failing(cause)))).map { error =>
      assertEquals(error.getCause, cause)
      assert(!error.getCause.isInstanceOf[SQLException], "a second SQLException layer was added")
    }

  test("the raw socket stays reachable for the TLS layer"):
    TransportErrors.connecting(failingBacked(new IOException("boom"))) match
      case backed: RawBackedSocket => assertEquals(backed.underlying, raw)
      case other                   => fail(s"RawBackedSocket was dropped by the decorator: $other")

  test("a plain socket does not gain the marker"):
    assert(!TransportErrors.connecting(failing(new IOException("boom"))).isInstanceOf[RawBackedSocket])
