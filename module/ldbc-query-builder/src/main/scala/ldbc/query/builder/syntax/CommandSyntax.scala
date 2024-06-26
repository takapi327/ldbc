/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.syntax

import ldbc.dsl.*
import ldbc.query.builder.statement.Command

trait CommandSyntax[F[_]]:

  extension (command: Command)

    /**
     * A method to execute an update operation against the MySQL server.
     *
     * {{{
     *   Table[User]
     *     .update("id", 1L)
     *     .set("name", "Alice")
     *     .set("age", 20)
     *     .where(_.id === 1L)
     *     .update
     * }}}
     *
     * @return
     *   The number of rows updated
     */
    def update: Executor[F, Int]

    /**
     * A method to execute an insert operation against the MySQL server.
     *
     * {{{
     *   Table[User]
     *     .insertInto(user => (user.name, user.age))
     *     .values(("Alice", 20))
     *     .returning[Long]
     * }}}
     *
     * @tparam T
     *   The type of the primary key
     * @return
     *   The primary key value
     */
    def returning[T <: String | Int | Long](using reader: ResultSetReader[F, T]): Executor[F, T]
