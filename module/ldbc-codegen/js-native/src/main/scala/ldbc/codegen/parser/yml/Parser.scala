/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.parser.yml

import scala.util.control.NoStackTrace

import cats.syntax.either.*

import io.circe.*
import org.virtuslab.yaml.*
import org.virtuslab.yaml.Node.*

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

  given Decoder[Column] = (c: HCursor) =>
    for
      name   <- c.downField("name").as[String]
      `type` <- c.downField("type").as[String]
    yield Column(name, `type`)

  given Decoder[Extend] = (c: HCursor) =>
    for `extends` <- c.downField("extends").as[Seq[String]]
    yield Extend(`extends`)

  given Decoder[Table] = (c: HCursor) =>
    for
      name     <- c.downField("name").as[String]
      columns  <- c.downField("columns").as[Option[Seq[Column]]]
      `object` <- c.downField("object").as[Option[Extend]]
      `class`  <- c.downField("class").as[Option[Extend]]
    yield Table(name, columns, `object`, `class`)

  given Decoder[Database] = (c: HCursor) =>
    for
      name   <- c.downField("name").as[String]
      tables <- c.downField("tables").as[Seq[Table]]
    yield Database(name, tables)

  given Decoder[Parser] = (c: HCursor) =>
    for database <- c.downField("database").as[Database]
    yield Parser(database)

  /**
   * Methods for generating models to extend models and tables from strings in Yaml format.
   *
   * @param str
   *   String in Yaml format
   * @return
   */
  def parse(str: String): Parser =
    str.asNode.leftMap(e => ParsingFailure(e.msg, WrappedYamlError(e))).flatMap(yamlToJson).flatMap(_.as[Parser]).valueOr(throw _)

  final case class WrappedYamlError(error: YamlError) extends Exception with NoStackTrace:
    override def getMessage(): String = error.msg

  private def yamlToJson(node: Node): Either[ParsingFailure, Json] =

    def convertScalarNode(node: ScalarNode): Either[ParsingFailure, Json] = node.tag match
      // case Tag.int if node.value.startsWith("0x") || node.value.contains("_") =>
      // TODO
      case Tag.int | Tag.float =>
        JsonNumber.fromString(node.value).map(Json.fromJsonNumber).toRight {
          val msg = s"Invalid numeric string ${node.value}"
          ParsingFailure(msg, new NumberFormatException(msg))
        }
      case Tag.boolean =>
        YamlDecoder.forBoolean
          .construct(node)
          .leftMap(e => ParsingFailure(e.msg, WrappedYamlError(e)))
          .map(Json.fromBoolean)
      case Tag.nullTag => Right(Json.Null)
      case CustomTag(other) =>
        Right(Json.fromJsonObject(JsonObject.singleton(other.stripPrefix("!"), Json.fromString(node.value))))
      case other => Right(Json.fromString(node.value))

    def convertKeyNode(node: Node) = node match
      case scalar: ScalarNode => Right(scalar.value)
      case _ => Left(ParsingFailure("Only string keys can be represented in JSON", null))

    if node == null then
      Right(Json.False)
    else node match
      case mapping: MappingNode =>
        mapping.mappings
          .foldLeft(
            Either.right[ParsingFailure, JsonObject](JsonObject.empty)
          ) { case (objEither, (keyNode, valueNode)) =>
            for
              obj <- objEither
              key <- convertKeyNode(keyNode)
              value <- yamlToJson(valueNode)
            yield obj.add(key, value)
          }
          .map(Json.fromJsonObject)
      case sequence: SequenceNode =>
        sequence.nodes
          .foldLeft(Either.right[ParsingFailure, List[Json]](List.empty[Json])) { (arrEither, node) =>
            for
              arr <- arrEither
              value <- yamlToJson(node)
            yield value :: arr
          }
          .map(arr => Json.fromValues(arr.reverse))
      case scalar: ScalarNode => convertScalarNode(scalar)
