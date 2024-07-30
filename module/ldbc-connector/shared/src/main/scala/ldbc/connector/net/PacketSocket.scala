/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scala.io.AnsiColor
import scala.concurrent.duration.Duration

import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.*

import fs2.Chunk
import fs2.io.net.Socket

import scodec.Decoder
import scodec.bits.BitVector

import ldbc.connector.data.CapabilitiesFlags
import ldbc.connector.net.packet.*
import ldbc.connector.net.packet.response.InitialPacket
import ldbc.connector.net.protocol.parseHeader

/**
 * A higher-level `BitVectorSocket` that speaks in terms of `Packet`.
 */
trait PacketSocket[F[_]]:

  /**
   * Receive the next `ResponsePacket`, or raise an exception if EOF is reached before a complete
   * message arrives.
   */
  def receive[P <: ResponsePacket](decoder: Decoder[P]): F[P]

  /** Send the specified request packet. */
  def send(request: RequestPacket): F[Unit]

object PacketSocket:

  def fromBitVectorSocket[F[_]: Concurrent: Console](
    bvs:           BitVectorSocket[F],
    debugEnabled:  Boolean,
    sequenceIdRef: Ref[F, Byte]
  ): PacketSocket[F] = new PacketSocket[F]:

    private def debug(msg: => String): F[Unit] =
      sequenceIdRef.get
        .flatMap(id => if debugEnabled then Console[F].println(s"[$id] $msg") else Concurrent[F].unit)

    override def receive[P <: ResponsePacket](decoder: Decoder[P]): F[P] =
      (for
        header <- bvs.read(4)
        payloadSize = parseHeader(header.toByteArray)
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
      sequenceIdRef.get.map(sequenceId =>
        val bits        = request.encode
        val payloadSize = bits.toByteArray.length
        val header = Chunk(
          payloadSize.toByte,
          ((payloadSize >> 8) & 0xff).toByte,
          ((payloadSize >> 16) & 0xff).toByte,
          sequenceId
        )
        header.toBitVector ++ bits
      )

    override def send(request: RequestPacket): F[Unit] =
      for
        bits <- buildRequest(request)
        _ <-
          debug(
            s"Client ${ AnsiColor.BLUE }→${ AnsiColor.RESET } Server: ${ AnsiColor.YELLOW }$request${ AnsiColor.RESET }"
          )
        _ <- bvs.write(bits)
        _ <- sequenceIdRef.update(sequenceId => ((sequenceId + 1) % 256).toByte)
      yield ()

  def apply[F[_]: Console: Temporal](
    debug:             Boolean,
    sockets:           Resource[F, Socket[F]],
    sslOptions:        Option[SSLNegotiation.Options[F]],
    sequenceIdRef:     Ref[F, Byte],
    initialPacketRef:  Ref[F, Option[InitialPacket]],
    readTimeout:       Duration,
    capabilitiesFlags: Set[CapabilitiesFlags]
  ): Resource[F, PacketSocket[F]] =
    BitVectorSocket(sockets, sequenceIdRef, initialPacketRef, sslOptions, readTimeout, capabilitiesFlags).map(
      fromBitVectorSocket(_, debug, sequenceIdRef)
    )
