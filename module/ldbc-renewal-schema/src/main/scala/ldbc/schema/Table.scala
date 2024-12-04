/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.language.dynamics
import scala.deriving.Mirror

import ldbc.dsl.codec.Decoder
import ldbc.statement.{ AbstractTable, Column }
import ldbc.schema.interpreter.Tuples

trait Table[T](val $name: String) extends AbstractTable[T]:

  type Column[A] = ldbc.statement.Column[A]

  protected final def column[A](name: String)(using Decoder.Elem[A]): Column[A] =
    ldbc.statement.Column[A](name, $name)

  override final def statement: String = $name

  override def toString: String = s"Table($$name)"

object Table:

  /** A type function to pull a type parameter it has from a type with one type parameter. */
  type Extract[T] = T match
    case Option[t] => Extract[t]
    case Array[t]  => Extract[t]
    case List[t]   => Extract[t]
    case Seq[t]    => Extract[t]
    case Set[t]    => Extract[t]
    case _         => T

  /** A type function that derives its type from the type parameters that Option has. */
  type ExtractOption[T] = T match
    case Option[t] => Extract[t]
    case _         => T

  private[ldbc] case class Opt[T](
    $name:   String,
    columns: List[Column[?]],
    *      : Column[Option[T]]
  ) extends AbstractTable.Opt[T],
            Dynamic:

    override def statement: String = $name

    transparent inline def selectDynamic[Tag <: Singleton](
      tag: Tag
    )(using
      mirror: Mirror.Of[T],
      index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    ): Column[
      Option[ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]
    ] =
      columns
        .apply(index.value)
        .asInstanceOf[Column[
          ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
        ]]
        .opt
