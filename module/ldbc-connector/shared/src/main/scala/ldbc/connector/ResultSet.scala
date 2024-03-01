/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import ldbc.connector.codec.Codec
import ldbc.connector.data.ColumnDefinitionFlags
import ldbc.connector.net.packet.response.*

trait ResultSet:

  def columns: Vector[ColumnDefinitionPacket]

  def rows: Vector[ResultSetRowPacket]

  def decode[T](codec: Codec[T]): List[T] = rows.map { row =>
    codec.decode(0, row.values) match
      case Left(value) =>
        val column = columns(value.offset)
        val dataType = column.flags.flatMap {
          case ColumnDefinitionFlags.UNSIGNED_FLAG => Some("UNSIGNED")
          case ColumnDefinitionFlags.ZEROFILL_FLAG => Some("ZEROFILL")
          case _                                   => None
        }
        throw new IllegalArgumentException(s"""
                                              |==========================
                                              |Failed to decode column: `${ column.name }`
                                              |Decode To: ${ column.columnType } ${ dataType.mkString(
                                               " "
                                             ) } -> ${ value.`type`.name.toUpperCase }
                                              |
                                              |Message [ ${ value.message } ]
                                              |==========================
                                              |""".stripMargin)
      case Right(value) => value
  }.toList
