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

object PacketSocket:

  val DEFAULT_MAX_PACKET_SIZE  = 65535
  val PROTOCOL_MAX_PACKET_SIZE = 16777215
  val MIN_PACKET_SIZE          = 0

  /**
   * Wraps a [[BitVectorSocket]] as a [[PacketSocket]].
   *
   * @param bvs              the underlying bit-vector socket
   * @param debugEnabled     whether to log each packet
   * @param sequenceIdRef    the MySQL packet sequence id
   * @param maxAllowedPacket the maximum accepted payload size
   */
  def fromBitVectorSocket[F[_]](
    bvs:              BitVectorSocket[F],
    debugEnabled:     Boolean,
    sequenceIdRef:    Ref[F, Byte],
    maxAllowedPacket: Int
  )(using F: Concurrent[F]): PacketSocket[F] = new PacketSocket[F]:

    private def debug(msg: => String): F[Unit] =
      F.whenA(debugEnabled) {
        sequenceIdRef.get.flatMap(id => F.delay(println(s"[$id] $msg")))
      }

    override def receive[P <: ResponsePacket](decoder: Decoder[P]): F[P] =
      (for
        header <- bvs.read(4)
        payloadSize = parseHeader(header.toByteArray)
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
      }

    private def buildRequest(request: RequestPacket): F[BitVector] =
      sequenceIdRef.get.map { sequenceId =>
        val bits        = request.encode
        val payloadSize = bits.toByteArray.length
        val header      = Array[Byte](
          payloadSize.toByte,
          ((payloadSize >> 8) & 0xff).toByte,
          ((payloadSize >> 16) & 0xff).toByte,
          sequenceId
        )
        ByteVector(header).toBitVector ++ bits
      }

    override def send(request: RequestPacket): F[Unit] =
      for
        bits <- buildRequest(request)
        _    <-
          debug(
            s"Client ${ AnsiColor.BLUE }→${ AnsiColor.RESET } Server: ${ AnsiColor.YELLOW }$request${ AnsiColor.RESET }"
          )
        _ <- bvs.write(bits)
        _ <- sequenceIdRef.update(sequenceId => ((sequenceId + 1) % 256).toByte)
      yield ()

    private def validatePacketSize(size: Int): F[Unit] =
      if size < MIN_PACKET_SIZE then F.raiseError(PacketTooBigException(size, maxAllowedPacket))
      else if size > maxAllowedPacket then F.raiseError(PacketTooBigException(size, maxAllowedPacket))
      else F.unit

  /**
   * Builds a [[PacketSocket]] over a connected socket.
   *
   * @param debug             whether to log each packet
   * @param sockets           the socket resource
   * @param sslOptions        TLS negotiation options, if TLS is requested
   * @param sequenceIdRef     the MySQL packet sequence id
   * @param initialPacketRef  receives the server's initial packet
   * @param readTimeout       the per-read timeout
   * @param capabilitiesFlags the negotiated capability flags
   * @param maxAllowedPacket  the maximum accepted payload size
   */
  def apply[F[_]](
    debug:             Boolean,
    sockets:           Resource[F, Socket[F]],
    sslOptions:        Option[SSLNegotiation.Options[F]],
    sequenceIdRef:     Ref[F, Byte],
    initialPacketRef:  Ref[F, Option[InitialPacket]],
    readTimeout:       Duration,
    capabilitiesFlags: Set[CapabilitiesFlags],
    maxAllowedPacket:  Int
  )(using F: Concurrent[F], tls: TlsUpgrade[F]): Resource[F, PacketSocket[F]] =
    BitVectorSocket(sockets, sequenceIdRef, initialPacketRef, sslOptions, readTimeout, capabilitiesFlags).map(
      fromBitVectorSocket(_, debug, sequenceIdRef, maxAllowedPacket)
    )
