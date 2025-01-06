/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import cats.syntax.all.*

import org.typelevel.twiddles.TwiddleSyntax

import ldbc.connector.data.{ Encoded, Type }

/**
 * Encoder of MySQL text-format data from Scala types.
 */
trait Encoder[A] extends TwiddleSyntax[Encoder]:
  outer =>

  protected lazy val empty: List[Option[Encoded]] = types.as(None)

  /**
   * Encode a value of type `A`, yielding a list of MySQL text-formatted strings, lifted to
   * `Option` to handle `NULL` values. Encoding failures raise unrecoverable errors.
   */
  def encode(a: A): List[Option[Encoded]]

  /** Types of encoded fields, in order. */
  def types: List[Type]

  /** `Encoder` is semigroupal: a pair of encoders make a encoder for a pair. */
  def product[B](fb: Encoder[B]): Encoder[(A, B)] =
    new Encoder[(A, B)]:
      override def encode(ab: (A, B)): List[Option[Encoded]] = outer.encode(ab._1) ++ fb.encode(ab._2)

      override val types: List[Type] = outer.types ++ fb.types
