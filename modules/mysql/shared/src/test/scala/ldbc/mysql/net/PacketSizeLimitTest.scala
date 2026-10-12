/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import scala.concurrent.duration.Duration

import scodec.{ Attempt, DecodeResult, Decoder }
import scodec.bits.{ BitVector, ByteVector }

import ldbc.effect.Ref
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.{ FTestPlatform, MySQLConfig }
import ldbc.mysql.data.{ CapabilitiesFlags, ServerStatusFlags }
import ldbc.mysql.exception.PacketTooBigException
import ldbc.mysql.net.packet.{ RequestPacket, ResponsePacket }
import ldbc.mysql.net.packet.response.{ GenericResponsePackets, InitialPacket, OKPacket }
import ldbc.mysql.net.protocol.{ parseHeader, Exchange }
import ldbc.mysql.util.Version
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

  /** Records everything written, and replays scripted chunks to whatever reads. */
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

  /**
   * Records each packet's header and throws the body away.
   *
   * A split of several megabytes can be checked for its shape — how many packets, how long each one
   * is, how the sequence id advances — without holding on to the bytes themselves.
   */
  private final class HeaderRecordingSocket(headers: Ref[Fx, Vector[(Int, Byte)]]) extends BitVectorSocket[Fx]:
    override def write(bits: BitVector): Fx[Unit] =
      val head = bits.take(32).toByteArray
      headers.update(_ :+ (parseHeader(head), head(3)))
    override def read(nBytes: Int): Fx[BitVector] =
      Fx.raiseError(new IllegalStateException("this socket only records writes"))

  /**
   * Fails the nth write and records how many got through.
   *
   * A failure on the first write is a different situation from one part-way through a split: the
   * latter leaves the peer holding an unfinished run of packets.
   */
  private final class FailingAtWriteSocket(failAt: Int, writes: Ref[Fx, Int]) extends BitVectorSocket[Fx]:
    override def write(bits: BitVector): Fx[Unit] =
      writes.updateAndGet(_ + 1).flatMap { count =>
        if count == failAt then Fx.raiseError(new java.io.IOException("broken pipe")) else Fx.unit
      }
    override def read(nBytes: Int): Fx[BitVector] =
      Fx.raiseError(new IllegalStateException("this socket only records writes"))

  /**
   * Reads back whatever was written to it, at most 8192 bytes per read the way a real one does.
   *
   * This is what makes a round trip possible: the framing on the way out is read by the framing on
   * the way in, with nothing in the test reproducing either.
   */
  private final class LoopbackSocket(buffer: Ref[Fx, ByteVector]) extends ldbc.net.Socket[Fx]:
    override def read(n: Int): Fx[Option[Array[Byte]]] =
      buffer.modify { left =>
        if left.isEmpty then (left, None)
        else
          val (head, tail) = left.splitAt(Math.min(n.toLong, 8192L))
          (tail, Some(head.toArray))
      }
    override def write(bytes: Array[Byte]): Fx[Unit] = buffer.update(_ ++ ByteVector(bytes))
    override def close():                   Fx[Unit] = Fx.unit

  /**
   * A request whose payload is generated on demand rather than held.
   *
   * `viewAt` hands back a vector that computes each byte when it is asked for, so a payload past the
   * protocol maximum costs nothing until something flattens it — which, on this path, is only ever
   * one chunk at a time. The pattern repeats every 251 bytes, a length coprime with the chunk size,
   * so a chunk written at the wrong offset cannot land on matching bytes.
   */
  private final class PatternedRequest(payloadSize: Int) extends RequestPacket:
    override protected def encodeBody: scodec.Attempt[BitVector] = scodec.Attempt.successful(encode)
    override def encode:   BitVector = ByteVector.viewAt(i => (i % 251).toByte, payloadSize.toLong).toBitVector
    override def toString: String    = s"PatternedRequest($payloadSize)"

  /** A response that records how large the reassembled message was, without flattening it. */
  private case class MeasuredPacket(size: Long) extends ResponsePacket

  private val measuringDecoder: Decoder[MeasuredPacket] =
    (bits: BitVector) => Attempt.Successful(DecodeResult(MeasuredPacket(bits.size / 8), BitVector.empty))

  /** A response that records the size and the bytes at the offsets a misplaced chunk would disturb. */
  private case class SampledPacket(size: Long, samples: List[Byte]) extends ResponsePacket

  private def samplingDecoder(offsets: List[Long]): Decoder[SampledPacket] =
    (bits: BitVector) =>
      Attempt.Successful(DecodeResult(SampledPacket(bits.size / 8, offsets.map(bits.getByte)), BitVector.empty))

  private final case class Harness(
    socket:          PacketSocket[Fx],
    written:         Ref[Fx, ByteVector],
    transportFailed: Ref[Fx, Boolean],
    limit:           Ref[Fx, Int],
    chunks:          Ref[Fx, List[BitVector]],
    sequenceId:      Ref[Fx, Byte]
  )

  private def socketWithLimit(limit: Int, script: List[BitVector] = Nil, startingSequenceId: Byte = 0x00): Fx[Harness] =
    for
      written             <- Ref.of[Fx, ByteVector](ByteVector.empty)
      chunks              <- Ref.of[Fx, List[BitVector]](script)
      sequenceIdRef       <- Ref.of[Fx, Byte](startingSequenceId)
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
      chunks,
      sequenceIdRef
    )

  private val protocolMax = PacketSocket.PROTOCOL_MAX_PACKET_SIZE

  /**
   * What one send put on the wire: the length and sequence id of every packet, and the sequence id
   * the socket is left holding for whatever is sent next.
   */
  private final case class SentPackets(headers: Vector[(Int, Byte)], nextSequenceId: Byte):
    def lengths:     Vector[Int]  = headers.map(_._1)
    def sequenceIds: Vector[Byte] = headers.map(_._2)

  /** Sends one request and reports the packets it was written as. */
  private def sendRecordingHeaders(
    limit:              Int,
    request:            RequestPacket,
    startingSequenceId: Byte = 0x00
  ): Fx[SentPackets] =
    for
      headers             <- Ref.of[Fx, Vector[(Int, Byte)]](Vector.empty)
      sequenceIdRef       <- Ref.of[Fx, Byte](startingSequenceId)
      transportFailedRef  <- Ref.of[Fx, Boolean](false)
      maxAllowedPacketRef <- Ref.of[Fx, Int](limit)
      socket = PacketSocket.fromBitVectorSocket[Fx](
                 new HeaderRecordingSocket(headers),
                 false,
                 sequenceIdRef,
                 maxAllowedPacketRef,
                 transportFailedRef
               )
      _    <- socket.send(request)
      out  <- headers.get
      next <- sequenceIdRef.get
    yield SentPackets(out, next)

  /** A body of the given size, generated on demand so a continuation-sized one costs nothing. */
  private def body(size: Int): BitVector =
    ByteVector.viewAt(i => (i % 251).toByte, size.toLong).toBitVector

  /**
   * Writes one payload through the framing and reads it straight back, over a socket that keeps the
   * bytes and nothing else.
   *
   * The offsets sampled are the ones a split can disturb: the first byte, the bytes either side of a
   * chunk boundary, and the last byte.
   */
  private def roundTrip(payloadSize: Int): Fx[(SampledPacket, List[Long])] =
    val offsets = List(0L, protocolMax.toLong - 1, protocolMax.toLong, protocolMax.toLong + 1, payloadSize.toLong - 1)
      .filter(offset => offset >= 0 && offset < payloadSize.toLong)
      .distinct
    for
      buffer              <- Ref.of[Fx, ByteVector](ByteVector.empty)
      carry               <- Ref.of[Fx, ByteVector](ByteVector.empty)
      sequenceIdRef       <- Ref.of[Fx, Byte](0x00)
      transportFailedRef  <- Ref.of[Fx, Boolean](false)
      maxAllowedPacketRef <- Ref.of[Fx, Int](protocolMax * 2 + 16)
      bvs    = BitVectorSocket.fromSocket[Fx](new LoopbackSocket(buffer), Duration.Inf, carry)
      socket = PacketSocket.fromBitVectorSocket[Fx](
                 bvs,
                 false,
                 sequenceIdRef,
                 maxAllowedPacketRef,
                 transportFailedRef
               )
      _      <- socket.send(new PatternedRequest(payloadSize))
      packet <- socket.receive(samplingDecoder(offsets))
    yield (packet, offsets)

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

  test("a request one byte past the configured limit is refused, even though it could be framed"):
    for
      harness <- socketWithLimit(PacketSocket.PROTOCOL_MAX_PACKET_SIZE)
      result  <- harness.socket.send(new PatternedRequest(PacketSocket.PROTOCOL_MAX_PACKET_SIZE + 1)).attempt
      bytes   <- harness.written.get.map(_.size.toInt)
    yield
      assert(
        result.left.exists(_.isInstanceOf[PacketTooBigException]),
        s"the limit applies to the whole message, which splitting does not exempt, but got $result"
      )
      assertEquals(bytes, 0, "nothing may reach the wire before the size is known to be acceptable")

  test("the server's limit narrows the configured one, and never widens it"):
    val configured = 16777215
    assertEquals(
      Protocol.effectivePacketLimit(configured, Map("max_allowed_packet" -> "67108864")),
      16777215,
      "a server that accepts more than the caller configured does not lift the caller's ceiling"
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

  test("a configuration beyond what a packet header can describe is left alone"):
    assertEquals(
      Protocol.effectivePacketLimit(Int.MaxValue, Map.empty),
      Int.MaxValue,
      "a message past the protocol maximum is carried by several packets, so framing is no reason to narrow"
    )
    assertEquals(
      Protocol.effectivePacketLimit(MySQLConfig.MAX_PACKET_SIZE, Map("max_allowed_packet" -> "67108864")),
      67108864,
      "what the server accepts still decides"
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

  test("a payload at exactly the protocol maximum is sent as a full packet followed by an empty one"):
    for sent <- sendRecordingHeaders(protocolMax, new PatternedRequest(protocolMax))
    yield assertEquals(
      sent.lengths,
      Vector(protocolMax, 0),
      "a full-sized last packet reads as a continuation, so an empty one has to close the message"
    )

  test("a payload that is a multiple of the protocol maximum ends with an empty packet"):
    val size = protocolMax * 2
    for sent <- sendRecordingHeaders(size, new PatternedRequest(size))
    yield assertEquals(sent.lengths, Vector(protocolMax, protocolMax, 0))

  test("a payload one byte over the protocol maximum becomes two packets"):
    val size = protocolMax + 1
    for sent <- sendRecordingHeaders(size, new PatternedRequest(size))
    yield assertEquals(sent.lengths, Vector(protocolMax, 1), "the remainder belongs in a packet of its own")

  test("the sequence id advances by one across every packet of a split message"):
    val size = protocolMax * 2
    for sent <- sendRecordingHeaders(size, new PatternedRequest(size))
    yield assertEquals(sent.sequenceIds, Vector[Byte](0, 1, 2))

  test("a payload under the protocol maximum is still written as one packet"):
    for sent <- sendRecordingHeaders(1024, new PatternedRequest(16))
    yield assertEquals(sent.lengths, Vector(16), "splitting must not change what a small message looks like")

  test("an empty payload is written as a single packet of length zero"):
    for sent <- sendRecordingHeaders(1024, new PatternedRequest(0))
    yield assertEquals(sent.lengths, Vector(0))

  test("a message split across two packets is reassembled into one"):
    val tailSize = 10
    for
      harness <- socketWithLimit(
                   protocolMax * 2,
                   List(header(protocolMax, 0), body(protocolMax), header(tailSize, 1), body(tailSize))
                 )
      result <- harness.socket.receive(measuringDecoder).attempt
      failed <- harness.transportFailed.get
    yield
      val packet = result.getOrElse(fail(s"a split message must be put back together, but got $result"))
      assertEquals(packet.size, protocolMax.toLong + tailSize)
      assertEquals(failed, false, "reassembly leaves the stream on a packet boundary")

  test("a message whose size is a multiple of the maximum ends at the empty packet"):
    for
      harness <- socketWithLimit(
                   protocolMax * 2,
                   List(header(protocolMax, 0), body(protocolMax), header(0, 1), BitVector.empty)
                 )
      result <- harness.socket.receive(measuringDecoder).attempt
    yield
      val packet = result.getOrElse(fail(s"an empty packet closes the message, but got $result"))
      assertEquals(packet.size, protocolMax.toLong)

  test("a continuation whose sequence id does not follow is refused"):
    for
      harness <- socketWithLimit(
                   protocolMax * 2,
                   List(header(protocolMax, 0), body(protocolMax), header(10, 5), body(10))
                 )
      result <- harness.socket.receive(measuringDecoder).attempt
      failed <- harness.transportFailed.get
    yield
      val error = result.left.getOrElse(fail("a sequence id that skips must be refused, not concatenated"))
      assert(error.getMessage.contains("out of order"), s"expected an ordering error, got $error")
      assertEquals(failed, true, "the headers are consumed, so the stream can no longer be resynchronised")

  test("a message that grows past the limit while being reassembled is refused"):
    val limit = 20000000
    for
      harness <- socketWithLimit(
                   limit,
                   List(header(protocolMax, 0), body(protocolMax), header(protocolMax, 1))
                 )
      result <- harness.socket.receive(measuringDecoder).attempt
      failed <- harness.transportFailed.get
      left   <- harness.chunks.get
    yield
      val error = result.left.getOrElse(fail("the running total has to be checked, not each packet alone"))
      assert(error.isInstanceOf[PacketTooBigException], s"expected a size error, got $error")
      assertEquals(failed, true)
      assertEquals(left.size, 0, "the second body must not have been read once the total was known to be too large")

  test("a single packet past the limit is refused before any continuation is considered"):
    for
      harness <- socketWithLimit(1024, List(header(protocolMax, 0)))
      result  <- harness.socket.receive(measuringDecoder).attempt
      failed  <- harness.transportFailed.get
    yield
      val error = result.left.getOrElse(fail("a first packet past the limit must be refused"))
      assert(error.isInstanceOf[PacketTooBigException], s"expected a size error, got $error")
      assertEquals(failed, true)

  test("a read that fails part-way through reassembly is a transport failure"):
    for
      harness <- socketWithLimit(protocolMax * 2, List(header(protocolMax, 0), body(protocolMax)))
      result  <- harness.socket.receive(measuringDecoder).attempt
      failed  <- harness.transportFailed.get
    yield
      assert(result.isLeft, s"the script ends mid-message, so the read must fail, but got $result")
      assertEquals(failed, true)

  test("a message split across three packets is reassembled into one"):
    val tailSize = 10
    for
      harness <- socketWithLimit(
                   protocolMax * 3,
                   List(
                     header(protocolMax, 0),
                     body(protocolMax),
                     header(protocolMax, 1),
                     body(protocolMax),
                     header(tailSize, 2),
                     body(tailSize)
                   )
                 )
      result <- harness.socket.receive(measuringDecoder).attempt
      failed <- harness.transportFailed.get
    yield
      val packet = result.getOrElse(fail(s"a chain longer than two packets must be followed, but got $result"))
      assertEquals(packet.size, protocolMax.toLong * 2 + tailSize)
      assertEquals(failed, false)

  test("a sequence id that skips on the third packet is refused"):
    for
      harness <- socketWithLimit(
                   protocolMax * 3,
                   List(
                     header(protocolMax, 0),
                     body(protocolMax),
                     header(protocolMax, 1),
                     body(protocolMax),
                     header(10, 7),
                     body(10)
                   )
                 )
      result <- harness.socket.receive(measuringDecoder).attempt
    yield
      val error = result.left.getOrElse(fail("the expected id has to advance with every continuation"))
      assert(error.getMessage.contains("out of order"), s"expected an ordering error, got $error")

  test("a reassembled message of exactly the limit is accepted"):
    val tailSize = 10
    val limit    = protocolMax + tailSize
    for
      harness <- socketWithLimit(
                   limit,
                   List(header(protocolMax, 0), body(protocolMax), header(tailSize, 1), body(tailSize))
                 )
      result <- harness.socket.receive(measuringDecoder).attempt
    yield
      val packet = result.getOrElse(fail(s"a total equal to the limit is within it, but got $result"))
      assertEquals(packet.size, limit.toLong)

  test("a reassembled message one byte past the limit is refused"):
    val limit = protocolMax + 10
    for
      harness <- socketWithLimit(
                   limit,
                   List(header(protocolMax, 0), body(protocolMax), header(11, 1), body(11))
                 )
      result <- harness.socket.receive(measuringDecoder).attempt
    yield
      val error = result.left.getOrElse(fail("one byte past the limit must be refused"))
      assert(error.isInstanceOf[PacketTooBigException], s"expected a size error, got $error")

  test("a write that fails part-way through a split is a transport failure"):
    val size = protocolMax * 2
    for
      writes              <- Ref.of[Fx, Int](0)
      sequenceIdRef       <- Ref.of[Fx, Byte](0x00)
      transportFailedRef  <- Ref.of[Fx, Boolean](false)
      maxAllowedPacketRef <- Ref.of[Fx, Int](size)
      socket = PacketSocket.fromBitVectorSocket[Fx](
                 new FailingAtWriteSocket(2, writes),
                 false,
                 sequenceIdRef,
                 maxAllowedPacketRef,
                 transportFailedRef
               )
      result <- socket.send(new PatternedRequest(size)).attempt
      count  <- writes.get
      failed <- transportFailedRef.get
    yield
      assert(result.isLeft, s"the second packet could not be written, so the send must fail, but got $result")
      assertEquals(count, 2, "the first packet had already reached the wire")
      assertEquals(failed, true, "an unfinished run of packets leaves the peer unable to resynchronise")

  test("the sequence id left for the next message follows the last packet of a split send"):
    val size = protocolMax * 2
    for sent <- sendRecordingHeaders(size, new PatternedRequest(size))
    yield
      assertEquals(sent.sequenceIds, Vector[Byte](0, 1, 2))
      assertEquals(sent.nextSequenceId, 3.toByte, "the next message must continue from where this one ended")

  test("the sequence id left for the next message follows the last packet of a reassembly"):
    for
      harness <- socketWithLimit(
                   protocolMax * 2,
                   List(header(protocolMax, 0), body(protocolMax), header(10, 1), body(10))
                 )
      _    <- harness.socket.receive(measuringDecoder)
      next <- harness.sequenceId.get
    yield assertEquals(next, 2.toByte, "the id has to advance past every packet of the message, not just the first")

  test("the sequence id wraps at a byte across a split send"):
    val size = protocolMax * 2
    for sent <- sendRecordingHeaders(size, new PatternedRequest(size), 254.toByte)
    yield
      assertEquals(
        sent.sequenceIds,
        Vector[Byte](254.toByte, 255.toByte, 0),
        "the protocol numbers packets in a single byte, so 255 is followed by 0"
      )
      assertEquals(sent.nextSequenceId, 1.toByte)

  test("a reassembly whose sequence id wraps at a byte is accepted"):
    for
      harness <- socketWithLimit(
                   protocolMax * 2,
                   List(header(protocolMax, 255.toByte), body(protocolMax), header(10, 0), body(10))
                 )
      result <- harness.socket.receive(measuringDecoder).attempt
      next   <- harness.sequenceId.get
    yield
      val packet = result.getOrElse(fail(s"0 is what follows 255, not a break in the order, but got $result"))
      assertEquals(packet.size, protocolMax.toLong + 10)
      assertEquals(next, 1.toByte)

  test("a message written to the wire reads back byte for byte"):
    val sizes = List(protocolMax - 1, protocolMax, protocolMax + 1, protocolMax * 2)
    sizes.foldLeft(Fx.pure(())) { (previous, size) =>
      previous.flatMap(_ => roundTrip(size)).map { (packet, offsets) =>
        assertEquals(packet.size, size.toLong, s"bytes were lost round-tripping a payload of $size")
        assertEquals(
          packet.samples,
          offsets.map(offset => (offset % 251).toByte),
          s"the bytes around a packet boundary moved round-tripping a payload of $size"
        )
      }
    }

  test("a payload one byte under the protocol maximum is an ordinary message"):
    val size    = PacketSocket.PROTOCOL_MAX_PACKET_SIZE - 1
    val payload = okPayload ++ BitVector.fill((size - okPayload.bytes.size.toInt).toLong * 8)(false)
    for
      harness <- socketWithLimit(PacketSocket.PROTOCOL_MAX_PACKET_SIZE, List(header(size, 0), payload))
      result  <- harness.socket.receive(okDecoder).attempt
      failed  <- harness.transportFailed.get
    yield
      assert(result.isRight, s"one byte under the limit carries no continuation, but got $result")
      assertEquals(failed, false)
