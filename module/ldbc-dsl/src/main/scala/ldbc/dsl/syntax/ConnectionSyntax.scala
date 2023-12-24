/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import cats.data.Kleisli

import ldbc.sql.{ DataSource, Connection }
import ldbc.dsl.Database

trait ConnectionSyntax[F[_]]:

  extension [T](connectionKleisli: Kleisli[F, Connection[F], T])

    /** Functions for managing the processing of connections in a read-only manner.
      */
    def readOnly(dataSource: DataSource[F]): F[T]

    def readOnly(database: Database[F]): F[T]

    /** Functions to manage the processing of connections for writing.
        */
    def autoCommit(dataSource: DataSource[F]): F[T]
  
    def autoCommit(database: Database[F]): F[T]

    /** Functions to manage the processing of connections in a transaction.
        */
    def transaction(dataSource: DataSource[F]): F[T]
  
    def transaction(database: Database[F]): F[T]

    /** Functions to manage the processing of connections, always rolling back.
      */
    def rollback(dataSource: DataSource[F]): F[T]

    def rollback(database: Database[F]): F[T]
