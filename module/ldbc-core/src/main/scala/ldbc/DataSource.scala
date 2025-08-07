/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import cats.effect.Resource

import ldbc.sql.Connection

/**
 * A factory for database connections that provides connections as managed resources.
 * 
 * DataSource is a fundamental abstraction in ldbc that encapsulates the logic for
 * establishing database connections. It provides a uniform interface for obtaining
 * connections regardless of the underlying implementation (JDBC, native MySQL protocol, etc.).
 * 
 * Implementations of this trait are responsible for:
 * - Connection pooling (if applicable)
 * - Connection configuration and initialization
 * - Resource lifecycle management
 * - Error handling during connection establishment
 * 
 * The connections are provided as [[cats.effect.Resource]] instances, ensuring that:
 * - Connections are properly initialized before use
 * - Resources are cleaned up when no longer needed
 * - Connection leaks are prevented through automatic resource management
 * 
 * @tparam F the effect type (e.g., IO, Future, etc.) that wraps the operations
 * 
 * @example {{{
 * // Using a DataSource to execute database operations
 * val dataSource: DataSource[IO] = MySQLDataSource.fromConfig(config)
 * 
 * val result: IO[List[User]] = dataSource.getConnection.use { connection =>
 *   sql"SELECT * FROM users".query[User].to[List].run(connection)
 * }
 * }}}
 * 
 * @see [[ldbc.connector.MySQLDataSource]] for the pure Scala implementation
 * @see [[jdbc.connector.MySQLDataSource]] for the JDBC-based implementation
 */
trait DataSource[F[_]]:

  /**
   * Creates a new database connection wrapped in a Resource.
   * 
   * The returned Resource ensures that the connection is properly:
   * - Acquired: Connection establishment, authentication, and initialization
   * - Used: The connection is ready for executing SQL statements
   * - Released: Connection cleanup, closing network resources, etc.
   * 
   * Each call to this method may return a new connection or a pooled connection,
   * depending on the implementation. Users should not make assumptions about
   * connection identity or state between calls.
   * 
   * @return a Resource that manages the lifecycle of a database connection
   * 
   * @note The connection is only valid within the Resource's use block.
   *       Attempting to use it outside will result in errors.
   */
  def getConnection: Resource[F, Connection[F]]
