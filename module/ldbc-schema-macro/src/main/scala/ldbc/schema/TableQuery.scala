/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.deriving.Mirror
import scala.compiletime.*

import ldbc.dsl.Parameter
import ldbc.dsl.codec.Encoder
import ldbc.schema.internal.QueryConcat
import ldbc.schema.statement.*

sealed trait TableQuery[A]:

  type Entity = TableQuery.Extract[A]

  private[ldbc] def table: A

  private[ldbc] def column: Column[Entity]

  private[ldbc] def params: List[Parameter.Dynamic]

  def statement: String

  def join[B, AB](other: TableQuery[B])(using QueryConcat.Aux[A, B, AB]): TableQuery.Join[A, B, AB] =
    TableQuery.Join(this, other)

  def select[C](func: A => Column[C]): Select[A, C] =
    val columns = func(table)
    Select(table, columns, s"SELECT ${ columns.alias.getOrElse(columns.name) } FROM $statement", params)

  private type ToTuple[T] <: Tuple = T match
    case h *: EmptyTuple => Tuple1[h]
    case h *: t      => h *: ToTuple[t]
    case Any => Tuple1[T]

  inline def insert[C](func: A => Column[C], values: C): Insert[A] =
    val columns = func(table)
    val parameterBinders = (values match
      case h *: EmptyTuple      => h *: EmptyTuple
      case h *: t      => h *: t *: EmptyTuple
      case h          => h *: EmptyTuple)
      .zip(Encoder.fold[ToTuple[C]])
      .toList
      .map {
        case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
      }
    Insert.Impl(
      table = table,
      statement = s"INSERT INTO $statement ${ columns.insertStatement }",
      params = params ++ parameterBinders
    )

  inline def insert(value: Entity)(using mirror: Mirror.Of[Entity]): Insert[A] =
    inline mirror match
      case s: Mirror.SumOf[Entity] => error("Sum type is not supported.")
      case p: Mirror.ProductOf[Entity] => derivedProduct(value, p)

  private inline def derivedProduct[P](value: P, mirror: Mirror.ProductOf[P]): Insert[A] =
    val tuples = Tuple.fromProduct(value.asInstanceOf[Product]).asInstanceOf[mirror.MirroredElemTypes]
    val parameterBinders = tuples
      .zip(Encoder.fold[mirror.MirroredElemTypes])
      .toList
      .map {
        case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
      }
    Insert.Impl(
      table = table,
      statement = s"INSERT INTO $statement ${ column.insertStatement }",
      params = params ++ parameterBinders
    )

  def update: Update[A] = Update[A](table, s"UPDATE $statement", params)

  /**
   * Method to construct a query to delete a table.
   */
  def delete: Delete[A] = Delete[A](table, s"DELETE FROM $statement", params)

  /**
   * Method to construct a query to drop a table.
   */
  def dropTable: Command = Command.Pure(s"DROP TABLE $statement", List.empty)

  /**
   * Method to construct a query to truncate a table.
   */
  def truncateTable: Command = Command.Pure(s"TRUNCATE TABLE $statement", List.empty)

  private[ldbc] def asVector(): Vector[TableQuery[?]] =
    this match
      case TableQuery.Join.On(left, right, _, _, _) => left.asVector() ++ right.asVector()
      case TableQuery.Join(left, right)             => left.asVector() ++ right.asVector()
      case r: TableQuery[?]                         => Vector(r)

object TableQuery:

  type Extract[T] = T match
    case Table[t] => t
    case Table[t] *: tn             => t *: Extract[tn]

  def apply[E <: Product, T <: Table[E]](table: T): TableQuery[T] =
    Impl[T, E](table, table.*, table.statement, List.empty)

  private[ldbc] case class Impl[A, B <: Product](
    table:     A,
    column: Column[Extract[A]],
    statement: String,
    params:    List[Parameter.Dynamic]
  ) extends TableQuery[A]//:

    //override type Entity = B

  case class Join[A, B, AB](
    left:  TableQuery[A],
    right: TableQuery[B]
  ) extends TableQuery[AB]:

    //override type Entity = left.Entity *: right.Entity *: EmptyTuple

    override def table: AB =
      Tuple.fromArray((left.asVector() ++ right.asVector()).map(_.table).toArray).asInstanceOf[AB]
    override def column: Column[Entity] = (left.column *: right.column).asInstanceOf[Column[Entity]]
    override def statement: String                  = s"${ left.statement } JOIN ${ right.statement }"
    override def params:    List[Parameter.Dynamic] = left.params ++ right.params

    def on(expression: AB => Expression): TableQuery[AB] =
      val expr = expression(table)
      Join.On(left, right, table, s"$statement ON ${ expr.statement }", params ++ expr.parameter)

  object Join:

    case class On[A, B, AB](
      left:      TableQuery[A],
      right:     TableQuery[B],
      table:     AB,
      statement: String,
      params:    List[Parameter.Dynamic]
    ) extends TableQuery[AB]:

      //override type Entity = left.Entity *: right.Entity *: EmptyTuple

      override def column: Column[Entity] = (left.column *: right.column).asInstanceOf[Column[Entity]]
