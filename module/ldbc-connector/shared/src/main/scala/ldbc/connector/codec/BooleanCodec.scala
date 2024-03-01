/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import ldbc.connector.data.Type

trait BooleanCodec:

  val boolean: Codec[Boolean] = Codec.simple(
    _.toString,
    {
      case "true" | "1"  => Right(true)
      case "false" | "0" => Right(false)
      case unknown       => Left(s"Invalid boolean value: $unknown")
    },
    Type.boolean
  )

object boolean extends BooleanCodec
