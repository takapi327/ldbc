/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import scodec.bits.{ BitVector, ByteVector }

import ldbc.effect.{ Deferred, Ref }
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.data.CapabilitiesFlags
import ldbc.mysql.exception.EofException
import ldbc.mysql.net.packet.request.ComPingPacket
import ldbc.mysql.net.packet.response.{ GenericResponsePackets, OKPacket }
import ldbc.mysql.FTestPlatform

class TransportFailureTest extends FTestPlatform:

  private final class ScriptedBitVectorSocket(chunks: Ref[Fx, List[BitVector]]) extends BitVectorSocket[Fx]:
    override def write(bits: BitVector): Fx[Unit]      = Fx.unit
    override def read(nBytes: Int):      Fx[BitVector] =
      chunks
        .modify {
          case head :: tail => (tail, Some(head))
          case Nil          => (Nil, None)
        }
        .flatMap {
          case Some(bits) => Fx.pure(bits)
          case None       => Fx.raiseError(EofException(nBytes, 0))
        }

  private final class FailingBitVectorSocket(error: Throwable) extends BitVectorSocket[Fx]:
    override def write(bits:  BitVector): Fx[Unit]      = Fx.raiseError(error)
    override def read(nBytes: Int):       Fx[BitVector] = Fx.raiseError(error)

  private final class NeverRespondingBitVectorSocket(entered: Deferred[Fx, Unit]) extends BitVectorSocket[Fx]:
    override def write(bits: BitVector): Fx[Unit]      = Fx.unit
    override def read(nBytes: Int):      Fx[BitVector] =
      entered.complete(()) *> Fx.async[BitVector](_ => Fx.Canceler.noop)

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

  private def socketOver(bvs: BitVectorSocket[Fx]): Fx[(PacketSocket[Fx], Ref[Fx, Boolean])] =
    for
      sequenceIdRef      <- Ref.of[Fx, Byte](0x00)
      transportFailedRef <- Ref.of[Fx, Boolean](false)
    yield (
      PacketSocket.fromBitVectorSocket[Fx](bvs, false, sequenceIdRef, 65535, transportFailedRef),
      transportFailedRef
    )

  private val decoder = GenericResponsePackets.decoder(Set.empty[CapabilitiesFlags])

  test("a read error marks the transport as failed"):
    val program = for
      pair   <- socketOver(new FailingBitVectorSocket(EofException(4, 0)))
      before <- pair._2.get
      _      <- pair._1.receive(decoder).attempt
      after  <- pair._2.get
    yield (before, after)

    assertFx(program, (false, true))

  test("a write error marks the transport as failed"):
    val program = for
      pair   <- socketOver(new FailingBitVectorSocket(new java.io.IOException("broken pipe")))
      _      <- pair._1.send(ComPingPacket()).attempt
      failed <- pair._2.get
    yield failed

    assertFx(program, true)

  test("a header claiming more than maxAllowedPacket marks the transport as failed"):
    val program = for
      chunks <- Ref.of[Fx, List[BitVector]](List(header(70000, 0)))
      pair   <- socketOver(new ScriptedBitVectorSocket(chunks))
      _      <- pair._1.receive(decoder).attempt
      failed <- pair._2.get
    yield failed

    assertFx(program, true, "PacketTooBigException was not recorded as a transport failure")

  test("a payload the decoder rejects marks the transport as failed"):
    val garbage = ByteVector(Array[Byte](0x7f, 0x01, 0x02)).toBitVector
    val program = for
      chunks <- Ref.of[Fx, List[BitVector]](List(header(3, 0), garbage))
      pair   <- socketOver(new ScriptedBitVectorSocket(chunks))
      _      <- pair._1.receive(decoder).attempt
      failed <- pair._2.get
    yield failed

    assertFx(program, true, "a decode failure was not recorded as a transport failure")

  test("cancelling a receive marks the transport as failed"):
    val program = for
      entered <- Deferred[Fx, Unit]
      pair    <- socketOver(new NeverRespondingBitVectorSocket(entered))
      fiber   <- pair._1.receive(decoder).start
      _       <- entered.get
      _       <- fiber.cancel
      failed  <- pair._2.get
    yield failed

    assertFx(program, true, "a cancelled receive was not recorded as a transport failure")

  test("a packet that decodes cleanly leaves the transport usable"):
    val program = for
      chunks   <- Ref.of[Fx, List[BitVector]](List(header(7, 0), okPayload))
      pair     <- socketOver(new ScriptedBitVectorSocket(chunks))
      received <- pair._1.receive(decoder)
      failed   <- pair._2.get
    yield (received.isInstanceOf[OKPacket], failed)

    assertFx(program, (true, false), "a successfully decoded packet was recorded as a transport failure")

  test("a successful send leaves the transport usable"):
    val program = for
      chunks <- Ref.of[Fx, List[BitVector]](List.empty)
      pair   <- socketOver(new ScriptedBitVectorSocket(chunks))
      _      <- pair._1.send(ComPingPacket())
      failed <- pair._2.get
    yield failed

    assertFx(program, false)
