/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.syntax

import ldbc.dsl.*
import ldbc.dsl.codec.Decoder
import ldbc.statement.Command

trait CommandSyntax[F[_]]:

  extension (command: Command)

    /**
     * A method to execute an update operation against the MySQL server.
     *
     * {{{
     *   TableQuery[User]
     *     .update(user => user.id *: user.name *: user.age)((1L, "Alice", 20))
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
     *   TableQuery[User]
     *     .insertInto(user => user.name *: user.age)
     *     .values(("Alice", 20))
     *     .returning[Long]
     * }}}
     *
     * @tparam T
     *   The type of the primary key
     * @return
     *   The primary key value
     */
    def returning[T <: String | Int | Long](using decoder: Decoder.Elem[T]): Executor[F, T]
