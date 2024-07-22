/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import scala.collection.immutable.SortedMap

import ldbc.connector.data.Parameter

/**
 * The subclass of {@link SQLException} thrown when the SQLState class value
 * is '<i>23</i>', or under vendor-specified conditions.
 * This indicates that an integrity
 * constraint (foreign key, primary key or unique key) has been violated.
 */
class SQLIntegrityConstraintViolationException(
  message:    String,
  sqlState:   Option[String]            = None,
  vendorCode: Option[Int]               = None,
  sql:        Option[String]            = None,
  detail:     Option[String]            = None,
  hint:       Option[String]            = None,
  params:     SortedMap[Int, Parameter] = SortedMap.empty
) extends SQLNonTransientException(message, sqlState, vendorCode, sql, detail, hint, params)
