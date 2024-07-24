/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import scala.collection.immutable.SortedMap

import ldbc.connector.data.Parameter

/**
 * The subclass of {@link SQLException} for the SQLState class
 * value '<i>08</i>', or under vendor-specified conditions.  This indicates
 * that the connection operation that failed might be able to succeed if
 * the operation is retried without any application-level changes.
 */
class SQLTransientConnectionException(
  message:    String,
  sqlState:   Option[String]            = None,
  vendorCode: Option[Int]               = None,
  sql:        Option[String]            = None,
  detail:     Option[String]            = None,
  hint:       Option[String]            = None,
  params:     SortedMap[Int, Parameter] = SortedMap.empty
) extends SQLTransientException(message, sqlState, vendorCode, sql, detail, hint, params)
