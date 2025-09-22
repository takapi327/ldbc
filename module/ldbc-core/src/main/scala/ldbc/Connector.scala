/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

/**
 * Connector trait for managing database connections and executing DBIO actions.
 *
 * @tparam F the effect type
 */
trait Connector[F[_]]:

  /**
   * Runs a DBIO action using the provided connection.
   */
  def run[A](dbio: DBIO[A]): F[A]
