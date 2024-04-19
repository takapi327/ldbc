/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

object SQLState:

  val TRANSIENT_CONNECTION_EXCEPTION           = "08000"
  val DATA_EXCEPTION                           = "22000"
  val INVALID_AUTHORIZATION_SPEC_EXCEPTION     = "28000"
  val INTEGRITY_CONSTRAINT_VIOLATION_EXCEPTION = "23000"
  val TRANSACTION_ROLLBACK_EXCEPTION           = "40000"
  val SYNTAX_ERROR_EXCEPTION                   = "42000"
  val FEATURE_NOT_SUPPORTED_EXCEPTION          = "0A000"
