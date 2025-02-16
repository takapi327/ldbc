/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.macros

import scala.quoted.*

import ldbc.dsl.codec.Codec

import ldbc.statement.formatter.Naming

import ldbc.schema.*

object DataTypeColumnMacros:

  def namedColumnImpl[A](
    alias:      Expr[Option[String]],
    dataType:   Expr[DataType[A]],
    isOptional: Expr[Boolean]
  )(using
    q:   Quotes,
    tpe: Type[A]
  ): Expr[() => DataTypeColumn[A]] =
    import quotes.reflect.*

    @scala.annotation.tailrec()
    def enclosingTerm(sym: Symbol): Symbol =
      sym match
        case _ if sym.flags is Flags.Macro => enclosingTerm(sym.owner)
        case _ if !sym.isTerm              => enclosingTerm(sym.owner)
        case _                             => sym

    val codec = Expr.summon[Codec[A]].getOrElse {
      report.errorAndAbort(s"Codec for type $tpe not found")
    }

    val naming = Expr.summon[Naming] match
      case Some(naming) => naming
      case None         => '{ Naming.SNAKE }

    val name = '{ $naming.format(${ Expr(enclosingTerm(Symbol.spliceOwner).name) }) }
    '{ () => DataTypeColumn.apply[A]($name, $alias, $dataType, $isOptional)(using $codec) }

  def namedNumericColumnImpl[A](
    alias:      Expr[Option[String]],
    dataType:   Expr[DataType[A]],
    isOptional: Expr[Boolean]
  )(using
    q:   Quotes,
    tpe: Type[A]
  ): Expr[() => DataTypeColumn.NumericColumn[A]] =
    import quotes.reflect.*

    @scala.annotation.tailrec()
    def enclosingTerm(sym: Symbol): Symbol =
      sym match
        case _ if sym.flags is Flags.Macro => enclosingTerm(sym.owner)
        case _ if !sym.isTerm              => enclosingTerm(sym.owner)
        case _                             => sym

    val codec = Expr.summon[Codec[A]].getOrElse {
      report.errorAndAbort(s"Codec for type $tpe not found")
    }

    val naming = Expr.summon[Naming] match
      case Some(naming) => naming
      case None         => '{ Naming.SNAKE }

    val name = '{ $naming.format(${ Expr(enclosingTerm(Symbol.spliceOwner).name) }) }
    '{ () => DataTypeColumn.numeric[A]($name, $alias, $dataType, $isOptional)(using $codec) }

  def namedDecimalColumnImpl[A](
    alias:      Expr[Option[String]],
    dataType:   Expr[(Int, Int) => DataType[A]],
    isOptional: Expr[Boolean]
  )(using
    q:   Quotes,
    tpe: Type[A]
  ): Expr[(Int, Int) => DataTypeColumn.NumericColumn[A]] =
    import quotes.reflect.*

    @scala.annotation.tailrec()
    def enclosingTerm(sym: Symbol): Symbol =
      sym match
        case _ if sym.flags is Flags.Macro => enclosingTerm(sym.owner)
        case _ if !sym.isTerm              => enclosingTerm(sym.owner)
        case _                             => sym

    val codec = Expr.summon[Codec[A]].getOrElse {
      report.errorAndAbort(s"Codec for type $tpe not found")
    }

    val naming = Expr.summon[Naming] match
      case Some(naming) => naming
      case None         => '{ Naming.SNAKE }

    val name = '{ $naming.format(${ Expr(enclosingTerm(Symbol.spliceOwner).name) }) }
    '{ (accuracy: Int, scale: Int) =>
      DataTypeColumn.numeric[A]($name, $alias, $dataType(accuracy, scale), $isOptional)(using $codec)
    }

  def namedDoubleColumnImpl[A](
    alias:      Expr[Option[String]],
    dataType:   Expr[Int => DataType[A]],
    isOptional: Expr[Boolean]
  )(using
    q:   Quotes,
    tpe: Type[A]
  ): Expr[Int => DataTypeColumn.NumericColumn[A]] =
    import quotes.reflect.*

    @scala.annotation.tailrec()
    def enclosingTerm(sym: Symbol): Symbol =
      sym match
        case _ if sym.flags is Flags.Macro => enclosingTerm(sym.owner)
        case _ if !sym.isTerm              => enclosingTerm(sym.owner)
        case _                             => sym

    val codec = Expr.summon[Codec[A]].getOrElse {
      report.errorAndAbort(s"Codec for type $tpe not found")
    }

    val naming = Expr.summon[Naming] match
      case Some(naming) => naming
      case None         => '{ Naming.SNAKE }

    val name = '{ $naming.format(${ Expr(enclosingTerm(Symbol.spliceOwner).name) }) }
    '{ (accuracy: Int) => DataTypeColumn.numeric[A]($name, $alias, $dataType(accuracy), $isOptional)(using $codec) }

  def namedStringColumnImpl[A](
    alias:      Expr[Option[String]],
    dataType:   Expr[DataType[A]],
    isOptional: Expr[Boolean]
  )(using
    q:   Quotes,
    tpe: Type[A]
  ): Expr[() => DataTypeColumn.StringColumn[A]] =
    import quotes.reflect.*

    @scala.annotation.tailrec()
    def enclosingTerm(sym: Symbol): Symbol =
      sym match
        case _ if sym.flags is Flags.Macro => enclosingTerm(sym.owner)
        case _ if !sym.isTerm              => enclosingTerm(sym.owner)
        case _                             => sym

    val codec = Expr.summon[Codec[A]].getOrElse {
      report.errorAndAbort(s"Codec for type $tpe not found")
    }

    val naming = Expr.summon[Naming] match
      case Some(naming) => naming
      case None         => '{ Naming.SNAKE }

    val name = '{ $naming.format(${ Expr(enclosingTerm(Symbol.spliceOwner).name) }) }
    '{ () => DataTypeColumn.string[A]($name, $alias, $dataType, $isOptional)(using $codec) }

  def namedStringLengthColumnImpl[A](
    alias:      Expr[Option[String]],
    dataType:   Expr[Int => DataType[A]],
    isOptional: Expr[Boolean]
  )(using
    q:   Quotes,
    tpe: Type[A]
  ): Expr[Int => DataTypeColumn.StringColumn[A]] =
    import quotes.reflect.*

    @scala.annotation.tailrec()
    def enclosingTerm(sym: Symbol): Symbol =
      sym match
        case _ if sym.flags is Flags.Macro => enclosingTerm(sym.owner)
        case _ if !sym.isTerm              => enclosingTerm(sym.owner)
        case _                             => sym

    val codec = Expr.summon[Codec[A]].getOrElse {
      report.errorAndAbort(s"Codec for type $tpe not found")
    }

    val naming = Expr.summon[Naming] match
      case Some(naming) => naming
      case None         => '{ Naming.SNAKE }

    val name = '{ $naming.format(${ Expr(enclosingTerm(Symbol.spliceOwner).name) }) }
    '{ (length: Int) => DataTypeColumn.string[A]($name, $alias, $dataType(length), $isOptional)(using $codec) }

  def namedTemporalColumnImpl[A](
    alias:      Expr[Option[String]],
    dataType:   Expr[DataType[A]],
    isOptional: Expr[Boolean]
  )(using
    q:   Quotes,
    tpe: Type[A]
  ): Expr[() => DataTypeColumn.TemporalColumn[A]] =
    import quotes.reflect.*

    @scala.annotation.tailrec()
    def enclosingTerm(sym: Symbol): Symbol =
      sym match
        case _ if sym.flags is Flags.Macro => enclosingTerm(sym.owner)
        case _ if !sym.isTerm              => enclosingTerm(sym.owner)
        case _                             => sym

    val codec = Expr.summon[Codec[A]].getOrElse {
      report.errorAndAbort(s"Codec for type $tpe not found")
    }

    val naming = Expr.summon[Naming] match
      case Some(naming) => naming
      case None         => '{ Naming.SNAKE }

    val name = '{ $naming.format(${ Expr(enclosingTerm(Symbol.spliceOwner).name) }) }
    '{ () => DataTypeColumn.temporal[A]($name, $alias, $dataType, $isOptional)(using $codec) }
