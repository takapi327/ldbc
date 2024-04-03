/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import cats.syntax.all.*

import scodec.*
import scodec.codecs.*
import scodec.bits.BitVector
import scodec.interop.cats.*

import ldbc.connector.data.*

/**
 * A request packet to set the option of the connection.
 * 
 * @param optionOperation
 *   Enables or disables an option for the connection. option can have one of the following values.
 *   - [[EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON]]
 *   - [[EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF]]
 */
case class ComSetOptionPacket(optionOperation: EnumMySQLSetOption) extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    ComSetOptionPacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_SET_OPTION Request"

object ComSetOptionPacket:
  
  val encoder: Encoder[ComSetOptionPacket] = Encoder { comSetOptionPacket =>
    Attempt.successful(
      BitVector(CommandId.COM_SET_OPTION) |+| uint16L.encode(comSetOptionPacket.optionOperation.code).require
    )
  }
