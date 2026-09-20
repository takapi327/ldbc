/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import scodec.Decoder

import ldbc.sql.SQLTransientConnectionException

import ldbc.effect.Ref
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.data.{ BinaryColumnValueDecoder, CapabilitiesFlags, ServerStatusFlags }
import ldbc.mysql.net.{ HostInfo, PacketSocket, Protocol }
import ldbc.mysql.net.packet.{ RequestPacket, ResponsePacket }
import ldbc.mysql.net.packet.response.InitialPacket
import ldbc.mysql.net.protocol.Exchange
import ldbc.mysql.util.Version
import ldbc.telemetry.*

class TransportFailureSilenceTest extends FTestPlatform:
  given Tracer[Fx] = Tracer.noop[Fx]

  private final class RecordingSocket(sent: Ref[Fx, Vector[RequestPacket]], failed: Boolean) extends PacketSocket[Fx]:
    override def receive[P <: ResponsePacket](decoder: Decoder[P]): Fx[P] =
      Fx.raiseError(new java.io.IOException("connection reset"))
    override def send(request: RequestPacket): Fx[Unit]    = sent.update(_ :+ request)
    override def transportFailed:              Fx[Boolean] = Fx.pure(failed)

  private val initialPacket = InitialPacket(
    protocolVersion = 10,
    serverVersion   = Version(8, 4, 0),
    threadId        = 1,
    capabilityFlags = Set.empty[CapabilitiesFlags],
    characterSet    = 45,
    statusFlags     = Set.empty[ServerStatusFlags],
    scrambleBuff    = Array.fill[Byte](20)(1),
    authPlugin      = "mysql_native_password"
  )

  private val hostInfo = HostInfo("127.0.0.1", 3306, "user", Some("secret"), Some("db"))

  private def protocolWith(failed: Boolean): Fx[(Protocol[Fx], Ref[Fx, Vector[RequestPacket]])] =
    for
      sent               <- Ref.of[Fx, Vector[RequestPacket]](Vector.empty)
      given Exchange[Fx] <- Exchange.apply[Fx]
      sequenceIdRef      <- Ref.of[Fx, Byte](0x01)
      initialPacketRef   <- Ref.of[Fx, Option[InitialPacket]](Some(initialPacket))
      protocol           <- Protocol.fromPacketSocket[Fx](
                    packetSocket                = new RecordingSocket(sent, failed),
                    hostInfo                    = hostInfo,
                    sslOptions                  = None,
                    allowPublicKeyRetrieval     = false,
                    capabilitiesFlags           = Set.empty[CapabilitiesFlags],
                    sequenceIdRef               = sequenceIdRef,
                    initialPacketRef            = initialPacketRef,
                    defaultAuthenticationPlugin = None,
                    plugins                     = Map.empty
                  )
    yield (protocol, sent)

  private def connectionOn(protocol: Protocol[Fx]): Fx[ConnectionImpl[Fx]] =
    for
      given Exchange[Fx] <- Exchange.apply[Fx]
      readOnly           <- Ref.of[Fx, Boolean](false)
      autoCommit         <- Ref.of[Fx, Boolean](false)
      connectionClosed   <- Ref.of[Fx, Boolean](false)
    yield ConnectionImpl[Fx](
      protocol           = protocol,
      serverVariables    = Map.empty,
      database           = None,
      readOnly           = readOnly,
      isAutoCommit       = autoCommit,
      connectionClosed   = connectionClosed,
      useCursorFetch     = false,
      useServerPrepStmts = false,
      databaseMetrics    = DatabaseMetrics.noop[Fx]
    )

  private def streamingResultSetOn(protocol: Protocol[Fx]): Fx[StreamingResultSet[Fx]] =
    for
      isClosed  <- Ref.of[Fx, Boolean](false)
      fetchSize <- Ref.of[Fx, Int](10)
    yield StreamingResultSet[Fx](
      protocol           = protocol,
      statementId        = 1L,
      columns            = Vector.empty,
      records            = Vector.empty,
      serverVariables    = Map.empty,
      version            = Version(8, 4, 0),
      isClosed           = isClosed,
      fetchSize          = fetchSize,
      useCursorFetch     = true,
      useServerPrepStmts = true,
      decoder            = BinaryColumnValueDecoder
    )

  test("close() on a failed transport writes nothing"):
    val program = for
      pair       <- protocolWith(failed = true)
      connection <- connectionOn(pair._1)
      _          <- connection.close()
      written    <- pair._2.get
      closed     <- connection.isClosed()
    yield (written.size, closed)

    assertFx(program, (0, true), "close() on a failed connection still wrote to the network")

  test("StreamingResultSet.next() on a failed transport writes nothing"):
    val program = for
      pair      <- protocolWith(failed = true)
      resultSet <- streamingResultSetOn(pair._1)
      outcome   <- resultSet.next().attempt
      written   <- pair._2.get
    yield (outcome.left.exists(_.isInstanceOf[SQLTransientConnectionException]), written.size)

    assertFx(program, (true, 0), "next() after a failure still sent COM_STMT_FETCH")

  test("a healthy StreamingResultSet still fetches"):
    val program = for
      pair      <- protocolWith(failed = false)
      resultSet <- streamingResultSetOn(pair._1)
      _         <- resultSet.next().attempt
      written   <- pair._2.get
    yield written.size

    assertFxBoolean(program.map(_ > 0), "next() on a healthy connection never wrote to the socket")
