/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import scala.concurrent.duration.Duration
import scala.io.AnsiColor

import scodec.bits.{ BitVector, ByteVector }
import scodec.Decoder

import ldbc.sql.SQLException

import ldbc.effect.{ Concurrent, Ref, Resource }
import ldbc.effect.syntax.*
import ldbc.mysql.data.CapabilitiesFlags
import ldbc.mysql.exception.PacketTooBigException
import ldbc.mysql.net.packet.*
import ldbc.mysql.net.packet.response.InitialPacket
import ldbc.mysql.net.protocol.parseHeader
import ldbc.net.{ Socket, TlsUpgrade }

/**
 * A higher-level [[BitVectorSocket]] that speaks in terms of `Packet`, framing each message with the
 * MySQL 4-byte header.
 *
 * A message too large for one packet is carried by several, and this is the layer that hides that:
 * callers hand over and receive whole messages, never the packets they travelled in.
 */
trait PacketSocket[F[_]]:

  /**
   * Receives the next `ResponsePacket`, following the continuation packets if the server split the
   * message across several, and failing if end-of-stream is reached before a complete message
   * arrives.
   */
  def receive[P <: ResponsePacket](decoder: Decoder[P]): F[P]

  /** Sends the specified request packet, splitting it across packets if it is too large for one. */
  def send(request: RequestPacket): F[Unit]

  /**
   * Whether the byte stream under this socket is no longer positioned at a packet boundary, because a
   * read or write escaped by error or by cancellation part-way through one.
   *
   * MySQL offers no way to resynchronise, so once this is true the session can only be discarded; it
   * never goes back to false. Reporting it is all this socket does — refusing further calls is left to
   * the layers above, which translate it into "this connection is closed".
   *
   * A server-reported `ERR_Packet` is not a transport failure: it decodes successfully and is returned
   * as a value, so it never reaches the recording path.
   */
  def transportFailed: F[Boolean]

