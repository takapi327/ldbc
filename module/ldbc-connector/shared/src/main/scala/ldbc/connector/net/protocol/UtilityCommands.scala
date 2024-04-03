/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import cats.*
import cats.syntax.all.*

import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.{ Tracer, Span }

import ldbc.connector.data.EnumMySQLSetOption
import ldbc.connector.net.PacketSocket
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*

/**
 * Utility commands
 * 
 * @tparam F
 *   the effect type
 */
trait UtilityCommands[F[_]]:

  /**
   * Quit the connection
   */
  def comQuit(): F[Unit]

  /**
   * Initialize the database
   * 
   * @param schema
   *   the name of a schema in which to work
   */
  def comInitDB(schema: String): F[Unit]

  /**
   * Get the statistics of the connection
   */
  def comStatistics(): F[StatisticsPacket]

  /**
   * Check if the server is alive.
   */
  def comPing(): F[Boolean]

  /**
   * Reset the connection
   */
  def comResetConnection(): F[Unit]

  /**
   * Set an option
   * 
   * @param optionOperation
   *   the option operation
   */
  def comSetOption(optionOperation: EnumMySQLSetOption): F[Unit]

object UtilityCommands:

  def apply[F[_]: Exchange: Tracer](
    socket:        PacketSocket[F],
    initialPacket: InitialPacket
  )(using ev: MonadError[F, Throwable]): UtilityCommands[F] =
    new UtilityCommands[F]:

      private val attributes = initialPacket.attributes ++ List(
        Attribute("type", "Utility Commands")
      )

      override def comQuit(): F[Unit] =
        exchange[F, Unit]("utility_commands") { (span: Span[F]) =>
          span.addAttributes((attributes :+ Attribute("command", "COM_QUIT"))*) *> socket.send(ComQuitPacket())
        }

      override def comInitDB(schema: String): F[Unit] =
        exchange[F, Unit]("utility_commands") { (span: Span[F]) =>
          span.addAttributes((attributes ++ List(Attribute("command", "COM_INIT_DB"), Attribute("schema", schema)))*) *>
            socket.send(ComInitDBPacket(schema)) *>
            socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute change schema"))
              case ok: OKPacket     => ev.unit
            }
        }

      override def comStatistics(): F[StatisticsPacket] =
        exchange[F, StatisticsPacket]("utility_commands") { (span: Span[F]) =>
          span.addAttributes((attributes :+ Attribute("command", "COM_STATISTICS"))*) *>
            socket.send(ComStatisticsPacket()) *>
            socket.receive(StatisticsPacket.decoder)
        }

      override def comPing(): F[Boolean] =
        exchange[F, Boolean]("utility_commands") { (span: Span[F]) =>
          span.addAttributes((attributes :+ Attribute("command", "COM_PING"))*) *>
            socket.send(ComPingPacket()) *>
            socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
              case error: ERRPacket => ev.pure(false)
              case ok: OKPacket     => ev.pure(true)
            }
        }

      override def comResetConnection(): F[Unit] =
        exchange[F, Unit]("utility_commands") { (span: Span[F]) =>
          span.addAttributes((attributes :+ Attribute("command", "COM_RESET_CONNECTION"))*) *>
            socket.send(ComResetConnectionPacket()) *>
            socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute reset connection"))
              case ok: OKPacket     => ev.unit
            }
        }
        
      override def comSetOption(optionOperation: EnumMySQLSetOption): F[Unit] =
        exchange[F, Unit]("utility_commands") { (span: Span[F]) =>
          span.addAttributes((attributes ++ List(Attribute("command", "COM_SET_OPTION"), Attribute("option", optionOperation.toString)))*) *>
            socket.send(ComSetOptionPacket(optionOperation)) *>
            socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute set option"))
              case eof: EOFPacket     => ev.unit
              case ok: OKPacket     => ev.unit
            }
        }
