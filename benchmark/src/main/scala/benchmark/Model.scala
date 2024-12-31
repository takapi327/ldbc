/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark

import ldbc.dsl.codec.*
import ldbc.query.builder.Table
import ldbc.query.builder.formatter.Naming

given Naming = Naming.PASCAL

case class Model1(
  c1: Int
) derives Table

object Model1:
  given Codec[Model1] = Codec[Int].to[Model1]

case class Model5(
  c1: Int,
  c2: Int,
  c3: Int,
  c4: Int,
  c5: Int
) derives Table

case class Model10(
  c1:  Int,
  c2:  Int,
  c3:  Int,
  c4:  Int,
  c5:  Int,
  c6:  Int,
  c7:  Int,
  c8:  Int,
  c9:  Int,
  c10: Int
) derives Table

case class Model20(
  c1:  Int,
  c2:  Int,
  c3:  Int,
  c4:  Int,
  c5:  Int,
  c6:  Int,
  c7:  Int,
  c8:  Int,
  c9:  Int,
  c10: Int,
  c11: Int,
  c12: Int,
  c13: Int,
  c14: Int,
  c15: Int,
  c16: Int,
  c17: Int,
  c18: Int,
  c19: Int,
  c20: Int
) derives Table

object Model20:
  given Codec[Model20] =
    Codec[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)]
      .to[Model20]

case class Model25(
  c1:  Int,
  c2:  Int,
  c3:  Int,
  c4:  Int,
  c5:  Int,
  c6:  Int,
  c7:  Int,
  c8:  Int,
  c9:  Int,
  c10: Int,
  c11: Int,
  c12: Int,
  c13: Int,
  c14: Int,
  c15: Int,
  c16: Int,
  c17: Int,
  c18: Int,
  c19: Int,
  c20: Int,
  c21: Int,
  c22: Int,
  c23: Int,
  c24: Int,
  c25: Int
) derives Table

object Model25:
  given Codec[Model25] = Codec[
    (
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int,
      Int
    )
  ].to[Model25]

case class City(
  id:          Int,
  name:        String,
  countryCode: String,
  district:    String,
  population:  Int
)

object City:
  given Table[City] = Table.derived[City]("city")
