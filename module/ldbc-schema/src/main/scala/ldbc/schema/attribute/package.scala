package ldbc.schema

package object attribute:

  private[ldbc] trait Attribute[T]:

    /**
     * Define an SQL query string for each attribute.
     *
     * @return
     *   SQL query string
     */
    def queryString: String

  private[ldbc] case class Comment[T](message: String) extends Attribute[T]:
    override def queryString: String = s"COMMENT '$message'"

  private[ldbc] case class Visible[T]() extends Attribute[T]:
    override def queryString: String = "/*!80023 VISIBLE */"

  private[ldbc] case class InVisible[T]() extends Attribute[T]:
    override def queryString: String = "/*!80023 INVISIBLE */"

  private[ldbc] trait ColumnFormat[T] extends Attribute[T]
  object ColumnFormat:
    case class Fixed[T]() extends ColumnFormat[T]:
      override def queryString: String = "/*!50606 COLUMN_FORMAT FIXED */"

    case class Dynamic[T]() extends ColumnFormat[T]:
      override def queryString: String = "/*!50606 COLUMN_FORMAT DYNAMIC */"

    case class Default[T]() extends ColumnFormat[T]:
      override def queryString: String = "/*!50606 COLUMN_FORMAT DEFAULT */"

  private[ldbc] trait Storage[T] extends Attribute[T]
  object Storage:
    case class Disk[T]() extends Storage[T]:
      override def queryString: String = "/*!50606 STORAGE DISK */"

    case class Memory[T]() extends Storage[T]:
      override def queryString: String = "/*!50606 STORAGE MEMORY */"
