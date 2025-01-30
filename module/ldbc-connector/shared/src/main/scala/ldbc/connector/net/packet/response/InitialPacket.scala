/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import java.nio.charset.StandardCharsets.UTF_8
import scodec.*
import scodec.bits.*
import cats.syntax.all.*
import org.typelevel.otel4s.Attribute
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
 * @param statusFlags The status flags are a bit-field.
 * @param scrambleBuff The scramble buffer.
 * @param authPlugin The authentication plugin.
 */
case class InitialPacket(
  protocolVersion: Int,
  serverVersion:   Version,
  threadId:        Int,
  capabilityFlags: Set[CapabilitiesFlags],
  characterSet:    Int,
  statusFlags:     Set[ServerStatusFlags],
  scrambleBuff:    Array[Byte],
  authPlugin:      String
) extends ResponsePacket:

  def attributes: List[Attribute[String]] = List(
    Attribute("protocol.version", protocolVersion.toString),
    Attribute("server.version", serverVersion.toString),
    Attribute("thread.id", threadId.toString),
    Attribute("character.set", characterSet.toString),
    Attribute("auth.plugin", authPlugin)
  )

  override def toString: String = "InitialPacket"

object InitialPacket:

  val decoder: Decoder[InitialPacket] =
    (bits: BitVector) =>
      val (protocolVersion, reminder0) = bits.splitAt(8)
      val bytes = reminder0.bytes.takeWhile(_ != 0)
      val serverVersion = new String(bytes.toArray, UTF_8)
      val remainder1 = reminder0.drop((bytes.size + 1) * 8) // +1 is a null character, so *8 is a byte to bit
      val (threadId, reminder2) = remainder1.splitAt(32)
      val (authPluginDataPart1, reminder3) = reminder2.splitAt(64)
      val reminder4 = reminder3.drop(8) // Skip filter [0x00]
      val (capabilityFlagsLower, reminder5) = reminder4.splitAt(16)
      val (characterSet, reminder6) = reminder5.splitAt(8)
      val (statusFlag, reminder7) = reminder6.splitAt(16)
      val (capabilityFlagsUpper, reminder8) = reminder7.splitAt(16)
      val capabilityFlags = (capabilityFlagsUpper.toInt(false, ByteOrdering.LittleEndian) << 16) | capabilityFlagsLower.toInt(false, ByteOrdering.LittleEndian)
      val (authPluginDataPart2Length, reminder9) = if (capabilityFlags & (1 << 19)) != 0 then
        val (v1, v2) = reminder8.splitAt(8)
        (v1.toInt(false), v2)
      else (0, reminder8)
      val reminder10 = reminder9.drop(10 * 8) // Skip reserved bytes (10 bytes)
      val (authPluginDataPart2, reminder11) = reminder10.splitAt(math.max(13, authPluginDataPart2Length - 8) * 8)
      val authPluginName = if (capabilityFlags & (1 << 19)) != 0 then
        val bytes = reminder11.bytes.takeWhile(_ != 0)
        new String(bytes.toArray, UTF_8)
      else ""

      val version = Version(serverVersion) match
        case Some(v) => v
        case None => Version(0, 0, 0)

      val packet = InitialPacket(
        protocolVersion = protocolVersion.toInt(false),
        serverVersion = version,
        threadId = threadId.toInt(true, ByteOrdering.LittleEndian),
        capabilityFlags = CapabilitiesFlags(capabilityFlags),
        characterSet = characterSet.toInt(false, ByteOrdering.LittleEndian),
        statusFlags = ServerStatusFlags(statusFlag.toInt(false, ByteOrdering.LittleEndian)),
        scrambleBuff = authPluginDataPart1.toByteArray ++ authPluginDataPart2.toByteArray.dropRight(1),
        authPlugin = authPluginName
      )

      Attempt.successful(DecodeResult(packet, bits))
