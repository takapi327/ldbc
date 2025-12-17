/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import java.nio.charset.StandardCharsets

import scala.collection.immutable.ListMap
import scala.concurrent.duration.*

import scodec.Decoder

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import fs2.hashing.Hashing
import fs2.io.net.Socket

import org.typelevel.otel4s.trace.{ Span, Tracer }
import org.typelevel.otel4s.Attribute

import ldbc.authentication.plugin.*

import ldbc.connector.authenticator.{ MysqlNativePasswordPlugin, Sha256PasswordPlugin, CachingSha2PasswordPlugin }
import ldbc.connector.data.*
import ldbc.connector.exception.*
import ldbc.connector.net.packet.*
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.protocol.*
import ldbc.connector.telemetry.*

/**
 * Protocol is a protocol to communicate with MySQL server.
 * It provides a way to authenticate, reset sequence id, and close the connection.
 *
 * @tparam F
 *   the effect type
 */
trait Protocol[F[_]] extends UtilityCommands[F], Authentication[F]:

  /**
   * Returns the initial packet.
   *
   * @return
   *   the initial packet
   */
  def initialPacket: InitialPacket

  /**
   * Class that holds MySQL host information.
   *
   * @return
   *   the host information
   */
  def hostInfo: HostInfo

  /**
   * Receive the next `ResponsePacket`, or raise an exception if EOF is reached before a complete
   * message arrives.
   */
  def receive[P <: ResponsePacket](decoder: Decoder[P]): F[P]

  /** Send the specified request packet. */
  def send(request: RequestPacket): F[Unit]

  /**
   * Resets the sequence id.
   */
  def resetSequenceId: F[Unit]

  /**
   * Resets the connection.
   */
  def resetConnection: F[Unit]

  /**
   * Controls whether or not multiple SQL statements are allowed to be executed at once.
   *
   * NOTE: It can only be used for batch processing with Insert, Update, and Delete statements.
   *
   * @param optionOperation
   * [[EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON]] or [[EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF]]
   */
  def setOption(optionOperation: EnumMySQLSetOption): F[Unit]

  /**
   * Enables multiple SQL statements to be executed at once.
   *
   * NOTE: It can only be used for batch processing with Insert, Update, and Delete statements.
   */
  def enableMultiQueries: F[Unit] = setOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON)

  /**
   * Disables multiple SQL statements to be executed at once.
   *
   * NOTE: It can only be used for batch processing with Insert, Update, and Delete statements.
   */
  def disableMultiQueries: F[Unit] = setOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF)

  /**
   * Repeats the process `times` times.
   *
   * @param times
   *   the number of times to repeat the process
   * @param decoder
   *   the decoder to decode the response packet
   * @tparam P
   *   the type of the response packet
   * @return
   *   a vector of the response packets
   */
  def repeatProcess[P <: ResponsePacket](times: Int, decoder: Decoder[P]): F[Vector[P]]

  /**
   * Reads until EOF is reached.
   *
   * @param decoder
   *   the decoder to decode the response packet
   * @tparam P
   *   the type of the response packet
   * @return
   *   a vector of the response packets
   */
  def readUntilEOF[P <: ResponsePacket](decoder: Decoder[P | EOFPacket | ERRPacket]): F[Vector[P]]

  /**
   * Returns the server variables.
   */
  def serverVariables(): F[Map[String, String]]

