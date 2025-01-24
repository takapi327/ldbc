/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import scodec.bits.ByteVector

trait AuthenticationPlugin[F[_]]:

  def name: String

  def hashPassword(password: String, scramble: Array[Byte]): F[ByteVector]
