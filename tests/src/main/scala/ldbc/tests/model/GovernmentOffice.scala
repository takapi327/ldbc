/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import java.time.LocalDate

import ldbc.query.builder.Table

case class GovernmentOffice(
  id:                Int,
  cityId:            Int,
  name:              String,
  establishmentDate: Option[LocalDate]
) derives Table
