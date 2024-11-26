/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.deriving.Mirror
import scala.compiletime.*
import scala.annotation.targetName

import ldbc.dsl.Parameter
import ldbc.dsl.codec.Encoder
import ldbc.statement.internal.QueryConcat

trait TableQuery[A, O]:

  type Entity = TableQuery.Extract[A]

  private[ldbc] def table: A

  private[ldbc] def column: Column[Entity]

  private[ldbc] def params: List[Parameter.Dynamic]

  def name: String

  def select[C](func: A => Column[C]): Select[A, C] =
    val columns = func(table)
    Select(table, columns, s"SELECT ${ columns.alias.getOrElse(columns.name) } FROM $name", params)

  def selectAll: Select[A, Entity] =
    Select(table, column, s"SELECT ${ column.alias.getOrElse(column.name) } FROM $name", params)

  protected type ToTuple[T] <: Tuple = T match
    case h *: EmptyTuple => Tuple1[h]
    case h *: t          => h *: ToTuple[t]
    case _               => Tuple1[T]

  inline def insertInto[C](func: A => Column[C])(values: C): Insert[A] =
    inline this match
      case Join.On(_, _, _, _, _) => error("Join Query does not yet support Insert processing.")
      case _ =>
        val columns = func(table)
        val parameterBinders = (values match
          case h *: EmptyTuple => h *: EmptyTuple
          case h *: t          => h *: t
          case h               => h *: EmptyTuple
        )
        .zip(Encoder.fold[ToTuple[C]])
          .toList
          .map {
            case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
          }
        Insert.Impl(
          table     = table,
          statement = s"INSERT INTO $name ${ columns.insertStatement }",
          params    = params ++ parameterBinders
        )

  inline def insertInto[C](func: A => Column[C])(values: List[C]): Insert[A] =
    inline this match
      case Join.On(_, _, _, _, _) => error("Join Query does not yet support Insert processing.")
      case _ =>
        val columns = func(table)
        val parameterBinders = values
          .map {
            case h *: EmptyTuple => h *: EmptyTuple
            case h *: t          => h *: t
            case h               => h *: EmptyTuple
          }
          .flatMap(
            _.zip(Encoder.fold[ToTuple[C]]).toList
              .map {
                case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
              }
              .toList
          )

        Insert.Impl(
          table = table,
          statement =
            s"INSERT INTO $name (${ columns.name }) VALUES ${ List.fill(values.length)(s"(${ List.fill(columns.values)("?").mkString(",") })").mkString(",") }",
          params = params ++ parameterBinders
        )

  inline def insert(using mirror: Mirror.Of[Entity])(
    values: mirror.MirroredElemTypes*
  ): Insert[A] =
    inline this match
      case Join.On(_, _, _, _, _) => error("Join Query does not yet support Insert processing.")
      case _ =>
        val parameterBinders = values
          .flatMap(_.zip(Encoder.fold[mirror.MirroredElemTypes]).toList)
          .map {
            case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
          }
          .toList
        Insert.Impl(
          table = table,
          statement =
            s"INSERT INTO $name (${ column.name }) VALUES ${ values.map(tuple => s"(${ tuple.toArray.map(_ => "?").mkString(",") })").mkString(",") }",
          params = params ++ parameterBinders
        )

  @targetName("insertProduct")
  inline def +=(value: Entity)(using mirror: Mirror.Of[Entity]): Insert[A] =
    inline this match
      case Join.On(_, _, _, _, _) => error("Join Query does not yet support Insert processing.")
      case _ =>
        inline mirror match
          case s: Mirror.SumOf[Entity]     => error("Sum type is not supported.")
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
      table     = table,
      statement = s"INSERT INTO $name ${ column.insertStatement }",
      params    = params ++ parameterBinders
    )

  @targetName("insertProducts")
  inline def ++=[P <: Product](values: List[P])(using check: P =:= Entity): Insert[A] =
    inline this match
      case Join.On(_, _, _, _, _) => error("Join Query does not yet support Insert processing.")
      case _ => TableQueryMacro.++=[A, P](table, name, column.asInstanceOf[Column[P]], params, values)

  inline def update[C](func: A => Column[C], values: C): Update[A] =
    inline this match
      case Join.On(_, _, _, _, _) => error("Join Query does not yet support Update processing.")
      case _ =>
        val columns = func(table)
        val parameterBinders = (values match
          case h *: EmptyTuple => h *: EmptyTuple
          case h *: t          => h *: t
          case h               => h *: EmptyTuple
        )
        .zip(Encoder.fold[ToTuple[C]])
          .toList
          .map {
            case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
          }
        Update.Impl[A](table, s"UPDATE $name SET ${ columns.updateStatement }", params ++ parameterBinders)

  inline def update[P <: Product](value: P)(using mirror: Mirror.ProductOf[P], check: P =:= Entity): Update[A] =
    val parameterBinders = Tuple
      .fromProductTyped(value)
      .zip(Encoder.fold[mirror.MirroredElemTypes])
      .toList
      .map {
        case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
      }
    val statement =
      s"UPDATE $name SET ${ column.updateStatement }"
    Update.Impl[A](table, statement, params ++ parameterBinders)

  /**
   * Method to construct a query to delete a table.
   */
  def delete: Delete[A] = Delete[A](table, s"DELETE FROM $name", params)

  /**
   * Method to construct a query to drop a table.
   */
  def dropTable: Command = Command.Pure(s"DROP TABLE $name", List.empty)

  /**
   * Method to construct a query to truncate a table.
   */
  def truncateTable: Command = Command.Pure(s"TRUNCATE TABLE $name", List.empty)

  def join[B, BO, AB, OO](
    other: TableQuery[B, BO]
  )(using QueryConcat.Aux[A, B, AB], QueryConcat.Aux[O, BO, OO]): Join[A, B, AB, OO] =
    Join(this, other)

  def leftJoin[B, BO, OB, OO](
    other: TableQuery[B, BO]
  )(using QueryConcat.Aux[A, BO, OB], QueryConcat.Aux[O, BO, OO]): Join[A, B, OB, OO] =
    Join.lef(this, other.toOption)

  def rightJoin[B, BO, OB, OO](other: TableQuery[B, BO])(using
    QueryConcat.Aux[O, B, OB],
    QueryConcat.Aux[O, BO, OO]
  ): Join[A, B, OB, OO] =
    Join.right(this.toOption, other)

  private[ldbc] def toOption: TableQuery[A, O]

  private[ldbc] def asVector(): Vector[TableQuery[?, ?]] =
    this match
      case Join.On(left, right, _, _, _) => left.asVector() ++ right.asVector()
      case r: TableQuery[?, ?]           => Vector(r)

