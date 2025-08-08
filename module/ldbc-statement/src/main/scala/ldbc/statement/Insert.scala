/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import cats.data.NonEmptyList

import ldbc.dsl.{ Parameter, SQL }

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
   *     .onDuplicateKeyUpdate(_.name)
   * }}}
   */
  def onDuplicateKeyUpdate[B](func: A => Column[B]): Insert.DuplicateKeyUpdate[A]

  /**
   * Methods for constructing INSERT ... ON DUPLICATE KEY UPDATE statements.
   *
   * {{{
   *   TableQuery[City]
   *     .insert((1L, "Tokyo"))
   *     .onDuplicateKeyUpdate(_.name, "Osaka")
   * }}}
   */
  def onDuplicateKeyUpdate[B](func: A => Column[B], value: B): Insert.DuplicateKeyUpdate[A]

object Insert:

  case class Impl[A](table: A, statement: String, params: List[Parameter.Dynamic]) extends Insert[A]:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    override def onDuplicateKeyUpdate[B](func: A => Column[B]): Insert.DuplicateKeyUpdate[A] =
      Insert.DuplicateKeyUpdate(
        table,
        s"$statement ON DUPLICATE KEY UPDATE ${ func(table).duplicateKeyUpdateStatement }",
        params
      )

    override def onDuplicateKeyUpdate[B](func: A => Column[B], value: B): Insert.DuplicateKeyUpdate[A] =
      val columns = func(table)
      Insert.DuplicateKeyUpdate(
        table,
        s"$statement ON DUPLICATE KEY UPDATE ${ columns.name } = ?",
        params ++ Parameter.Dynamic.many(columns.encoder.encode(value))
      )

  case class Into[A, B](table: A, statement: String, columns: Column[B]):

    /**
     * Method for constructing INSERT ... VALUES statements.
     *
     * {{{
     *   TableQuery[City]
     *     .insertInto(city => city.id *: city.name)
     *     .values((1L, "Tokyo"))
     * }}}
     *
     * @param head
     *   The values to be inserted.
     * @param tail
     *   The values to be inserted.
     */
    def values(head: B, tail: B*): Values[A] = values(NonEmptyList(head, tail.toList))

    /**
     * Method for constructing INSERT ... VALUES statements.
     *
     * {{{
     *   TableQuery[City]
     *     .insertInto(city => city.id *: city.name)
     *     .values(NonEmptyList.one(1L, "Tokyo"))
     * }}}
     *
     * @param values
     *   The values to be inserted.
     */
    def values(values: NonEmptyList[B]): Values[A] =
      val parameterBinders: List[Parameter.Dynamic] = values.toList.flatMap { value =>
        Parameter.Dynamic.many(columns.encoder.encode(value))
      }
      Values(
        table,
        s"$statement (${ columns.name }) VALUES ${ List.fill(values.length)(s"(${ List.fill(columns.values)("?").mkString(",") })").mkString(",") }",
        parameterBinders
      )

    /**
     * Method for constructing INSERT ... SELECT statements.
     *
     * {{{
     *   TableQuery[City]
     *     .insertInto(city => city.id *: city.name)
     *     .select(
     *       TableQuery[Country]
     *         .select(country => country.id *: country.name)
     *     )
     * }}}
     *
     * @param select
     *   The SELECT statement to be inserted.
     * @tparam C
     *   The type of the column to be inserted.
     */
    def select[C](select: Select[C, B]): Values[A] =
      Values(
        table,
        s"$statement (${ columns.name }) ${ select.statement }",
        select.params
      )

    /**
     * Method for constructing INSERT ... SELECT statements.
     *
     * {{{
     *   TableQuery[City]
     *     .insertInto(city => city.id *: city.name)
     *     .select(
     *       TableQuery[Country]
     *         .select(country => country.id *: country.name)
     *         .where(_.name === "Japan")
     *     )
     * }}}
     *
     * @param where
     *   The WHERE statement to be inserted.
     * @tparam C
     *   The type of the column to be inserted.
     */
    def select[C](where: Where.Q[C, B]): Values[A] =
      Values(
        table,
        s"$statement (${ columns.name }) ${ where.statement }",
        where.params
      )

  case class Values[A](
    table:     A,
    statement: String,
    params:    List[Parameter.Dynamic]
  ) extends Insert[A]:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    override def onDuplicateKeyUpdate[C](func: A => Column[C]): Insert.DuplicateKeyUpdate[A] =
      Insert.DuplicateKeyUpdate(
        table,
        s"$statement ON DUPLICATE KEY UPDATE ${ func(table).duplicateKeyUpdateStatement }",
        params
      )

    override def onDuplicateKeyUpdate[B](func: A => Column[B], value: B): Insert.DuplicateKeyUpdate[A] =
      val columns = func(table)
      Insert.DuplicateKeyUpdate(
        table,
        s"$statement ON DUPLICATE KEY UPDATE ${ columns.name } = ?",
        params ++ Parameter.Dynamic.many(columns.encoder.encode(value))
      )

  case class DuplicateKeyUpdate[A](table: A, statement: String, params: List[Parameter.Dynamic]) extends Command:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)
