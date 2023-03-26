/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import javax.sql.DataSource

import cats.data.Kleisli

import ldbc.sql.Connection

trait ConnectionSyntax[F[_]]:

  extension [T](connectionKleisli: Kleisli[F, Connection[F], T])
    def readOnly: Kleisli[F, DataSource, T]

    def autoCommit: Kleisli[F, DataSource, T]

    def transaction: Kleisli[F, DataSource, T]
