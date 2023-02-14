/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.sql

import cats.data.NonEmptyList

import ldbc.sql.free.Column

trait Key:

  def label: String

  def queryString: String

trait Index extends Key:

  def indexType: Option[Index.Type]

  def keyPart: NonEmptyList[Column[?]]

  def indexOption: Option[Index.IndexOption]

object Index:

  enum Type:
    case BTREE
    case HASH

  case class IndexOption(
    keyBlockSize: Option[1 | 2 | 4 | 8 | 16],
    indexType: Option[Type],
    parserName: Option[String],
    comment: Option[String],
    engineAttribute: Option[String],
    secondaryEngineAttribute: Option[String],
  ):

    def queryString: String =
      keyBlockSize.fold("")(size => s" KEY_BLOCK_SIZE = $size")
        + indexType.fold("")(index => s" USING $index")
        + parserName.fold("")(name => s" WITH PARSER $name")
        + comment.fold("")(str => s" COMMENT '$str'")
        + engineAttribute.fold("")(engine => s" ENGINE_ATTRIBUTE = '$engine'")
        + secondaryEngineAttribute.fold("")(secondary => s" SECONDARY_ENGINE_ATTRIBUTE = '$secondary'")

private[ldbc] case class IndexKey(
  indexName:   Option[String],
  indexType:   Option[Index.Type],
  keyPart:     NonEmptyList[Column[?]],
  indexOption: Option[Index.IndexOption]
) extends Index:

  override def label: String = "INDEX"

  override def queryString: String =
    label
      + indexName.fold("")(str => s" `$str`")
      + s" (${keyPart.map(column => s"`${column.label}`").toList.mkString(", ")})"
      + indexType.fold("")(index => s" USING $index")
      + indexOption.fold("")(option => s"${option.queryString}")

private[ldbc] case class PrimaryKey(
  indexType:   Option[Index.Type],
  keyPart:     NonEmptyList[Column[?]],
  indexOption: Option[Index.IndexOption]
) extends Index:

  override def label: String = "PRIMARY KEY"

  override def queryString: String =
    label
      + s" (${keyPart.map(column => s"`${column.label}`").toList.mkString(", ")})"
      + indexType.fold("")(index => s" USING $index")
      + indexOption.fold("")(option => s"${option.queryString}")

private[ldbc] case class UniqueKey(
  indexName:   Option[String],
  indexType:   Option[Index.Type],
  keyPart:     NonEmptyList[Column[?]],
  indexOption: Option[Index.IndexOption]
) extends Index:

  override def label: String = "UNIQUE KEY"

  override def queryString: String =
    label
      + indexName.fold("")(str => s" `$str`")
    + s" (${keyPart.map(column => s"`${column.label}`").toList.mkString(", ")})"
      + indexType.fold("")(index => s" USING $index")
      + indexOption.fold("")(option => s"${option.queryString}")

private[ldbc] case class ForeignKey(
  indexName: Option[String],
  colName:   NonEmptyList[Column[?]],
  reference: Reference
) extends Key:

  override def label: String = "FOREIGN KEY"

  override def queryString: String =
    label
      + indexName.fold("")(str => s" `$str`")
      + s" (${colName.map(column => s"`${column.label}`").toList.mkString(", ")})"
      + s" ${reference.queryString}"

private[ldbc] case class Constraint(
  symbol: String,
  key: PrimaryKey | UniqueKey | ForeignKey
) extends Key:

  def label: String = "CONSTRAINT"

  private val keyQueryString: String = key match
    case p: PrimaryKey => p.queryString
    case u: UniqueKey  => u.queryString
    case f: ForeignKey => f.queryString

  def queryString: String = s"$label `$symbol` $keyQueryString"
