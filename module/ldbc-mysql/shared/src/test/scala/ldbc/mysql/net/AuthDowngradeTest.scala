/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import scodec.bits.ByteVector
import scodec.Decoder

import ldbc.effect.Ref
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.authenticator.MysqlClearPasswordPlugin
import ldbc.mysql.authenticator.MysqlNativePasswordPlugin
import ldbc.mysql.data.{ CapabilitiesFlags, ServerStatusFlags }
import ldbc.mysql.net.packet.{ RequestPacket, ResponsePacket }
import ldbc.mysql.net.packet.request.AuthSwitchResponsePacket
import ldbc.mysql.net.packet.response.{ AuthSwitchRequestPacket, InitialPacket, OKPacket }
import ldbc.mysql.net.protocol.Exchange
import ldbc.mysql.util.Version
import ldbc.mysql.FTestPlatform
import ldbc.telemetry.*

/**
 * Verification test for the security finding: the confidentiality guard is applied only on the
 * initial handshake, not on the server-driven `AuthSwitchRequest` path. A rogue / MITM server can
 * therefore ask a non-SSL client to switch to `mysql_clear_password` and harvest the cleartext
 * password, bypassing the guard that `startAuthentication` would otherwise enforce.
 *
 * The test drives the real auth flow over a scripted socket. It asserts the SECURE behaviour: the
 * client must fail with an "SSL connection required" error and must NOT send the cleartext password.
 * While the guard is missing on the switch path, this test FAILS.
 */
class AuthDowngradeTest extends FTestPlatform:

  given Tracer[Fx] = Tracer.noop[Fx]

  /** A PacketSocket that records everything sent and replays a scripted list of server packets. */
  private final class ScriptedSocket(
    sent:      Ref[Fx, Vector[RequestPacket]],
    toReceive: Ref[Fx, List[ResponsePacket]]
  ) extends PacketSocket[Fx]:
    override def receive[P <: ResponsePacket](decoder: Decoder[P]): Fx[P] =
      toReceive
        .modify {
          case head :: tail => (tail, Some(head))
          case Nil          => (Nil, None)
        }
        .flatMap {
          case Some(packet) => Fx.pure(packet.asInstanceOf[P])
          case None         => Fx.raiseError(new RuntimeException("scripted socket exhausted"))
        }
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

  test("a rogue AuthSwitchRequest to mysql_clear_password over a non-SSL connection must be rejected") {
    val password  = "secret"
    val cleartext = ByteVector(password.getBytes("UTF-8"))

    val program = for
      given Exchange[Fx] <- Exchange.apply[Fx]
      sent               <- Ref.of[Fx, Vector[RequestPacket]](Vector.empty)
      toReceive          <- Ref.of[Fx, List[ResponsePacket]](
                     List[ResponsePacket](
                       AuthSwitchRequestPacket(0xfe, "mysql_clear_password", Array.fill[Byte](20)(2)),
                       OKPacket(0x00, 0L, 0L, Set.empty[ServerStatusFlags], None, None, None, None)
                     )
                   )
      protocol = Protocol.Impl(
                   initialPacket               = initialPacket,
                   hostInfo                    = hostInfo,
                   socket                      = new ScriptedSocket(sent, toReceive),
                   useSSL                      = false,
                   allowPublicKeyRetrieval     = false,
                   capabilityFlags             = Set.empty[CapabilitiesFlags],
                   sequenceIdRef               = null,
                   defaultAuthenticationPlugin = None,
                   plugins                     = Map(
                     "mysql_native_password" -> MysqlNativePasswordPlugin[Fx],
                     "mysql_clear_password"  -> MysqlClearPasswordPlugin[Fx]
                   )
                 )
      result <- protocol.startAuthentication("user", password).attempt
      sends  <- sent.get
    yield
      val leaked = sends.exists {
        case r: AuthSwitchResponsePacket => r.hashedPassword == cleartext
        case _                           => false
      }
      (result, leaked)

    program.map {
      case (result, leaked) =>
        assert(!leaked, "cleartext password was sent over a non-SSL connection")
        assert(result.isLeft, s"expected authentication to fail (SSL required), but got $result")
        assert(
          result.left.exists(_.getMessage.contains("SSL connection required")),
          s"expected an 'SSL connection required' error, got $result"
        )
    }
  }

  test("changeUser to a confidentiality-requiring plugin over a non-SSL connection must be rejected") {
    val clearTextInitialPacket = initialPacket.copy(authPlugin = "mysql_clear_password")

    val program = for
      given Exchange[Fx] <- Exchange.apply[Fx]
      sent               <- Ref.of[Fx, Vector[RequestPacket]](Vector.empty)
      toReceive          <- Ref.of[Fx, List[ResponsePacket]](List.empty[ResponsePacket])
      protocol = Protocol.Impl(
                   initialPacket               = clearTextInitialPacket,
                   hostInfo                    = hostInfo,
                   socket                      = new ScriptedSocket(sent, toReceive),
                   useSSL                      = false,
                   allowPublicKeyRetrieval     = false,
                   capabilityFlags             = Set.empty[CapabilitiesFlags],
                   sequenceIdRef               = null,
                   defaultAuthenticationPlugin = None,
                   plugins                     = Map("mysql_clear_password" -> MysqlClearPasswordPlugin[Fx])
                 )
      result <- protocol.changeUser("user", "secret").attempt
      sends  <- sent.get
    yield (result, sends)

    program.map {
      case (result, sends) =>
        assert(sends.isEmpty, s"expected no packet to be sent, but sent: $sends")
        assert(
          result.left.exists(_.getMessage.contains("SSL connection required")),
          s"expected an 'SSL connection required' error, got $result"
        )
    }
  }
