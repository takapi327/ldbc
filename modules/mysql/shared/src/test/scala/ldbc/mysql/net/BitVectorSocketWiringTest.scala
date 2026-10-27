/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import java.io.IOException

import scala.concurrent.duration.Duration

import ldbc.sql.SQLException

import ldbc.effect.{ Ref, Resource }
import ldbc.fx.{ Fx, FxSuite }
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.mysql.data.CapabilitiesFlags
import ldbc.mysql.net.packet.response.InitialPacket
import ldbc.net.{ Socket, TlsUpgrade }

class BitVectorSocketWiringTest extends FxSuite:

  private final class Delegating(inner: Socket[Fx]) extends Socket[Fx]:
    override def read(n:      Int):         Fx[Option[Array[Byte]]] = inner.read(n)
    override def write(bytes: Array[Byte]): Fx[Unit]                = inner.write(bytes)
    override def close():                   Fx[Unit]                = inner.close()

  private given TlsUpgrade[Fx] = new TlsUpgrade[Fx]:
    override def client(socket: Socket[Fx], host: String, port: Int, ssl: ldbc.net.SSL): Fx[Socket[Fx]] =
      Fx.pure(new Delegating(socket))

  private val sslOptions = SSLNegotiation.Options[Fx](
    tlsConfig  = ldbc.net.SSL.Trusted,
    host       = "127.0.0.1",
    port       = 3306,
    fallbackOk = false,
    logger     = None
  )

  private val handshake: Array[Byte] =
    val version = "8.4.0".getBytes("UTF-8")
    val plugin  = "mysql_native_password".getBytes("UTF-8")
    val builder = Array.newBuilder[Byte]
    builder += 10.toByte
    builder ++= version
    builder += 0
    builder ++= Array[Byte](1, 0, 0, 0)
    builder ++= Array.fill[Byte](8)(1)
    builder += 0
    builder ++= Array[Byte](0, 0)
    builder += 45.toByte
    builder ++= Array[Byte](0, 0)
    builder ++= Array[Byte](0x08, 0x00)
    builder += 21.toByte
    builder ++= Array.fill[Byte](10)(0)
    builder ++= Array.fill[Byte](13)(2)
    builder ++= plugin
    builder += 0
    builder.result()

  private def header(size: Int, seq: Byte): Array[Byte] =
    Array((size & 0xff).toByte, ((size >> 8) & 0xff).toByte, ((size >> 16) & 0xff).toByte, seq)

  private final class HandshakeThenFail(reads: Ref[Fx, List[Array[Byte]]]) extends Socket[Fx]:
    override def read(n: Int): Fx[Option[Array[Byte]]] =
      reads
        .modify {
          case head :: tail => (tail, Some(head))
          case Nil          => (Nil, None)
        }
        .flatMap {
          case Some(bytes) => Fx.pure(Some(bytes))
          case None        => Fx.raiseError(new IOException("connection reset by peer"))
        }
    override def write(bytes: Array[Byte]): Fx[Unit] = Fx.unit
    override def close():                   Fx[Unit] = Fx.unit

  private def failureAfterHandshake(ssl: Option[SSLNegotiation.Options[Fx]]): Fx[SQLException] =
    for
      reads         <- Ref.of[Fx, List[Array[Byte]]](List(header(handshake.length, 0), handshake))
      sequenceIdRef <- Ref.of[Fx, Byte](0x01)
      initialRef    <- Ref.of[Fx, Option[InitialPacket]](None)
      error         <- BitVectorSocket[Fx](
                 Resource.pure(new HandshakeThenFail(reads)),
                 sequenceIdRef,
                 initialRef,
                 ssl,
                 Duration.Inf,
                 Set.empty[CapabilitiesFlags]
               ).use(socket => interceptFx[SQLException](socket.read(4).void))
    yield error

  test("a failure after the handshake is reported as a communication link failure"):
    failureAfterHandshake(None).map(error => assertEquals(error.getSQLState, "08S01"))

  test("the wrapping is a single layer on a plaintext connection"):
    failureAfterHandshake(None).map { error =>
      assert(!error.getCause.isInstanceOf[SQLException], s"a second SQLException layer was added: ${ error.getCause }")
      assert(error.getCause.isInstanceOf[IOException], s"the original cause was lost: ${ error.getCause }")
    }

  test("a failure during the handshake is reported as an inability to connect"):
    val program = for
      reads         <- Ref.of[Fx, List[Array[Byte]]](List.empty)
      sequenceIdRef <- Ref.of[Fx, Byte](0x01)
      initialRef    <- Ref.of[Fx, Option[InitialPacket]](None)
      error         <- interceptFx[SQLException](
                 BitVectorSocket[Fx](
                   Resource.pure(new HandshakeThenFail(reads)),
                   sequenceIdRef,
                   initialRef,
                   None,
                   Duration.Inf,
                   Set.empty[CapabilitiesFlags]
                 ).use(_ => Fx.unit)
               )
    yield assertEquals(error.getSQLState, "08001")
    program

  test("a failure after a TLS handshake is also a communication link failure"):
    failureAfterHandshake(Some(sslOptions)).map(error => assertEquals(error.getSQLState, "08S01"))

  test("the wrapping is a single layer on a TLS connection"):
    failureAfterHandshake(Some(sslOptions)).map { error =>
      assert(!error.getCause.isInstanceOf[SQLException], s"a second SQLException layer was added: ${ error.getCause }")
      assert(error.getCause.isInstanceOf[IOException], s"the original cause was lost: ${ error.getCause }")
    }
