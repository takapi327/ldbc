/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.core

/** A model representing SQL database information.
  */
trait Database:

  /** Database Type */
  val databaseType: Database.Type

  /** Database Name */
  val name: String

  /** Database Schema */
  val schema: String

  /** Database Schema Meta Information */
  val schemaMeta: Option[String]

  /** Database Catalog */
  val catalog: Option[String]

  /** A value to represent the character set. */
  val character: Option[Character]

  /** A value to represent the collation. */
  val collate: Option[Collate[String]]

  /** Connection host to database */
  val host: String

  /** Connection port to database */
  val port: Option[Int]

  /** List of Tables in Database */
  val tables: Set[Table[?]]

object Database:

  /** Enum representing the database type, only databases that are already supported by the library will be managed.
    */
  enum Type(val name: String, val driver: String):
    case MySQL    extends Type("mysql", "com.mysql.cj.jdbc.Driver")
    case AWSMySQL extends Type("mysql:aws", "software.aws.rds.jdbc.mysql.Driver")
