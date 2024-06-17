/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import ldbc.query.builder.Table
import ldbc.query.builder.formatter.Naming

given Naming = Naming.PASCAL

case class City(
  id:          Int,
  name:        String,
  countryCode: String,
  district:    String,
  population:  Int
) derives Table
