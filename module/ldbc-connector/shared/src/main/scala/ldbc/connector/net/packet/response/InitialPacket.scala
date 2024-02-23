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

import ldbc.connector.data.*
import ldbc.connector.util.Version

/**
 * Initial packet sent by the server to the client.
 *
 * @param protocolVersion The protocol version.
 * @param serverVersion The server version.
 * @param threadId The thread ID.
 * @param capabilityFlags The capability flags.
 * @param characterSet The character set.
 * @param scrambleBuff The scramble buffer.
 * @param authPlugin The authentication plugin.
 */
case class InitialPacket(
  protocolVersion: Int,
  serverVersion:   Version,
  threadId:        Int,
  capabilityFlags: Seq[CapabilitiesFlags],
  characterSet:    Int,
  scrambleBuff:    Array[Byte],
  authPlugin:      String
) extends ResponsePacket:

  override def toString: String = "InitialPacket"

object InitialPacket:

  private val protocolVersionCodec: Codec[Int] = uint8
  private val threadIdCodec:        Codec[Int] = int32
  private val authPluginDataPart1Codec: Codec[(Byte, Byte, Byte, Byte, Byte, Byte, Byte, Byte)] =
    byte :: byte :: byte :: byte :: byte :: byte :: byte :: byte
  private val capabilityFlagsLowerCodec: Codec[Int] = int16
  private val capabilityFlagsUpperCodec: Codec[Int] = int16

  val decoder: Decoder[InitialPacket] =
    for
      protocolVersion <- protocolVersionCodec.asDecoder
      serverVersion   <- nullTerminatedStringCodec.asDecoder
      threadId        <- threadIdCodec.asDecoder
      authPluginDataPart1 <- authPluginDataPart1Codec.map {
                               case (a, b, c, d, e, f, g, h) => Array(a, b, c, d, e, f, g, h)
                             }
      _                    <- ignore(8)     // Skip filter [0x00]
      capabilityFlagsLower <- capabilityFlagsLowerCodec.asDecoder
      characterSet         <- uint8L.asDecoder
      _                    <- ignore(16) // Skip character set and status flags
      capabilityFlagsUpper <- capabilityFlagsUpperCodec.asDecoder
      capabilityFlags = (capabilityFlagsUpper << 16) | capabilityFlagsLower
      authPluginDataPart2Length <- if (capabilityFlags & (1 << 19)) != 0 then uint8.asDecoder else Decoder.pure(0)
      _                         <- ignore(10 * 8) // Skip reserved bytes (10 bytes)
      authPluginDataPart2       <- bytes(math.max(13, authPluginDataPart2Length - 8)).asDecoder
      authPluginName <-
        if (capabilityFlags & (1 << 19)) != 0 then nullTerminatedStringCodec.asDecoder else Decoder.pure("")
    yield
      val capabilityFlags = (capabilityFlagsUpper << 16) | capabilityFlagsLower
      val version = Version(serverVersion) match
        case Some(v) => v
        case None    => Version(0, 0, 0)

      InitialPacket(
        protocolVersion,
        version,
        threadId,
        CapabilitiesFlags(capabilityFlags),
        characterSet,
        authPluginDataPart1 ++ authPluginDataPart2.toArray.dropRight(1),
        authPluginName
      )
