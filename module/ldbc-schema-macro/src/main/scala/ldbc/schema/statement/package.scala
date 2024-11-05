/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.dsl.SQL
import ldbc.dsl.codec.Decoder

package object statement:

  trait Query[A, B] extends SQL:

    def table: A

    def columns: Column[B]

    def decoder: Decoder[B] = columns.decoder

  trait Command extends SQL
