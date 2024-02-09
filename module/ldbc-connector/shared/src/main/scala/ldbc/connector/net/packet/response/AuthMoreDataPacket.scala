/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.codecs.*

import cats.syntax.all.*

/**
 * We need to make sure that when sending plugin supplied data to the client they are not considered a special out-of-band command, like e.g.
 *
 * ERR_Packet, Protocol::AuthSwitchRequest: or OK_Packet. To avoid this the server will send all plugin data packets "wrapped" in a command \1. Note that the client will continue sending its replies unrwapped: Protocol::AuthSwitchResponse:
 *
 * @param status
 *   Type: int<1>
 *   Name: 0x01
 *   Description: status tag
 * @param authenticationMethodData
 *   Type: string<EOF>
 *   Name: authentication method data
 *   Description: Extra authentication data beyond the initial challenge
 */
case class AuthMoreDataPacket(
  status:                   Int,
  authenticationMethodData: Int
) extends AuthenticationPacket:

  override def toString: String = "Protocol::AuthMoreData"

object AuthMoreDataPacket:

  val STATUS = 1

  val decoder: Decoder[AuthMoreDataPacket] =
    for
      status <- uint4
      data   <- uint4
    yield AuthMoreDataPacket(status, data)
