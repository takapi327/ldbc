/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import scala.concurrent.duration.Duration

import scodec.bits.ByteVector

import ldbc.sql.SQLTimeoutException

import ldbc.effect.Ref
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.exception.EofException
import ldbc.mysql.FTestPlatform
import ldbc.net.Socket

class BitVectorSocketEofTest extends FTestPlatform:
  private final class ScriptedSocket(chunks: Ref[Fx, List[Array[Byte]]]) extends Socket[Fx]:
    override def read(n: Int): Fx[Option[Array[Byte]]] =
      chunks.modify {
        case head :: tail => (tail, Some(head))
        case Nil          => (Nil, None)
      }
    override def write(bytes: Array[Byte]): Fx[Unit] = Fx.unit
    override def close():                   Fx[Unit] = Fx.unit

  private def socketOver(chunks: List[Array[Byte]]): Fx[BitVectorSocket[Fx]] =
    for
      remaining <- Ref.of[Fx, List[Array[Byte]]](chunks)
      carry     <- Ref.of[Fx, ByteVector](ByteVector.empty)
    yield BitVectorSocket.fromSocket[Fx](new ScriptedSocket(remaining), Duration.Inf, carry)

  test("end of stream before any byte raises EofException, not a timeout"):
    val program = for
      socket  <- socketOver(Nil)
      outcome <- socket.read(4).attempt
    yield outcome.left.map(_.getClass.getSimpleName).left.getOrElse("succeeded unexpectedly")

    assertFx(program, "EofException", "EOF がタイムアウトとして報告されている")

  test("EofException reports how many bytes were requested and how many arrived"):
    val program = for
      socket  <- socketOver(List(Array[Byte](1, 2, 3)))
      outcome <- socket.read(8).attempt
    yield outcome.left.toOption.collect { case eof: EofException => (eof.bytesRequested, eof.bytesRead) }

    assertFx(program, Some((8, 3)), "EofException が要求バイト数と既読バイト数を正しく持っていない")

  test("end of stream is not reported as SQLTimeoutException"):
    val program = for
      socket  <- socketOver(Nil)
      outcome <- socket.read(4).attempt
    yield outcome.left.exists(_.isInstanceOf[SQLTimeoutException])

    assertFx(program, false)

  test("a read satisfied before end of stream still succeeds"):
    val program = for
      socket <- socketOver(List(Array[Byte](1, 2), Array[Byte](3, 4)))
      bits   <- socket.read(4)
    yield bits.bytes.toArray.toList

    assertFx(program, List[Byte](1, 2, 3, 4))
