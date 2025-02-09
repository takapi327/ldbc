/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import cats.syntax.all.*
import cats.MonadThrow

import fs2.io.net.Socket

import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.InitialPacket

/**
 * Initial packet is the first packet sent by the server to the client.
 * It contains the server version, connection id, and authentication plugin data.
 * The client uses this information to determine the authentication method to use.
 *
 * @tparam F
 *   the effect type
 */
trait Initial[F[_]]:

  def start: F[InitialPacket]

object Initial:

  def apply[F[_]](socket: Socket[F])(using F: MonadThrow[F]): Initial[F] =
    new Initial[F]:
      override def start: F[InitialPacket] =
        for
          header <- socket.read(4).flatMap {
                      case Some(chunk) => F.pure(chunk)
                      case None        => F.raiseError(new SQLException("Failed to read header"))
                    }
          payloadSize = parseHeader(header.toArray)
          payload <- socket.read(payloadSize).flatMap {
                       case Some(chunk) => F.pure(chunk)
                       case None        => F.raiseError(new SQLException("Failed to read payload"))
                     }
          initialPacket <- InitialPacket.decoder
                             .decode(payload.toBitVector)
                             .fold(
                               err =>
                                 F.raiseError[InitialPacket](
                                   new SQLException(
                                     message = err.message,
                                     detail  = Some(s"Failed to decode initial packet: ${ payload.toBitVector.toHex }")
                                   )
                                 ),
                               result => F.pure(result.value)
                             )
        yield initialPacket
