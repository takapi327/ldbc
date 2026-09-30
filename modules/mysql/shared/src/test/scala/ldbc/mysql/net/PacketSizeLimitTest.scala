/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import scala.concurrent.duration.Duration

import scodec.bits.{ BitVector, ByteVector }

import ldbc.effect.Ref
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.data.{ CapabilitiesFlags, ServerStatusFlags }
import ldbc.mysql.exception.PacketTooBigException
import ldbc.mysql.net.packet.response.{ GenericResponsePackets, InitialPacket, OKPacket }
import ldbc.mysql.net.packet.RequestPacket
import ldbc.mysql.net.protocol.Exchange
import ldbc.mysql.util.Version
import ldbc.mysql.FTestPlatform
import ldbc.telemetry.Tracer

/**
 * Covers the size limit on both directions of a packet, and the point at which the server's own limit
 * replaces the configured one.
 */
class PacketSizeLimitTest extends FTestPlatform:

  /** A request of exactly the given payload size, so a test can aim at the limit precisely. */
  private final class SizedRequest(payloadSize: Int) extends RequestPacket:
    override protected def encodeBody: scodec.Attempt[BitVector] = scodec.Attempt.successful(encode)
    override def encode:               BitVector                 = ByteVector.fill(payloadSize.toLong)(0x41).toBitVector
    override def toString:             String                    = s"SizedRequest($payloadSize)"

  /** Records everything written, and replays scripted chunks to whatever reads. */
  /**
   * A request whose payload does not fit in an `Int`.
   *
   * `BitVector` reports its size as a `Long`, so a run of this length can be described without
   * allocating it — which is what lets the size check be exercised at a scale no real statement
   * would reach in a test.
   */
  private final class OverlongRequest extends RequestPacket:
    override protected def encodeBody: scodec.Attempt[BitVector] = scodec.Attempt.successful(encode)
    override def encode:               BitVector                 = BitVector.fill(3L * 1024 * 1024 * 1024 * 8)(false)
    override def toString:             String                    = "OverlongRequest"

  /**
   * A socket that hands back at most 8192 bytes per read, the way a real one does.
   *
   * `Socket.read` promises *up to* n bytes; splitting the payload here is what makes the accumulation
   * in [[BitVectorSocket]] observable.
   */
  private final class ChunkedSocket(remaining: Ref[Fx, ByteVector], reads: Ref[Fx, Int]) extends ldbc.net.Socket[Fx]:
    override def read(n: Int): Fx[Option[Array[Byte]]] =
      reads.update(_ + 1) *> remaining.modify { left =>
        if left.isEmpty then (left, None)
        else
          val (head, tail) = left.splitAt(Math.min(n.toLong, 8192L))
          (tail, Some(head.toArray))
      }
    override def write(bytes: Array[Byte]): Fx[Unit] = Fx.unit
    override def close():                   Fx[Unit] = Fx.unit

  private final class ScriptedBitVectorSocket(written: Ref[Fx, ByteVector], chunks: Ref[Fx, List[BitVector]])
    extends BitVectorSocket[Fx]:
    override def write(bits: BitVector): Fx[Unit]      = written.update(_ ++ bits.bytes)
    override def read(nBytes: Int):      Fx[BitVector] =
      chunks
        .modify {
          case head :: tail => (tail, Some(head))
          case Nil          => (Nil, None)
        }
        .flatMap {
          case Some(bits) => Fx.pure(bits)
          case None       => Fx.raiseError(new IllegalStateException("the script ran out of chunks"))
        }

  private final case class Harness(
    socket:          PacketSocket[Fx],
    written:         Ref[Fx, ByteVector],
    transportFailed: Ref[Fx, Boolean],
    limit:           Ref[Fx, Int],
    chunks:          Ref[Fx, List[BitVector]]
  )

  private def socketWithLimit(limit: Int, script: List[BitVector] = Nil): Fx[Harness] =
    for
      written             <- Ref.of[Fx, ByteVector](ByteVector.empty)
      chunks              <- Ref.of[Fx, List[BitVector]](script)
      sequenceIdRef       <- Ref.of[Fx, Byte](0x00)
      transportFailedRef  <- Ref.of[Fx, Boolean](false)
      maxAllowedPacketRef <- Ref.of[Fx, Int](limit)
    yield Harness(
      PacketSocket.fromBitVectorSocket[Fx](
        new ScriptedBitVectorSocket(written, chunks),
        false,
        sequenceIdRef,
        maxAllowedPacketRef,
        transportFailedRef
      ),
      written,
      transportFailedRef,
      maxAllowedPacketRef,
      chunks
    )

  private def header(payloadSize: Int, sequenceId: Byte): BitVector =
    ByteVector(
      Array[Byte](
        payloadSize.toByte,
        ((payloadSize >> 8) & 0xff).toByte,
        ((payloadSize >> 16) & 0xff).toByte,
        sequenceId
      )
    ).toBitVector

  /** `0x00` status, then the length-encoded rows/id and the 2-byte status and warning fields. */
  private val okPayload: BitVector =
    ByteVector(Array[Byte](0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00)).toBitVector

  private val okDecoder = GenericResponsePackets.decoder(Set.empty[CapabilitiesFlags])

  test("a request within the limit is written"):
    for
      harness         <- socketWithLimit(1024)
      _               <- harness.socket.send(new SizedRequest(1000))
      bytes           <- harness.written.get.map(_.size.toInt)
      transportFailed <- harness.transportFailed.get
    yield
      assertEquals(bytes, 1004, "the payload plus its 4-byte header reached the socket")
      assertEquals(transportFailed, false)

  test("a request over the limit is refused before anything is written"):
    for
      harness         <- socketWithLimit(1024)
      result          <- harness.socket.send(new SizedRequest(2000)).attempt
      bytes           <- harness.written.get.map(_.size.toInt)
      transportFailed <- harness.transportFailed.get
    yield
      assert(
        result.left.exists(_.isInstanceOf[PacketTooBigException]),
        s"expected the send to be refused by size, got $result"
      )
      assertEquals(bytes, 0, "a refused send must not put any bytes on the wire")
      assertEquals(
        transportFailed,
        false,
        "refusing a send leaves the stream on a packet boundary, so the connection stays usable"
      )

  test("a request that the 3-byte length cannot describe is refused rather than truncated"):
    for
      harness <- socketWithLimit(PacketSocket.PROTOCOL_MAX_PACKET_SIZE)
      result  <- harness.socket.send(new SizedRequest(PacketSocket.PROTOCOL_MAX_PACKET_SIZE + 1)).attempt
      bytes   <- harness.written.get.map(_.size.toInt)
    yield
      assert(
        result.left.exists(_.isInstanceOf[PacketTooBigException]),
        s"a payload of ${ PacketSocket.PROTOCOL_MAX_PACKET_SIZE + 1 } cannot be framed, but got $result"
      )
      assertEquals(bytes, 0)

  test("the server's limit narrows the configured one, and never widens it"):
    val configured = 16777215
    assertEquals(
      Protocol.effectivePacketLimit(configured, Map("max_allowed_packet" -> "67108864")),
      16777215,
      "a server that allows more than the protocol can frame cannot lift the protocol's own ceiling"
    )
    assertEquals(
      Protocol.effectivePacketLimit(configured, Map("max_allowed_packet" -> "1048576")),
      1048576,
      "a server that accepts less than the caller configured decides the limit"
    )
    assertEquals(
      Protocol.effectivePacketLimit(1048576, Map("max_allowed_packet" -> "67108864")),
      1048576,
      "a ceiling the caller set deliberately is not widened by a permissive server"
    )
    Fx.unit

  test("an unreported or unusable server limit leaves the configured one in place"):
    assertEquals(Protocol.effectivePacketLimit(1048576, Map.empty), 1048576)
    assertEquals(Protocol.effectivePacketLimit(1048576, Map("max_allowed_packet" -> "")), 1048576)
    assertEquals(Protocol.effectivePacketLimit(1048576, Map("max_allowed_packet" -> "unknown")), 1048576)
    assertEquals(Protocol.effectivePacketLimit(1048576, Map("max_allowed_packet" -> "0")), 1048576)
    Fx.unit

  test("a configuration beyond what a packet header can describe is capped"):
    assertEquals(
      Protocol.effectivePacketLimit(Int.MaxValue, Map.empty),
      PacketSocket.PROTOCOL_MAX_PACKET_SIZE
    )
    Fx.unit

  test("a refused send leaves the connection able to carry the next one"):
    for
      harness <- socketWithLimit(1024)
      _       <- harness.socket.send(new SizedRequest(2000)).attempt
      _       <- harness.socket.send(new SizedRequest(100))
      written <- harness.written.get
    yield
      assertEquals(written.size.toInt, 104, "only the accepted request reached the wire")
      assertEquals(
        written.take(4).toArray.toList,
        List[Byte](100, 0, 0, 0),
        "the sequence id is still 0, so the refused send did not consume one"
      )

  test("the receive limit follows the negotiated value"):
    val oversized = List(header(2000, 0))
    for
      harness <- socketWithLimit(1024, oversized)
      refused <- harness.socket.receive(okDecoder).attempt
      _       <- harness.limit.set(4096)
      _       <- harness.chunks.set(List(header(okPayload.bytes.size.toInt, 0), okPayload))
      allowed <- harness.socket.receive(okDecoder).attempt
    yield
      assert(
        refused.left.exists(_.isInstanceOf[PacketTooBigException]),
        s"2000 bytes is past the 1024 limit, but got $refused"
      )
      assert(
        allowed.exists(_.isInstanceOf[OKPacket]),
        s"once the limit is raised the same size must be accepted, but got $allowed"
      )

  test("adopting the server's limit changes what the socket will actually send"):
    given Tracer[Fx]  = Tracer.noop[Fx]
    val initialPacket = InitialPacket(
      protocolVersion = 10,
      serverVersion   = Version(8, 4, 0),
      threadId        = 1,
      capabilityFlags = Set.empty[CapabilitiesFlags],
      characterSet    = 45,
      statusFlags     = Set.empty[ServerStatusFlags],
      scrambleBuff    = Array.fill[Byte](20)(1),
      authPlugin      = "mysql_native_password"
    )
    for
      harness            <- socketWithLimit(PacketSocket.PROTOCOL_MAX_PACKET_SIZE)
      given Exchange[Fx] <- Exchange.apply[Fx]
      sequenceIdRef      <- Ref.of[Fx, Byte](0x00)
      initialPacketRef   <- Ref.of[Fx, Option[InitialPacket]](Some(initialPacket))
      protocol           <- Protocol.fromPacketSocket[Fx](
                    packetSocket                = harness.socket,
                    hostInfo                    = HostInfo("127.0.0.1", 3306, "user", None, None),
                    sslOptions                  = None,
                    allowPublicKeyRetrieval     = false,
                    capabilitiesFlags           = Set.empty[CapabilitiesFlags],
                    sequenceIdRef               = sequenceIdRef,
                    initialPacketRef            = initialPacketRef,
                    maxAllowedPacketRef         = harness.limit,
                    defaultAuthenticationPlugin = None,
                    plugins                     = Map.empty
                  )
      beforeAdopting <- protocol.send(new SizedRequest(4000)).attempt
      _              <- protocol.adoptServerPacketLimit(Map("max_allowed_packet" -> "1024"))
      afterAdopting  <- protocol.send(new SizedRequest(4000)).attempt
      effective      <- harness.limit.get
    yield
      assert(beforeAdopting.isRight, s"4000 bytes fits the protocol maximum, but got $beforeAdopting")
      assert(
        afterAdopting.left.exists(_.isInstanceOf[PacketTooBigException]),
        "the socket kept using the old limit, so the negotiated value is not reaching it"
      )
      assertEquals(effective, 1024)

  test("a size beyond what an Int can hold is reported as a size, not as a negative number"):
    for
      harness <- socketWithLimit(1024)
      result  <- harness.socket.send(new OverlongRequest).attempt
      written <- harness.written.get
    yield
      val error = result.left.getOrElse(fail("a payload past Int.MaxValue must be refused"))
      assert(error.isInstanceOf[PacketTooBigException], s"expected a size error, got $error")
      assert(
        !error.getMessage.contains("-"),
        s"the reported size wrapped to a negative number: ${ error.getMessage }"
      )
      assertEquals(written.size.toInt, 0)

  test("a payload delivered in several reads is accumulated into one"):
    val payloadSize = 20000
    val payload     = ByteVector.fill(payloadSize.toLong)(0x41)
    for
      remaining <- Ref.of[Fx, ByteVector](payload)
      reads     <- Ref.of[Fx, Int](0)
      carry     <- Ref.of[Fx, ByteVector](ByteVector.empty)
      socket = BitVectorSocket.fromSocket[Fx](new ChunkedSocket(remaining, reads), Duration.Inf, carry)
      body  <- socket.read(payloadSize)
      count <- reads.get
    yield
      assert(count > 2, s"the payload must arrive over several reads for this to mean anything ($count)")
      assertEquals(
        body.bytes.size.toInt,
        payloadSize,
        "a payload delivered over several reads was not put back together"
      )

  test("bytes read past the requested boundary are carried into the next read"):
    val first  = ByteVector.fill(10L)(0x01)
    val second = ByteVector.fill(6L)(0x02)
    for
      remaining <- Ref.of[Fx, ByteVector](first ++ second)
      reads     <- Ref.of[Fx, Int](0)
      carry     <- Ref.of[Fx, ByteVector](ByteVector.empty)
      socket = BitVectorSocket.fromSocket[Fx](new ChunkedSocket(remaining, reads), Duration.Inf, carry)
      head  <- socket.read(10)
      tail  <- socket.read(6)
      count <- reads.get
    yield
      assertEquals(head.bytes, first)
      assertEquals(tail.bytes, second, "the surplus from the first read was dropped instead of carried")
      assertEquals(count, 1, "the whole 16 bytes arrived in one read, so no second read should have happened")
