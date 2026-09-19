/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import scodec.Decoder

import ldbc.sql.SQLException

import ldbc.effect.Ref
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.data.{ CapabilitiesFlags, ServerStatusFlags }
import ldbc.mysql.exception.EofException
import ldbc.mysql.net.packet.{ RequestPacket, ResponsePacket }
import ldbc.mysql.net.packet.response.{ InitialPacket, OKPacket }
import ldbc.mysql.net.protocol.Exchange
import ldbc.mysql.util.Version
import ldbc.mysql.FTestPlatform
import ldbc.telemetry.*

/**
 * Anything that escapes the packet boundary — an I/O error, a decode failure, an EOF — must leave the
 * connection marked as failed, because at that point we no longer know how much of the byte stream
 * was consumed and MySQL gives us no way to resynchronise.
 *
 * The guard lives inside `Protocol.Impl`, wrapping whichever `PacketSocket` was injected, so these
 * tests can drive it with a scripted socket instead of a real server.
 *
 * Two of the cases are there to pin down the boundary. `readUntilEOF` and `repeatProcess` reach the
 * injected socket directly rather than through `Protocol`'s own `receive`, so they would slip past
 * a guard placed on those methods. Conversely an `ERR_Packet` decodes cleanly and is returned as a
 * value — only the caller turns it into an error — and must not be mistaken for a broken transport,
 * or perfectly good connections would be thrown away.
 */
class TransportFailureTest extends FTestPlatform:

  given Tracer[Fx] = Tracer.noop[Fx]

  /** A PacketSocket whose `receive` always fails, standing in for a broken stream. */
  private final class FailingSocket(error: Throwable) extends PacketSocket[Fx]:
    override def receive[P <: ResponsePacket](decoder: Decoder[P]):    Fx[P]    = Fx.raiseError(error)
    override def send(request:                         RequestPacket): Fx[Unit] = Fx.unit

  /** A PacketSocket that succeeds, to show the flag is not set on a healthy exchange. */
  private final class HealthySocket(response: ResponsePacket) extends PacketSocket[Fx]:
    override def receive[P <: ResponsePacket](decoder: Decoder[P]): Fx[P] =
      Fx.pure(response.asInstanceOf[P])
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
      _        <- protocol.readUntilEOF(ldbc.mysql.net.packet.response.OKPacket.decoder(Set.empty)).attempt
      failed   <- protocol.transportFailed
    yield failed

    assertFx(program, true)

  test("a server-reported error does not mark the transport as failed"):
    val ok      = OKPacket(0x00, 0L, 0L, Set.empty[ServerStatusFlags], None, None, None, None)
    val program = for
      protocol <- protocolWith(new HealthySocket(ok))
      _        <- protocol.comPing().attempt
      failed   <- protocol.transportFailed
    yield failed

    assertFx(program, false)
