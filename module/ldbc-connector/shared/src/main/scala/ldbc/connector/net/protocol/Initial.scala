/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import scala.io.AnsiColor

import cats.{ Monad, ApplicativeError }
import cats.syntax.all.*

import cats.effect.Temporal
import cats.effect.std.Console

import fs2.Chunk
import fs2.io.net.Socket

import ldbc.connector.exception.MySQLException
import ldbc.connector.net.packet.response.InitialPacket

trait Initial[F[_]]:

  def start: F[InitialPacket]

object Initial:

  private def parseHeader(chunk: Chunk[Byte]): Int =
    val headerBytes = chunk.toArray
    (headerBytes(0) & 0xff) | ((headerBytes(1) & 0xff) << 8) | ((headerBytes(2) & 0xff) << 16)

  def apply[F[_]: Temporal: Console](socket: Socket[F])(using ev: ApplicativeError[F, Throwable]): Initial[F] =
    new Initial[F]:
      override def start: F[InitialPacket] =
        for
          header <- socket.read(4).flatMap {
                      case Some(chunk) => Monad[F].pure(chunk)
                      case None        => ev.raiseError(new MySQLException("Failed to read header"))
                    }
          payloadSize = parseHeader(header)
          payload <- socket.read(payloadSize).flatMap {
                       case Some(chunk) => Monad[F].pure(chunk)
                       case None        => ev.raiseError(new MySQLException("Failed to read payload"))
                     }
          initialPacket <- InitialPacket.decoder
                             .decode(payload.toBitVector)
                             .fold(
                               err =>
                                 ev.raiseError[InitialPacket](
                                   new MySQLException(
                                     s"Failed to decode initial packet: $err ${ payload.toBitVector.toHex }"
                                   )
                                 ),
                               result => Monad[F].pure(result.value)
                             )
          _ <-
            Console[F].println(
              s"[1] Client ${ AnsiColor.BLUE }←${ AnsiColor.RESET } Server: ${ AnsiColor.GREEN }$initialPacket${ AnsiColor.RESET }"
            )
        yield initialPacket
