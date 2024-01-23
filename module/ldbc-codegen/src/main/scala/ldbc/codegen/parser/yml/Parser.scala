/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.parser.yml

import cats.syntax.either.*

import io.circe.*
import io.circe.generic.semiauto.deriveDecoder
import io.circe.yaml

/**
 * A model for storing the results of parsing strings in Yaml format.
 *
 * @param database
 *   List of A model for organizing tables and models for extending models on a per-database basis.
 */
case class Parser(database: Parser.Database)

object Parser:

  /**
   * Model for changing the Scala type of table columns.
   *
   * @param name
   *   Column name
   * @param `type`
   *   Scala Type
   */
  case class Column(name: String, `type`: String)

  /**
   * A model for mixing in values to classes and objects.
   *
   * @param `extends`
   *   List of values to be mixed in for classes and objects.
   */
  case class Extend(`extends`: Seq[String])

  /**
   * Tables and models for extending the model.
   *
   * @param name
   *   Table name
   * @param columns
   *   List of models to change the Scala type of table columns.
   * @param `object`
   *   A model for mixing in values to objects.
   * @param `class`
   *   A model for mixing in values to class.
   */
  case class Table(name: String, columns: Option[Seq[Column]], `object`: Option[Extend], `class`: Option[Extend]):

    def findColumn(name: String): Option[Column] =
      columns.flatMap(_.find(_.name == name))

  /**
   * A model for organizing tables and models for extending models on a per-database basis.
   *
   * @param name
   *   Database name
   * @param tables
   *   List of tables and models to extend the model.
   */
  case class Database(name: String, tables: Seq[Table])

  given Decoder[Column]   = deriveDecoder
  given Decoder[Extend]   = deriveDecoder
  given Decoder[Table]    = deriveDecoder
  given Decoder[Database] = deriveDecoder
  given Decoder[Parser]   = deriveDecoder

  /**
   * Methods for generating models to extend models and tables from strings in Yaml format.
   *
   * @param str
   *   String in Yaml format
   * @return
   */
  def parse(str: String): Parser =
    yaml.parser.parse(str).flatMap(_.as[Parser]).valueOr(throw _)
