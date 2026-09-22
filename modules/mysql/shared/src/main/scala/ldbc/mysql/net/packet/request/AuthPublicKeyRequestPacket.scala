/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.packet
package request

import scodec.*
import scodec.bits.BitVector

import ldbc.mysql.net.protocol.Authentication

/**
 * A request for the server's RSA public key, sent during authentication over an unencrypted
 * connection so the password can be encrypted before it crosses the wire.
 *
 * The payload is a single byte whose value identifies the plugin doing the asking. It is not a
 * command: the server reads the first payload byte as a command id only once the connection has
 * reached the command phase, and this is sent while it is still authenticating. Sending a
 * [[ComQuitPacket]] here would put the same byte on the wire and work by accident, but it would read
 * as "close the connection", show up that way in the packet log, and tie this request to a command id
 * that is free to change independently.
 */
sealed trait AuthPublicKeyRequestPacket extends RequestPacket:

  /** The byte identifying which plugin is asking. See [[Authentication.PUBLIC_KEY_REQUEST_SHA256]]. */
  def marker: Byte

  override protected def encodeBody: Attempt[BitVector] = Attempt.successful(BitVector(marker))

  override def encode: BitVector = encodeBody.require

object AuthPublicKeyRequestPacket:

  /** The request `sha256_password` sends. */
  case object Sha256 extends AuthPublicKeyRequestPacket:
    override def marker:   Byte   = Authentication.PUBLIC_KEY_REQUEST_SHA256
    override def toString: String = "Protocol::AuthPublicKeyRequest(sha256_password)"

  /** The request `caching_sha2_password` sends during full authentication. */
  case object CachingSha2 extends AuthPublicKeyRequestPacket:
    override def marker:   Byte   = Authentication.PUBLIC_KEY_REQUEST_CACHING_SHA2
    override def toString: String = "Protocol::AuthPublicKeyRequest(caching_sha2_password)"
