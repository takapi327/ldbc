/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import ldbc.statement.formatter.Naming

import ldbc.query.builder.{ Column, Table }

import ldbc.schema.{ Table as SchemaTable, * }

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

  def id:          Column[Int]    = int("ID").unsigned.autoIncrement
  def name:        Column[String] = char(35, "Name").default("''")
  def countryCode: Column[String] = char(3, "CountryCode").default("''")
  def district:    Column[String] = char(20, "District").default("''")
  def population:  Column[Int]    = int().default(0)

  override def keys: List[Key] = List(
    PRIMARY_KEY(id),
    INDEX_KEY("CountryCode", countryCode),
    CONSTRAINT(
      "city_ibfk_1",
      FOREIGN_KEY(
        countryCode,
        REFERENCE(TableQuery[CountryTable])(_.code)
      )
    )
  )

  override def * : Column[City] = (id *: name *: countryCode *: district *: population).to[City]
