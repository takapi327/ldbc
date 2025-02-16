/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

trait Schema:

  /**
   * Method to construct a query to create a table.
   *
   * @return
   *   DDL to create a table.
   */
  def create: Schema.DDL

  /**
   * Method to construct a query to create a table if it does not exist.
   *
   * @return
   *   DDL to create a table if it does not exist.
   */
  def createIfNotExists: Schema.DDL

  /**
   * Method to construct a query to drop a table.
   *
   * @return
   *   DDL to drop a table.
   */
  def drop: Schema.DDL

  /**
   * Method to construct a query to drop a table if it exists.
   *
   * @return
   *   DDL to drop a table if it exists.
   */
  def dropIfExists: Schema.DDL

  /**
   * Method to construct a query to truncate a table.
   *
   * @return
   *   DDL to truncate a table.
   */
  def truncate: Schema.DDL
  
  def ++(other: Schema): Schema

object Schema:

  trait DDL:

    def statements: List[String]
    
    def ++(other: DDL): DDL
    
  object DDL:
    
    private case class Impl(statements: List[String]) extends DDL:
      
      override def ++(other: DDL): DDL = Impl(statements ++ other.statements)
    
    def apply(statement: String): DDL = Impl(List(statement))

  case class Impl(
    create: Schema.DDL,
    createIfNotExists: Schema.DDL,
    drop: Schema.DDL,
    dropIfExists: Schema.DDL,
    truncate: Schema.DDL
  ) extends Schema:

    override def ++(other: Schema): Schema =
      this.copy(
        create = this.create ++ other.create,
        createIfNotExists = this.createIfNotExists ++ other.createIfNotExists,
        drop = this.drop ++ other.drop,
        dropIfExists = this.dropIfExists ++ other.dropIfExists,
        truncate = this.truncate ++ other.truncate
      )
      
  def apply(
    create: Schema.DDL,
    createIfNotExists: Schema.DDL,
    drop: Schema.DDL,
    dropIfExists: Schema.DDL,
    truncate: Schema.DDL
  ): Schema = Impl(create, createIfNotExists, drop, dropIfExists, truncate)

  def empty: Schema = Impl(
    create = DDL(""),
    createIfNotExists = DDL(""),
    drop = DDL(""),
    dropIfExists = DDL(""),
    truncate = DDL("")
  )
