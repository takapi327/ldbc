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

  /**
   * Determine the authentication plugin.
   *
   * @param pluginName
   *   Plugin name
   * @param version
   *   MySQL Server version
   */
  protected def determinatePlugin(pluginName: String, version: Version): Either[MySQLException, AuthenticationPlugin] =
    pluginName match
      case "mysql_native_password" => Right(MysqlNativePasswordPlugin())
      case "sha256_password"       => Right(Sha256PasswordPlugin())
      case "caching_sha2_password" => Right(CachingSha2PasswordPlugin(version))
      case _                       => Left(new MySQLException(s"Unknown authentication plugin: $pluginName"))

  /**
   * Start the authentication process.
   *
   * @param username
   *   Username
   * @param password
   *   Password
   */
  def start(username: String, password: String): F[Unit]

  /**
   * Change the user.
   *
   * @param user
   *   Username
   * @param password
   *   Password
   */
  def changeUser(user: String, password: String): F[Unit]

object Authentication:

  private val FULL_AUTH = "4"

  def apply[F[_]: Exchange: Tracer](
    socket:                  PacketSocket[F],
    initialPacket:           InitialPacket,
    database:                Option[String],
    useSSL:                  Boolean = false,
    allowPublicKeyRetrieval: Boolean = false,
    capabilityFlags:         List[CapabilitiesFlags]
  )(using
    ev: MonadError[F, Throwable]
  ): Authentication[F] =
    new Authentication[F]:

      private val attributes = initialPacket.attributes ++ List(
        database.map(db => Attribute("database", db))
      ).flatten

      /**
       * Read until the authentication is OK.
       * If an error is returned from the server, it throws an exception and exits.
       * 
       * @param plugin
       *   Authentication plugin
       */
      private def readUntilOk(plugin: AuthenticationPlugin, password: String, scrambleBuff: Option[Array[Byte]] = None): F[Unit] =
        socket.receive(AuthenticationPacket.decoder(initialPacket.capabilityFlags)).flatMap {
          case more: AuthMoreDataPacket
            if (allowPublicKeyRetrieval || useSSL) && more.authenticationMethodData.mkString("") == FULL_AUTH =>
            plugin match
              case plugin: CachingSha2PasswordPlugin =>
                cachingSha2Authentication(plugin, password, scrambleBuff.getOrElse(initialPacket.scrambleBuff)) *> readUntilOk(
                  plugin,
                  password
                )
              case plugin: Sha256PasswordPlugin =>
                sha256Authentication(plugin, password, scrambleBuff.getOrElse(initialPacket.scrambleBuff)) *> readUntilOk(plugin, password)
              case _ => ev.raiseError(new MySQLException("Unexpected authentication method"))
          case more: AuthMoreDataPacket        => readUntilOk(plugin, password)
          case packet: AuthSwitchRequestPacket => changeAuthenticationMethod(packet, password)
          case _: OKPacket                     => ev.unit
          case error: ERRPacket                => ev.raiseError(error.toException("Connection error"))
          case unknown: UnknownPacket          => ev.raiseError(unknown.toException("Error during database operation"))
          case _                               => ev.raiseError(new MySQLException("Unexpected packet"))
        }

      /**
       * If authentication method mismatch happens,
       * server sends to client the Protocol::AuthSwitchRequest: which contains the name of the client authentication method to be used and the first authentication payload generated by the new method.
       * Client should switch to the requested authentication method and continue the exchange as dictated by that method.
       * 
       * @param switchRequestPacket
       *   Authentication method Switch Request Packet
       */
      private def changeAuthenticationMethod(switchRequestPacket: AuthSwitchRequestPacket, password: String): F[Unit] =
        determinatePlugin(switchRequestPacket.pluginName, initialPacket.serverVersion) match
          case Left(error) => ev.raiseError(error) *> socket.send(ComQuitPacket())
          case Right(plugin: CachingSha2PasswordPlugin) =>
            val hashedPassword = plugin.hashPassword(password, switchRequestPacket.pluginProvidedData)
            socket.send(AuthSwitchResponsePacket(hashedPassword)) *> readUntilOk(
              plugin,
              password,
              Some(switchRequestPacket.pluginProvidedData)
            )
          case Right(plugin: Sha256PasswordPlugin) =>
            sha256Authentication(plugin, password, switchRequestPacket.pluginProvidedData) *> readUntilOk(plugin, password)
          case Right(plugin) =>
            val hashedPassword = plugin.hashPassword(password, switchRequestPacket.pluginProvidedData)
            socket.send(AuthSwitchResponsePacket(hashedPassword)) *> readUntilOk(
              plugin,
              password,
              Some(switchRequestPacket.pluginProvidedData)
            )

      /**
       * Plain text handshake
       * 
       * @param plugin
       *   Authentication plugin
       * @param scrambleBuff
       *   Scramble buffer for authentication payload
       */
      private def plainTextHandshake(plugin: AuthenticationPlugin, password: String, scrambleBuff: Array[Byte]): F[Unit] =
        val hashedPassword = plugin.hashPassword(password, scrambleBuff)
        socket.send(AuthSwitchResponsePacket(hashedPassword))

      /**
       * SSL handshake.
       * Send a plain password to use SSL/TLS encrypted secure communication.
       */
      private def sslHandshake(password: String): F[Unit] =
        socket.send(AuthSwitchResponsePacket((password + "\u0000").getBytes(StandardCharsets.UTF_8)))

      /**
       * Allow public key retrieval request.
       * RSA-encrypted communication, where the public key is used to encrypt the password for communication.
       * 
       * @param plugin
       *   Authentication plugin
       * @param scrambleBuff
       *   Scramble buffer for authentication payload
       */
      private def allowPublicKeyRetrievalRequest(plugin: Sha256PasswordPlugin, password: String, scrambleBuff: Array[Byte]): F[Unit] =
        socket.receive(AuthMoreDataPacket.decoder).flatMap { moreData =>
          // TODO: When converted to Array[Byte], it contains an extra 1 for some reason. This causes an error in public key parsing when executing Scala JS. Therefore, the first 1Byte is excluded.
          val publicKeyString = moreData.authenticationMethodData
            .drop(1)
            .map("%02x" format _)
            .map(hex => Integer.parseInt(hex, 16).toChar)
            .mkString("")
          val encryptPassword =
            plugin.encryptPassword(password, scrambleBuff, publicKeyString)
          socket.send(AuthSwitchResponsePacket(encryptPassword))
        }

      /**
       * SHA-256 authentication
       * 
       * @param plugin
       *   Authentication plugin
       * @param scrambleBuff
       *   Scramble buffer for authentication payload
       */
      private def sha256Authentication(plugin: Sha256PasswordPlugin, password: String, scrambleBuff: Array[Byte]): F[Unit] =
        (useSSL, allowPublicKeyRetrieval) match
          case (true, _)     => sslHandshake(password)
          case (false, true) => socket.send(ComQuitPacket()) *> allowPublicKeyRetrievalRequest(plugin, password, scrambleBuff)
          case (_, _)        => plainTextHandshake(plugin, password, scrambleBuff)

      /**
       * Caching SHA-2 authentication
       * 
       * @param plugin
       *   Authentication plugin
       * @param scrambleBuff
       *   Scramble buffer for authentication payload
       */
      private def cachingSha2Authentication(plugin: CachingSha2PasswordPlugin, password: String, scrambleBuff: Array[Byte]): F[Unit] =
        (useSSL, allowPublicKeyRetrieval) match
          case (true, _)     => sslHandshake(password)
          case (false, true) => socket.send(ComInitDBPacket("")) *> allowPublicKeyRetrievalRequest(plugin, password, scrambleBuff)
          case (_, _)        => plainTextHandshake(plugin, password, scrambleBuff)

      /**
       * Handshake with the server.
       * 
       * @param plugin
       *   Authentication plugin
       */
      private def handshake(plugin: AuthenticationPlugin, username: String, password: String): F[Unit] =
        val hashedPassword = plugin.hashPassword(password, initialPacket.scrambleBuff)
        val handshakeResponse = HandshakeResponsePacket(
          capabilityFlags,
          username,
          Array(hashedPassword.length.toByte) ++ hashedPassword,
          plugin.name,
          initialPacket.characterSet,
          database
        )
        socket.send(handshakeResponse)

      override def start(username: String, password: String): F[Unit] =
        exchange[F, Unit]("database.authentication") { (span: Span[F]) =>
          span.addAttributes((attributes ++ List(Attribute("username", username)))*) *> (
            determinatePlugin(initialPacket.authPlugin, initialPacket.serverVersion) match
              case Left(error)   => ev.raiseError(error) *> socket.send(ComQuitPacket())
              case Right(plugin) => handshake(plugin, username, password) *> readUntilOk(plugin, password)
          )
        }

      override def changeUser(user: String, password: String): F[Unit] =
        exchange[F, Unit]("authentication.change.user") { (span: Span[F]) =>
          span.addAttributes((
            initialPacket.attributes ++
              List(Attribute("type", "Utility Commands")) ++
              List(Attribute("command", "COM_CHANGE_USER"), Attribute("user", user))
          )*) *> (
            determinatePlugin(initialPacket.authPlugin, initialPacket.serverVersion) match
              case Left(error)   => ev.raiseError(error) *> socket.send(ComQuitPacket())
              case Right(plugin) =>
                socket.send(ComChangeUserPacket(capabilityFlags, user, database, initialPacket.characterSet, initialPacket.authPlugin, plugin.hashPassword(password, initialPacket.scrambleBuff))) *>
                  readUntilOk(plugin, password)
          )
        }
