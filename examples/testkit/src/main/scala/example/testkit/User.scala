/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package example

import ldbc.dsl.codec.Codec

case class User(id: Long, name: String, email: String)

object User:
  given Codec[User] = Codec.derived[User]
