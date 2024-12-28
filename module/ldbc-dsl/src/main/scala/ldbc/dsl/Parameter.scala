/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.syntax.all.*

import ldbc.dsl.codec.Encoder

/**
 * Trait for setting Scala and Java values to PreparedStatement.
 */
trait Parameter:

  /** Query parameters to be plugged into the Statement. */
  def value: String

object Parameter:

  case class Static(value: String) extends Parameter:
    override def toString: String = value

  trait Dynamic extends Parameter
  object Dynamic:
    case class Success(encoded: Encoder.Supported) extends Dynamic:
      override def value: String = encoded.toString
    case class Failure(errors: List[String]) extends Dynamic:
      override def value: String = errors.mkString(", ")

    def many[A](encoded: Encoder.Encoded): List[Dynamic] =
      encoded match
        case Encoder.Encoded.Success(list) => list.map(value => Success(value))
        case Encoder.Encoded.Failure(errors) => List(Failure(errors.toList))

  given [A](using encoder: Encoder[A]): Conversion[A, Dynamic] with
    override def apply(value: A): Dynamic = encoder.encode(value) match
      case Encoder.Encoded.Success(list) =>
        list match
          case head :: Nil => Dynamic.Success(head)
          case _ => Dynamic.Failure(List("Multiple values are not allowed"))
      case Encoder.Encoded.Failure(errors) => Dynamic.Failure(errors.toList)
