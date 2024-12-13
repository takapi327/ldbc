/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.model

import java.time.LocalDate

import ldbc.schema.*

case class GovernmentOffice(
  id:                Int,
  cityId:            Int,
  name:              String,
  establishmentDate: Option[LocalDate]
)

class GovernmentOfficeTable extends Table[GovernmentOffice]("government_office"):

  def id:                Column[Int]               = column[Int]("ID")
  def cityId:            Column[Int]               = column[Int]("CityID")
  def name:              Column[String]            = column[String]("Name")
  def establishmentDate: Column[Option[LocalDate]] = column[Option[LocalDate]]("EstablishmentDate")

  override def * : Column[GovernmentOffice] = (id *: cityId *: name *: establishmentDate).to[GovernmentOffice]
