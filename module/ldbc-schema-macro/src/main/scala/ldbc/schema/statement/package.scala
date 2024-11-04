/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.dsl.Parameter
import ldbc.dsl.codec.Decoder

// TODO: Scheduled to migrate to schema
import ldbc.query.builder.statement.Query as QuerySQL
import ldbc.query.builder.statement.Command as CommandSQL

package object statement:

  trait Query[A, B] extends QuerySQL[B]:

    def table: A

    def columns: Column[B]

    def params: List[Parameter.Dynamic]

    override def decoder: Decoder[B] = columns.decoder

  trait Command extends CommandSQL
