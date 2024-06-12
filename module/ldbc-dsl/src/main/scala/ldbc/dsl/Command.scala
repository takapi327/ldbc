/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

/**
 * Trait that provides functions to perform write operations against the MySQL server.
 *
 * @tparam F
 *   The effect type
 */
trait Command[F[_]]:

  /**
   * A method to execute an update operation against the MySQL server.
   *
   * @return
   *   The number of rows updated
   */
  def update: Executor[F, Int]

  /**
   * A method to execute an insert operation against the MySQL server.
   *
   * @tparam T
   *   The type of the primary key
   * @return
   *   The primary key value
   */
  def returning[T <: String | Int | Long](using reader: ResultSetReader[F, T]): Executor[F, T]
