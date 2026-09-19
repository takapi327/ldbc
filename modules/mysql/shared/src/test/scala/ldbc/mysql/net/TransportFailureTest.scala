/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import scodec.Decoder

import ldbc.sql.SQLException

import ldbc.effect.{ Deferred, Ref }
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.data.{ CapabilitiesFlags, ServerStatusFlags }
import ldbc.mysql.exception.EofException
import ldbc.mysql.net.packet.{ RequestPacket, ResponsePacket }
import ldbc.mysql.net.packet.response.{ ERRPacket, InitialPacket, OKPacket }
import ldbc.mysql.net.protocol.Exchange
import ldbc.mysql.util.Version
import ldbc.mysql.FTestPlatform
import ldbc.telemetry.*

/**
 * Anything that escapes the packet boundary — an I/O error, a decode failure, an EOF, a cancelled
 * read — must leave the connection marked as failed, because at that point we no longer know how
 * much of the byte stream was consumed and MySQL gives us no way to resynchronise.
 *
 * The guard lives inside `Protocol.Impl`, wrapping whichever `PacketSocket` was injected, so these
 * tests can drive it with a scripted socket instead of a real server.
 *
 * Two of the cases pin down the boundary. `readUntilEOF` and `repeatProcess` reach the injected
 * socket directly rather than through `Protocol`'s own `receive`, so they would slip past a guard
 * placed on those methods. Conversely an `ERR_Packet` decodes cleanly and is returned as a value —
 * only the caller turns it into an error — and must not be mistaken for a broken transport, or
 * perfectly good connections would be thrown away.
 *
 * The cancellation case is driven through `receive` rather than a `comXxx` command on purpose:
 * commands run inside `Exchange`'s `uncancelable`, which masks cancellation so the guard is never
 * reached. Covering the `onCancel` half directly keeps that branch from rotting while it waits for
 * cancellation to become reachable from the outside.
 */
class TransportFailureTest extends FTestPlatform:

  given Tracer[Fx] = Tracer.noop[Fx]

  /** A PacketSocket whose `receive` always fails, standing in for a broken stream. */
  private final class FailingSocket(error: Throwable) extends PacketSocket[Fx]:
    override def receive[P <: ResponsePacket](decoder: Decoder[P]):    Fx[P]    = Fx.raiseError(error)
    override def send(request:                         RequestPacket): Fx[Unit] = Fx.unit

  /** A PacketSocket that replays one response and records everything written to it. */
  private final class RecordingSocket(response: ResponsePacket, sent: Ref[Fx, Vector[RequestPacket]])
    extends PacketSocket[Fx]:
    override def receive[P <: ResponsePacket](decoder: Decoder[P]): Fx[P] =
      Fx.pure(response.asInstanceOf[P])
    override def send(request: RequestPacket): Fx[Unit] = sent.update(_ :+ request)

  /** A PacketSocket whose `receive` never completes, so the caller can only be cancelled out of it. */
  private final class NeverRespondingSocket(entered: Deferred[Fx, Unit]) extends PacketSocket[Fx]:
    override def receive[P <: ResponsePacket](decoder: Decoder[P]): Fx[P] =
      entered.complete(()) *> Fx.async[P](_ => Fx.Canceler.noop)
    override def send(request: RequestPacket): Fx[Unit] = Fx.unit

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

  private val okPacket  = OKPacket(0x00, 0L, 0L, Set.empty[ServerStatusFlags], None, None, None, None)
  private val errPacket = ERRPacket(0xff, 1049, 0x23, Some("42000"), "Unknown database 'nope'")

  private def protocolWith(socket: PacketSocket[Fx]): Fx[Protocol[Fx]] =
    for
      given Exchange[Fx] <- Exchange.apply[Fx]
      sequenceIdRef      <- Ref.of[Fx, Byte](0x01)
    yield Protocol.Impl[Fx](
      initialPacket               = initialPacket,
      hostInfo                    = hostInfo,
      rawSocket                   = socket,
      useSSL                      = false,
      allowPublicKeyRetrieval     = false,
      capabilityFlags             = Set.empty[CapabilitiesFlags],
      sequenceIdRef               = sequenceIdRef,
      defaultAuthenticationPlugin = None,
      plugins                     = Map.empty
    )

  test("a decode failure on receive marks the transport as failed"):
    val program = for
      protocol <- protocolWith(new FailingSocket(new SQLException("malformed packet")))
      before   <- protocol.transportFailed
      _        <- protocol.comPing().attempt
      after    <- protocol.transportFailed
    yield (before, after)

    assertFx(program, (false, true))

  test("an EOF while reading marks the transport as failed"):
    val program = for
      protocol <- protocolWith(new FailingSocket(EofException(4, 0)))
      _        <- protocol.comPing().attempt
      failed   <- protocol.transportFailed
    yield failed

    assertFx(program, true)

  test("a failure raised through readUntilEOF is caught too"):
    val program = for
      protocol <- protocolWith(new FailingSocket(new SQLException("truncated row")))
      _        <- protocol.readUntilEOF(OKPacket.decoder(Set.empty)).attempt
      failed   <- protocol.transportFailed
    yield failed

    assertFx(program, true)

  test("cancelling a receive marks the transport as failed"):
    val program = for
      entered  <- Deferred[Fx, Unit]
      protocol <- protocolWith(new NeverRespondingSocket(entered))
      fiber    <- protocol.receive(OKPacket.decoder(Set.empty)).start
      _        <- entered.get
      _        <- fiber.cancel
      failed   <- protocol.transportFailed
    yield failed

    assertFx(program, true, "キャンセルされた receive が故障として記録されていない")

  test("an ERR_Packet raises but leaves the transport usable"):
    val sentRef = Ref.unsafe[Fx, Vector[RequestPacket]](Vector.empty)
    val program = for
      protocol <- protocolWith(new RecordingSocket(errPacket, sentRef))
      outcome  <- protocol.comInitDB("nope").attempt
      failed   <- protocol.transportFailed
    yield (outcome.isLeft, failed)

    assertFx(program, (true, false), "サーバが返した ERR_Packet を接続の故障として扱っている")

  test("a successful exchange leaves the transport usable"):
    val sentRef = Ref.unsafe[Fx, Vector[RequestPacket]](Vector.empty)
    val program = for
      protocol <- protocolWith(new RecordingSocket(okPacket, sentRef))
      _        <- protocol.comPing().attempt
      failed   <- protocol.transportFailed
    yield failed

    assertFx(program, false)
