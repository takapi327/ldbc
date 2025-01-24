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

import cats.effect.kernel.Sync

import fs2.hashing.HashAlgorithm
import fs2.hashing.Hashing
import fs2.Chunk
trait Sha256PasswordPlugin[F[_]: Hashing: Sync] extends AuthenticationPlugin[F] with Sha256PasswordPluginPlatform[F]:
  protected def xorString(from: Array[Byte], scramble: Array[Byte], length: Int): Array[Byte] =
    val scrambleLength = scramble.length
    (0 until length).map(pos => (from(pos) ^ scramble(pos % scrambleLength)).toByte).toArray

  override def name: String = "sha256_password"
  override def hashPassword(password: String, scramble: Array[Byte]): F[ByteVector] =
    if password.isEmpty then Sync[F].pure(ByteVector.empty)
    else
      val sha256Hashing = Hashing[F].hash(HashAlgorithm.SHA256)

      def hash01 = fs2
        .Stream(password.getBytes(StandardCharsets.UTF_8)*)
        .through(sha256Hashing)
        .compile
        .lastOrError
      for
        hash1 <- hash01
        hash3 <- fs2.Stream
                   .chunk(hash1.bytes)
                   .through(sha256Hashing)
                   .map(_.bytes ++ Chunk(scramble*)) // hash2 + scramble
                   .unchunks
                   .through(sha256Hashing)
                   .map(_.bytes.toByteVector) // hash3
                   .compile
                   .lastOrError
      yield hash1.bytes.toByteVector.xor(hash3)

  def transformation: String = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"

object Sha256PasswordPlugin:

  def apply[F[_]: Hashing: Sync](): Sha256PasswordPlugin[F] = new Sha256PasswordPlugin {}
