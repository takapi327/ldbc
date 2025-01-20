/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import java.nio.charset.StandardCharsets

import scodec.bits.ByteVector

import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps

import cats.effect.Concurrent

import fs2.hashing.HashAlgorithm
import fs2.hashing.Hashing
import fs2.Chunk
trait AuthenticationPlugin[F[_]: Hashing](using F: Concurrent[F]):

  def name: String

  def hashPassword(password: String, scramble: Array[Byte]): F[ByteVector] =
    if password.isEmpty then F.pure(ByteVector.empty)
    else
      val sha256Hashing = Hashing[F].hash(HashAlgorithm.SHA256)

      def hash01 = fs2
        .Stream(password.getBytes(StandardCharsets.UTF_8)*)
        .covary[F]
        .through(sha256Hashing)
        .compile
        .lastOrError
      for
        hash1 <- hash01
        hash3 <- fs2.Stream
                   .chunk(hash1.bytes)
                   .covary[F]
                   .through(sha256Hashing)
                   .map(_.bytes) // hash1
                   .unchunks
                   .through(sha256Hashing)
                   .map(_.bytes ++ Chunk(scramble*)) // hash2 + scramble
                   .unchunks
                   .through(sha256Hashing)
                   .map(_.bytes.toByteVector) // hash3
                   .compile
                   .lastOrError
      yield hash1.bytes.toByteVector.xor(hash3)
