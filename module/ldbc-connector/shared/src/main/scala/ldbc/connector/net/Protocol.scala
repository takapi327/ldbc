/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import java.nio.charset.StandardCharsets

import scala.concurrent.duration.*
import scala.collection.immutable.ListMap

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import fs2.io.net.Socket

import scodec.Decoder

import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.{ Tracer, Span }

import ldbc.connector.data.*
import ldbc.connector.authenticator.*
import ldbc.connector.exception.*
import ldbc.connector.net.packet.*
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.protocol.*

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
   * @param acc
   *   the accumulator
   * @tparam P
   *   the type of the response packet
   * @return
   *   a vector of the response packets
   */
  def readUntilEOF[P <: ResponsePacket](decoder: Decoder[P | EOFPacket | ERRPacket], acc: Vector[P]): F[Vector[P]]

  /**
   * Returns the server variables.
   */
  def serverVariables(): F[Map[String, String]]

object Protocol:

  private val SELECT_SERVER_VARIABLES_QUERY =
    "SELECT @@session.auto_increment_increment AS auto_increment_increment, @@character_set_client AS character_set_client, @@character_set_connection AS character_set_connection, @@character_set_results AS character_set_results, @@character_set_server AS character_set_server, @@collation_server AS collation_server, @@collation_connection AS collation_connection, @@init_connect AS init_connect, @@interactive_timeout AS interactive_timeout, @@license AS license, @@lower_case_table_names AS lower_case_table_names, @@max_allowed_packet AS max_allowed_packet, @@net_write_timeout AS net_write_timeout, @@performance_schema AS performance_schema, @@sql_mode AS sql_mode, @@system_time_zone AS system_time_zone, @@time_zone AS time_zone, @@transaction_isolation AS transaction_isolation, @@wait_timeout AS wait_timeout"

  private[ldbc] case class Impl[F[_]: Temporal: Tracer](
    initialPacket:           InitialPacket,
    hostInfo:                HostInfo,
    socket:                  PacketSocket[F],
    useSSL:                  Boolean = false,
    allowPublicKeyRetrieval: Boolean = false,
    capabilityFlags:         List[CapabilitiesFlags],
    sequenceIdRef:           Ref[F, Byte]
  )(using ev: MonadError[F, Throwable], ex: Exchange[F])
    extends Protocol[F]:

    private val attributes = initialPacket.attributes ++ List(
      hostInfo.database.map(db => Attribute("database", db))
    ).flatten

    override def receive[P <: ResponsePacket](decoder: Decoder[P]): F[P] = socket.receive(decoder)

    override def send(request: RequestPacket): F[Unit] = socket.send(request)

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
        span.addAttributes(
          (attributes ++ List(Attribute("command", "COM_SET_OPTION"), Attribute("option", optionOperation.toString)))*
        ) *>
          socket.send(ComSetOptionPacket(optionOperation)) *>
          socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
            case error: ERRPacket => ev.raiseError(error.toException("Failed to execute set option"))
            case eof: EOFPacket   => ev.unit
            case ok: OKPacket     => ev.unit
          }
      }

    override def resetSequenceId: F[Unit] =
      sequenceIdRef.update(_ => 0.toByte)

    override def resetConnection: F[Unit] = resetSequenceId *> comResetConnection()

    override def setOption(optionOperation: EnumMySQLSetOption): F[Unit] =
      resetSequenceId *> comSetOption(optionOperation)

    override def repeatProcess[P <: ResponsePacket](times: Int, decoder: Decoder[P]): F[Vector[P]] =
      def read(remaining: Int, acc: Vector[P]): F[Vector[P]] =
        if remaining <= 0 then ev.pure(acc)
        else socket.receive(decoder).flatMap(result => read(remaining - 1, acc :+ result))

      read(times, Vector.empty[P])

    override def readUntilEOF[P <: ResponsePacket](
      decoder: Decoder[P | EOFPacket | ERRPacket],
      acc:     Vector[P]
    ): F[Vector[P]] =
      socket.receive(decoder).flatMap {
        case _: EOFPacket     => ev.pure(acc)
        case error: ERRPacket => ev.raiseError(error.toException)
        case row              => readUntilEOF(decoder, acc :+ row.asInstanceOf[P])
      }

    override def serverVariables(): F[Map[String, String]] =
      resetSequenceId *>
        send(ComQueryPacket(SELECT_SERVER_VARIABLES_QUERY, initialPacket.capabilityFlags, ListMap.empty)) *>
        receive(ColumnsNumberPacket.decoder(initialPacket.capabilityFlags)).flatMap {
          case _: OKPacket => ev.pure(Map.empty)
          case error: ERRPacket =>
            ev.raiseError(error.toException(Some(SELECT_SERVER_VARIABLES_QUERY), None))
          case result: ColumnsNumberPacket =>
            for
              columnDefinitions <-
                repeatProcess(
                  result.size,
                  ColumnDefinitionPacket.decoder(initialPacket.capabilityFlags)
                )
              resultSetRow <-
                readUntilEOF[ResultSetRowPacket](
                  ResultSetRowPacket.decoder(initialPacket.capabilityFlags, columnDefinitions),
                  Vector.empty
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
      plugin:       AuthenticationPlugin,
      password:     String,
      scrambleBuff: Option[Array[Byte]] = None
    ): F[Unit] =
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
                password
              )
            case plugin: Sha256PasswordPlugin =>
              sha256Authentication(
                plugin,
                password,
                scrambleBuff.getOrElse(initialPacket.scrambleBuff)
              ) *> readUntilOk(plugin, password)
            case unknown => ev.raiseError(new SQLInvalidAuthorizationSpecException(
              s"Unexpected authentication method: $unknown",
              detail = Some("This error may be due to lack of support on the ldbc side or a newly added plugin on the MySQL side."),
              hint = Some("Report Issues here: https://github.com/takapi327/ldbc/issues/new?assignees=&labels=&projects=&template=feature_request.md&title=")
            ))
        case more: AuthMoreDataPacket        => readUntilOk(plugin, password)
        case packet: AuthSwitchRequestPacket => changeAuthenticationMethod(packet, password)
        case _: OKPacket                     => ev.unit
        case error: ERRPacket                => ev.raiseError(error.toException(s"Check that the ${hostInfo.host}:${hostInfo.port} server is running or that the authentication information, etc. used for the connection is correct."))
        case unknown: UnknownPacket          => ev.raiseError(unknown.toException("Error during database operation"))
        case unknown => ev.raiseError(new SQLInvalidAuthorizationSpecException(
          "Unexpected packets processed",
          detail = Some("This error may be due to a lack of support on the ldbc side or a change in behaviour on the MySQL side."),
          hint = Some("Report Issues here: https://github.com/takapi327/ldbc/issues/new?assignees=&labels=&projects=&template=bug_report.md&title=")
        ))
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
          sha256Authentication(plugin, password, switchRequestPacket.pluginProvidedData) *> readUntilOk(
            plugin,
            password
          )
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
     * Authentication plugin
     * @param scrambleBuff
     * Scramble buffer for authentication payload
     */
    private def plainTextHandshake(
      plugin:       AuthenticationPlugin,
      password:     String,
      scrambleBuff: Array[Byte]
    ): F[Unit] =
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
     * Authentication plugin
     * @param scrambleBuff
     * Scramble buffer for authentication payload
     */
    private def allowPublicKeyRetrievalRequest(
      plugin:       Sha256PasswordPlugin,
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
        socket.send(AuthSwitchResponsePacket(encryptPassword))
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
    ): F[Unit] =
      (useSSL, allowPublicKeyRetrieval) match
        case (true, _) => sslHandshake(password)
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
    ): F[Unit] =
      (useSSL, allowPublicKeyRetrieval) match
        case (true, _) => sslHandshake(password)
        case (false, true) =>
          socket.send(ComInitDBPacket("")) *> allowPublicKeyRetrievalRequest(plugin, password, scrambleBuff)
        case (_, _) => plainTextHandshake(plugin, password, scrambleBuff)

    /**
     * Handshake with the server.
     *
     * @param plugin
     * Authentication plugin
     */
    private def handshake(plugin: AuthenticationPlugin, username: String, password: String): F[Unit] =
      val hashedPassword = plugin.hashPassword(password, initialPacket.scrambleBuff)
      val handshakeResponse = HandshakeResponsePacket(
        capabilityFlags,
        username,
        Array(hashedPassword.length.toByte) ++ hashedPassword,
        plugin.name,
        initialPacket.characterSet,
        hostInfo.database
      )
      socket.send(handshakeResponse)

    override def startAuthentication(username: String, password: String): F[Unit] =
      exchange[F, Unit]("database.authentication") { (span: Span[F]) =>
        span.addAttributes((attributes ++ List(Attribute("username", username)))*) *> (
          determinatePlugin(initialPacket.authPlugin, initialPacket.serverVersion) match
            case Left(error)   => ev.raiseError(error) *> socket.send(ComQuitPacket())
            case Right(plugin) => handshake(plugin, username, password) *> readUntilOk(plugin, password)
        )
      }

    override def changeUser(user: String, password: String): F[Unit] =
      exchange[F, Unit]("authentication.change.user") { (span: Span[F]) =>
        span.addAttributes(
          (
            attributes ++
              List(Attribute("type", "Utility Commands")) ++
              List(Attribute("command", "COM_CHANGE_USER"), Attribute("user", user))
          )*
        ) *> (
          determinatePlugin(initialPacket.authPlugin, initialPacket.serverVersion) match
            case Left(error) => ev.raiseError(error) *> socket.send(ComQuitPacket())
            case Right(plugin) =>
              socket.send(
                ComChangeUserPacket(
                  capabilityFlags,
                  user,
                  hostInfo.database,
                  initialPacket.characterSet,
                  initialPacket.authPlugin,
                  plugin.hashPassword(password, initialPacket.scrambleBuff)
                )
              ) *>
                readUntilOk(plugin, password)
        )
      }

  def apply[F[_]: Temporal: Console: Tracer: Exchange](
    sockets:                 Resource[F, Socket[F]],
    hostInfo:                HostInfo,
    debug:                   Boolean,
    sslOptions:              Option[SSLNegotiation.Options[F]],
    allowPublicKeyRetrieval: Boolean = false,
    readTimeout:             Duration,
    capabilitiesFlags:       List[CapabilitiesFlags]
  ): Resource[F, Protocol[F]] =
    for
      sequenceIdRef    <- Resource.eval(Ref[F].of[Byte](0x01))
      initialPacketRef <- Resource.eval(Ref[F].of[Option[InitialPacket]](None))
      packetSocket <-
        PacketSocket[F](debug, sockets, sslOptions, sequenceIdRef, initialPacketRef, readTimeout, capabilitiesFlags)
      protocol <- Resource.eval(
                    fromPacketSocket(
                      packetSocket,
                      hostInfo,
                      sslOptions,
                      allowPublicKeyRetrieval,
                      capabilitiesFlags,
                      sequenceIdRef,
                      initialPacketRef
                    )
                  )
    yield protocol

  def fromPacketSocket[F[_]: Temporal: Tracer: Exchange](
    packetSocket:            PacketSocket[F],
    hostInfo:                HostInfo,
    sslOptions:              Option[SSLNegotiation.Options[F]],
    allowPublicKeyRetrieval: Boolean = false,
    capabilitiesFlags:       List[CapabilitiesFlags],
    sequenceIdRef:           Ref[F, Byte],
    initialPacketRef:        Ref[F, Option[InitialPacket]]
  )(using ev: MonadError[F, Throwable]): F[Protocol[F]] =
    for initialPacketOpt <- initialPacketRef.get
    yield initialPacketOpt match
      case Some(initialPacket) =>
        Impl(
          initialPacket,
          hostInfo,
          packetSocket,
          sslOptions.isDefined,
          allowPublicKeyRetrieval,
          capabilitiesFlags,
          sequenceIdRef
        )
      case None => throw new SQLException(
        "Initial packet is not set",
        detail = Some("This error may be due to a lack of support on the ldbc side or a change in behaviour on the MySQL side."),
        hint = Some("Report Issues here: https://github.com/takapi327/ldbc/issues/new?assignees=&labels=&projects=&template=bug_report.md&title=")
      )
