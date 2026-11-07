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
   * Wraps a freshly connected socket, reporting failures as "unable to connect" until
   * [[Phased.markEstablished]] says otherwise.
   *
   * There is deliberately only ever one of these per connection, sitting directly on the raw socket.
   * The alternative — one wrapper for the handshake and a second one afterwards — cannot work, because
   * a TLS upgrade wraps whatever socket it is handed: the handshake-phase wrapper would survive
   * underneath it and keep claiming `08001` long after the connection was established, on the
   * platforms whose TLS layer goes through `Socket` rather than the raw socket. Switching one
   * wrapper's phase keeps the layering flat and the reported state honest on every platform.
   */
  def phased[F[_]](socket: Socket[F])(using MonadThrow[F]): Phased[F] =
    val phase   = new Phase
    val wrapped = socket match
      case backed: RawBackedSocket => new BackedTranslating(socket, phase, backed.underlying)
      case _                       => new Translating(socket, phase)
    new Phased(wrapped, phase)

  /** Which half of a connection's life the socket is in. */
  private final class Phase:
    @volatile private var established: Boolean = false

    def markEstablished(): Unit = established = true

    def sqlState: String =
      if established then SQLState.COMMUNICATION_LINK_FAILURE else SQLState.UNABLE_TO_CONNECT

    def message: String =
      if established then "The connection to the MySQL server was lost"
      else "Failed to establish a connection to the MySQL server"

  /** The wrapped socket together with the switch that moves it past the handshake. */
  final class Phased[F[_]] private[TransportErrors] (val socket: Socket[F], private val phase: Phase):

    /**
     * Marks the connection established, so later failures are reported as a lost link rather than as
     * an inability to connect. Called once the handshake and any TLS upgrade have completed.
     */
    def markEstablished(): Unit = phase.markEstablished()

  /**
   * Re-raises transport failures as [[ldbc.sql.SQLTransientConnectionException]], keeping the original
   * as the cause.
   *
   * Anything that is already a [[ldbc.sql.SQLException]] passes through untouched. Those carry a
   * meaning of their own — `EofException` distinguishes a closed peer from a timeout, for one — and
   * burying them under another layer would lose it.
   */
  private class Translating[F[_]](socket: Socket[F], phase: Phase)(using F: MonadThrow[F]) extends Socket[F]:

    private def translate[A](fa: F[A]): F[A] =
      F.handleErrorWith(fa) {
        case already: SQLException => F.raiseError(already)
        case error                 =>
          F.raiseError(
            SQLTransientConnectionException(
              message  = phase.message,
              sqlState = Some(phase.sqlState),
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

  /**
   * The decorator, preserving [[ldbc.net.RawBackedSocket]] when the wrapped socket carries it.
   *
   * The platform TLS layers reach through that trait for the concrete raw socket they have to drive,
   * so a decorator that dropped it would silently disable TLS on Scala.js and Scala Native.
   */
  private class BackedTranslating[F[_]: MonadThrow](
    socket: Socket[F],
    phase:  Phase,
    raw:    RawSocket
  ) extends Translating[F](socket, phase),
            RawBackedSocket:
    override def underlying: RawSocket = raw
