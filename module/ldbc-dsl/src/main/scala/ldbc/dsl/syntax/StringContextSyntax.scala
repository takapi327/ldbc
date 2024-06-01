/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.syntax

import cats.effect.Temporal

import ldbc.dsl.*

/**
 * Trait for generating SQL models from string completion knowledge.
 *
 * @tparam F
 *   The effect type
 */
trait StringContextSyntax[F[_]: Temporal]:

  extension (sc: StringContext)
    def sql(args: ParameterBinder[F]*): SQL[F] =
      val strings     = sc.parts.iterator
      val expressions = args.iterator
      Mysql(strings.mkString("?"), expressions.toList)

    def q(args: String*): SQL[F] =
      val strings     = sc.parts.iterator
      val expressions = args.iterator
      val query = strings.zipAll(expressions, "", "").foldLeft("") {
        case (acc, (str, expr)) => acc + str + expr
      }
      Mysql(query, List.empty)

    def p(args: ParameterBinder[F]*): SQL[F] =
      val strings     = sc.parts.iterator
      val expressions = args.iterator
      Mysql(strings.mkString("?"), expressions.toList)
