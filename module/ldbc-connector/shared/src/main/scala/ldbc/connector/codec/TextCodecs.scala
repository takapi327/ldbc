/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import scodec.Codec

trait TextCodecs:

  def binary(): Codec[Array[Byte]] =
    Codec[String].xmap(
      s => s.getBytes("UTF-8"),
      bytes => new String(bytes),
    )
  given Codec[Array[Byte]] = binary()

object text extends TextCodecs
