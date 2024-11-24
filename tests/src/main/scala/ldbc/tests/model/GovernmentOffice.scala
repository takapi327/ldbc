/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import java.time.LocalDate

import ldbc.query.builder.{Table, Column}

case class GovernmentOffice(
  @Column("ID") id:                Int,
  @Column("CityID") cityId:            Int,
  name:              String,
  establishmentDate: Option[LocalDate]
) derives Table
