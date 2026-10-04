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

import ldbc.sql.SQLFeatureNotSupportedException

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
 */
trait PacketSocket[F[_]]:

  /**
   * Receives the next `ResponsePacket`, failing if end-of-stream is reached before a complete message
   * arrives.
   */
  def receive[P <: ResponsePacket](decoder: Decoder[P]): F[P]

  /** Sends the specified request packet. */
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

  /** The largest payload a single packet can describe: the header carries the length in 3 bytes. */
  val PROTOCOL_MAX_PACKET_SIZE = 16777215

  val MIN_PACKET_SIZE = 0

  /**
   * Wraps a [[BitVectorSocket]] as a [[PacketSocket]].
   *
   * @param bvs                 the underlying bit-vector socket
   * @param debugEnabled        whether to log each packet
   * @param sequenceIdRef       the MySQL packet sequence id
   * @param maxAllowedPacketRef the largest payload either side may put in one packet. Mutable because
   *                            the server's own limit is only knowable once the session is up, and
   *                            adopting it is what keeps a send from being rejected by the peer
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

    override def receive[P <: ResponsePacket](decoder: Decoder[P]): F[P] =
      guard((for
        header <- bvs.read(4)
        payloadSize = parseHeader(header.toByteArray)
        _       <- rejectContinuedPayload(payloadSize)
        _       <- validatePacketSize(payloadSize)
        payload <- bvs.read(payloadSize)
        response = decoder.decodeValue(payload).require
        _ <-
          debug(
            s"Client ${ AnsiColor.BLUE }←${ AnsiColor.RESET } Server: ${ AnsiColor.GREEN }$response${ AnsiColor.RESET }"
          )
        _ <- sequenceIdRef.update(_ => ((header.toByteArray(3) + 1) % 256).toByte)
      yield response).onError {
        case t =>
          debug(
            s"Client ${ AnsiColor.BLUE }←${ AnsiColor.RESET } Server: ${ AnsiColor.RED }${ t.getMessage }${ AnsiColor.RESET }"
          )
      })

    private def buildRequest(payload: BitVector): F[BitVector] =
      sequenceIdRef.get.map { sequenceId =>
        val bits        = payload
        val payloadSize = bits.toByteArray.length
        val header      = Array[Byte](
          payloadSize.toByte,
          ((payloadSize >> 8) & 0xff).toByte,
          ((payloadSize >> 16) & 0xff).toByte,
          sequenceId
        )
        ByteVector(header).toBitVector ++ bits
      }

    /**
     * Sends a request, refusing one that is too large before any of it reaches the wire.
     *
     * The size check sits outside [[guard]] on purpose. A refused send writes nothing, so the byte
     * stream is still positioned at a packet boundary and the connection remains usable; recording a
     * transport failure here would discard a healthy connection over a single oversized statement.
     *
     * Without the check the length would simply be truncated to its low 3 bytes, and the peer would
     * read the overflow as the packets that follow. That desynchronises the stream in a way neither
     * side can detect, which surfaces much later as a lost connection rather than as the oversized
     * request it was.
     */
    override def send(request: RequestPacket): F[Unit] =
      val payload = request.encode
      validatePacketSize((payload.size + 7) / 8) *>
        guard(for
          bits <- buildRequest(payload)
          _    <-
            debug(
              s"Client ${ AnsiColor.BLUE }→${ AnsiColor.RESET } Server: ${ AnsiColor.YELLOW }$request${ AnsiColor.RESET }"
            )
          _ <- bvs.write(bits)
          _ <- sequenceIdRef.update(sequenceId => ((sequenceId + 1) % 256).toByte)
        yield ())

    /**
     * Rejects a packet the connection cannot carry.
     *
     * The size is taken as a `Long` because an outgoing payload is measured before it is framed, and
     * one over two gigabytes would wrap to a negative `Int` — reporting a nonsensical size for what
     * is plainly an oversized request. Sizes that large are clamped for reporting so the message
     * still names a number the reader can act on.
     *
     * The caller derives that length from the bit count rather than from `bytes`, which would
     * compact the whole payload into contiguous storage first. Beyond two gigabytes scodec refuses
     * to compact at all, so measuring that way would fail with an error about bit vectors instead of
     * the size limit that is actually being exceeded.
     */
    /**
     * Refuses a message the server has split across several packets.
     *
     * A payload of exactly [[PROTOCOL_MAX_PACKET_SIZE]] is the protocol's way of saying "there is
     * more": the packets that follow belong to the same message and are meant to be concatenated.
     * Reading only the first one yields a truncated message whose remainder stays in the stream, so
     * the row decodes to nothing and the bytes left behind are read as the reply to whatever is
     * asked next. Both failures surface far from their cause.
     *
     * The check is a size comparison because that is the whole of the protocol's signal — there is
     * no flag to consult. Note that it is an equality, not the "greater than" of
     * [[validatePacketSize]]: a payload at exactly the limit is the one case that means something
     * other than what it appears to.
     *
     * Unlike the size check on the way out, this one happens with the header already consumed, so
     * the stream is no longer on a packet boundary. Raising from inside the recording path is
     * therefore correct: this connection cannot be used again.
     */
    private def rejectContinuedPayload(payloadSize: Int): F[Unit] =
      F.whenA(payloadSize == PROTOCOL_MAX_PACKET_SIZE) {
        F.raiseError(
          new SQLFeatureNotSupportedException(
            message = s"The server split this response across multiple packets, which is not supported.",
            detail  = Some(
              s"A payload of exactly $PROTOCOL_MAX_PACKET_SIZE bytes means the message continues in the " +
                "packets that follow. Reassembling them is not implemented, so the connection is discarded " +
                "rather than reporting a truncated result."
            ),
            hint   = Some("Reduce the size of the value being read, for example by selecting a substring of it."),
            vendor = "MySQL"
          )
        )
      }

    private def validatePacketSize(size: Long): F[Unit] =
      maxAllowedPacketRef.get.flatMap { maxAllowedPacket =>
        if size < MIN_PACKET_SIZE || size > maxAllowedPacket.toLong then
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
   * @param maxAllowedPacketRef the largest payload either side may put in one packet
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
