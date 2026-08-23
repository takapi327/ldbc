/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

import scodec.bits.{ BitVector, ByteVector }

import ldbc.sql.SQLTimeoutException

import ldbc.effect.{ Concurrent, Ref, Resource }
import ldbc.effect.syntax.*
import ldbc.mysql.data.CapabilitiesFlags
import ldbc.mysql.net.packet.response.InitialPacket
import ldbc.mysql.net.protocol.Initial
import ldbc.net.{ Socket, TlsUpgrade }

/**
 * A higher-level [[ldbc.net.Socket]] interface defined in terms of `BitVector`, carrying leftover
 * bytes between reads so callers can request an exact number of bytes.
 */
trait BitVectorSocket[F[_]]:

  /** Writes the specified bits to the socket. */
  def write(bits: BitVector): F[Unit]

  /**
   * Reads `nBytes` bytes (not bits!) from the socket, failing if end-of-stream is reached before
   * `nBytes` bytes are received.
   */
  def read(nBytes: Int): F[BitVector]

object BitVectorSocket:

  /**
   * Wraps an existing [[ldbc.net.Socket]] as a [[BitVectorSocket]].
   *
   * @param socket      the underlying socket
   * @param readTimeout the per-read timeout (or `Duration.Inf` for none)
   * @param carryRef    holds the bytes read past the last request boundary
   */
  def fromSocket[F[_]](socket: Socket[F], readTimeout: Duration, carryRef: Ref[F, ByteVector])(using
    F: Concurrent[F]
  ): BitVectorSocket[F] =
    new BitVectorSocket[F]:

      private val withTimeout: F[Option[Array[Byte]]] => F[Option[Array[Byte]]] = readTimeout match
        case _: Duration.Infinite   => identity
        case finite: FiniteDuration => _.timeout(finite)

      private def readUntilN(nBytes: Int, carry: ByteVector): F[BitVector] =
        if carry.size < nBytes.toLong then
          withTimeout(socket.read(8192)).flatMap {
            case Some(bytes) => readUntilN(nBytes, carry ++ ByteVector(bytes))
            case None        => F.raiseError(SQLTimeoutException("Timeout while reading from socket"))
          }
        else
          val (output, remainder) = carry.splitAt(nBytes.toLong)
          carryRef.set(remainder).as(output.toBitVector)

      override def write(bits: BitVector): F[Unit] =
        socket.write(bits.bytes.toArray)

      override def read(nBytes: Int): F[BitVector] =
        carryRef.get.flatMap(carry => readUntilN(nBytes, carry))

  /**
   * Builds a [[BitVectorSocket]] over a connected socket: reads the initial handshake packet,
   * optionally upgrades to TLS, and prepares the carry buffer.
   *
   * @param sockets           the socket resource
   * @param sequenceIdRef     the MySQL packet sequence id
   * @param initialPacketRef  receives the server's initial packet
   * @param sslOptions        TLS negotiation options, if TLS is requested
   * @param readTimeout       the per-read timeout
   * @param capabilitiesFlags the negotiated capability flags
   */
  def apply[F[_]](
    sockets:           Resource[F, Socket[F]],
    sequenceIdRef:     Ref[F, Byte],
    initialPacketRef:  Ref[F, Option[InitialPacket]],
    sslOptions:        Option[SSLNegotiation.Options[F]],
    readTimeout:       Duration,
    capabilitiesFlags: Set[CapabilitiesFlags]
  )(using F: Concurrent[F], tls: TlsUpgrade[F]): Resource[F, BitVectorSocket[F]] =
    for
      socket        <- sockets
      initialPacket <- Resource.eval(Initial(socket).start)
      _             <- Resource.eval(initialPacketRef.set(Some(initialPacket)))
      socket$       <- sslOptions.fold(Resource.pure(socket))(option =>
                   SSLNegotiation.negotiateSSL(socket, capabilitiesFlags, option, sequenceIdRef)
                 )
      carryRef <- Resource.eval(Ref.of[F, ByteVector](ByteVector.empty))
    yield fromSocket(socket$, readTimeout, carryRef)
