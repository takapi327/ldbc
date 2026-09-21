/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import ldbc.sql.{ SQLException, SQLTransientConnectionException }

import ldbc.effect.MonadThrow
import ldbc.mysql.data.SQLState
import ldbc.net.{ RawBackedSocket, RawSocket, Socket }

/**
 * Restates transport failures as JDBC connection exceptions.
 *
 * `ldbc-net` deliberately knows nothing about SQL — it has no dependency on `ldbc-sql` and is shared by
 * anything that needs a socket — so it reports failures as `java.io.IOException`. Left alone, that
 * `IOException` reaches user code unchanged, and a caller catching [[ldbc.sql.SQLException]] around a
 * query would miss the very failures most worth catching. This decorator performs the translation at
 * the boundary where the transport becomes a MySQL connection.
 *
 * The failures are transient: whatever went wrong with this socket, the pool can hand out a different
 * connection and the operation may well succeed. That is the opposite of how a server-sent SQLSTATE of
 * class `08` is classified (see `ERRPacket.toException`), and deliberately so — here it is known that
 * the connection, not the request, is what failed.
 */
private[mysql] object TransportErrors:

  /**
   * Wraps the socket used while the connection is still being established, covering the initial
   * handshake and the TLS negotiation.
   */
  def connecting[F[_]: MonadThrow](socket: Socket[F]): Socket[F] =
    wrap(socket, SQLState.UNABLE_TO_CONNECT, "Failed to establish a connection to the MySQL server")

  /** Wraps the socket used for command traffic once the connection is established. */
  def established[F[_]: MonadThrow](socket: Socket[F]): Socket[F] =
    wrap(socket, SQLState.COMMUNICATION_LINK_FAILURE, "The connection to the MySQL server was lost")

  /**
   * Applies the decorator, preserving [[ldbc.net.RawBackedSocket]] when the wrapped socket carries it.
   *
   * The platform TLS layers reach through that trait for the concrete raw socket they have to drive,
   * so a decorator that dropped it would silently disable TLS on Scala.js and Scala Native.
   */
  private def wrap[F[_]: MonadThrow](socket: Socket[F], sqlState: String, message: String): Socket[F] =
    socket match
      case backed: RawBackedSocket => new BackedTranslating(socket, sqlState, message, backed.underlying)
      case _                       => new Translating(socket, sqlState, message)

  /**
   * Re-raises transport failures as [[ldbc.sql.SQLTransientConnectionException]], keeping the original
   * as the cause.
   *
   * Anything that is already a [[ldbc.sql.SQLException]] passes through untouched. Those carry a
   * meaning of their own — `EofException` distinguishes a closed peer from a timeout, for one — and
   * burying them under another layer would lose it.
   */
  private class Translating[F[_]](socket: Socket[F], sqlState: String, message: String)(using F: MonadThrow[F])
    extends Socket[F]:

    private def translate[A](fa: F[A]): F[A] =
      F.handleErrorWith(fa) {
        case already: SQLException => F.raiseError(already)
        case error                 =>
          F.raiseError(
            SQLTransientConnectionException(
              message  = message,
              sqlState = Some(sqlState),
              detail   = Option(error.getMessage),
              hint     = Some("Discard this session and retry with a new one."),
              vendor   = "MySQL",
              cause    = Some(error)
            )
          )
      }

    override def read(n:      Int):         F[Option[Array[Byte]]] = translate(socket.read(n))
    override def write(bytes: Array[Byte]): F[Unit]                = translate(socket.write(bytes))
    override def close():                   F[Unit]                = translate(socket.close())

  private class BackedTranslating[F[_]: MonadThrow](
    socket:   Socket[F],
    sqlState: String,
    message:  String,
    raw:      RawSocket
  ) extends Translating[F](socket, sqlState, message),
            RawBackedSocket:
    override def underlying: RawSocket = raw