object TableQuery:

  type Extract[T] = T match
    case AbstractTable[t]       => t
    case AbstractTable[t] *: tn => t *: Extract[tn]

object TableQueryMacro:

  import scala.quoted.*

  @targetName("insertProducts")
  private[ldbc] inline def ++=[A, B <: Product](
    table:  A,
    name:   String,
    column: Column[B],
    params: List[Parameter.Dynamic],
    values: List[B]
  ): Insert[A] =
    ${ derivedProducts('table, 'name, 'column, 'params, 'values) }

  private[ldbc] def derivedProducts[A: Type, B <: Product](
    table:  Expr[A],
    name:   Expr[String],
    column: Expr[Column[B]],
    params: Expr[List[Parameter.Dynamic]],
    values: Expr[List[B]]
  )(using quotes: Quotes, tpe: Type[B]): Expr[Insert[A]] =
    import quotes.reflect.*

    val symbol = TypeRepr.of[B].typeSymbol

    val encodes = Expr.ofSeq(
      symbol.caseFields
        .map { field =>
          field.tree match
            case ValDef(name, tpt, _) =>
              tpt.tpe.asType match
                case '[tpe] =>
                  val encoder = Expr.summon[Encoder[tpe]].getOrElse {
                    report.errorAndAbort(s"Encoder for type $tpe not found")
                  }
                  encoder.asExprOf[Encoder[tpe]]
                case _ =>
                  report.errorAndAbort(s"Type $tpt is not a type")
        }
    )

    val lists: Expr[List[Tuple]] = '{
      $values
        .map(value => Tuple.fromProduct(value))
    }

    val parameterBinders = '{
      $lists.flatMap(list =>
        list.toList
          .zip($encodes)
          .map {
            case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
          }
      )
    }

    '{
      Insert.Impl(
        table = $table,
        statement =
          s"INSERT INTO ${ $name } (${ $column.name }) VALUES ${ $lists.map(list => s"(${ list.toList.map(_ => "?").mkString(",") })").mkString(",") }",
        params = $params ++ $parameterBinders
      )
    }
