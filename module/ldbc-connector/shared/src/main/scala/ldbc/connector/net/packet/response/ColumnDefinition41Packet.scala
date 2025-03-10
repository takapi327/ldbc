/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import java.nio.charset.StandardCharsets.UTF_8

import scodec.*
import scodec.bits.BitVector

import cats.syntax.all.*

import ldbc.connector.data.*

/**
 * The column definition packet is sent by the server to the client in response to a COM_QUERY command.
 *
 * @param catalog
 *   Type: string<lenenc>
 *   Name: catalog
 *   Description: The catalog used. Currently always "def"
 * @param schema
 *   Type: string<lenenc>
 *   Name: schema
 *   Description: schema name
 * @param table
 *   Type: string<lenenc>
 *   Name: table
 *   Description: virtual table name
 * @param orgTable
 *   Type: string<lenenc>
 *   Name: org_table
 *   Description: physical table name
 * @param name
 *   Type: string<lenenc>
 *   Name: name
 *   Description: virtual column name
 * @param orgName
 *   Type: string<lenenc>
 *   Name: org_name
 *   Description: physical column name
 * @param length
 *   Type: int<lenenc>
 *   Name: length of fixed length fields
 *   Description: 0x0c
 * @param characterSet
 *   Type: int<2>
 *   Name: character_set
 *   Description: the column character set as defined in Character Set
 * @param columnLength
 *   Type: int<4>
 *   Name: column_length
 *   Description: maximum length of the field
 * @param columnType
 *   Type: int<1>
 *   Name: type
 *   Description: type of the column as defined in enum_field_types
 * @param flags
 *   Type: int<2>
 *   Name: flags
 *   Description: Flags as defined in Column Definition Flags
 * @param decimals
 *   Type: int<1>
 *   Name: decimals
 *   Description: max shown decimal digits
 *     - 0x00 for integers and static strings
 *     - 0x1f for dynamic strings, double, float
 *     - 0x00 to 0x51 for decimals
 */
case class ColumnDefinition41Packet(
  catalog:      String,
  schema:       String,
  table:        String,
  orgTable:     String,
  name:         String,
  orgName:      String,
  length:       Int,
  characterSet: Int,
  columnLength: Long,
  columnType:   ColumnDataType,
  flags:        Seq[ColumnDefinitionFlags],
  decimals:     Int
) extends ColumnDefinitionPacket:

  override def toString: String = "Protocol::ColumnDefinition41"

object ColumnDefinition41Packet:

  val decoder: Decoder[ColumnDefinition41Packet] =
    (bits: BitVector) =>
      val bytes  = bits.toByteArray
      var offset = 0

      val catalogSize = bytes(offset) & 0xff
      offset += 1
      val catalog = new String(bytes, offset, catalogSize, UTF_8)
      offset += catalogSize

      val schemaSize = bytes(offset) & 0xff
      offset += 1
      val schema = new String(bytes, offset, schemaSize, UTF_8)
      offset += schemaSize

      val tableSize = bytes(offset) & 0xff
      offset += 1
      val table = new String(bytes, offset, tableSize, UTF_8)
      offset += tableSize

      val orgTableSize = bytes(offset) & 0xff
      offset += 1
      val orgTable = new String(bytes, offset, orgTableSize, UTF_8)
      offset += orgTableSize

      val nameSize = bytes(offset) & 0xff
      offset += 1
      val name = new String(bytes, offset, nameSize, UTF_8)
      offset += nameSize

      val orgNameSize = bytes(offset) & 0xff
      offset += 1
      val orgName = new String(bytes, offset, orgNameSize, UTF_8)
      offset += orgNameSize

      val length = bytes(offset) & 0xff
      offset += 1

      val characterSet = (bytes(offset) & 0xff) | ((bytes(offset + 1) & 0xff) << 8)
      offset += 2

      val columnLength = (bytes(offset) & 0xff) |
        ((bytes(offset + 1) & 0xff) << 8) |
        ((bytes(offset + 2) & 0xff) << 16) |
        ((bytes(offset + 3) & 0xff) << 24)
      offset += 4

      val columnType = bytes(offset) & 0xff
      offset += 1

      val flags = ColumnDefinitionFlags(bytes(offset) & 0xff)
      offset += 2

      val decimals = bytes(offset) & 0xff

      val packet = ColumnDefinition41Packet(
        catalog      = catalog,
        schema       = schema,
        table        = table,
        orgTable     = orgTable,
        name         = name,
        orgName      = orgName,
        length       = length,
        characterSet = characterSet,
        columnLength = columnLength,
        columnType   = ColumnDataType(columnType),
        flags        = flags,
        decimals     = decimals
      )
      Attempt.successful(DecodeResult(packet, bits))
