/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.packet

import ldbc.mysql.net.packet.request.{ AuthPublicKeyRequestPacket, ComInitDBPacket, ComQuitPacket }

class AuthPublicKeyRequestPacketTest extends munit.FunSuite:

  test("the sha256_password request is a single 0x01 byte"):
    assertEquals(AuthPublicKeyRequestPacket.Sha256.encode.bytes.toArray.toSeq, Seq(0x01.toByte))

  test("the caching_sha2_password request is a single 0x02 byte"):
    assertEquals(AuthPublicKeyRequestPacket.CachingSha2.encode.bytes.toArray.toSeq, Seq(0x02.toByte))

  test("the payloads match what the command packets used to produce"):
    assertEquals(
      AuthPublicKeyRequestPacket.Sha256.encode.bytes.toArray.toSeq,
      ComQuitPacket().encode.bytes.toArray.toSeq
    )
    assertEquals(
      AuthPublicKeyRequestPacket.CachingSha2.encode.bytes.toArray.toSeq,
      ComInitDBPacket("").encode.bytes.toArray.toSeq
    )

  test("each request names itself rather than a command"):
    assert(AuthPublicKeyRequestPacket.Sha256.toString.contains("AuthPublicKeyRequest"))
    assert(AuthPublicKeyRequestPacket.CachingSha2.toString.contains("AuthPublicKeyRequest"))
