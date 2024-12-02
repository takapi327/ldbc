/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }
import ldbc.dsl.codec.Encoder

/**
 * Trait for building Statements to be added.
 *
 * @tparam A
 *  The type of Table. in the case of Join, it is a Tuple of type Table.
 */
sealed trait Insert[A] extends Command:

  /** A model for generating queries from Table information. */
  def table: A

  /** 
   * Methods for constructing INSERT ... ON DUPLICATE KEY UPDATE statements. 
   * 
   * {{{
   *   TableQuery[City]
   *     .insert((1L, "Tokyo"))
   *     .onDuplicateKeyUpdate
   * }}}
   */
  def onDuplicateKeyUpdate: Insert.DuplicateKeyUpdate[A]

object Insert:

  case class Impl[A, B](table: A, statement: String, params: List[Parameter.Dynamic]) extends Insert[A]:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    override def onDuplicateKeyUpdate: Insert.DuplicateKeyUpdate[A] =
      Insert.DuplicateKeyUpdate(
        table,
        s"$statement ON DUPLICATE KEY UPDATE",
        params
      )

  case class Into[A, B](table: A, statement: String, columns: Column[B]):

    inline def values(values: B*): Values[A] =
      val parameterBinders = values
        .map {
          case h *: EmptyTuple => h *: EmptyTuple
          case h *: t          => h *: t
          case h               => h *: EmptyTuple
        }
        .flatMap(
          _.zip(Encoder.fold[B]).toList
            .map {
              case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
            }
            .toList
        )
      Values(
        table,
        s"$statement (${ columns.name }) VALUES ${ List.fill(values.length)(s"(${ List.fill(columns.values)("?").mkString(",") })").mkString(",") }",
        parameterBinders.toList
      )

  case class Values[A](
    table:     A,
    statement: String,
    params:    List[Parameter.Dynamic]
  ) extends Insert[A]:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    override def onDuplicateKeyUpdate: Insert.DuplicateKeyUpdate[A] =
      DuplicateKeyUpdate(
        table,
        s"$statement ON DUPLICATE KEY UPDATE",
        params
      )

  case class DuplicateKeyUpdate[A](table: A, statement: String, params: List[Parameter.Dynamic]) extends Command:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    /**
     * A method for setting the value of a column in a table.
     *
     * {{{
     *   TableQuery[City]
     *     .insertInto(city => city.name *: city.population)
     *     .values(("Tokyo", 13929286))
     *     .onDuplicateKeyUpdate
     *     .set(_.population, 13929286)
     * }}}
     * 
     * @param func
     *   Function to construct an expression using the columns that Table has.
     * @param value
     *   The value to be set in the column.
     * @tparam B
     *   Scala types to be converted by Encoder
     */
    def set[B](func: A => Column[B], value: B)(using Encoder[B]): Insert.DuplicateKeyUpdate[A] =
      val columns = func(table)
      this.copy(
        statement = s"$statement ${ columns.name } = ?",
        params    = params :+ Parameter.Dynamic(value)
      )

    /**
     * A method for setting the value of a column in a table.
     *
     * {{{
     *   TableQuery[City]
     *     .insertInto(city => city.name *: city.population)
     *     .values(("Tokyo", 13929286))
     *     .onDuplicateKeyUpdate
     *     .setValues(_.population)
     * }}}
     * 
     * @param func
     *   Function to construct an expression using the columns that Table has.
     * @tparam B
     *   Scala types to be converted by Encoder
     */
    def setValues[B](func: A => Column[B]): Insert.DuplicateKeyUpdate[A] =
      val columns = func(table)
      this.copy(
        statement = s"$statement ${ columns.duplicateKeyUpdateStatement }"
      )
