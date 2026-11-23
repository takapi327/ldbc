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
import ldbc.connector.net.packet.request.{ ComChangeUserPacket, ComQueryPacket, HandshakeResponse41Packet }
import ldbc.connector.net.packet.response.{ InitialPacket, OKPacket }
import ldbc.connector.net.protocol.Exchange
import ldbc.connector.util.Version

/**
 * Tests that the session character set is always utf8mb4, the character set every statement is encoded in.
 *
 * Regression guard for the finding that the client echoed the character set the server announced in its greeting.
 * With a server default such as sjis, gbk or big5, the session then parsed the UTF-8 statements in that character set,
 * where the backslash inserted by escaping can be taken as the second byte of a multibyte character, so the quote after
 * it closed the string literal. As in MySQL Connector/J, the client must request utf8mb4 and must not trust the
 * greeting, which also carries only the lower byte of the server collation ID.
 */
class CharsetNegotiationTest extends CatsEffectSuite:

  given Tracer[IO]  = Tracer.noop[IO]
  given Hashing[IO] = Hashing.forSync[IO]

  /** The collation ID of `sjis_japanese_ci`, a character set where 0x5C can be the second byte of a character. */
  private val sjisJapaneseCi = 13

  /** A PacketSocket that records everything sent and replays a scripted list of server packets. */
  private final class ScriptedSocket(
    sent:      Ref[IO, Vector[RequestPacket]],
    toReceive: Ref[IO, List[ResponsePacket]]
  ) extends PacketSocket[IO]:
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
    override def send(request: RequestPacket): IO[Unit] = sent.update(_ :+ request)

  private val ok = OKPacket(0x00, 0L, 0L, Set.empty[ServerStatusFlags], None, None, None, None)

  private def initialPacket(serverVersion: Version) = InitialPacket(
    protocolVersion = 10,
    serverVersion   = serverVersion,
    threadId        = 1,
    capabilityFlags = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41),
    characterSet    = sjisJapaneseCi,
    statusFlags     = Set.empty[ServerStatusFlags],
    scrambleBuff    = Array.fill[Byte](20)(1),
    authPlugin      = "mysql_native_password"
  )

  /** Runs `use` on a protocol over a scripted socket, and returns the packets the client sent. */
  private def sentBy(serverVersion: Version, replies: List[ResponsePacket])(
    use: Protocol[IO] => IO[Unit]
  ): IO[Vector[RequestPacket]] =
    for
      given Exchange[IO] <- Exchange[IO]
      sent               <- Ref[IO].of(Vector.empty[RequestPacket])
      toReceive          <- Ref[IO].of(replies)
      sequenceIdRef      <- Ref[IO].of[Byte](0x00)
      protocol = Protocol.Impl[IO](
                   initialPacket               = initialPacket(serverVersion),
                   hostInfo                    = HostInfo("127.0.0.1", 3306, "user", Some("secret"), Some("db")),
                   socket                      = new ScriptedSocket(sent, toReceive),
                   useSSL                      = false,
                   allowPublicKeyRetrieval     = false,
                   capabilityFlags             = Set(CapabilitiesFlags.CLIENT_PROTOCOL_41),
                   sequenceIdRef               = sequenceIdRef,
                   defaultAuthenticationPlugin = None,
                   plugins                     = Map("mysql_native_password" -> MysqlNativePasswordPlugin[IO]())
                 )
      _     <- use(protocol)
      sends <- sent.get
    yield sends

  test("the handshake requests utf8mb4_0900_ai_ci whatever character set the server announces") {
    sentBy(Version(8, 4, 0), List(ok))(_.startAuthentication("user", "secret")).map { sends =>
      assertEquals(sends.collect { case packet: HandshakeResponse41Packet => packet.characterSet }, Vector(255))
    }
  }

  test("the handshake requests utf8mb4_general_ci from a server older than 8.0.1") {
    sentBy(Version(5, 7, 44), List(ok))(_.startAuthentication("user", "secret")).map { sends =>
      assertEquals(sends.collect { case packet: HandshakeResponse41Packet => packet.characterSet }, Vector(45))
    }
  }

  test("CHANGE_USER requests utf8mb4 whatever character set the server announces") {
    sentBy(Version(8, 4, 0), List(ok))(_.changeUser("user", "secret")).map { sends =>
      assertEquals(sends.collect { case packet: ComChangeUserPacket => packet.characterSet }, Vector(255))
    }
  }

  test("SET NAMES utf8mb4 is sent when the session character set is not utf8mb4") {
    val variables = Map("character_set_client" -> "sjis", "character_set_connection" -> "sjis")
    sentBy(Version(8, 4, 0), List(ok))(_.ensureUtf8mb4Session(variables).void).map { sends =>
      assertEquals(sends.collect { case packet: ComQueryPacket => packet.sql }, Vector("SET NAMES utf8mb4"))
    }
  }

  test("nothing is sent when the session character set is already utf8mb4") {
    val variables = Map("character_set_client" -> "utf8mb4", "character_set_connection" -> "utf8mb4")
    sentBy(Version(8, 4, 0), List.empty)(_.ensureUtf8mb4Session(variables).void).map { sends =>
      assertEquals(sends, Vector.empty)
    }
  }
