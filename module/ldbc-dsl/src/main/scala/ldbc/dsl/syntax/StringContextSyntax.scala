/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.syntax

import cats.effect.Temporal

import ldbc.sql.ParameterBinder

import ldbc.dsl.*

/**
 * Trait for generating SQL models from string completion knowledge.
 *
 * @tparam F
 *   The effect type
 */
trait StringContextSyntax[F[_]: Temporal]:

  extension (sc: StringContext)

    def p(args: ParameterBinder[F]*): SQL[F] =
      val strings     = sc.parts.iterator
      val expressions = args.iterator
      Mysql(strings.mkString("?"), expressions.toList)

    def sql(args: ParameterBinder[F]*): SQL[F] =
      val query = sc.parts.iterator.mkString("?")

      // If it is Static, the value is replaced with the ? If it is a ParameterBinder, it is replaced with ? and create a list of ParameterBinders.
      val (expressions, parameters) = args.foldLeft((query, List.empty[ParameterBinder[F]])) {
        case ((query, parameters), s: ParameterBinder.Static[?]) =>
          (query.replaceFirst("\\?", s.toString), parameters)
        case ((query, parameters), p: ParameterBinder[?]) =>
          (query, parameters :+ p)
      }

      Mysql(expressions, parameters)
