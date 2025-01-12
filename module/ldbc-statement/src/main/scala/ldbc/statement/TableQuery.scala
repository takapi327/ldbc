/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName
import scala.compiletime.*
import scala.deriving.Mirror

import ldbc.dsl.codec.Encoder
import ldbc.dsl.Parameter

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

  /**
   * Method to construct a query to insert a table.
   *
   * {{{
   *   TableQuery[City]
   *     .insertInto(city => city.id *: city.name)
   *     .values((1L, "Tokyo"))
   * }}}
   * 
   * If you want to use a join, consider using select as follows
   * 
   * {{{
   *    TableQuery[City]
   *      .insertInto(city => city.id *: city.name)
   *      .select(
   *        TableQuery[Country]
   *          .join(TableQuery[City])
   *          .on((country, city) => country.id === city.countryId)
   *          .select((country, city) => country.id *: city.name)
   *          .where((country, city) => city.population > 1000000)
   *      )
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
        val parameterBinders: List[Parameter.Dynamic] = values.flatMap { value =>
          Parameter.Dynamic.many(column.encoder.asInstanceOf[Encoder[mirror.MirroredElemTypes]].encode(value))
        }.toList
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
   */
  @targetName("insertProduct")
  inline def +=(value: Entity): Insert[A] =
    inline this match
      case Join.On(_, _, _, _, _) => error("Join Query does not yet support Insert processing.")
      case _ =>
        Insert.Impl(
          table     = table,
          statement = s"INSERT INTO $name ${ column.insertStatement }",
          params    = params ++ Parameter.Dynamic.many(column.encoder.encode(value))
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
   */
  @targetName("insertProducts")
  inline def ++=(values: List[Entity]): Insert[A] =
    inline this match
      case Join.On(_, _, _, _, _) => error("Join Query does not yet support Insert processing.")
      case _ =>
        Insert.Impl(
          table = table,
          statement =
            s"INSERT INTO $name (${ column.name }) VALUES ${ values.map(_ => s"(${ List.fill(column.values)("?").mkString(",") })").mkString(",") }",
          params = params ++ values.flatMap { value =>
            Parameter.Dynamic.many(column.encoder.encode(value))
          }
        )

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
        val columns          = func(table)
        val parameterBinders = Parameter.Dynamic.many(columns.encoder.encode(values))
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
   */
  def update(value: Entity): Update[A] =
    val parameterBinders = Parameter.Dynamic.many(column.encoder.encode(value))
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
