/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import java.nio.charset.StandardCharsets

import scala.collection.immutable.ListMap
import scala.concurrent.duration.*

import scodec.Decoder

import ldbc.sql.{ SQLException, SQLInvalidAuthorizationSpecException }
import ldbc.sql.Attribute

import ldbc.authentication.plugin.*
import ldbc.fx.{ Fx, Ref, Resource }
import ldbc.fx.syntax.*
import ldbc.mysql.authenticator.{ CachingSha2PasswordPlugin, MysqlNativePasswordPlugin, Sha256PasswordPlugin }
import ldbc.mysql.data.*
import ldbc.mysql.net.packet.*
import ldbc.mysql.net.packet.request.*
import ldbc.mysql.net.packet.response.*
import ldbc.mysql.net.protocol.*
import ldbc.mysql.telemetry.*
import ldbc.mysql.telemetry.{ DbAttributes, ServerAttributes }
import ldbc.mysql.telemetry.{ Span, StatusCode, Tracer }

/**
 * Protocol is a protocol to communicate with MySQL server.
 * It provides a way to authenticate, reset sequence id, and close the connection.
 *
 * @tparam F
 *   the effect type
 */
trait Protocol extends UtilityCommands, Authentication:

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
  def receive[P <: ResponsePacket](decoder: Decoder[P]): Fx[P]

  /** Send the specified request packet. */
  def send(request: RequestPacket): Fx[Unit]

  /**
   * Resets the sequence id.
   */
  def resetSequenceId: Fx[Unit]

  /**
   * Resets the connection.
   */
  def resetConnection: Fx[Unit]

  /**
   * Controls whether or not multiple SQL statements are allowed to be executed at once.
   *
   * NOTE: It can only be used for batch processing with Insert, Update, and Delete statements.
   *
   * @param optionOperation
   * [[EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON]] or [[EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF]]
   */
  def setOption(optionOperation: EnumMySQLSetOption): Fx[Unit]

  /**
   * Enables multiple SQL statements to be executed at once.
   *
   * NOTE: It can only be used for batch processing with Insert, Update, and Delete statements.
   */
  def enableMultiQueries: Fx[Unit] = setOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON)

  /**
   * Disables multiple SQL statements to be executed at once.
   *
   * NOTE: It can only be used for batch processing with Insert, Update, and Delete statements.
   */
  def disableMultiQueries: Fx[Unit] = setOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF)

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
  def repeatProcess[P <: ResponsePacket](times: Int, decoder: Decoder[P]): Fx[Vector[P]]

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
  def readUntilEOF[P <: ResponsePacket](decoder: Decoder[P | EOFPacket | ERRPacket]): Fx[Vector[P]]

  /**
   * Returns the server variables.
   */
  def serverVariables(): Fx[Map[String, String]]

