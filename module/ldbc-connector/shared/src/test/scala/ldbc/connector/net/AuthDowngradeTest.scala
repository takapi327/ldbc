/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scodec.Decoder
import scodec.bits.ByteVector

import cats.effect.*

import fs2.hashing.Hashing

import org.typelevel.otel4s.trace.Tracer

import munit.CatsEffectSuite

import ldbc.authentication.plugin.MysqlClearPasswordPlugin

import ldbc.connector.authenticator.MysqlNativePasswordPlugin
import ldbc.connector.data.{ CapabilitiesFlags, ServerStatusFlags }
import ldbc.connector.util.Version
import ldbc.connector.net.packet.{ RequestPacket, ResponsePacket }
import ldbc.connector.net.packet.request.AuthSwitchResponsePacket
import ldbc.connector.net.packet.response.{ AuthSwitchRequestPacket, InitialPacket, OKPacket }
import ldbc.connector.net.protocol.Exchange

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
class AuthDowngradeTest extends CatsEffectSuite:

  given Tracer[IO]  = Tracer.noop[IO]
  given Hashing[IO] = Hashing.forSync[IO]

  /** A PacketSocket that records everything sent and replays a scripted list of server packets. */
  private final class ScriptedSocket(
    sent:      Ref[IO, Vector[RequestPacket]],
    toReceive: Ref[IO, List[ResponsePacket]]
  ) extends PacketSocket[IO]:
    override def receive[P <: ResponsePacket](decoder: Decoder[P]): IO[P] =
      toReceive.modify {
        case head :: tail => (tail, Some(head))
        case Nil          => (Nil, None)
      }.flatMap {
        case Some(packet) => IO.pure(packet.asInstanceOf[P])
        case None         => IO.raiseError(new RuntimeException("scripted socket exhausted"))
      }
    override def send(request: RequestPacket): IO[Unit] = sent.update(_ :+ request)

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
      given Exchange[IO] <- Exchange[IO]
      sent               <- Ref[IO].of(Vector.empty[RequestPacket])
      toReceive          <- Ref[IO].of(
                     List[ResponsePacket](
                       // server forces a switch to the cleartext plugin ...
                       AuthSwitchRequestPacket(0xfe, "mysql_clear_password", Array.fill[Byte](20)(2)),
                       // ... then would accept the harvested cleartext.
                       OKPacket(0x00, 0L, 0L, Set.empty[ServerStatusFlags], None, None, None, None)
                     )
                   )
      protocol = Protocol.Impl[IO](
                   initialPacket               = initialPacket,
                   hostInfo                    = hostInfo,
                   socket                      = new ScriptedSocket(sent, toReceive),
                   useSSL                      = false,
                   allowPublicKeyRetrieval     = false,
                   capabilityFlags             = Set.empty[CapabilitiesFlags],
                   sequenceIdRef               = null,
                   defaultAuthenticationPlugin = None,
                   plugins = Map(
                     "mysql_native_password" -> MysqlNativePasswordPlugin[IO](),
                     "mysql_clear_password"  -> MysqlClearPasswordPlugin[IO]()
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

    program.map { case (result, leaked) =>
      assert(!leaked, "cleartext password was sent over a non-SSL connection")
      assert(result.isLeft, s"expected authentication to fail (SSL required), but got $result")
      assert(
        result.left.exists(_.getMessage.contains("SSL connection required")),
        s"expected an 'SSL connection required' error, got $result"
      )
    }
  }
