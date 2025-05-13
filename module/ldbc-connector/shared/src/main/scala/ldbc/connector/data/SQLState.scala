/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

object SQLState:

  val TRANSIENT_CONNECTION_EXCEPTION           = "08000"
  val DATA_EXCEPTION                           = "22000"
  val BATCH_DELETE_EXCEPTION                   = "22007"
  val INVALID_AUTHORIZATION_SPEC_EXCEPTION     = "28000"
  val INTEGRITY_CONSTRAINT_VIOLATION_EXCEPTION = "23000"
  val TRANSACTION_ROLLBACK_EXCEPTION           = "40000"
  val SYNTAX_ERROR_EXCEPTION                   = "42000"
  val FEATURE_NOT_SUPPORTED_EXCEPTION          = "0A000"
  val BATCH_UPDATE_EXCEPTION                   = "HY000"