object Protocol:

  private val SELECT_SERVER_VARIABLES_QUERY =
    "SELECT @@session.auto_increment_increment AS auto_increment_increment, @@character_set_client AS character_set_client, @@character_set_connection AS character_set_connection, @@character_set_results AS character_set_results, @@character_set_server AS character_set_server, @@collation_server AS collation_server, @@collation_connection AS collation_connection, @@init_connect AS init_connect, @@interactive_timeout AS interactive_timeout, @@license AS license, @@lower_case_table_names AS lower_case_table_names, @@max_allowed_packet AS max_allowed_packet, @@net_write_timeout AS net_write_timeout, @@performance_schema AS performance_schema, @@sql_mode AS sql_mode, @@system_time_zone AS system_time_zone, @@time_zone AS time_zone, @@transaction_isolation AS transaction_isolation, @@wait_timeout AS wait_timeout"

  private[ldbc] case class Impl(
    initialPacket:               InitialPacket,
    hostInfo:                    HostInfo,
    socket:                      PacketSocket,
    useSSL:                      Boolean = false,
    allowPublicKeyRetrieval:     Boolean = false,
    capabilityFlags:             Set[CapabilitiesFlags],
    sequenceIdRef:               Ref[Byte],
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[Fx]],
    plugins:                     Map[String, AuthenticationPlugin[Fx]]
  )(using tracer: Tracer, ex: Exchange)
    extends Protocol:

    private val attributes = List(
      DbAttributes.DbSystemName(DbAttributes.DbSystemNameValue.Mysql.value),
      ServerAttributes.ServerAddress(hostInfo.host),
      ServerAttributes.ServerPort(hostInfo.port.toLong),
      TelemetryAttribute.dbMysqlVersion(initialPacket.serverVersion.toString),
      TelemetryAttribute.dbMysqlThreadId(initialPacket.threadId)
    ) ++ hostInfo.database
      .map(db => DbAttributes.DbNamespace(db))
      .toList

    override def receive[P <: ResponsePacket](decoder: Decoder[P]): Fx[P] = socket.receive(decoder)

    override def send(request: RequestPacket): Fx[Unit] = socket.send(request)

    override def comQuit(): Fx[Unit] =
      exchange[Unit](TelemetrySpanName.CONNECTION_CLOSE) { (span: Span) =>
        span.addAttributes(attributes*) *> socket.send(ComQuitPacket())
      }

    override def comInitDB(schema: String): Fx[Unit] =
      exchange[Unit](TelemetrySpanName.CHANGE_DATABASE) { (span: Span) =>
        span.addAttributes((attributes ++ List(DbAttributes.DbNamespace(schema)))*) *>
          socket.send(ComInitDBPacket(schema)) *>
          socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
            case error: ERRPacket =>
              val ex = error.toException(s"Failed to change schema to '$schema'")
              span.recordException(ex, error.attributes*) *>
                span.setStatus(StatusCode.Error, ex.getMessage) *>
                Fx.raiseError(ex)
            case ok: OKPacket => Fx.unit
          }
      }

    override def comStatistics(): Fx[StatisticsPacket] =
      exchange[StatisticsPacket](TelemetrySpanName.COMMAND_STATISTICS) { (span: Span) =>
        span.addAttributes(attributes*) *>
          socket.send(ComStatisticsPacket()) *>
          socket.receive(StatisticsPacket.decoder)
      }

    override def comPing(): Fx[Boolean] =
      exchange[Boolean](TelemetrySpanName.PING) { (span: Span) =>
        span.addAttributes(attributes*) *>
          socket.send(ComPingPacket()) *>
          socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
            case error: ERRPacket =>
              val ex = error.toException
              span.recordException(ex, error.attributes*) *>
                span.setStatus(StatusCode.Error, ex.getMessage) *>
                Fx.pure(false)
            case ok: OKPacket => Fx.pure(true)
          }
      }

    override def comResetConnection(): Fx[Unit] =
      exchange[Unit](TelemetrySpanName.CONNECTION_RESET) { (span: Span) =>
        span.addAttributes(attributes*) *>
          socket.send(ComResetConnectionPacket()) *>
          socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
            case error: ERRPacket =>
              val ex = error.toException("Failed to execute reset connection")
              span.recordException(ex, error.attributes*) *>
                span.setStatus(StatusCode.Error, ex.getMessage) *>
                Fx.raiseError(ex)
            case ok: OKPacket => Fx.unit
          }
      }

    override def comSetOption(optionOperation: EnumMySQLSetOption): Fx[Unit] =
      exchange[Unit](TelemetrySpanName.SET_OPTION_MULTI_STATEMENTS(optionOperation.code)) { (span: Span) =>
        span.addAttributes(attributes*) *>
          socket.send(ComSetOptionPacket(optionOperation)) *>
          socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
            case error: ERRPacket =>
              val ex = error.toException("Failed to execute set option")
              span.recordException(ex, error.attributes*) *>
                span.setStatus(StatusCode.Error, ex.getMessage) *>
                Fx.raiseError(ex)
            case eof: EOFPacket => Fx.unit
            case ok: OKPacket   => Fx.unit
          }
      }

    override def resetSequenceId: Fx[Unit] =
      sequenceIdRef.update(_ => 0.toByte)

    override def resetConnection: Fx[Unit] = resetSequenceId *> comResetConnection()

    override def setOption(optionOperation: EnumMySQLSetOption): Fx[Unit] =
      resetSequenceId *> comSetOption(optionOperation)

    override def repeatProcess[P <: ResponsePacket](times: Int, decoder: Decoder[P]): Fx[Vector[P]] =
      val builder = Vector.newBuilder[P]

      def read(remaining: Int): Fx[Vector[P]] =
        if remaining <= 0 then Fx.pure(builder.result())
        else
          socket.receive(decoder).flatMap { result =>
            builder += result
            read(remaining - 1)
          }

      read(times)

    override def readUntilEOF[P <: ResponsePacket](decoder: Decoder[P | EOFPacket | ERRPacket]): Fx[Vector[P]] =
      val builder = Vector.newBuilder[P]
      def loop: Fx[Vector[P]] =
        socket.receive(decoder).flatMap {
          case _: EOFPacket     => Fx.pure(builder.result())
          case error: ERRPacket =>
            Fx.raiseError(error.toException("Error during database operation"))
          case row =>
            builder += row.asInstanceOf[P]
            loop
        }

      loop

    override def serverVariables(): Fx[Map[String, String]] =
      resetSequenceId *>
        send(ComQueryPacket(SELECT_SERVER_VARIABLES_QUERY, initialPacket.capabilityFlags, ListMap.empty)) *>
        receive(ColumnsNumberPacket.decoder(initialPacket.capabilityFlags)).flatMap {
          case _: OKPacket      => Fx.pure(Map.empty)
          case error: ERRPacket =>
            Fx.raiseError(error.toException(Some(SELECT_SERVER_VARIABLES_QUERY), None))
          case result: ColumnsNumberPacket =>
            for
              columnDefinitions <-
                repeatProcess(
                  result.size,
                  ColumnDefinitionPacket.decoder(initialPacket.capabilityFlags)
                )
              resultSetRow <- readUntilEOF[ResultSetRowPacket](
                                textResultSetRowDecoder(initialPacket.capabilityFlags)
                              )
            yield columnDefinitions.zipWithIndex.flatMap {
              case (col, i) =>
                resultSetRow.headOption.map { row =>
                  val fieldBytes = TextColumnValueDecoder.extractColumn(row.rawBytes, i, Vector.empty)
                  col.name -> fieldBytes.map(b => new String(b, col.charset)).getOrElse("")
                }
            }.toMap
        }

    /**
     * Read until the authentication is OK.
     * If an error is returned from the server, it throws an exception and exits.
     *
     * @param plugin
     * Authentication plugin
     */
    private def readUntilOk(
      plugin:       AuthenticationPlugin[Fx],
      password:     String,
      span:         Span,
      scrambleBuff: Option[Array[Byte]] = None
    ): Fx[Unit] =
      socket.receive(AuthenticationPacket.decoder(initialPacket.capabilityFlags)).flatMap {
        case more: AuthMoreDataPacket
          if (allowPublicKeyRetrieval || useSSL) && more.authenticationMethodData
            .mkString("") == Authentication.FULL_AUTH =>
          plugin match
            case plugin: CachingSha2PasswordPlugin =>
              cachingSha2Authentication(
                plugin,
                password,
                scrambleBuff.getOrElse(initialPacket.scrambleBuff)
              ) *> readUntilOk(
                plugin,
                password,
                span
              )
            case plugin: Sha256PasswordPlugin =>
              sha256Authentication(
                plugin,
                password,
                scrambleBuff.getOrElse(initialPacket.scrambleBuff)
              ) *> readUntilOk(plugin, password, span)
            case unknown =>
              Fx.raiseError(
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
        case more: AuthMoreDataPacket        => readUntilOk(plugin, password, span)
        case packet: AuthSwitchRequestPacket => changeAuthenticationMethod(packet, password, span)
        case _: OKPacket                     => Fx.unit
        case error: ERRPacket                =>
          Fx.raiseError(
            error.toException(
              s"Check that the ${ hostInfo.host }:${ hostInfo.port } server is running or that the authentication information, etc. used for the connection is correct."
            )
          )
        case unknown: UnknownPacket => Fx.raiseError(unknown.toException("Error during database operation"))
        case unknown                =>
          Fx.raiseError(
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
    private def changeAuthenticationMethod(
      switchRequestPacket: AuthSwitchRequestPacket,
      password:            String,
      span:                Span
    ): Fx[Unit] =
      resolveAuthPlugin(switchRequestPacket.pluginName, span).flatMap {
        case plugin: CachingSha2PasswordPlugin =>
          for
            hashedPassword <- plugin.hashPassword(password, switchRequestPacket.pluginProvidedData)
            _              <- socket.send(AuthSwitchResponsePacket(hashedPassword))
            _              <- readUntilOk(
                   plugin,
                   password,
                   span,
                   Some(switchRequestPacket.pluginProvidedData)
                 )
          yield ()
        case plugin: Sha256PasswordPlugin =>
          sha256Authentication(plugin, password, switchRequestPacket.pluginProvidedData) *> readUntilOk(
            plugin,
            password,
            span
          )
        case plugin =>
          for
            hashedPassword <- plugin.hashPassword(password, switchRequestPacket.pluginProvidedData)
            _              <- socket.send(AuthSwitchResponsePacket(hashedPassword))
            _              <- readUntilOk(
                   plugin,
                   password,
                   span,
                   Some(switchRequestPacket.pluginProvidedData)
                 )
          yield ()
      }

    /**
     * Plain text handshake
     *
     * @param plugin
     * Authentication plugin
     * @param scrambleBuff
     * Scramble buffer for authentication payload
     */
    private def plainTextHandshake(
      plugin:       AuthenticationPlugin[Fx],
      password:     String,
      scrambleBuff: Array[Byte]
    ): Fx[Unit] =
      plugin
        .hashPassword(password, scrambleBuff)
        .flatMap(hashedPassword => socket.send(AuthSwitchResponsePacket(hashedPassword)))

    /**
     * SSL handshake.
     * Send a plain password to use SSL/TLS encrypted secure communication.
     */
    private def sslHandshake(password: String): Fx[Unit] =
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
    ): Fx[Unit] =
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
      plugin:       Sha256PasswordPlugin,
      password:     String,
      scrambleBuff: Array[Byte]
    ): Fx[Unit] =
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
      plugin:       CachingSha2PasswordPlugin,
      password:     String,
      scrambleBuff: Array[Byte]
    ): Fx[Unit] =
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
    private def handshake(plugin: AuthenticationPlugin[Fx], username: String, password: String): Fx[Unit] =
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

    override def startAuthentication(username: String, password: String): Fx[Unit] =
      exchange[Unit](TelemetrySpanName.CONNECTION_CREATE) { (span: Span) =>
        val resolvedPlugin: Fx[AuthenticationPlugin[Fx]] =
          defaultAuthenticationPlugin match
            case Some(plugin) => checkRequiresConfidentiality(plugin, span).as(plugin)
            case None         => resolveAuthPlugin(initialPacket.authPlugin, span)
        span.addAttributes(
          (attributes ++ List(
            TelemetryAttribute.dbMysqlAuthPlugin(initialPacket.authPlugin),
            Attribute("username", username)
          ))*
        ) *> resolvedPlugin.flatMap(plugin =>
          handshake(plugin, username, password) *> readUntilOk(plugin, password, span)
        )
      }

    override def changeUser(user: String, password: String): Fx[Unit] =
      exchange[Unit](TelemetrySpanName.CHANGE_USER) { (span: Span) =>
        span.addAttributes(attributes*) *> resolveAuthPlugin(initialPacket.authPlugin, span).flatMap(plugin =>
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
            _ <- readUntilOk(plugin, password, span)
          yield ()
        )
      }

    /**
     * Resolves the authentication plugin the server asked for by name and immediately enforces the
     * confidentiality guard, before any authentication response is generated. This runs on every
     * plugin (re)selection — initial handshake, Change User, and a server-driven AuthSwitchRequest —
     * so a plugin that transmits credentials in cleartext can never be used over a non-SSL
     * connection. Fails closed (sends ComQuit, then raises) on an unknown plugin.
     */
    private def resolveAuthPlugin(pluginName: String, span: Span): Fx[AuthenticationPlugin[Fx]] =
      determinatePlugin(pluginName) match
        case Left(error) =>
          span.recordException(error) *>
            span.setStatus(StatusCode.Error, error.getMessage) *>
            socket.send(ComQuitPacket()) *>
            Fx.raiseError(error)
        case Right(plugin) =>
          checkRequiresConfidentiality(plugin, span).as(plugin)

    private def checkRequiresConfidentiality(plugin: AuthenticationPlugin[Fx], span: Span): Fx[Unit] =
      if plugin.requiresConfidentiality && !useSSL then
        val error = new SQLInvalidAuthorizationSpecException(
          s"SSL connection required for plugin '${ plugin.name }'. Check if 'ssl' is enabled.",
          hint = Some(
            """// You can enable SSL. Use SSL.System to verify the server certificate;
              |// SSL.Trusted (trust-all) is for development / self-signed certificates only.
              |           MySQLDataSource.build[IO](....).setSSL(SSL.System)
              |""".stripMargin
          )
        )
        span.recordException(error) *>
          span.setStatus(StatusCode.Error, error.getMessage) *>
          Fx.raiseError(error)
      else Fx.unit

    private def determinatePlugin(pluginName: String): Either[ldbc.sql.SQLException, AuthenticationPlugin[Fx]] =
      plugins
        .get(pluginName)
        .toRight(
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

  def apply(
    sockets:                     Resource[ldbc.net.Socket],
    hostInfo:                    HostInfo,
    debug:                       Boolean,
    sslOptions:                  Option[SSLNegotiation.Options],
    allowPublicKeyRetrieval:     Boolean = false,
    readTimeout:                 Duration,
    capabilitiesFlags:           Set[CapabilitiesFlags],
    maxAllowedPacket:            Int,
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[Fx]],
    plugins:                     Map[String, AuthenticationPlugin[Fx]]
  )(using Tracer, Exchange): Resource[Protocol] =
    for
      sequenceIdRef    <- Resource.eval(Ref.of[Byte](0x01))
      initialPacketRef <- Resource.eval(Ref.of[Option[InitialPacket]](None))
      packetSocket     <-
        PacketSocket(
          debug,
          sockets,
          sslOptions,
          sequenceIdRef,
          initialPacketRef,
          readTimeout,
          capabilitiesFlags,
          maxAllowedPacket
        )
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

  def fromPacketSocket(
    packetSocket:                PacketSocket,
    hostInfo:                    HostInfo,
    sslOptions:                  Option[SSLNegotiation.Options],
    allowPublicKeyRetrieval:     Boolean = false,
    capabilitiesFlags:           Set[CapabilitiesFlags],
    sequenceIdRef:               Ref[Byte],
    initialPacketRef:            Ref[Option[InitialPacket]],
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[Fx]],
    plugins:                     Map[String, AuthenticationPlugin[Fx]]
  )(using Tracer, Exchange): Fx[Protocol] =
    initialPacketRef.get.flatMap {
      case Some(initialPacket) =>
        Fx.pure(
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
              MYSQL_NATIVE_PASSWORD.toString -> MysqlNativePasswordPlugin(),
              SHA256_PASSWORD.toString       -> Sha256PasswordPlugin(),
              CACHING_SHA2_PASSWORD.toString -> CachingSha2PasswordPlugin(initialPacket.serverVersion)
            ) ++ plugins
          )
        )
      case None =>
        Fx.raiseError(
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
