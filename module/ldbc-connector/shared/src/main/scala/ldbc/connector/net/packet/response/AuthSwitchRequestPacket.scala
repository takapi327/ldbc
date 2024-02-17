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
 * Authentication method Switch Request Packet
 *
 * If both server and the client support [[ldbc.connector.data.CapabilitiesFlags.CLIENT_PLUGIN_AUTH]] capability,
 * server can send this packet tp ask client to use another authentication method.
 *
 * @param status
 *   Type: int<1>
 *   Name: 0xFE (254)
 *   Description: status tag
 * @param pluginName
 *   Type: string<NUL>
 *   Name: plugin name
 *   Description: name of the client authentication plugin to switch to
 * @param pluginProvidedData
 *   Type: string<EOF>
 *   Name: plugin provided data
 *   Description: Initial authentication data for that client plugin
 */
case class AuthSwitchRequestPacket(
                                    status:                   Int,
                                    pluginName: String,
                                    pluginProvidedData: Array[Byte]
                                  ) extends AuthenticationPacket:

  override def toString: String = "Protocol::AuthSwitchRequest"

object AuthSwitchRequestPacket:

  val STATUS = 254

  val decoder: Decoder[AuthSwitchRequestPacket] =
    for
      pluginName <- nullTerminatedStringCodec.asDecoder
      pluginProvidedData <- bytes.asDecoder
    yield AuthSwitchRequestPacket(STATUS, pluginName, pluginProvidedData.toArray.dropRight(1))
