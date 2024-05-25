/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.syntax

import cats.data.Kleisli

import ldbc.sql.{ DataSource, Connection }

trait ConnectionSyntax[F[_]]:

  extension [T](connectionKleisli: Kleisli[F, Connection[F], T])

    /**
     * Functions for managing the processing of connections in a read-only manner.
     */
    def readOnly(dataSource: DataSource[F]): F[T]

    /**
     * Functions to manage the processing of connections for writing.
     */
    def autoCommit(dataSource: DataSource[F]): F[T]

    /**
     * Functions to manage the processing of connections in a transaction.
     */
    def transaction(dataSource: DataSource[F]): F[T]

    /**
     * Functions to manage the processing of connections, always rolling back.
     */
    def rollback(dataSource: DataSource[F]): F[T]
