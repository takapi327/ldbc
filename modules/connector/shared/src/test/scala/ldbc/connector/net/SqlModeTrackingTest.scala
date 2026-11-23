/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scodec.Decoder

import cats.effect.*

import fs2.hashing.Hashing

import org.typelevel.otel4s.trace.Tracer

import munit.CatsEffectSuite

import ldbc.connector.authenticator.MysqlNativePasswordPlugin
import ldbc.connector.data.{ CapabilitiesFlags, ServerStatusFlags }
import ldbc.connector.net.packet.{ RequestPacket, ResponsePacket }
import ldbc.connector.net.packet.response.{ InitialPacket, OKPacket }
import ldbc.connector.net.protocol.Exchange
import ldbc.connector.util.Version

/**
 * Tests that the tracked `NO_BACKSLASH_ESCAPES` state follows the session sql_mode.
 *
 * Regression guard for the finding that the state could fall out of step with the session: the reply to
 * COM_CHANGE_USER, which resets the sql_mode, was read past the tracking, and the state was seeded from the server
 * greeting, which is sent before `init_connect` runs. A stale state escapes string literals for the wrong sql_mode, so
 * a quote can break out of its literal.
 */
class SqlModeTrackingTest extends CatsEffectSuite:

  given Tracer[IO]  = Tracer.noop[IO]
  given Hashing[IO] = Hashing.forSync[IO]

  /** A PacketSocket that ignores what is sent and replays a scripted list of server packets. */
  private final class ScriptedSocket(toReceive: Ref[IO, List[ResponsePacket]]) extends PacketSocket[IO]:
    override def receive[P <: ResponsePacket](decoder: Decoder[P]): IO[P] =
      toReceive
        .modify {
          case head :: tail => (tail, Some(head))
          case Nil          => (Nil, None)
        }
        .flatMap {
          case Some(packet) => IO.pure(packet.asInstanceOf[P])
          case None         => IO.raiseError(new RuntimeException("scripted socket exhausted"))
        }
    override def send(request: RequestPacket): IO[Unit] = IO.unit

  private def ok(statusFlags: Set[ServerStatusFlags]) = OKPacket(0x00, 0L, 0L, statusFlags, None, None, None, None)

  private val noBackslashEscapes = Set(ServerStatusFlags.SERVER_STATUS_NO_BACKSLASH_ESCAPES)

  /** Runs `use` on a protocol whose greeting carries `greetingFlags`, and returns the tracked state afterwards. */
  private def trackedAfter(greetingFlags: Set[ServerStatusFlags], replies: List[ResponsePacket])(
    use: Protocol[IO] => IO[Unit]
  ): IO[Boolean] =
    for
      given Exchange[IO] <- Exchange[IO]
      toReceive          <- Ref[IO].of(replies)
      sequenceIdRef      <- Ref[IO].of[Byte](0x00)
      protocol = Protocol.Impl[IO](
                   initialPacket = InitialPacket(
                     protocolVersion = 10,
                     serverVersion   = Version(8, 4, 0),
                     threadId        = 1,
                     capabilityFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41),
                     characterSet    = 255,
                     statusFlags     = greetingFlags,
                     scrambleBuff    = Array.fill[Byte](20)(1),
                     authPlugin      = "mysql_native_password"
                   ),
                   hostInfo                    = HostInfo("127.0.0.1", 3306, "user", Some("secret"), Some("db")),
                   socket                      = new ScriptedSocket(toReceive),
                   useSSL                      = false,
                   allowPublicKeyRetrieval     = false,
                   capabilityFlags             = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41),
                   sequenceIdRef               = sequenceIdRef,
                   defaultAuthenticationPlugin = None,
                   plugins                     = Map("mysql_native_password" -> MysqlNativePasswordPlugin[IO]())
                 )
      _       <- use(protocol)
      tracked <- protocol.noBackslashEscapes
    yield tracked

  test("the reply to CHANGE_USER clears NO_BACKSLASH_ESCAPES when the reset session no longer has it") {
    trackedAfter(noBackslashEscapes, List(ok(Set.empty)))(_.changeUser("user", "secret")).map { tracked =>
      assertEquals(tracked, false)
    }
  }

  test("the reply to CHANGE_USER sets NO_BACKSLASH_ESCAPES when the reset session has it") {
    trackedAfter(Set.empty, List(ok(noBackslashEscapes)))(_.changeUser("user", "secret")).map { tracked =>
      assertEquals(tracked, true)
    }
  }

  test("the sql_mode read after authentication sets NO_BACKSLASH_ESCAPES the greeting did not carry") {
    val variables = Map("sql_mode" -> "STRICT_TRANS_TABLES,NO_BACKSLASH_ESCAPES")
    trackedAfter(Set.empty, List.empty)(_.adoptSessionSqlMode(variables)).map { tracked =>
      assertEquals(tracked, true)
    }
  }

  test("the sql_mode read after authentication clears NO_BACKSLASH_ESCAPES the greeting carried") {
    val variables = Map("sql_mode" -> "STRICT_TRANS_TABLES")
    trackedAfter(noBackslashEscapes, List.empty)(_.adoptSessionSqlMode(variables)).map { tracked =>
      assertEquals(tracked, false)
    }
  }
