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

class MysqlNativePasswordPlugin[F[_]: Hashing: Sync] extends AuthenticationPlugin[F]:

  override def name: String = "mysql_native_password"
  override def hashPassword(password: String, scramble: Array[Byte]): F[ByteVector] =
    if password.isEmpty then Sync[F].pure(ByteVector.empty)
    else
      val sha1Hashing = Hashing[F].hash(HashAlgorithm.SHA1)

      def hash01 = fs2
        .Stream(password.getBytes(StandardCharsets.UTF_8)*)
        .through(sha1Hashing)
        .compile
        .lastOrError
      for
        hash1 <- hash01
        hash3 <- fs2.Stream
                   .chunk(hash1.bytes)
                   .through(sha1Hashing)
                   .map(hash2 => Chunk(scramble*) ++ hash2.bytes) // scramble + hash2
                   .unchunks
                   .through(sha1Hashing)
                   .map(_.bytes.toByteVector) // hash3
                   .compile
                   .lastOrError
      yield hash1.bytes.toByteVector.xor(hash3)
object MysqlNativePasswordPlugin:
  def apply[F[_]: Hashing: Sync](): MysqlNativePasswordPlugin[F] = new MysqlNativePasswordPlugin()
