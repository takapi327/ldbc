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

/**
 * Trait for constructing SQL Statement from Table information.
 *
 * @tparam A
 *   The type of Table. in the case of Join, it is a Tuple of type Table.
 * @tparam O
 *   The type of Optional Table. in the case of Join, it is a Tuple of type Optional Table.
 */
trait TableQuery[A, O]:

  type Entity = TableQuery.Extract[A]

  private[ldbc] def table: A

  private[ldbc] def column: Column[Entity]

  private[ldbc] def params: List[Parameter.Dynamic]

  /** Name of Table */
  def name: String

  /**
   * Method to construct a query to select a table.
   *
   * {{{
   *   TableQuery[City]
   *     .select(city => city.id *: city.name)
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   * @tparam C
   *   Scala types to be converted by Decoder
   */
  def select[C](func: A => Column[C]): Select[A, C] =
    val columns = func(table)
    Select(table, columns, s"SELECT ${ columns.alias.getOrElse(columns.name) } FROM $name", params)

  /**
   * Method to construct a query to select all columns of a table.
   *
   * {{{
   *   TableQuery[City]
   *     .selectAll
   * }}}
   */
  def selectAll: Select[A, Entity] =
    Select(table, column, s"SELECT ${ column.alias.getOrElse(column.name) } FROM $name", params)

  protected type ToTuple[T] <: Tuple = T match
    case h *: EmptyTuple => Tuple1[h]
    case h *: t          => h *: ToTuple[t]
    case _               => Tuple1[T]

  /**
   * Method to construct a query to insert a table.
   *
   * {{{
   *   TableQuery[City]
   *     .insertInto(city => city.id *: city.name)
   *     .values((1L, "Tokyo"))
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   * @tparam C
   *   Scala types to be converted by Encoder
   */
  inline def insertInto[C](func: A => Column[C]): Insert.Into[A, C] =
    inline this match
      case Join.On(_, _, _, _, _) => error("Join Query does not yet support Insert processing.")
      case _                      => Insert.Into(table, s"INSERT INTO $name", func(table))

  /**
   * Method to construct a query to insert a table.
   *
   * {{{
   *   TableQuery[City]
   *     .insert((1L, "Tokyo"))
   * }}}
   *
   * @param mirror
   *   Mirror of Entity
   * @param values
   *   Value to be inserted into the table
   */
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

  /**
   * Method to construct a query to insert a table.
   *
   * {{{
   *   TableQuery[City] += City(1L, "Tokyo")
   * }}}
   *
   * @param value
   *   Value to be inserted into the table
   * @param mirror
   *   Mirror of Entity
   */
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

  /**
   * Method to construct a query to insert a table.
   *
   * {{{
   *   TableQuery[City] ++= List(City(1L, "Tokyo"), City(2L, "Osaka"))
   * }}}
   *
   * @param values
   *   Value to be inserted into the table
   * @param check
   *   Check if the type of the value is the same as the Entity
   * @tparam P
   *   Scala types to be converted by Encoder
   */
  @targetName("insertProducts")
  inline def ++=[P <: Product](values: List[P])(using check: P =:= Entity): Insert[A] =
    inline this match
      case Join.On(_, _, _, _, _) => error("Join Query does not yet support Insert processing.")
      case _ => TableQueryMacro.++=[A, P](table, name, column.asInstanceOf[Column[P]], params, values)

  /**
   * Method to construct a query to update a table.
   *
   * {{{
   *   TableQuery[City]
   *     .update(city => city.id *: city.name)((1L, "Tokyo"))
   * }}}
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   * @param values
   *   Value to be updated in the table
   * @tparam C
   *   Scala types to be converted by Encoder
   */
  inline def update[C](func: A => Column[C])(values: C): Update[A] =
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

  /**
   * Method to construct a query to update a table.
   *
   * {{{
   *   TableQuery[City]
   *     .update(City(1L, "Tokyo"))
   * }}}
   *
   * @param value
   *   Value to be updated in the table
   * @param mirror
   *   Mirror of Entity
   * @param check
   *   Check if the type of the value is the same as the Entity
   * @tparam P
   *   Scala types to be converted by Encoder
   */
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
   *
   * {{{
   *   TableQuery[City]
   *     .delete
   * }}}
   */
  def delete: Delete[A] = Delete[A](table, s"DELETE FROM $name", params)

  /**
   * Method to construct a query to drop a table.
   *
   * {{{
   *   TableQuery[City]
   *     .dropTable
   * }}}
   */
  def dropTable: Command = Command.Pure(s"DROP TABLE $name", List.empty)

  /**
   * Method to construct a query to truncate a table.
   *
   * {{{
   *   TableQuery[City]
   *     .truncateTable
   * }}}
   */
  def truncateTable: Command = Command.Pure(s"TRUNCATE TABLE $name", List.empty)

  /**
   * Method to construct a query to join a table.
   *
   * {{{
   *   TableQuery[City]
   *     .join(TableQuery[Country])
   *     .on((city, country) => city.countryId === country.id)
   * }}}
   */
  def join[B, BO, AB, OO](
    other: TableQuery[B, BO]
  )(using QueryConcat.Aux[A, B, AB], QueryConcat.Aux[O, BO, OO]): Join[A, B, AB, OO] =
    Join(this, other)

  /**
   * Method to construct a query to left join a table.
   *
   * {{{
   *   TableQuery[City]
   *     .leftJoin(TableQuery[Country])
   *     .on((city, country) => city.countryId === country.id)
   * }}}
   */
  def leftJoin[B, BO, OB, OO](
    other: TableQuery[B, BO]
  )(using QueryConcat.Aux[A, BO, OB], QueryConcat.Aux[O, BO, OO]): Join[A, B, OB, OO] =
    Join.lef(this, other.toOption)

  /**
   * Method to construct a query to right join a table.
   *
   * {{{
   *   TableQuery[City]
   *     .rightJoin(TableQuery[Country])
   *     .on((city, country) => city.countryId === country.id)
   * }}}
   */
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

private[ldbc] object TableQueryMacro:

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
