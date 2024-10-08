/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import scala.language.dynamics
import scala.deriving.Mirror
import scala.compiletime.*
import scala.compiletime.ops.int.*
import scala.annotation.targetName
import ldbc.sql.ResultSet
import ldbc.dsl.*
import ldbc.dsl.codec.{ Decoder, Encoder }
import ldbc.query.builder.statement.*
import ldbc.query.builder.interpreter.*
import ldbc.query.builder.formatter.Naming
import ldbc.query.builder.interpreter.Tuples.InverseColumnMap

sealed trait MySQLTable[P]:

  /**
   * Type of scala types.
   */
  type ElemTypes <: Tuple

  /**
   * The name of the table.
   */
  def _name: String

  /**
   * A method to get all columns defined in the table.
   */
  @targetName("all")
  def * : Tuple.Map[ElemTypes, Column]

  /** Function to get a value of type P from a ResultSet */
  def decoder: Decoder[P]

/**
 * Trait for generating SQL table information.
 *
 * @tparam P
 *   A class that implements a [[Product]] that is one-to-one with the table definition.
 */
trait Table[P <: Product] extends MySQLTable[P], Dynamic:

  type ElemLabels <: Tuple

  /**
   * An alias for the table.
   */
  def _alias: Option[String]

  /**
   * A method to get the table name.
   */
  def label: String = _alias match
    case Some(alias) if alias == _name => _name
    case Some(alias)                   => s"${ _name } AS $alias"
    case None                          => _name

  /**
   * A method to get the column names defined in the table.
   */
  def columnNames: List[String]

  /**
   * Function for setting alias names for tables.
   *
   * @param name
   *   Alias name
   * @return
   *   Table with alias name
   */
  def as(name: String): Table[P]

  /**
   * Function for setting table names.
   *
   * @param name
   *   Table name
   * @return
   *   Table with table name
   */
  def setName(name: String): Table[P]

  /**
   * A method to get a specific column defined in the table.
   *
   * @param tag
   *   A type with a single instance. Here, Column is passed.
   * @param mirror
   *   product isomorphism map
   * @param index
   *   Position of the specified type in tuple X
   * @tparam Tag
   *   Type with a single instance
   */
  transparent inline def selectDynamic[Tag <: Singleton](
    tag: Tag
  )(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
    *.productElement(index.value)
      .asInstanceOf[Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]

  /**
   * A method to perform a simple Select.
   * 
   * {{{
   *   Table[Person].select(person => (person.id, person.name))
   *   // SELECT id, name FROM person
   * }}}
   *
   * @param func
   *   Function to retrieve columns from Table.
   * @tparam T
   *   Type of value to be obtained
   * @return
   *   Select model
   */
  def select[T](func: Table[P] => T): Select[P, T, Tuples.InverseColumnMap[T]] =
    val columns = func(this)
    val decodes: Array[Decoder[?]] = columns match
      case v: Tuple =>
        v.toArray.map {
          case column: Column[t] => column.decoder
        }
      case v: Column[t] => Array(v.decoder)
    val decoder: Decoder[Tuples.InverseColumnMap[T]] =
      new Decoder[InverseColumnMap[T]]((resultSet: ResultSet, prefix: Option[String]) =>
        val results = decodes.map(_.decode(resultSet, None))
        Tuple.fromArray(results).asInstanceOf[Tuples.InverseColumnMap[T]]
      )
    val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v
    val statement = s"SELECT $str FROM $label"
    Select(this, statement, columns, Nil, decoder)

  /**
   * A method to perform a simple Select.
   *
   * {{{
   *   Table[Person].selectAll
   *   // SELECT id, name, age FROM person
   * }}}
   */
  def selectAll: Select[P, Tuple.Map[ElemTypes, Column], P] =
    val statement = s"SELECT ${ *.toList.distinct.mkString(", ") } FROM $label"
    Select(this, statement, *, Nil, decoder)

  /**
   * A method to perform a simple Join.
   * 
   * {{{
   *   Table[Person].join(Table[City])((person, city) => person.cityId == city.id)
   *   // ... person JOIN city ON person.cityId = city.id
   * }}}
   *
   * @param other
   *   [[Table]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam O
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  def join[O <: Product](other: Table[O])(
    on: Table[P] *: Tuple1[Table[O]] => Expression
  ): Join[Table[P] *: Tuple1[Table[O]], Table[P] *: Tuple1[Table[O]]] =
    val main = _alias.fold(as(_name))(_ => this)
    val sub  = other._alias.fold(other.as(other._name))(_ => other)
    val joins: Table[P] *: Tuple1[Table[O]] = main *: Tuple(sub)
    Join.Impl[Table[P] *: Tuple1[Table[O]], Table[P] *: Tuple1[Table[O]]](
      main,
      joins,
      joins,
      List(s"${ Join.JoinType.JOIN.statement } ${ sub.label } ON ${ on(joins).statement }")
    )

  /**
   * Method to perform Left Join.
   * 
   * {{{
   *   Table[Person].leftJoin(Table[City])((person, city) => person.cityId == city.id)
   *   // ... person LEFT JOIN city ON person.cityId = city.id
   * }}}
   *
   * @param other
   *   [[Table]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam O
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  def leftJoin[O <: Product](other: Table[O])(
    on: Table[P] *: Tuple1[Table[O]] => Expression
  ): Join[Table[P] *: Tuple1[Table[O]], Table[P] *: Tuple1[TableOpt[O]]] =
    val main:  Table[P]                     = _alias.fold(as(_name))(_ => this)
    val sub:   Table[O]                     = other._alias.fold(other.as(other._name))(_ => other)
    val joins: Table[P] *: Tuple1[Table[O]] = main *: Tuple(sub)
    Join.Impl[Table[P] *: Tuple1[Table[O]], Table[P] *: Tuple1[TableOpt[O]]](
      main,
      joins,
      main *: Tuple(TableOpt.Impl(sub)),
      List(s"${ Join.JoinType.LEFT_JOIN.statement } ${ sub.label } ON ${ on(joins).statement }")
    )

  /**
   * Method to perform Right Join.
   * 
   * {{{
   *   Table[Person].rightJoin(Table[City])((person, city) => person.cityId == city.id)
   *   // ... person RIGHT JOIN city ON person.cityId = city.id
   * }}}
   *
   * @param other
   *   [[Table]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam O
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  def rightJoin[O <: Product](other: Table[O])(
    on: Table[P] *: Tuple1[Table[O]] => Expression
  ): Join[Table[P] *: Tuple1[Table[O]], TableOpt[P] *: Tuple1[Table[O]]] =
    val main = _alias.fold(as(_name))(_ => this)
    val sub  = other._alias.fold(other.as(other._name))(_ => other)
    val joins: Table[P] *: Tuple1[Table[O]] = main *: Tuple(sub)
    Join.Impl[Table[P] *: Tuple1[Table[O]], TableOpt[P] *: Tuple1[Table[O]]](
      main,
      joins,
      TableOpt.Impl(main) *: Tuple(sub),
      List(s"${ Join.JoinType.RIGHT_JOIN.statement } ${ sub.label } ON ${ on(joins).statement }")
    )

  /**
   * A method to build a query model that inserts data into all columns defined in the table.
   *
   * @param mirror
   *   product isomorphism map
   * @param values
   *   A list of Tuples constructed with all the property types that Table has.
   */
  inline def insert(using mirror: Mirror.ProductOf[P])(
    values: mirror.MirroredElemTypes*
  ): Insert[P] =
    val parameterBinders = values
      .flatMap(_.zip(Encoder.fold[mirror.MirroredElemTypes]).toList)
      .map {
        case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
      }
      .toList
    val statement = s"INSERT INTO ${ _name } (${ *.toList
        .mkString(", ") }) VALUES${ values.map(tuple => s"(${ tuple.toArray.map(_ => "?").mkString(", ") })").mkString(", ") }"
    Insert.Impl[P](this, statement, parameterBinders)

  /**
   * A method to build a query model that inserts data into specified columns defined in a table.
   *
   * @param func
   *   Function to retrieve columns from Table.
   * @tparam T
   *   Type of value to be obtained
   */
  inline def insertInto[T](func: Table[P] => T)(using
    Tuples.IsColumn[T] =:= true
  ): SelectInsert[P, T] =
    val encoder: Encoder.MapToTuple[Column.Extract[T]] = Encoder.fold[Column.Extract[T]]
    SelectInsert[P, T](this, func(this), encoder)

  /**
   * A method to build a query model that inserts data from the model into all columns defined in the table.
   *
   * @param value
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   * @param mirror
   *   product isomorphism map
   */
  @targetName("insertProduct")
  inline def +=(value: P)(using mirror: Mirror.ProductOf[P]): Insert[P] =
    val tuples = Tuple.fromProductTyped(value)
    val parameterBinders = tuples
      .zip(Encoder.fold[mirror.MirroredElemTypes])
      .toList
      .map {
        case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
      }
    Insert.Impl[P](
      this,
      s"INSERT INTO ${ _name } (${ *.toList.mkString(", ") }) VALUES(${ tuples.toArray.map(_ => "?").mkString(", ") })",
      parameterBinders
    )

  /**
   * A method to build a query model that inserts data from multiple models into all columns defined in a table.
   *
   * @param values
   * A class that implements a [[Product]] that is one-to-one with the table definition.
   * @param mirror
   * product isomorphism map
   */
  @targetName("insertProducts")
  inline def ++=(values: List[P])(using mirror: Mirror.ProductOf[P]): Insert[P] =
    val tuples = values.map(Tuple.fromProductTyped)
    val parameterBinders = tuples
      .flatMap(_.zip(Encoder.fold[mirror.MirroredElemTypes]).toList)
      .map {
        case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
      }
    val statement = s"INSERT INTO ${ _name } (${ *.toList
        .mkString(", ") }) VALUES${ tuples.map(tuple => s"(${ tuple.toArray.map(_ => "?").mkString(", ") })").mkString(", ") }"
    Insert.Impl[P](this, statement, parameterBinders)

  /**
   * A method to build a query model that updates specified columns defined in a table.
   *
   * @param tag
   *   A type with a single instance. Here, Column is passed.
   * @param value
   *   A value of type T to be inserted into the specified column.
   * @param mirror
   *   product isomorphism map
   * @param index
   *   Position of the specified type in tuple X
   * @param check
   *   A value to verify that the specified type matches the type of the specified column that the Table has.
   * @tparam Tag
   *   Type with a single instance
   * @tparam T
   *   Scala types that match SQL DataType
   */
  inline def update[Tag <: Singleton, T](tag: Tag, value: T)(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    check:  T =:= Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Update[P] =
    type PARAM = Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    val params    = List(Parameter.Dynamic[PARAM](check(value))(using Encoder.infer[PARAM]))
    val statement = s"UPDATE ${ _name } SET ${ selectDynamic[Tag](tag).name } = ?"
    Update[P](
      table     = this,
      params    = params,
      statement = statement
    )

  /**
   * A method to build a query model that updates all columns defined in the table using the model.
   *
   * @param value
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   * @param mirror
   *   product isomorphism map
   */
  inline def update(value: P)(using mirror: Mirror.ProductOf[P]): Update[P] =
    val parameterBinders = Tuple
      .fromProductTyped(value)
      .zip(Encoder.fold[mirror.MirroredElemTypes])
      .toList
      .map {
        case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
      }
    val statement =
      s"UPDATE ${ _name } SET ${ columnNames.map(name => s"$name = ?").mkString(", ") }"
    Update[P](
      table     = this,
      statement = statement,
      params    = parameterBinders
    )

  /**
   * Method to construct a query to delete a table.
   */
  def delete: Delete[P, Tuple.Map[ElemTypes, Column]] =
    Delete[P, Tuple.Map[ElemTypes, Column]](this, *, s"DELETE FROM ${ _name }")

  /**
   * Method to construct a query to drop a table.
   */
  def dropTable: Command = Command.Pure(s"DROP TABLE `${ _name }`", List.empty)

  /**
   * Method to construct a query to truncate a table.
   */
  def truncateTable: Command = Command.Pure(s"TRUNCATE TABLE `${ _name }`", List.empty)

object Table:

  def apply[P <: Product](using t: Table[P]):                  Table[P] = t
  def apply[P <: Product](name:    String)(using t: Table[P]): Table[P] = t.setName(name)

  private[ldbc] case class Impl[P <: Product, ElemLabels0 <: Tuple, ElemTypes0 <: Tuple](
    _name:       String,
    _alias:      Option[String],
    columnNames: List[String],
    columns:     Tuple.Map[ElemTypes0, Column],
    decoder:     Decoder[P]
  ) extends Table[P]:
    override type ElemLabels = ElemLabels0
    override type ElemTypes  = ElemTypes0
    @targetName("all")
    override def * : Tuple.Map[ElemTypes, Column] = columns
    override def as(name: String): Table[P] =
      val aliasColumns = columns.map([t] => (t: t) => t.asInstanceOf[Column[t]].as(name))
      this.copy(_alias = Some(name), columns = aliasColumns)
    override def setName(name: String): Table[P] = this.copy(_name = name)

  private inline def listOfLabels[T <: Tuple]: List[String] =
    inline erasedValue[T] match {
      case _: EmptyTuple => List.empty
      case _: (t *: ts) =>
        val stringOf = summonInline[t <:< String]
        inline constValueOpt[t] match {
          case Some(value) =>
            stringOf(value) +: listOfLabels[ts]
          case None =>
            error(
              "Types of field labels must be literal string types.\n" + "Found:    " + constValue[
                t
              ] + "\nRequired: (a literal string type)"
            )
        }
    }

  inline def derived[P <: Product](using m: Mirror.ProductOf[P], naming: Naming = Naming.SNAKE): Table[P] =
    val labels  = constValueTuple[m.MirroredElemLabels].toArray.map(_.toString)
    val decodes = Decoder.getDecoders[m.MirroredElemTypes].toArray
    val columns = labels.zip(decodes).map {
      case (label: String, decoder: Decoder.Elem[t]) => Column.Impl[t](label, None)(using decoder)
    }
    val decoder: Decoder[P] = new Decoder[P]((resultSet: ResultSet, prefix: Option[String]) =>
      m.fromTuple(
        Tuple
          .fromArray(columns.map(_.decoder.decode(resultSet, prefix)))
          .asInstanceOf[m.MirroredElemTypes]
      )
    )
    Impl[P, m.MirroredElemLabels, m.MirroredElemTypes](
      _name       = naming.format(constValue[m.MirroredLabel]),
      _alias      = None,
      columnNames = listOfLabels[m.MirroredElemLabels].map(naming.format),
      columns     = Tuple.fromArray(columns).asInstanceOf[Tuple.Map[m.MirroredElemTypes, ldbc.query.builder.Column]],
      decoder     = decoder
    )

  type Extract[T] <: Tuple = T match
    case MySQLTable[t]               => t *: EmptyTuple
    case MySQLTable[t] *: EmptyTuple => t *: EmptyTuple
    case MySQLTable[t] *: ts         => t *: Extract[ts]

private[ldbc] trait TableOpt[P <: Product] extends MySQLTable[Option[P]], Dynamic:

  transparent inline def selectDynamic[Tag <: Singleton](
    tag: Tag
  )(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    decoder: Decoder.Elem[Option[
      ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
    ]]
  ): Column[
    Option[ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]
  ] =
    *.productElement(index.value)
      .asInstanceOf[Column[
        ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
      ]]
      .opt

object TableOpt:

  private[ldbc] case class Impl[P <: Product](table: Table[P]) extends TableOpt[P]:
    override type ElemTypes = table.ElemTypes
    override def _name: String = table._name
    @targetName("all")
    override def * : Tuple.Map[ElemTypes, Column] = table.*
    override def decoder: Decoder[Option[P]] =
      val columns = *.toArray
      new Decoder[Option[P]]((resultSet: ResultSet, prefix: Option[String]) =>
        val result = columns.map {
          case column: Column[t] => column.opt.decoder.decode(resultSet, prefix)
        }
        if result.flatten.length == columns.length then Option(table.decoder.decode(resultSet, prefix))
        else None
      )
