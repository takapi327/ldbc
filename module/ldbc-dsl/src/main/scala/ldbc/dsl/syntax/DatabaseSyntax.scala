/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.dsl.syntax

import ldbc.core.Database as CoreDatabase
import ldbc.dsl.Database
import ldbc.sql.DataSource

/** Trait to provide a way to convert from Database model to Database model with connection information to db.
  *
  * @tparam F
  *   The effect type
  */
trait DatabaseSyntax[F[_]]:

  extension (database: CoreDatabase)

    def fromDriverManager(): Database[F]

    def fromDriverManager(
      user:     String,
      password: String
    ): Database[F]

    def fromDataSource(dataSource: DataSource[F]): Database[F]
