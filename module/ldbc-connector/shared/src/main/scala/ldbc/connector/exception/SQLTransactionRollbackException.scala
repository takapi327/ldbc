/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import scala.collection.immutable.ListMap

import ldbc.connector.data.Parameter

/**
 * The subclass of {@link SQLException} thrown when the SQLState class value
 * is '<i>40</i>', or under vendor-specified conditions. This indicates that the
 * current statement was automatically rolled back by the database because
 * of deadlock or other transaction serialization failures.
 */
class SQLTransactionRollbackException(
  message:          String,
  sqlState:         Option[String] = None,
  vendorCode:       Option[Int]    = None,
  sql:              Option[String] = None,
  detail:           Option[String] = None,
  hint:             Option[String] = None,
  params:          ListMap[Int, Parameter] = ListMap.empty,
) extends SQLTransientException(message, sqlState, vendorCode, sql, detail, hint, params)
