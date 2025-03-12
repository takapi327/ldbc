/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import java.nio.charset.StandardCharsets.UTF_8

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

  def decode(bytes: Array[Byte]): InitialPacket =
    var offset = 0

    val protocolVersion = bytes(offset)
    offset += 1

    val serverVersionEnd = bytes.indexWhere(_ == 0, offset)
    val serverVersion    = new String(bytes.slice(offset, serverVersionEnd), UTF_8)
    offset = serverVersionEnd + 1

    val threadId = (bytes(offset + 3) & 0xff) << 24 |
      (bytes(offset + 2) & 0xff) << 16 |
      (bytes(offset + 1) & 0xff) << 8 |
      (bytes(offset) & 0xff)
    offset += 4

    val authPluginDataPart1 = bytes.slice(offset, offset + 8)
    offset += 8

    offset += 1 // Skip filter [0x00]

    val capabilityFlagsLower = (bytes(offset + 1) & 0xff) << 8 | (bytes(offset) & 0xff)
    offset += 2

    val characterSet = bytes(offset)
    offset += 1

    val statusFlags = (bytes(offset + 1) & 0xff) << 8 | (bytes(offset) & 0xff)
    offset += 2

    val capabilityFlagsUpper = (bytes(offset + 1) & 0xff) << 8 | (bytes(offset) & 0xff)
    offset += 2

    val capabilityFlags: Int = (capabilityFlagsUpper << 16) | capabilityFlagsLower

    val (authPluginDataPart2Length, newOffset) =
      if (capabilityFlags & (1 << 19)) != 0 then (bytes(offset) & 0xff, offset + 1)
      else (0, offset)
    offset = newOffset

    offset += 10 // Skip reserved bytes (10 bytes)

    val authPluginDataPart2Length2 = math.max(13, authPluginDataPart2Length - 8)
    val authPluginDataPart2        = bytes.slice(offset, offset + authPluginDataPart2Length2)
    offset += authPluginDataPart2Length2

    val authPluginName = if (capabilityFlags & (1 << 19)) != 0 then
      val end = bytes.indexWhere(_ == 0, offset)
      new String(bytes.slice(offset, end), UTF_8)
    else ""

    val version = Version(serverVersion) match
      case Some(v) => v
      case None    => Version(0, 0, 0)

    InitialPacket(
      protocolVersion = protocolVersion & 0xff,
      serverVersion   = version,
      threadId        = threadId,
      capabilityFlags = CapabilitiesFlags(capabilityFlags),
      characterSet    = characterSet & 0xff,
      statusFlags     = ServerStatusFlags(statusFlags),
      scrambleBuff    = authPluginDataPart1 ++ authPluginDataPart2.dropRight(1),
      authPlugin      = authPluginName
    )
