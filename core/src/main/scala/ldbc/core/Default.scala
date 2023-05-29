/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

/** Trait for setting SQL Default values
  */
trait Default:

  /** Value set as the default value for DataType */
  def value: String

  /** Define SQL query string for each DataType.
    *
    * @return
    *   SQL query string
    */
  def queryString: String

  override def toString: String = queryString

object Default:

  /** Object for setting NULL as the Default value when the SQL DataType is NULL-allowed.
    */
  object Null extends Default:

    override def value: String = "NULL"

    override def queryString: String = s"DEFAULT $value"

  /** Model to be used when a value matching the DataType type is set.
    *
    * @param _value
    *   Value set as the default value for DataType
    * @tparam T
    *   Scala types that match SQL DataType
    */
  case class Value[T](_value: T) extends Default:

    override def value: String = _value.toString

    override def queryString: String = s"DEFAULT '$value'"

  /** Model for setting TimeStamp-specific Default values.
    *
    * @param withOn
    *   Value to determine whether to set additional information
    */
  case class TimeStamp(withOn: Boolean) extends Default:

    override def value: String = "CURRENT_TIMESTAMP"

    override def queryString: String =
      if withOn then s"DEFAULT $value ON UPDATE CURRENT_TIMESTAMP"
      else s"DEFAULT $value"
