/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import cats.syntax.all.*

import scodec.*
import scodec.codecs.*

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
    for
      catalog      <- variableSizeBytes(uint8, utf8).asDecoder
      schema       <- variableSizeBytes(uint8, utf8).asDecoder
      table        <- variableSizeBytes(uint8, utf8).asDecoder
      orgTable     <- variableSizeBytes(uint8, utf8).asDecoder
      name         <- variableSizeBytes(uint8, utf8).asDecoder
      orgName      <- variableSizeBytes(uint8, utf8).asDecoder
      length       <- uint8.asDecoder
      characterSet <- uint16.asDecoder
      columnLength <- uint32.asDecoder
      columnType   <- uint8.asDecoder
      flags        <- uint16L.asDecoder
      decimals     <- int(1).asDecoder
    yield ColumnDefinition41Packet(
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
      flags        = ColumnDefinitionFlags(flags),
      decimals     = decimals
    )
