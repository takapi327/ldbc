/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.syntax

/**
 * Trait for generating SQL models from string completion knowledge.
 */
trait StringContextSyntax:

  extension (sc: StringContext)

    def p(args: ldbc.dsl.Parameter.Dynamic*): ldbc.dsl.Mysql =
      val strings     = sc.parts.iterator
      val expressions = args.iterator
      ldbc.dsl.Mysql(strings.mkString("?"), expressions.toList)

    def sql(args: ldbc.dsl.Parameter*): ldbc.dsl.Mysql =
      val query = sc.parts.iterator.mkString("?")

      // If it is Static, the value is replaced with the ? If it is a Parameter.Binder, it is replaced with ? and create a list of Parameter.Binders.
      val (expressions, parameters) = args.foldLeft((query, List.empty[ldbc.dsl.Parameter.Dynamic])) {
        case ((query, parameters), s: ldbc.dsl.Parameter.Static) =>
          // quoteReplacement so that backslashes / dollar signs in the static value (e.g. an escaped
          // identifier from `ident`) are inserted verbatim instead of being treated as regex
          // replacement metacharacters.
          (query.replaceFirst("\\?", java.util.regex.Matcher.quoteReplacement(s.toString)), parameters)
        case ((query, parameters), p: ldbc.dsl.Parameter.Dynamic) =>
          (query, parameters :+ p)
        case ((query, parameters), _) =>
          (query, parameters)
      }

      ldbc.dsl.Mysql(expressions, parameters)
