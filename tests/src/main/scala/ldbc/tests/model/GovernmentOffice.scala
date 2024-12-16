/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import java.time.LocalDate

import ldbc.query.builder.{ Table, Column }
import ldbc.schema.Table as SchemaTable

case class GovernmentOffice(
  @Column("ID") id:         Int,
  @Column("CityID") cityId: Int,
  name:                     String,
  establishmentDate:        Option[LocalDate]
) derives Table

class GovernmentOfficeTable extends SchemaTable[GovernmentOffice]("government_office"):

  def id:                Column[Int]               = column[Int]("ID")
  def cityId:            Column[Int]               = column[Int]("CityID")
  def name:              Column[String]            = column[String]("Name")
  def establishmentDate: Column[Option[LocalDate]] = column[Option[LocalDate]]("EstablishmentDate")

  override def * : Column[GovernmentOffice] = (id *: cityId *: name *: establishmentDate).to[GovernmentOffice]
