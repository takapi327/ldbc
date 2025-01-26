/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import scodec.Codec

trait NumericCodecs:

  given Codec[BigDecimal] =
    Codec[String].xmap(
      str => BigDecimal(str),
      _.toString()
    )

object numeric extends NumericCodecs
