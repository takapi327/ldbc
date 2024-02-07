/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.bits.BitVector

transparent trait Packet

trait RequestPacket extends Packet:
  protected def encodeBody: Attempt[BitVector]

  def encode: BitVector

trait ResponsePacket extends Packet
