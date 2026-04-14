/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.testkit

import cats.effect.kernel.{ Async, Resource }

import ldbc.Connector
import ldbc.connector.{ Connector as LdbcConnector, MySQLDataSource }

/**
 * Provides a [[Connector]] backed by a [[TestConnection]] that automatically rolls back
 * all changes when the [[Resource]] is released.
 *
 * Framework-agnostic: can be used with MUnit, Weaver, ScalaTest, or any other test framework.
 */
object RollbackHandler:

  /**
   * Acquires a [[Connector]] whose underlying connection has `autocommit=false` and
   * intercepts `commit()` as a no-op. On release, all pending changes are rolled back.
   *
   * DDL statements (CREATE TABLE, DROP TABLE, etc.) cause an implicit COMMIT in MySQL
   * and therefore cannot be rolled back. Do not use this with DDL.
   *
   * @param dataSource the [[MySQLDataSource]] to obtain a connection from
   * @return a [[Resource]] that provides a [[Connector]] with automatic rollback on release
   */
  def resource[F[_]: Async](dataSource: MySQLDataSource[F, ?]): Resource[F, Connector[F]] =
    dataSource.getConnection.flatMap { rawConnection =>
      Resource.make(
        rawConnection.setAutoCommit(false).as(
          LdbcConnector.fromConnection[F](new TestConnection[F](rawConnection))
        )
      )(_ => rawConnection.rollback())
    }
