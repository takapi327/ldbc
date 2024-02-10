/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.syntax.temporal.*

import fs2.Chunk
import fs2.io.net.Socket

import scodec.bits.BitVector

import ldbc.connector.exception.*
import ldbc.connector.net.packet.response.InitialPacket

/**
 *  A higher-level `Socket` interface defined in terms of `BitVector`.
 */
trait BitVectorSocket[F[_]]:

  /** Write the specified bits to the socket. */
  def write(bits: BitVector): F[Unit]

  /**
   * Read `nBytes` bytes (not bits!) from the socket, or fail with an exception if EOF is reached
   * before `nBytes` bytes are received.
   */
  def read(nBytes: Int): F[BitVector]

object BitVectorSocket:

  private def parseHeader(chunk: Chunk[Byte]): Int =
    val headerBytes = chunk.toArray
    (headerBytes(0) & 0xff) | ((headerBytes(1) & 0xff) << 8) | ((headerBytes(2) & 0xff) << 16)

  private def readInitialPacket[F[_]: Temporal](
    socket: Socket[F]
  )(using ev: ApplicativeError[F, Throwable]): F[InitialPacket] =
    for
      header <- socket.read(4).flatMap {
                  case Some(chunk) => Monad[F].pure(chunk)
                  case None        => ev.raiseError(new Exception("Failed to read header"))
                }
      payloadSize = parseHeader(header)
      payload <- socket.read(payloadSize).flatMap {
                   case Some(chunk) => Monad[F].pure(chunk)
                   case None        => ev.raiseError(new Exception("Failed to read payload"))
                 }
      initialPacket <- InitialPacket.decoder
                         .decode(payload.toBitVector)
                         .fold(
                           err =>
                             ev.raiseError[InitialPacket](
                               new MySQLException(
                                 None,
                                 s"Failed to decode initial packet: $err ${ payload.toBitVector.toHex }"
                               )
                             ),
                           result => Monad[F].pure(result.value)
                         )
    yield initialPacket

  /**
   * Construct a `BitVectorSocket` by wrapping an existing `Socket`.
   *
   * @param socket the underlying `Socket`
   * @group Constructors
   */
  def fromSocket[F[_]](
    socket:      Socket[F],
    readTimeout: Duration,
    carryRef:    Ref[F, Chunk[Byte]]
  )(using F: Temporal[F]): BitVectorSocket[F] =
    new BitVectorSocket[F]:

      private val withTimeout: F[Option[Chunk[Byte]]] => F[Option[Chunk[Byte]]] = readTimeout match
        case _: Duration.Infinite   => identity
        case finite: FiniteDuration => _.timeout(finite)

      private def readUntilN(nBytes: Int, carry: Chunk[Byte]): F[BitVector] =
        if carry.size < nBytes then
          withTimeout(socket.read(8192)).flatMap {
            case Some(bytes) => readUntilN(nBytes, carry ++ bytes)
            case None        => F.raiseError(EofException(nBytes, carry.size))
          }
        else
          val (output, remainder) = carry.splitAt(nBytes)
          carryRef.set(remainder).as(output.toBitVector)

      override def write(bits: BitVector): F[Unit] =
        socket.write(Chunk.byteVector(bits.bytes))

      override def read(nBytes: Int): F[BitVector] =
        // nb: unsafe for concurrent reads but protected by protocol mutex
        carryRef.get.flatMap(carry => readUntilN(nBytes, carry))

  def apply[F[_]: Temporal](
    sockets:          Resource[F, Socket[F]],
    sequenceIdRef:    Ref[F, Byte],
    initialPacketRef: Ref[F, Option[InitialPacket]],
    sslOptions:       Option[SSLNegotiation.Options[F]],
    readTimeout:      Duration
  ): Resource[F, BitVectorSocket[F]] =
    for
      socket        <- sockets
      initialPacket <- Resource.eval(readInitialPacket(socket))
      _             <- Resource.eval(initialPacketRef.set(Some(initialPacket)))
      socket$ <- sslOptions.fold(socket.pure[Resource[F, *]])(option =>
                   SSLNegotiation.negotiateSSL(socket, initialPacket.capabilityFlags, option, sequenceIdRef)
                 )
      carryRef <- Resource.eval(Ref[F].of(Chunk.empty[Byte]))
    yield fromSocket(socket$, readTimeout, carryRef)
