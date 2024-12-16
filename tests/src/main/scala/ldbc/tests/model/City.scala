/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import ldbc.query.builder.{ Table, Column }
import ldbc.query.builder.formatter.Naming
import ldbc.schema.Table as SchemaTable

given Naming = Naming.PASCAL

case class City(
  @Column("ID") id: Int,
  name:             String,
  countryCode:      String,
  district:         String,
  population:       Int
)

object City:

  given Table[City] = Table.derived[City]("city")

class CityTable extends SchemaTable[City]("city"):

  def id:          Column[Int]    = column[Int]("ID")
  def name:        Column[String] = column[String]("Name")
  def countryCode: Column[String] = column[String]("CountryCode")
  def district:    Column[String] = column[String]("District")
  def population:  Column[Int]    = column[Int]("Population")

  override def * : Column[City] = (id *: name *: countryCode *: district *: population).to[City]
