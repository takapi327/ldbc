/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.model

import ldbc.core.TableOption

object Table:

  case class CreateStatement(
    tableName:         String,
    columnDefinitions: List[ColumnDefinition],
    keyDefinitions:    List[Key],
    options:           Option[List[TableOption]]
  )

  case class DropStatement(tableName: String)

  def buildTableOptionCode(option: TableOption): String =
    option match
      case TableOption.AutoExtendSize(value)           => s"TableOption.AutoExtendSize($value)"
      case TableOption.AutoIncrement(value)            => s"TableOption.AutoIncrement($value)"
      case TableOption.AVGRowLength(value)             => s"TableOption.AVGRowLength($value)"
      case TableOption.Character(value)                => s"Character.$value"
      case TableOption.CheckSum(value)                 => s"TableOption.CheckSum($value)"
      case TableOption.Collate(value)                  => s"Collate.$value"
      case TableOption.Comment(value)                  => s"TableOption.Comment(\"$value\")"
      case TableOption.Compression(value)              => s"TableOption.Compression(\"$value\")"
      case TableOption.Connection(value)               => s"TableOption.Connection(\"$value\")"
      case TableOption.Directory(str, value)           => s"TableOption.Directory(\"$str\", \"$value\")"
      case TableOption.DelayKeyWrite(value)            => s"TableOption.DelayKeyWrite($value)"
      case TableOption.Encryption(value)               => s"TableOption.Encryption(\"$value\")"
      case TableOption.Engine(value)                   => s"TableOption.Engine(\"$value\")"
      case TableOption.EngineAttribute(value)          => s"TableOption.EngineAttribute(\"$value\")"
      case TableOption.InsertMethod(value)             => s"TableOption.InsertMethod(\"$value\")"
      case TableOption.KeyBlockSize(value)             => s"TableOption.KeyBlockSize($value)"
      case TableOption.MaxRows(value)                  => s"TableOption.MaxRows($value)"
      case TableOption.MinRows(value)                  => s"TableOption.MinRows($value)"
      case TableOption.PackKeys(value)                 => s"TableOption.PackKeys(\"$value\")"
      case TableOption.RowFormat(value)                => s"TableOption.RowFormat(\"$value\")"
      case TableOption.SecondaryEngineAttribute(value) => s"TableOption.SecondaryEngineAttribute(\"$value\")"
      case TableOption.StatsAutoRecalc(value)          => s"TableOption.StatsAutoRecalc(\"$value\")"
      case TableOption.StatsPersistent(value)          => s"TableOption.StatsPersistent(\"$value\")"
      case TableOption.StatsSamplePages(value)         => s"TableOption.StatsSamplePages($value)"
      case TableOption.Tablespace(name, value)         => s"TableOption.Tablespace(\"$name\", \"$value\")"
      case TableOption.Union(value) => s"TableOption.Union(List(${ value.map(str => s"\"$str\"").mkString(",") }))"