object Protocol:

  private val SELECT_SERVER_VARIABLES_QUERY =
    "SELECT @@session.auto_increment_increment AS auto_increment_increment, @@character_set_client AS character_set_client, @@character_set_connection AS character_set_connection, @@character_set_results AS character_set_results, @@character_set_server AS character_set_server, @@collation_server AS collation_server, @@collation_connection AS collation_connection, @@init_connect AS init_connect, @@interactive_timeout AS interactive_timeout, @@license AS license, @@lower_case_table_names AS lower_case_table_names, @@max_allowed_packet AS max_allowed_packet, @@net_write_timeout AS net_write_timeout, @@performance_schema AS performance_schema, @@sql_mode AS sql_mode, @@system_time_zone AS system_time_zone, @@time_zone AS time_zone, @@transaction_isolation AS transaction_isolation, @@wait_timeout AS wait_timeout"

  private[ldbc] case class Impl[F[_]: Async: Tracer](
    initialPacket:               InitialPacket,
    hostInfo:                    HostInfo,
    socket:                      PacketSocket[F],
    useSSL:                      Boolean = false,
    allowPublicKeyRetrieval:     Boolean = false,
    capabilityFlags:             Set[CapabilitiesFlags],
    sequenceIdRef:               Ref[F, Byte],
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[F]],
    plugins: Map[String, AuthenticationPlugin[F]]
  )(using ev: MonadError[F, Throwable], ex: Exchange[F])
    extends Protocol[F]:

    private val attributes = List(
      TelemetryAttribute.dbSystemName,
      TelemetryAttribute.serverAddress(hostInfo.host),
      TelemetryAttribute.serverPort(hostInfo.port),
      TelemetryAttribute.dbMysqlVersion(initialPacket.serverVersion.toString),
      TelemetryAttribute.dbMysqlThreadId(initialPacket.threadId)
    ) ++ hostInfo.database
      .map(db => TelemetryAttribute.dbNamespace(db))
      .toList

    override def receive[P <: ResponsePacket](decoder: Decoder[P]): F[P] = socket.receive(decoder)

    override def send(request: RequestPacket): F[Unit] = socket.send(request)

    override def comQuit(): F[Unit] =
      exchange[F, Unit](TelemetrySpanName.CONNECTION_CLOSE) { (span: Span[F]) =>
        span.addAttributes(attributes*) *> socket.send(ComQuitPacket())
      }

    override def comInitDB(schema: String): F[Unit] =
      exchange[F, Unit](TelemetrySpanName.CHANGE_DATABASE) { (span: Span[F]) =>
        span.addAttributes((attributes ++ List(TelemetryAttribute.dbNamespace(schema)))*) *>
          socket.send(ComInitDBPacket(schema)) *>
          socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
            case error: ERRPacket =>
              val ex = error.toException(s"Failed to change schema to '$schema'")
              span.recordException(ex, error.attributes*) *> ev.raiseError(ex)
            case ok: OKPacket => ev.unit
          }
      }

    override def comStatistics(): F[StatisticsPacket] =
      exchange[F, StatisticsPacket](TelemetrySpanName.COMMAND_STATISTICS) { (span: Span[F]) =>
        span.addAttributes(attributes*) *>
          socket.send(ComStatisticsPacket()) *>
          socket.receive(StatisticsPacket.decoder)
      }

    override def comPing(): F[Boolean] =
      exchange[F, Boolean](TelemetrySpanName.PING) { (span: Span[F]) =>
        span.addAttributes(attributes*) *>
          socket.send(ComPingPacket()) *>
          socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
            case error: ERRPacket => span.recordException(error.toException, error.attributes*) *> ev.pure(false)
            case ok: OKPacket     => ev.pure(true)
          }
      }

    override def comResetConnection(): F[Unit] =
      exchange[F, Unit](TelemetrySpanName.CONNECTION_RESET) { (span: Span[F]) =>
        span.addAttributes(attributes*) *>
          socket.send(ComResetConnectionPacket()) *>
          socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
            case error: ERRPacket =>
              val ex = error.toException("Failed to execute reset connection")
              span.recordException(ex, error.attributes*) *> ev.raiseError(ex)
            case ok: OKPacket => ev.unit
          }
      }

    override def comSetOption(optionOperation: EnumMySQLSetOption): F[Unit] =
      exchange[F, Unit](TelemetrySpanName.SET_OPTION_MULTI_STATEMENTS(optionOperation.code)) { (span: Span[F]) =>
        span.addAttributes(attributes*) *>
          socket.send(ComSetOptionPacket(optionOperation)) *>
          socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
            case error: ERRPacket =>
              val ex = error.toException("Failed to execute set option")
              span.recordException(ex, error.attributes*) *> ev.raiseError(ex)
            case eof: EOFPacket => ev.unit
            case ok: OKPacket   => ev.unit
          }
      }

    override def resetSequenceId: F[Unit] =
      sequenceIdRef.update(_ => 0.toByte)

    override def resetConnection: F[Unit] = resetSequenceId *> comResetConnection()

    override def setOption(optionOperation: EnumMySQLSetOption): F[Unit] =
      resetSequenceId *> comSetOption(optionOperation)

    override def repeatProcess[P <: ResponsePacket](times: Int, decoder: Decoder[P]): F[Vector[P]] =
      val builder = Vector.newBuilder[P]

      def read(remaining: Int): F[Vector[P]] =
        if remaining <= 0 then ev.pure(builder.result())
        else
          socket.receive(decoder).flatMap { result =>
            builder += result
            read(remaining - 1)
          }

      read(times)

    override def readUntilEOF[P <: ResponsePacket](decoder: Decoder[P | EOFPacket | ERRPacket]): F[Vector[P]] =
      val builder = Vector.newBuilder[P]
      def loop: F[Vector[P]] =
        socket.receive(decoder).flatMap {
          case _: EOFPacket     => ev.pure(builder.result())
          case error: ERRPacket =>
            ev.raiseError(error.toException("Error during database operation"))
          case row =>
            builder += row.asInstanceOf[P]
            loop
        }

      loop

    override def serverVariables(): F[Map[String, String]] =
      resetSequenceId *>
        send(ComQueryPacket(SELECT_SERVER_VARIABLES_QUERY, initialPacket.capabilityFlags, ListMap.empty)) *>
        receive(ColumnsNumberPacket.decoder(initialPacket.capabilityFlags)).flatMap {
          case _: OKPacket      => ev.pure(Map.empty)
          case error: ERRPacket =>
            ev.raiseError(error.toException(Some(SELECT_SERVER_VARIABLES_QUERY), None))
          case result: ColumnsNumberPacket =>
            for
              columnDefinitions <-
                repeatProcess(
                  result.size,
                  ColumnDefinitionPacket.decoder(initialPacket.capabilityFlags)
                )
              resultSetRow <- readUntilEOF[ResultSetRowPacket](
                                ResultSetRowPacket.decoder(initialPacket.capabilityFlags, columnDefinitions.length)
                              )
            yield columnDefinitions
              .zip(resultSetRow.flatMap(_.values))
              .map {
                case (columnDefinition, resultSetRow) => columnDefinition.name -> resultSetRow.getOrElse("")
              }
              .toMap
        }

    /**
     * Read until the authentication is OK.
     * If an error is returned from the server, it throws an exception and exits.
     *
     * @param plugin
     * Authentication plugin
     */
    private def readUntilOk(
      plugin:       AuthenticationPlugin[F],
      password:     String,
      scrambleBuff: Option[Array[Byte]] = None
    ): F[Unit] =
      socket.receive(AuthenticationPacket.decoder(initialPacket.capabilityFlags)).flatMap {
        case more: AuthMoreDataPacket
          if (allowPublicKeyRetrieval || useSSL) && more.authenticationMethodData
            .mkString("") == Authentication.FULL_AUTH =>
          plugin match
            case plugin: CachingSha2PasswordPlugin[F] =>
              cachingSha2Authentication(
                plugin,
                password,
                scrambleBuff.getOrElse(initialPacket.scrambleBuff)
              ) *> readUntilOk(
                plugin,
                password
              )
            case plugin: Sha256PasswordPlugin[F] =>
              sha256Authentication(
                plugin,
                password,
                scrambleBuff.getOrElse(initialPacket.scrambleBuff)
              ) *> readUntilOk(plugin, password)
            case unknown =>
              ev.raiseError(
                new SQLInvalidAuthorizationSpecException(
                  s"Unexpected authentication method: $unknown",
                  detail = Some(
                    "This error may be due to lack of support on the ldbc side or a newly added plugin on the MySQL side."
                  ),
                  hint = Some(
                    "Report Issues here: https://github.com/takapi327/ldbc/issues/new?assignees=&labels=&projects=&template=feature_request.md&title="
                  )
                )
              )
        case more: AuthMoreDataPacket        => readUntilOk(plugin, password)
        case packet: AuthSwitchRequestPacket => changeAuthenticationMethod(packet, password)
        case _: OKPacket                     => ev.unit
        case error: ERRPacket                =>
          ev.raiseError(
            error.toException(
              s"Check that the ${ hostInfo.host }:${ hostInfo.port } server is running or that the authentication information, etc. used for the connection is correct."
            )
          )
        case unknown: UnknownPacket => ev.raiseError(unknown.toException("Error during database operation"))
        case unknown                =>
          ev.raiseError(
            new SQLInvalidAuthorizationSpecException(
              "Unexpected packets processed",
              detail = Some(
                "This error may be due to a lack of support on the ldbc side or a change in behaviour on the MySQL side."
              ),
              hint = Some(
                "Report Issues here: https://github.com/takapi327/ldbc/issues/new?assignees=&labels=&projects=&template=bug_report.md&title="
              )
            )
          )
      }

    /**
     * If authentication method mismatch happens,
     * server sends to client the Protocol::AuthSwitchRequest: which contains the name of the client authentication method to be used and the first authentication payload generated by the new method.
     * Client should switch to the requested authentication method and continue the exchange as dictated by that method.
     *
     * @param switchRequestPacket
     * Authentication method Switch Request Packet
     */
    private def changeAuthenticationMethod(switchRequestPacket: AuthSwitchRequestPacket, password: String): F[Unit] =
      determinatePlugin(switchRequestPacket.pluginName) match
        case Left(error)                                 => ev.raiseError(error) *> socket.send(ComQuitPacket())
        case Right(plugin: CachingSha2PasswordPlugin[F]) =>
          for
            hashedPassword <- plugin.hashPassword(password, switchRequestPacket.pluginProvidedData)
            _              <- socket.send(AuthSwitchResponsePacket(hashedPassword))
            _              <- readUntilOk(
                   plugin,
                   password,
                   Some(switchRequestPacket.pluginProvidedData)
                 )
          yield ()
        case Right(plugin: Sha256PasswordPlugin[F]) =>
          sha256Authentication(plugin, password, switchRequestPacket.pluginProvidedData) *> readUntilOk(
            plugin,
            password
          )
        case Right(plugin) =>
          for
            hashedPassword <- plugin.hashPassword(password, switchRequestPacket.pluginProvidedData)
            _              <- socket.send(AuthSwitchResponsePacket(hashedPassword))
            _              <- readUntilOk(
                   plugin,
                   password,
                   Some(switchRequestPacket.pluginProvidedData)
                 )
          yield ()

    /**
     * Plain text handshake
     *
     * @param plugin
     * Authentication plugin
     * @param scrambleBuff
     * Scramble buffer for authentication payload
     */
    private def plainTextHandshake(
      plugin:       AuthenticationPlugin[F],
      password:     String,
      scrambleBuff: Array[Byte]
    ): F[Unit] =
      plugin
        .hashPassword(password, scrambleBuff)
        .flatMap(hashedPassword => socket.send(AuthSwitchResponsePacket(hashedPassword)))

    /**
     * SSL handshake.
     * Send a plain password to use SSL/TLS encrypted secure communication.
     */
    private def sslHandshake(password: String): F[Unit] =
      socket.send(AuthSwitchResponsePacket.unsafeFromBytes((password + "\u0000").getBytes(StandardCharsets.UTF_8)))

    /**
     * Allow public key retrieval request.
     * RSA-encrypted communication, where the public key is used to encrypt the password for communication.
     *
     * @param plugin
     * Authentication plugin
     * @param scrambleBuff
     * Scramble buffer for authentication payload
     */
    private def allowPublicKeyRetrievalRequest(
      plugin:       EncryptPasswordPlugin,
      password:     String,
      scrambleBuff: Array[Byte]
    ): F[Unit] =
      socket.receive(AuthMoreDataPacket.decoder).flatMap { moreData =>
        // TODO: When converted to Array[Byte], it contains an extra 1 for some reason. This causes an error in public key parsing when executing Scala JS. Therefore, the first 1Byte is excluded.
        val publicKeyString = moreData.authenticationMethodData
          .drop(1)
          .map("%02x" format _)
          .map(hex => Integer.parseInt(hex, 16).toChar)
          .mkString("")
        val encryptPassword =
          plugin.encryptPassword(password, scrambleBuff, publicKeyString)
        socket.send(AuthSwitchResponsePacket.unsafeFromBytes(encryptPassword))
      }

    /**
     * SHA-256 authentication
     *
     * @param plugin
     * Authentication plugin
     * @param scrambleBuff
     * Scramble buffer for authentication payload
     */
    private def sha256Authentication(
      plugin:       Sha256PasswordPlugin[F],
      password:     String,
      scrambleBuff: Array[Byte]
    ): F[Unit] =
      (useSSL, allowPublicKeyRetrieval) match
        case (true, _)     => sslHandshake(password)
        case (false, true) =>
          socket.send(ComQuitPacket()) *> allowPublicKeyRetrievalRequest(plugin, password, scrambleBuff)
        case (_, _) => plainTextHandshake(plugin, password, scrambleBuff)

    /**
     * Caching SHA-2 authentication
     *
     * @param plugin
     * Authentication plugin
     * @param scrambleBuff
     * Scramble buffer for authentication payload
     */
    private def cachingSha2Authentication(
      plugin:       CachingSha2PasswordPlugin[F],
      password:     String,
      scrambleBuff: Array[Byte]
    ): F[Unit] =
      (useSSL, allowPublicKeyRetrieval) match
        case (true, _)     => sslHandshake(password)
        case (false, true) =>
          socket.send(ComInitDBPacket("")) *> allowPublicKeyRetrievalRequest(plugin, password, scrambleBuff)
        case (_, _) => plainTextHandshake(plugin, password, scrambleBuff)

    /**
     * Handshake with the server.
     *
     * @param plugin
     * Authentication plugin
     */
    private def handshake(plugin: AuthenticationPlugin[F], username: String, password: String): F[Unit] =
      for
        hashedPassword <- plugin.hashPassword(password, initialPacket.scrambleBuff)
        handshakeResponse = HandshakeResponsePacket(
                              capabilityFlags,
                              username,
                              hashedPassword.length.toByte +: hashedPassword.toArray,
                              plugin.name.toString,
                              initialPacket.characterSet,
                              hostInfo.database
                            )
        _ <- socket.send(handshakeResponse)
      yield ()

    override def startAuthentication(username: String, password: String): F[Unit] =
      exchange[F, Unit](TelemetrySpanName.CONNECTION_CREATE) { (span: Span[F]) =>
        span.addAttributes(
          (attributes ++ List(
            TelemetryAttribute.dbMysqlAuthPlugin(initialPacket.authPlugin),
            Attribute("username", username)
          ))*
        ) *> (
          defaultAuthenticationPlugin match
            case Some(plugin) =>
              checkRequiresConfidentiality(plugin, span) *> handshake(plugin, username, password) *> readUntilOk(
                plugin,
                password
              )
            case None =>
              determinatePlugin(initialPacket.authPlugin) match
                case Left(error) => span.recordException(error) *> ev.raiseError(error) *> socket.send(ComQuitPacket())
                case Right(plugin) =>
                  checkRequiresConfidentiality(plugin, span) *> handshake(plugin, username, password) *> readUntilOk(
                    plugin,
                    password
                  )
        )
      }

    override def changeUser(user: String, password: String): F[Unit] =
      exchange[F, Unit](TelemetrySpanName.CHANGE_USER) { (span: Span[F]) =>
        span.addAttributes(attributes*) *> (
          determinatePlugin(initialPacket.authPlugin) match
            case Left(error)   => span.recordException(error) *> ev.raiseError(error) *> socket.send(ComQuitPacket())
            case Right(plugin) =>
              for
                hashedPassword <- plugin.hashPassword(password, initialPacket.scrambleBuff)
                _              <- socket.send(
                       ComChangeUserPacket(
                         capabilityFlags,
                         user,
                         hostInfo.database,
                         initialPacket.characterSet,
                         initialPacket.authPlugin,
                         hashedPassword
                       )
                     )
                _ <- readUntilOk(plugin, password)
              yield ()
        )
      }

    private def checkRequiresConfidentiality(plugin: AuthenticationPlugin[F], span: Span[F]): F[Unit] =
      if plugin.requiresConfidentiality && !useSSL then
        val error = new SQLInvalidAuthorizationSpecException(
          s"SSL connection required for plugin “${ plugin.name }”. Check if ‘ssl’ is enabled.",
          hint = Some(
            """// You can enable SSL.
              |           MySQLDataSource.build[IO](....).setSSL(SSL.Trusted)
              |""".stripMargin
          )
        )
        span.recordException(error) *> ev.raiseError(error)
      else ev.unit

    private def determinatePlugin(pluginName: String): Either[SQLException, AuthenticationPlugin[F]] =
      plugins.get(pluginName).toRight(
        new SQLInvalidAuthorizationSpecException(
          s"Unknown authentication plugin: $pluginName",
          detail = Some(
            "This error may be due to lack of support on the ldbc side or a newly added plugin on the MySQL side."
          ),
          hint = Some(
            "Report Issues here: https://github.com/takapi327/ldbc/issues/new?assignees=&labels=&projects=&template=feature_request.md&title="
          )
        )
      )

  def apply[F[_]: Async: Console: Tracer: Exchange: Hashing](
    sockets:                     Resource[F, Socket[F]],
    hostInfo:                    HostInfo,
    debug:                       Boolean,
    sslOptions:                  Option[SSLNegotiation.Options[F]],
    allowPublicKeyRetrieval:     Boolean = false,
    readTimeout:                 Duration,
    capabilitiesFlags:           Set[CapabilitiesFlags],
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[F]],
    plugins: Map[String, AuthenticationPlugin[F]]
  ): Resource[F, Protocol[F]] =
    for
      sequenceIdRef    <- Resource.eval(Ref[F].of[Byte](0x01))
      initialPacketRef <- Resource.eval(Ref[F].of[Option[InitialPacket]](None))
      packetSocket     <-
        PacketSocket[F](debug, sockets, sslOptions, sequenceIdRef, initialPacketRef, readTimeout, capabilitiesFlags)
      protocol <- Resource.eval(
                    fromPacketSocket(
                      packetSocket,
                      hostInfo,
                      sslOptions,
                      allowPublicKeyRetrieval,
                      capabilitiesFlags,
                      sequenceIdRef,
                      initialPacketRef,
                      defaultAuthenticationPlugin,
                      plugins
                    )
                  )
    yield protocol

  def fromPacketSocket[F[_]: Tracer: Exchange: Hashing](
    packetSocket:                PacketSocket[F],
    hostInfo:                    HostInfo,
    sslOptions:                  Option[SSLNegotiation.Options[F]],
    allowPublicKeyRetrieval:     Boolean = false,
    capabilitiesFlags:           Set[CapabilitiesFlags],
    sequenceIdRef:               Ref[F, Byte],
    initialPacketRef:            Ref[F, Option[InitialPacket]],
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[F]],
    plugins: Map[String, AuthenticationPlugin[F]]
  )(using ev: Async[F]): F[Protocol[F]] =
    initialPacketRef.get.flatMap {
      case Some(initialPacket) =>
        ev.pure(
          Impl(
            initialPacket,
            hostInfo,
            packetSocket,
            sslOptions.isDefined,
            allowPublicKeyRetrieval,
            capabilitiesFlags,
            sequenceIdRef,
            defaultAuthenticationPlugin,
            Map(
              MYSQL_NATIVE_PASSWORD.toString -> MysqlNativePasswordPlugin[F](),
              SHA256_PASSWORD.toString -> Sha256PasswordPlugin[F](),
              CACHING_SHA2_PASSWORD.toString -> CachingSha2PasswordPlugin[F](initialPacket.serverVersion)
            ) ++ plugins
          )
        )
      case None =>
        ev.raiseError(
          new SQLException(
            "Initial packet is not set",
            detail = Some(
              "This error may be due to a lack of support on the ldbc side or a change in behaviour on the MySQL side."
            ),
            hint = Some(
              "Report Issues here: https://github.com/takapi327/ldbc/issues/new?assignees=&labels=&projects=&template=bug_report.md&title="
            )
          )
        )
    }
