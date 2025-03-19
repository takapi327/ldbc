/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query

import ldbc.dsl.syntax.HelperFunctionsSyntax

/**
 * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
 * might look something like this.
 *
 * example:
 * {{{
 *   import ldbc.query.builder.*
 * }}}
 */
package object builder extends HelperFunctionsSyntax:

  type TableQuery[T] = ldbc.statement.TableQuery[Table[T], Table.Opt[T]]
