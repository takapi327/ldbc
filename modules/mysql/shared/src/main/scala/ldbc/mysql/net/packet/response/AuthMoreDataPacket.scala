/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.packet
package response

import scodec.*
import scodec.bits.BitVector

/**
 * We need to make sure that when sending plugin supplied data to the client they are not considered a special out-of-band command, like e.g.
 *
 * ERR_Packet, Protocol::AuthSwitchRequest: or OK_Packet. To avoid this the server will send all plugin data packets "wrapped" in a command \1. Note that the client will continue sending its replies unrwapped: Protocol[F]::AuthSwitchResponse:
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
  authenticationMethodData: Array[Byte]
) extends AuthenticationPacket:

  override def toString: String = "Protocol::AuthMoreData"

object AuthMoreDataPacket:

  val STATUS = 1

  /**
   * Decoder of AuthMoreData, reading the status tag rather than assuming it.
   *
   * The tag is part of the payload, so this decoder is handed the packet untouched — including the
   * leading `0x01` — and strips it itself. Leaving that to the caller is what makes the same field
   * mean two different things depending on which code path decoded it, and puts the knowledge of the
   * wire format somewhere other than the decoder.
   *
   * The split is written out rather than composed from `uint8 :: bits`. Measured on this layout the
   * combinator form runs at roughly a third of the speed for the short full-authentication marker and
   * two thirds for a public key, which matches the note on [[EOFPacket.decoder]].
   */
  val decoder: Decoder[AuthMoreDataPacket] =
    (bits: BitVector) =>
      val status = bits.take(8).toInt(signed = false)
      val data   = bits.drop(8).bytes.toArray
      Attempt.successful(DecodeResult(AuthMoreDataPacket(status, data), BitVector.empty))
