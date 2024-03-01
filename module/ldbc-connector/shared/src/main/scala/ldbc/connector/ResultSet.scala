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
      case Left(value) => throw new IllegalArgumentException(s"decode error, ${value.message}")
      case Right(value) => value
  }.toList
