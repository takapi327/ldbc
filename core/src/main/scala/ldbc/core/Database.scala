/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.core

/**
 * A model representing SQL database information.
 */
trait Database:

  /** Database Type */
  val databaseType: String

  /** Database Name */
  val name: String

  /** Database Schema */
  val schema: String

  /** Database Schema Meta Information */
  val schemaMeta: Option[String]

  /** Database Catalog */
  val catalog: Option[String]

  /** A value to represent the character set and collation. */
  val characterSet: Option[Character]

  /** Connection host to database */
  val host: String

  /** Connection port to database */
  val port: Int