object PacketSocket:

  /**
   * The largest payload a single packet can describe: the header carries the length in 3 bytes. This
   * is the step size messages are split into, not a limit on how large a message may be.
   */
  val PROTOCOL_MAX_PACKET_SIZE = 16777215

  /**
   * Wraps a [[BitVectorSocket]] as a [[PacketSocket]].
   *
   * @param bvs                 the underlying bit-vector socket
   * @param debugEnabled        whether to log each packet
   * @param sequenceIdRef       the MySQL packet sequence id
   * @param maxAllowedPacketRef the largest message either side may send, counted across every packet
   *                            it is split into. Mutable because the server's own limit is only
   *                            knowable once the session is up, and adopting it is what keeps a send
   *                            from being rejected by the peer
   * @param transportFailedRef  records whether the byte stream position has been lost
   */
  def fromBitVectorSocket[F[_]](
    bvs:                 BitVectorSocket[F],
    debugEnabled:        Boolean,
    sequenceIdRef:       Ref[F, Byte],
    maxAllowedPacketRef: Ref[F, Int],
    transportFailedRef:  Ref[F, Boolean]
  )(using F: Concurrent[F]): PacketSocket[F] = new PacketSocket[F]:

    override def transportFailed: F[Boolean] = transportFailedRef.get

    /**
     * Records a transport failure for anything that leaves a packet half-read or half-written.
     * `onCancel` is currently unreachable — commands run inside `Exchange`'s `uncancelable`, and a
     * `readTimeout` arrives here as an error — but it keeps the branch from silently reopening if
     * cancellation ever becomes reachable.
     */
    private def guard[A](fa: F[A]): F[A] =
      F.onCancel(fa.onError { case _ => transportFailedRef.set(true) })(transportFailedRef.set(true))

    private def debug(msg: => String): F[Unit] =
      F.whenA(debugEnabled) {
        sequenceIdRef.get.flatMap(id => F.delay(println(s"[$id] $msg")))
      }

    /** The next sequence id, wrapping at a byte like the protocol does. */
    private def nextSequenceId(current: Byte): Byte = ((current + 1) % 256).toByte

    /** One packet: the 3-byte length, the sequence id, then the body. */
    private def framed(body: BitVector, length: Int, sequenceId: Byte): BitVector =
      val header = Array[Byte](
        length.toByte,
        ((length >> 8) & 0xff).toByte,
        ((length >> 16) & 0xff).toByte,
        sequenceId
      )
      ByteVector(header).toBitVector ++ body

    /**
     * Reads one logical message, following the chain of packets it may be split into.
     *
     * A payload of exactly [[PROTOCOL_MAX_PACKET_SIZE]] is the protocol's way of saying "there is
     * more": the packets that follow belong to the same message. The chain ends with the first packet
     * shorter than that, which is why a message whose size is an exact multiple of the maximum ends
     * with an empty one.
     *
     * Nothing bounds how long that chain may be, so the running total is checked against the limit
     * before each body is read. That check is what makes this loop terminate against a peer that
     * keeps sending maximum-sized packets.
     */
    private def readFramedPayload: F[BitVector] =
      def loop(acc: BitVector, accSize: Long, expected: Option[Byte]): F[BitVector] =
        bvs.read(4).flatMap { header =>
          val head        = header.toByteArray
          val payloadSize = parseHeader(head)
          val sequenceId  = head(3)
          val total       = accSize + payloadSize.toLong
          verifySequence(expected, sequenceId) *>
            validateMessageSize(total) *>
            bvs.read(payloadSize).flatMap { body =>
              sequenceIdRef.set(nextSequenceId(sequenceId)) *>
                (if payloadSize == PROTOCOL_MAX_PACKET_SIZE then
                   loop(acc ++ body, total, Some(nextSequenceId(sequenceId)))
                 else F.pure(acc ++ body))
            }
        }
      loop(BitVector.empty, 0L, None)

    override def receive[P <: ResponsePacket](decoder: Decoder[P]): F[P] =
      guard(
        readFramedPayload
          .flatMap(payload => F.delay(decoder.decodeValue(payload).require))
          .flatTap { response =>
            debug(
              s"Client ${ AnsiColor.BLUE }←${ AnsiColor.RESET } Server: ${ AnsiColor.GREEN }$response${ AnsiColor.RESET }"
            )
          }
          .onError {
            case t =>
              debug(
                s"Client ${ AnsiColor.BLUE }←${ AnsiColor.RESET } Server: ${ AnsiColor.RED }${ t.getMessage }${ AnsiColor.RESET }"
              )
          }
      )

    /**
     * Writes a payload as however many packets the protocol needs to carry it.
     *
     * A packet header describes its length in three bytes, so a payload at or above that is sent as a
     * run of maximum-sized packets. The receiver uses the size itself as the signal that more is
     * coming, which is why a payload that happens to be an exact multiple needs an empty packet at
     * the end: without it the last full-sized packet would read as "there is more" and the peer would
     * wait for a continuation that never arrives. A payload that is already short enough takes this
     * same path and comes out as the single packet it always was.
     *
     * Chunks are cut from the `BitVector` rather than from `bytes`. Slicing a bit vector walks its
     * structure without flattening it; asking for `bytes` would copy the whole payload into
     * contiguous storage, and doing that once per chunk would copy a one-gigabyte message sixty-four
     * times. Flattening is left to `write`, which does it to the chunk it is given and nothing more.
     *
     * The sequence id travels through the loop as its return value so the ref is written once,
     * however many packets it took.
     */
    private def writeChunked(payload: BitVector): F[Unit] =
      val totalBytes = (payload.size + 7) / 8

      def loop(offset: Long, sequenceId: Byte): F[Byte] =
        val chunkLen = Math.min(totalBytes - offset, PROTOCOL_MAX_PACKET_SIZE.toLong)
        val chunk    = payload.slice(offset * 8, (offset + chunkLen) * 8)
        bvs.write(framed(chunk, chunkLen.toInt, sequenceId)).flatMap { _ =>
          val next = nextSequenceId(sequenceId)
          if chunkLen == PROTOCOL_MAX_PACKET_SIZE.toLong then loop(offset + chunkLen, next)
          else F.pure(next)
        }

      sequenceIdRef.get.flatMap(loop(0L, _)).flatMap(sequenceIdRef.set)

    /**
     * Sends a request, refusing one that is too large before any of it reaches the wire.
     *
     * The size check sits outside [[guard]] on purpose. A refused send writes nothing, so the byte
     * stream is still positioned at a packet boundary and the connection remains usable; recording a
     * transport failure here would discard a healthy connection over a single oversized statement.
     *
     * The write itself is inside [[guard]] because splitting takes several steps: a failure part-way
     * through leaves the peer holding an unfinished run of packets, which no amount of retrying can
     * resynchronise.
     */
    override def send(request: RequestPacket): F[Unit] =
      val payload = request.encode
      validateMessageSize((payload.size + 7) / 8) *>
        guard(
          debug(
            s"Client ${ AnsiColor.BLUE }→${ AnsiColor.RESET } Server: ${ AnsiColor.YELLOW }$request${ AnsiColor.RESET }"
          ) *> writeChunked(payload)
        )

    /**
     * Checks that a continuation carries the sequence id that must follow.
     *
     * Only the packets of one message are checked against each other; the first one is not, because
     * what precedes it depends on the command and is tracked separately. Reassembly otherwise has
     * nothing but the payload size to go on, so a run of packets that arrived out of order or lost
     * one in the middle would be concatenated without complaint. The sequence id is the only
     * corroboration available.
     */
    private def verifySequence(expected: Option[Byte], actual: Byte): F[Unit] =
      expected match
        case Some(want) if want != actual =>
          F.raiseError(
            new SQLException(
              "The packets of this response arrived out of order",
              detail = Some(
                s"A message split across packets numbers them consecutively; this one carried sequence id " +
                  s"$actual where $want was due, so the reassembled message would not be what the server sent."
              ),
              hint = Some(
                "Report Issues here: https://github.com/takapi327/ldbc/issues/new?assignees=&labels=&projects=&template=bug_report.md&title="
              )
            )
          )
        case _ => F.unit

    /**
     * Refuses a message larger than this connection agreed to carry.
     *
     * The limit applies to the whole message, not to a packet. A single packet can only ever be
     * [[PROTOCOL_MAX_PACKET_SIZE]], so checking each one in isolation would let a message of any size
     * through as long as it arrived in small enough pieces; on the way in the running total is what
     * gets checked, and on the way out the length of the payload before it is split.
     *
     * The size is taken as a `Long` because an outgoing payload is measured before it is framed, and
     * one over two gigabytes would wrap to a negative `Int` — reporting a nonsensical size for what
     * is plainly an oversized request. Sizes that large are clamped for reporting so the message
     * still names a number the reader can act on. There is no lower bound to check: a payload cannot
     * be of negative length, and a header describes a length between zero and the protocol maximum.
     *
     * The caller derives that length from the bit count rather than from `bytes`, which would
     * compact the whole payload into contiguous storage first. Beyond two gigabytes scodec refuses
     * to compact at all, so measuring that way would fail with an error about bit vectors instead of
     * the size limit that is actually being exceeded.
     */
    private def validateMessageSize(size: Long): F[Unit] =
      maxAllowedPacketRef.get.flatMap { maxAllowedPacket =>
        if size > maxAllowedPacket.toLong then
          F.raiseError(PacketTooBigException(Math.min(size, Int.MaxValue.toLong).toInt, maxAllowedPacket))
        else F.unit
      }

  /**
   * Builds a [[PacketSocket]] over a connected socket.
   *
   * @param debug             whether to log each packet
   * @param sockets           the socket resource
   * @param sslOptions        TLS negotiation options, if TLS is requested
   * @param sequenceIdRef     the MySQL packet sequence id
   * @param initialPacketRef  receives the server's initial packet
   * @param readTimeout       the per-read timeout
   * @param capabilitiesFlags   the negotiated capability flags
   * @param maxAllowedPacketRef the largest message either side may send, counted across every packet
   *                            it is split into
   * @param transportFailedRef  records whether the byte stream position has been lost
   */
  def apply[F[_]](
    debug:               Boolean,
    sockets:             Resource[F, Socket[F]],
    sslOptions:          Option[SSLNegotiation.Options[F]],
    sequenceIdRef:       Ref[F, Byte],
    initialPacketRef:    Ref[F, Option[InitialPacket]],
    readTimeout:         Duration,
    capabilitiesFlags:   Set[CapabilitiesFlags],
    maxAllowedPacketRef: Ref[F, Int],
    transportFailedRef:  Ref[F, Boolean]
  )(using F: Concurrent[F], tls: TlsUpgrade[F]): Resource[F, PacketSocket[F]] =
    BitVectorSocket(sockets, sequenceIdRef, initialPacketRef, sslOptions, readTimeout, capabilitiesFlags).map(
      fromBitVectorSocket(_, debug, sequenceIdRef, maxAllowedPacketRef, transportFailedRef)
    )
