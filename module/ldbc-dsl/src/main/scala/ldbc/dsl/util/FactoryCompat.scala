/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.util

import scala.annotation.implicitNotFound
import scala.collection.mutable
import scala.collection.Factory

@implicitNotFound(msg = "Cannot construct a factory of type ${C} with elements of type ${A}.")
trait FactoryCompat[-A, +C]:
  def newBuilder: mutable.Builder[A, C]

object FactoryCompat:
  given [A, C](using factory: Factory[A, C]): FactoryCompat[A, C] = new FactoryCompat[A, C]:
    override def newBuilder: mutable.Builder[A, C] = factory.newBuilder

  given [A]: FactoryCompat[A, Option[A]] = new FactoryCompat[A, Option[A]]:
    override def newBuilder: mutable.Builder[A, Option[A]] = new mutable.Builder[A, Option[A]]:
      private var value:   Option[A] = None
      def addOne(elem: A): this.type = { value = Some(elem); this }
      def clear():         Unit      = { value = None }
      def result():        Option[A] = value
