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

import ldbc.fx.{ Fx, Ref, Resource }
import ldbc.fx.syntax.*
import ldbc.mysql.data.CapabilitiesFlags
import ldbc.mysql.exception.PacketTooBigException
import ldbc.mysql.net.packet.*
import ldbc.mysql.net.packet.response.InitialPacket
import ldbc.mysql.net.protocol.parseHeader
import ldbc.net.Socket

/**
 * A higher-level [[BitVectorSocket]] that speaks in terms of `Packet`, framing each message with the
 * MySQL 4-byte header.
 */
trait PacketSocket:

  /**
   * Receives the next `ResponsePacket`, failing if end-of-stream is reached before a complete message
   * arrives.
   */
  def receive[P <: ResponsePacket](decoder: Decoder[P]): Fx[P]

  /** Sends the specified request packet. */
  def send(request: RequestPacket): Fx[Unit]

object PacketSocket:

  val DEFAULT_MAX_PACKET_SIZE  = 65535    // 64KB (JDBC Driver default)
  val PROTOCOL_MAX_PACKET_SIZE = 16777215 // 16MB (MySQL protocol limit)
  val MIN_PACKET_SIZE          = 0

  /**
   * Wraps a [[BitVectorSocket]] as a [[PacketSocket]].
   *
   * @param bvs              the underlying bit-vector socket
   * @param debugEnabled     whether to log each packet
   * @param sequenceIdRef    the MySQL packet sequence id
   * @param maxAllowedPacket the maximum accepted payload size
   */
  def fromBitVectorSocket(
    bvs:              BitVectorSocket,
    debugEnabled:     Boolean,
    sequenceIdRef:    Ref[Byte],
    maxAllowedPacket: Int
  ): PacketSocket = new PacketSocket:

    private def debug(msg: => String): Fx[Unit] =
      Fx.whenA(debugEnabled) {
        sequenceIdRef.get.flatMap(id => Fx.delay(println(s"[$id] $msg")))
      }

    override def receive[P <: ResponsePacket](decoder: Decoder[P]): Fx[P] =
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

    private def buildRequest(request: RequestPacket): Fx[BitVector] =
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

    override def send(request: RequestPacket): Fx[Unit] =
      for
        bits <- buildRequest(request)
        _    <-
          debug(
            s"Client ${ AnsiColor.BLUE }→${ AnsiColor.RESET } Server: ${ AnsiColor.YELLOW }$request${ AnsiColor.RESET }"
          )
        _ <- bvs.write(bits)
        _ <- sequenceIdRef.update(sequenceId => ((sequenceId + 1) % 256).toByte)
      yield ()

    private def validatePacketSize(size: Int): Fx[Unit] =
      if size < MIN_PACKET_SIZE then Fx.raiseError(PacketTooBigException(size, maxAllowedPacket))
      else if size > maxAllowedPacket then Fx.raiseError(PacketTooBigException(size, maxAllowedPacket))
      else Fx.unit

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
  def apply(
    debug:             Boolean,
    sockets:           Resource[Socket],
    sslOptions:        Option[SSLNegotiation.Options],
    sequenceIdRef:     Ref[Byte],
    initialPacketRef:  Ref[Option[InitialPacket]],
    readTimeout:       Duration,
    capabilitiesFlags: Set[CapabilitiesFlags],
    maxAllowedPacket:  Int
  ): Resource[PacketSocket] =
    BitVectorSocket(sockets, sequenceIdRef, initialPacketRef, sslOptions, readTimeout, capabilitiesFlags).map(
      fromBitVectorSocket(_, debug, sequenceIdRef, maxAllowedPacket)
    )
