/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import ldbc.dsl.syntax.HelperFunctionsSyntax
import ldbc.dsl.DBIO

import ldbc.statement.Schema

/**
 * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
 * might look something like this.
 *
 * example:
 * {{{
 *   import ldbc.schema.*
 * }}}
 */
package object schema extends Alias, DataTypes, HelperFunctionsSyntax:

  implicit final def schemaDDLOps(ddl: Schema.DDL): DBIO[Array[Int]] =
    DBIO.sequence(ddl.statements)
