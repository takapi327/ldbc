/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql.util

import scala.annotation.implicitNotFound
import scala.collection.Factory
import scala.collection.mutable

@implicitNotFound(msg = "Cannot construct a factory of type ${C} with elements of type ${A}.")
trait FactoryCompat[-A, +C]:
  def newBuilder: mutable.Builder[A, C]

object FactoryCompat:
  given [A, C](using factory: Factory[A, C]): FactoryCompat[A, C] = new FactoryCompat[A, C]:
    override def newBuilder: mutable.Builder[A, C] = factory.newBuilder
