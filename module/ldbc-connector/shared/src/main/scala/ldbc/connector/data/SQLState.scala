/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

object SQLState:
  
  val TRANSIENT_CONNECTION_EXCEPTION = "08"
  val DATA_EXCEPTION = "22"
  val INVALID_AUTHORIZATION_SPEC_EXCEPTION = "28"
  val INTEGRITY_CONSTRAINT_VIOLATION_EXCEPTION = "23"
  val TRANSACTION_ROLLBACK_EXCEPTION = "40"
  val SYNTAX_ERROR_EXCEPTION = "42"
  val FEATURE_NOT_SUPPORTED_EXCEPTION = "0A"
