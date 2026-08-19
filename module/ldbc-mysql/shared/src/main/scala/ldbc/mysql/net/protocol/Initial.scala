/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.protocol

import ldbc.sql.SQLException
import ldbc.fx.Fx

import ldbc.net.Socket

import ldbc.mysql.net.packet.response.InitialPacket

/**
 * Initial packet is the first packet sent by the server to the client. It contains the server version,
 * connection id, and authentication plugin data. The client uses this information to determine the
 * authentication method to use.
 */
trait Initial:

  def start: Fx[InitialPacket]

object Initial:

  /**
   * Reads the server's initial handshake packet from `socket`.
   *
   * @param socket the freshly connected socket
   */
  def apply(socket: Socket): Initial =
    new Initial:
      override def start: Fx[InitialPacket] =
        for
          header <- socket.read(4).flatMap {
                      case Some(bytes) => Fx.pure(bytes)
                      case None        => Fx.raiseError(new SQLException("Failed to read header"))
                    }
          payloadSize = parseHeader(header)
          payload <- socket.read(payloadSize).flatMap {
                       case Some(bytes) => Fx.pure(bytes)
                       case None        => Fx.raiseError(new SQLException("Failed to read payload"))
                     }
        yield InitialPacket.decode(payload)
