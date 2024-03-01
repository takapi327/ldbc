/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import ldbc.connector.codec.Codec
import ldbc.connector.net.packet.response.*

trait ResultSet:
  
  def columns: Vector[ColumnDefinitionPacket]

  def rows: Vector[ResultSetRowPacket]

  def decode[T](codec: Codec[T]): List[T] = rows.map { row =>
    codec.decode(0, row.values) match
      case Left(value) =>
        val column = columns(value.offset)
        throw new IllegalArgumentException(s"""
                                              |==========================
                                              |Failed to decode column: `${ column.name }`
                                              |Decode To: ${ column.columnType } -> ${ value.`type`.name.toUpperCase }
                                              |
                                              |Message [ ${ value.message } ]
                                              |==========================
                                              |""".stripMargin)
      case Right(value) => value
  }.toList
