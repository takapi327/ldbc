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

/**
 * The column definition packet is sent by the server to the client in response to a COM_QUERY command.
 *
 * @param table
 *   Type: string<lenenc>
 *   Name: table
 *   Description: Table name
 * @param name
 *   Type: string<lenenc>
 *   Name: name
 *   Description: Column name
 * @param length
 *   Type: int<lenenc>
 *   Name: length of fixed length fields
 *   Description: 0x0c
 * @param columnType
 *   Type: int<1>
 *   Name: type
 *   Description: type of the column as defined in enum_field_types
 * @param flagsLength
 *   Type: int<lenenc>
 *   Name: length of flags + decimals fields	
 *   Description: [03]
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
case class ColumnDefinition320Packet(
  table:       String,
  name:        String,
  length:      Int,
  columnType:  ColumnDataType,
  flagsLength: Int,
  flags:       Seq[ColumnDefinitionFlags],
  decimals:    Int
) extends ColumnDefinitionPacket:

  override def toString: String = "Protocol::ColumnDefinition41"

object ColumnDefinition320Packet:

  val decoder: Decoder[ColumnDefinition320Packet] =
    for
      table       <- variableSizeBytes(uint8, utf8).asDecoder
      name        <- variableSizeBytes(uint8, utf8).asDecoder
      length      <- uint8.asDecoder
      columnType  <- uint8.asDecoder
      flagsLength <- uint8.asDecoder
      flags       <- uint16L.asDecoder
      decimals    <- int(1).asDecoder
    yield ColumnDefinition320Packet(
      table       = table,
      name        = name,
      length      = length,
      columnType  = ColumnDataType(columnType),
      flagsLength = flagsLength,
      flags       = ColumnDefinitionFlags(flags),
      decimals    = decimals
    )
