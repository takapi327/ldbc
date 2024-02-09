/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

trait AuthenticationPlugin:

  def name: String

  def hashPassword(password: String, scramble: Array[Byte]): Array[Byte]
