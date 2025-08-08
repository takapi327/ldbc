/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import scala.collection.immutable.SortedMap

import ldbc.connector.data.Parameter

/**
 * The subclass of {@link SQLException} is thrown in situations where a
 * previously failed operation might be able to succeed when the operation is
 * retried without any intervention by application-level functionality.
 */
class SQLTransientException(
  message:    String,
  sqlState:   Option[String]            = None,
  vendorCode: Option[Int]               = None,
  sql:        Option[String]            = None,
  detail:     Option[String]            = None,
  hint:       Option[String]            = None,
  params:     SortedMap[Int, Parameter] = SortedMap.empty
) extends SQLException(message, sqlState, vendorCode, sql, detail, hint, params)
