/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.syntax

import cats.effect.Temporal

import ldbc.sql.Parameter

import ldbc.dsl.*

/**
 * Trait for generating SQL models from string completion knowledge.
 *
 * @tparam F
 *   The effect type
 */
trait StringContextSyntax[F[_]: Temporal]:

  extension (sc: StringContext)

    def p(args: Parameter.DynamicBinder*): SQL[F] =
      val strings     = sc.parts.iterator
      val expressions = args.iterator
      Mysql(strings.mkString("?"), expressions.toList)

    def sql(args: Parameter.Binder*): SQL[F] =
      val query = sc.parts.iterator.mkString("?")

      // If it is Static, the value is replaced with the ? If it is a Parameter.Binder, it is replaced with ? and create a list of Parameter.Binders.
      val (expressions, parameters) = args.foldLeft((query, List.empty[Parameter.DynamicBinder])) {
        case ((query, parameters), s: Parameter.StaticBinder) =>
          (query.replaceFirst("\\?", s.toString), parameters)
        case ((query, parameters), p: Parameter.DynamicBinder) =>
          (query, parameters :+ p)
        case ((query, parameters), _) =>
          (query, parameters)
      }

      Mysql(expressions, parameters)
