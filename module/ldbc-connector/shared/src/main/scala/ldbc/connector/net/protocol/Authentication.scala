/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import java.nio.charset.StandardCharsets

import cats.*
import cats.syntax.all.*

import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.{ Tracer, Span }

import ldbc.connector.authenticator.*
import ldbc.connector.util.Version
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

  def start(): F[Unit]

  protected def determinatePlugin(pluginName: String, version: Version): Either[MySQLException, AuthenticationPlugin] =
    pluginName match
      case "mysql_native_password" => Right(MysqlNativePasswordPlugin())
      case "sha256_password"       => Right(Sha256PasswordPlugin())
      case "caching_sha2_password" => Right(CachingSha2PasswordPlugin(version))
      case _                       => Left(new MySQLException(s"Unknown authentication plugin: $pluginName"))

object Authentication:

  def apply[F[_]: Exchange: Tracer](
    socket:                  PacketSocket[F],
    initialPacket:           InitialPacket,
    username:                String,
    password:                String,
    database:                Option[String],
    useSSL:                  Boolean = false,
    allowPublicKeyRetrieval: Boolean = false
  )(using
    ev: MonadError[F, Throwable]
  ): Authentication[F] =
    new Authentication[F]:

      private def readUntilOk(plugin: AuthenticationPlugin): F[Unit] =
        socket.receive(AuthenticationPacket.decoder(initialPacket.capabilityFlags)).flatMap {
          case more: AuthMoreDataPacket
            if allowPublicKeyRetrieval && more.authenticationMethodData.mkString("") == "4" =>
            plugin match
              case plugin: CachingSha2PasswordPlugin =>
                cachingSha2Authentication(plugin, initialPacket.scrambleBuff) *> readUntilOk(plugin)
              case plugin: Sha256PasswordPlugin =>
                sha256Authentication(plugin, initialPacket.scrambleBuff) *> readUntilOk(plugin)
              case _ => ev.raiseError(new MySQLException("Unexpected authentication method"))
          case more: AuthMoreDataPacket if more.authenticationMethodData.mkString("") == "4" => readUntilOk(plugin)
          case more: AuthMoreDataPacket if more.authenticationMethodData.mkString("") == "3" => readUntilOk(plugin)
          case packet: AuthSwitchRequestPacket => changeAuthenticationMethod(packet)
          case _: OKPacket                     => ev.unit
          case error: ERRPacket                => ev.raiseError(error.toException("Connection error"))
          case unknown: UnknownPacket          => ev.raiseError(unknown.toException("Error during database operation"))
        }

      /**
       * If authentication method mismatch happens,
       * server sends to client the Protocol::AuthSwitchRequest: which contains the name of the client authentication method to be used and the first authentication payload generated by the new method.
       * Client should switch to the requested authentication method and continue the exchange as dictated by that method.
       * 
       * @param switchRequestPacket
       *   Authentication method Switch Request Packet
       */
      private def changeAuthenticationMethod(switchRequestPacket: AuthSwitchRequestPacket): F[Unit] =
        determinatePlugin(switchRequestPacket.pluginName, initialPacket.serverVersion) match
          case Left(error) => ev.raiseError(error) *> socket.send(ComQuitPacket())
          case Right(plugin: CachingSha2PasswordPlugin) =>
            cachingSha2Authentication(plugin, switchRequestPacket.pluginProvidedData) *> readUntilOk(plugin)
          case Right(plugin: Sha256PasswordPlugin) =>
            sha256Authentication(plugin, switchRequestPacket.pluginProvidedData) *> readUntilOk(plugin)
          case Right(plugin) =>
            val hashedPassword = plugin.hashPassword(password, switchRequestPacket.pluginProvidedData)
            socket.send(AuthSwitchResponsePacket(hashedPassword)) *> readUntilOk(plugin)

      private def plainTextHandshake(plugin: AuthenticationPlugin, scrambleBuff: Array[Byte]): F[Unit] =
        val hashedPassword = plugin.hashPassword(password, scrambleBuff)
        socket.send(AuthSwitchResponsePacket(hashedPassword))

      private def sslHandshake(): F[Unit] =
        socket.send(AuthSwitchResponsePacket((password + "\u0000").getBytes(StandardCharsets.UTF_8)))

      private def allowPublicKeyRetrievalRequest(plugin: Sha256PasswordPlugin, scrambleBuff: Array[Byte]): F[Unit] =
        socket.receive(AuthMoreDataPacket.decoder).flatMap { moreData =>
          val publicKeyString = moreData.authenticationMethodData
            .map("%02x" format _)
            .map(hex => Integer.parseInt(hex, 16).toChar)
            .mkString("")
          val encryptPassword =
            plugin.encryptPassword(password, scrambleBuff, publicKeyString)
          socket.send(AuthSwitchResponsePacket(encryptPassword))
        }

      private def sha256Authentication(plugin: Sha256PasswordPlugin, scrambleBuff: Array[Byte]): F[Unit] =
        (useSSL, allowPublicKeyRetrieval) match
          case (true, _)     => sslHandshake()
          case (false, true) => socket.send(ComQuitPacket()) *> allowPublicKeyRetrievalRequest(plugin, scrambleBuff)
          case (_, _)        => plainTextHandshake(plugin, scrambleBuff)

      private def cachingSha2Authentication(plugin: CachingSha2PasswordPlugin, scrambleBuff: Array[Byte]): F[Unit] =
        (useSSL, allowPublicKeyRetrieval) match
          case (true, _)     => sslHandshake()
          case (false, true) => socket.send(ComInitDBPacket()) *> allowPublicKeyRetrievalRequest(plugin, scrambleBuff)
          case (_, _)        => plainTextHandshake(plugin, scrambleBuff)

      private def handshake(plugin: AuthenticationPlugin): F[Unit] =
        val hashedPassword = plugin.hashPassword(password, initialPacket.scrambleBuff)
        val handshakeResponse = HandshakeResponsePacket(
          initialPacket.capabilityFlags,
          username,
          Array(hashedPassword.length.toByte) ++ hashedPassword,
          plugin.name
        )
        socket.send(handshakeResponse)

      override def start(): F[Unit] =
        exchange[F, Unit]("authentication") { (span: Span[F]) =>
          database.fold(span.addAttribute(Attribute("username", username)))(database =>
            span.addAttributes(Attribute("username", username), Attribute("database", database))
          ) *> (
            determinatePlugin(initialPacket.authPlugin, initialPacket.serverVersion) match
              case Left(error)   => ev.raiseError(error) *> socket.send(ComQuitPacket())
              case Right(plugin) => handshake(plugin) *> readUntilOk(plugin)
          )
        }
