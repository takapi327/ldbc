/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.nio.ByteBuffer

/**
 * Regression test for review finding B4: a `SocketChannel.read` result of `0` (readable but no bytes
 * yet) must be classified as "keep waiting", never as end-of-stream. Conflating `0` with EOF makes a
 * spurious wakeup look like a closed connection.
 */
class IoEngineReadTest extends munit.FunSuite:

  test("a 0-byte read is More, not EOF"):
    assertEquals(NioSocket.interpret(ByteBuffer.allocate(8), -1), NioSocket.Eof)
    assertEquals(NioSocket.interpret(ByteBuffer.allocate(8), 0), NioSocket.More)

  test("a positive read yields exactly the bytes"):
    val buf = ByteBuffer.allocate(8)
    buf.put(Array[Byte](1, 2, 3))
    NioSocket.interpret(buf, 3) match
      case NioSocket.Data(bytes) => assert(bytes.sameElements(Array[Byte](1, 2, 3)))
      case other                 => fail(s"expected Data, got $other")
