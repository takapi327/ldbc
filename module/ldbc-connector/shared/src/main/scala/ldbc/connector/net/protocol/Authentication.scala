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

import ldbc.connector.authenticator.*
import ldbc.connector.data.CapabilitiesFlags
import ldbc.connector.exception.MySQLException
import ldbc.connector.net.PacketSocket
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.packet.request.*

/**
 * Protocol to handle the Authentication Phase
 * 
 * Assume the client wants to log in via user account U and that user account is defined to use authentication method server_method. The fast authentication path is used when:
 * 
 * - the server used server_method to generate authentication data in the Protocol::Handshake packet.
 * - the client used a client_authentication_method in Protocol::HandshakeResponse: that is compatible with the server_method used by the server.
 * 
 * In that case the first round of authentication has been already commenced during the handshake.
 * Now, depending on the authentication method server_method, further authentication can be exchanged until the server either accepts or refuses the authentication.
 *
 * @tparam F
 *   The effect type
 */
trait Authentication[F[_]]:

  def apply(username: String, password: String, database: Option[String]): F[Unit]

object Authentication:

  def apply[F[_]: Exchange: Tracer](socket: PacketSocket[F], initialPacket: InitialPacket)(using
    ev: MonadError[F, Throwable]
  ): Authentication[F] =
    new Authentication[F]:

      private def readUntilOk(password: String): F[Unit] =
        socket.receive(AuthenticationPacket.decoder(initialPacket.capabilityFlags)).flatMap {
          case _: AuthMoreDataPacket => readUntilOk(password)
          case switchRequestPacket: AuthSwitchRequestPacket =>
            determinatePlugin(switchRequestPacket.pluginName) match
              case Left(error) => ev.raiseError(error) *> readUntilOk(password)
              case Right(plugin) =>
                authSwitchResponse(password, plugin, switchRequestPacket.pluginProvidedData) *>
                  readUntilOk(password)
          case _: OKPacket            => ev.unit
          case error: ERRPacket       => ev.raiseError(error.toException("Connection error"))
          case unknown: UnknownPacket => ev.raiseError(unknown.toException("Error during database operation"))
        }

      private def authSwitchResponse(
        password:     String,
        plugin:   AuthenticationPlugin,
        scrambleBuff: Array[Byte]
      ): F[Unit] =
        val hashedPassword = plugin.hashPassword(password, scrambleBuff)
        socket.send(AuthSwitchResponsePacket(hashedPassword))

      private def handshake(
        username:        String,
        password:        String,
        plugin:          AuthenticationPlugin,
        capabilityFlags: Seq[CapabilitiesFlags],
        scrambleBuff:    Array[Byte]
      ): F[Unit] =
        val hashedPassword = plugin.hashPassword(password, scrambleBuff)
        val handshakeResponse = HandshakeResponsePacket(
          capabilityFlags,
          username,
          Array(hashedPassword.length.toByte) ++ hashedPassword,
          plugin.name
        )
        socket.send(handshakeResponse)

      override def apply(username: String, password: String, database: Option[String]): F[Unit] =
        exchange[F, Unit]("authentication") { (span: Span[F]) =>
          for
            _ <- database.fold(span.addAttribute(Attribute("username", username)))(database =>
              span.addAttributes(Attribute("username", username), Attribute("database", database))
            )
            _ <- determinatePlugin(initialPacket.authPlugin) match
              case Left(error)   => ev.raiseError(error) *> socket.send(ComQuitPacket())
              case Right(plugin) => handshake(username, password, plugin, initialPacket.capabilityFlags, initialPacket.scrambleBuff) *> readUntilOk(password)
          yield ()
        }

  private def determinatePlugin(pluginName: String): Either[MySQLException, AuthenticationPlugin] =
    pluginName match
      case "mysql_native_password" => Right(new MysqlNativePasswordPlugin)
      case "caching_sha2_password" => Right(CachingSha2PasswordPlugin(None, None))
      case _                       => Left(new MySQLException(s"Unknown authentication plugin: $pluginName"))
