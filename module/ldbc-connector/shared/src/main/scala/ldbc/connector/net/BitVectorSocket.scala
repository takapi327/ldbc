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

import ldbc.connector.exception.EofException

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

  /**
   * Construct a `BitVectorSocket` by wrapping an existing `Socket`.
   *
   * @param socket the underlying `Socket`
   * @group Constructors
   */
  def fromSocket[F[_]](
    socket: Socket[F],
    readTimeout: Duration,
    carryRef: Ref[F, Chunk[Byte]]
  )(using F: Temporal[F]): BitVectorSocket[F] =
    new BitVectorSocket[F]:

      private val withTimeout: F[Option[Chunk[Byte]]] => F[Option[Chunk[Byte]]] = readTimeout match
        case _: Duration.Infinite => identity
        case finite: FiniteDuration => _.timeout(finite)

      private def readUntilN(nBytes: Int, carry: Chunk[Byte]): F[BitVector] =
        if carry.size < nBytes then
          withTimeout(socket.read(8192)).flatMap {
            case Some(bytes) => readUntilN(nBytes, carry ++ bytes)
            case None => F.raiseError(EofException(nBytes, carry.size))
          }
        else
          val (output, remainder) = carry.splitAt(nBytes)
          carryRef.set(remainder).as(output.toBitVector)

      override def write(bits: BitVector): F[Unit] =
        socket.write(Chunk.byteVector(bits.bytes))

      override def read(nBytes: Int): F[BitVector] =
        // nb: unsafe for concurrent reads but protected by protocol mutex
        carryRef.get.flatMap(carry => readUntilN(nBytes, carry))

  def apply[F[_] : Temporal](
    socket: Resource[F, Socket[F]],
    sslOptions: Option[SSLNegotiation.Options[F]],
    readTimeout: Duration
  ): Resource[F, BitVectorSocket[F]] =
    for
      socket <- socket
      carryRef <- Resource.eval(Ref[F].of(Chunk.empty[Byte]))
    yield fromSocket(socket, readTimeout, carryRef)
