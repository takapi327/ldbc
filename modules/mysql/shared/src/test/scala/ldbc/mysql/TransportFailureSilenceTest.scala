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

/**
 * Once the transport has failed, the point of the change is not only that operations raise — it is
 * that they stop writing to the socket. A connection whose stream position is unknown must not
 * receive a `ROLLBACK`, a `COM_QUIT`, a `COM_STMT_FETCH` or anything else.
 *
 * Every case here therefore asserts on what was *sent*, not just on the exception, because an
 * implementation that keeps talking to a dead peer would still raise and would still look green to
 * a test that only checks the error.
 *
 * `close()` is driven with `autoCommit = false` so that a healthy teardown would send `ROLLBACK`
 * and then `COM_QUIT` — without that, "wrote nothing" would hold trivially. The healthy
 * `StreamingResultSet` case is there for the same reason: the guard must not make the ordinary
 * fetch path unreachable.
 */
class TransportFailureSilenceTest extends FTestPlatform:

  given Tracer[Fx] = Tracer.noop[Fx]

  /** A PacketSocket that records writes and always fails reads, so the guard trips on first use. */
  private final class BrokenRecordingSocket(sent: Ref[Fx, Vector[RequestPacket]]) extends PacketSocket[Fx]:
    override def receive[P <: ResponsePacket](decoder: Decoder[P]): Fx[P] =
      Fx.raiseError(new java.io.IOException("connection reset"))
    override def send(request: RequestPacket): Fx[Unit] = sent.update(_ :+ request)

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

  /** Builds a protocol on a recording socket and trips the guard, returning both. */
  private def failedProtocol: Fx[(Protocol[Fx], Ref[Fx, Vector[RequestPacket]])] =
    for
      sent               <- Ref.of[Fx, Vector[RequestPacket]](Vector.empty)
      given Exchange[Fx] <- Exchange.apply[Fx]
      sequenceIdRef      <- Ref.of[Fx, Byte](0x01)
      protocol = Protocol.Impl[Fx](
                   initialPacket               = initialPacket,
                   hostInfo                    = hostInfo,
                   rawSocket                   = new BrokenRecordingSocket(sent),
                   useSSL                      = false,
                   allowPublicKeyRetrieval     = false,
                   capabilityFlags             = Set.empty[CapabilitiesFlags],
                   sequenceIdRef               = sequenceIdRef,
                   defaultAuthenticationPlugin = None,
                   plugins                     = Map.empty
                 )
      _ <- protocol.comPing().attempt
      _ <- sent.set(Vector.empty)
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

  test("close() on a failed transport writes nothing"):
    val program = for
      failed     <- failedProtocol
      connection <- connectionOn(failed._1)
      _          <- connection.close()
      written    <- failed._2.get
      closed     <- connection.isClosed()
    yield (written.size, closed)

    assertFx(program, (0, true), "故障した接続の close() がネットワークに書き込んでいる")

  test("StreamingResultSet.next() on a failed transport writes nothing"):
    val program = for
      failed    <- failedProtocol
      isClosed  <- Ref.of[Fx, Boolean](false)
      fetchSize <- Ref.of[Fx, Int](10)
      resultSet = StreamingResultSet[Fx](
                    protocol           = failed._1,
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
      outcome <- resultSet.next().attempt
      written <- failed._2.get
    yield (outcome.left.exists(_.isInstanceOf[SQLTransientConnectionException]), written.size)

    assertFx(program, (true, 0), "故障後の next() が COM_STMT_FETCH を送っている")

  test("a healthy StreamingResultSet still fetches"):
    val program = for
      sent               <- Ref.of[Fx, Vector[RequestPacket]](Vector.empty)
      given Exchange[Fx] <- Exchange.apply[Fx]
      sequenceIdRef      <- Ref.of[Fx, Byte](0x01)
      protocol = Protocol.Impl[Fx](
                   initialPacket               = initialPacket,
                   hostInfo                    = hostInfo,
                   rawSocket                   = new BrokenRecordingSocket(sent),
                   useSSL                      = false,
                   allowPublicKeyRetrieval     = false,
                   capabilityFlags             = Set.empty[CapabilitiesFlags],
                   sequenceIdRef               = sequenceIdRef,
                   defaultAuthenticationPlugin = None,
                   plugins                     = Map.empty
                 )
      isClosed  <- Ref.of[Fx, Boolean](false)
      fetchSize <- Ref.of[Fx, Int](10)
      resultSet = StreamingResultSet[Fx](
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
      _       <- resultSet.next().attempt
      written <- sent.get
    yield written.size

    assertFxBoolean(program.map(_ > 0), "健全な接続で next() が一度もソケットに書いていない")
