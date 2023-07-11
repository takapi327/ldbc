/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql

import cats.{ Monad, MonadError, Traverse, Alternative }
import cats.data.Kleisli
import cats.implicits.*

/** Trait for generating the specified data type from a ResultSet.
  *
  * @tparam F
  *   The effect type
  * @tparam T
  *   Type you want to build with data obtained from ResultSet
  */
trait ResultSetConsumer[F[_], T]:

  /** Method for generating the specified data type from a ResultSet.
    *
    * @param resultSet
    *   A table of data representing a database result set, which is usually generated by executing a statement that
    *   queries the database.
    * @return
    *   Type you want to build with data obtained from ResultSet
    */
  def consume(resultSet: ResultSet[F]): F[T]

object ResultSetConsumer:

  given [F[_], T](using
    consumer: ResultSetConsumer[F, Option[T]],
    error:    MonadError[F, Throwable]
  ): ResultSetConsumer[F, T] with
    override def consume(resultSet: ResultSet[F]): F[T] =
      consumer.consume(resultSet).flatMap {
        case Some(value) => error.pure(value)
        case None        => error.raiseError(new NoSuchElementException(""))
      }

  given [F[_]: Monad, T](using resultSetKleisli: Kleisli[F, ResultSet[F], T]): ResultSetConsumer[F, Option[T]] with
    override def consume(resultSet: ResultSet[F]): F[Option[T]] =
      for
        hasNext <- resultSet.next()
        result  <- if hasNext then resultSetKleisli.run(resultSet).map(_.some) else Monad[F].pure(None)
      yield result

  given [F[_]: Monad, T, S[_]: Traverse: Alternative](using
    resultSetKleisli: Kleisli[F, ResultSet[F], T]
  ): ResultSetConsumer[F, S[T]] with
    override def consume(resultSet: ResultSet[F]): F[S[T]] =
      Monad[F].whileM[S, T](resultSet.next())(resultSetKleisli.run(resultSet))
