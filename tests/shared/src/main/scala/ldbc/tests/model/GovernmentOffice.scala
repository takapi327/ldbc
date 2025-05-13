/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import java.time.LocalDate

import ldbc.dsl.codec.Codec

import ldbc.query.builder.{ Column, Table }

import ldbc.schema.Table as SchemaTable

case class GovernmentOffice(
  @Column("ID") id:         Int,
  @Column("CityID") cityId: Int,
  name:                     String,
  establishmentDate:        Option[LocalDate]
)

object GovernmentOffice:

  given Codec[GovernmentOffice] = Codec.derived[GovernmentOffice]
  given Table[GovernmentOffice] = Table.derived[GovernmentOffice]("government_office")

class GovernmentOfficeTable extends SchemaTable[GovernmentOffice]("government_office"):

  def id:                Column[Int]               = column[Int]("ID")
  def cityId:            Column[Int]               = column[Int]("CityID")
  def name:              Column[String]            = column[String]("Name")
  def establishmentDate: Column[Option[LocalDate]] = column[Option[LocalDate]]("EstablishmentDate")

  override def * : Column[GovernmentOffice] = (id *: cityId *: name *: establishmentDate).to[GovernmentOffice]
