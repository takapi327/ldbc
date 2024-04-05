/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import cats.syntax.all.*

import scodec.*
import scodec.bits.BitVector
import scodec.interop.cats.*

import ldbc.connector.data.*

case class ComChangeUserPacket(
  capabilitiesFlags: Seq[CapabilitiesFlags],
  user: String,
  database: Option[String],
  characterSet:      Int,
  pluginName: String,
  hashedPassword:    Array[Byte]
) extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    ComChangeUserPacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_CHANGE_USER Request"

object ComChangeUserPacket:

  val encoder: Encoder[ComChangeUserPacket] = Encoder { comChangeUserPacket =>
    
    val authPluginData = if comChangeUserPacket.capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_RESERVED2) then
      BitVector(comChangeUserPacket.hashedPassword.length) ++ BitVector(comChangeUserPacket.hashedPassword)
    else BitVector(comChangeUserPacket.hashedPassword)
    
    val database = comChangeUserPacket.database match
      case Some(db) => nullTerminatedStringCodec.encode(db).require
      case None => BitVector.empty
      
    val characterSet = if comChangeUserPacket.capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41) then
      BitVector(comChangeUserPacket.characterSet)
    else BitVector.empty

    val pluginName =
      if comChangeUserPacket.capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_PLUGIN_AUTH) then
        nullTerminatedStringCodec.encode(comChangeUserPacket.pluginName).require
      else BitVector.empty

    val attrs =
      if comChangeUserPacket.capabilitiesFlags.contains(CapabilitiesFlags.CLIENT_CONNECT_ATTRS) then BitVector(0x00)
      else BitVector.empty

    Attempt.successful(
      BitVector(CommandId.COM_CHANGE_USER) |+|
        nullTerminatedStringCodec.encode(comChangeUserPacket.user).require |+|
        authPluginData |+|
        database |+|
        characterSet |+|
        pluginName |+|
        attrs
    )
  }
