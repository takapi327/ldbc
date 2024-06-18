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
    def update: Executor[F, Int]

    def returning[T <: String | Int | Long](using reader: ResultSetReader[F, T]): Executor[F, T]
