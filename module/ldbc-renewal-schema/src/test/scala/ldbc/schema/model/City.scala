/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.model

import ldbc.schema.*

case class City(
                 id: Int,
                 name: String,
                 countryCode: String,
                 district: String,
                 population: Int
               )

class CityTable extends Table[City]("city"):

  def id: Column[Int] = column[Int]("ID")
  def name: Column[String] = column[String]("Name")
  def countryCode: Column[String] = column[String]("CountryCode")
  def district: Column[String] = column[String]("District")
  def population: Column[Int] = column[Int]("Population")

  override def * : Column[City] = (id *: name *: countryCode *: district *: population).to[City]
