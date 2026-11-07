/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.data

/**
 * SQLSTATE values ldbc raises or recognises.
 *
 * A SQLSTATE is five characters: the first two are the class, the remaining three the subclass. JDBC
 * defines its [[ldbc.sql.SQLException]] subclasses per *class* value, so anything that maps a state to
 * an exception matches on [[classOf]] rather than the whole string — `08000` and `08S01` are both
 * connection errors and must not be treated as unrelated.
 */
object SQLState:

  /** Connection exception. */
  val CONNECTION_EXCEPTION = "08"

  /** Data exception. */
  val DATA_EXCEPTION = "22"

  /** Integrity constraint violation. */
  val INTEGRITY_CONSTRAINT_VIOLATION = "23"

  /** Invalid authorization specification. */
  val INVALID_AUTHORIZATION_SPEC = "28"

  /** Transaction rollback. */
  val TRANSACTION_ROLLBACK = "40"

  /** Syntax error or access rule violation. */
  val SYNTAX_ERROR = "42"

  /** Feature not supported. */
  val FEATURE_NOT_SUPPORTED = "0A"

  /** Unable to establish a connection. Raised by ldbc while a connection is being set up. */
  val UNABLE_TO_CONNECT = "08001"

  /** Communication link failure. Raised by ldbc once a connection is established. */
  val COMMUNICATION_LINK_FAILURE = "08S01"

  /** The class of a SQLSTATE, or the whole string when it is shorter than a class. */
  def classOf(sqlState: String): String =
    if sqlState.length >= 2 then sqlState.substring(0, 2) else sqlState
