/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import scala.math.pow

/**
 * Values for the capabilities flag bitmask used by the MySQL protocol.
 * 
 * Currently need to fit into 32 bits.
 *
 * Each bit represents an optional feature of the protocol.
 *
 * Both the client and the server are sending these.
 *
 * The intersection of the two determines what optional parts of the protocol will be used.
 * 
 * see: https://dev.mysql.com/doc/dev/mysql-server/latest/group__group__cs__capabilities__flags.html
 * 
 * @param code
 *  Numeric bitset
 */
enum CapabilitiesFlags(val code: Long):
  case CLIENT_LONG_PASSWORD                  extends CapabilitiesFlags(1 << 0)
  case CLIENT_FOUND_ROWS                     extends CapabilitiesFlags(1 << 1)
  case CLIENT_LONG_FLAG                      extends CapabilitiesFlags(1 << 2)
  case CLIENT_CONNECT_WITH_DB                extends CapabilitiesFlags(1 << 3)
  case CLIENT_NO_SCHEMA                      extends CapabilitiesFlags(1 << 4)
  case CLIENT_COMPRESS                       extends CapabilitiesFlags(1 << 5)
  case CLIENT_ODBC                           extends CapabilitiesFlags(1 << 6)
  case CLIENT_LOCAL_FILES                    extends CapabilitiesFlags(1 << 7)
  case CLIENT_IGNORE_SPACE                   extends CapabilitiesFlags(1 << 8)
  case CLIENT_PROTOCOL_41                    extends CapabilitiesFlags(1 << 9)
  case CLIENT_INTERACTIVE                    extends CapabilitiesFlags(1 << 10)
  case CLIENT_SSL                            extends CapabilitiesFlags(1 << 11)
  case CLIENT_IGNORE_SIGPIPE                 extends CapabilitiesFlags(1 << 12)
  case CLIENT_TRANSACTIONS                   extends CapabilitiesFlags(1 << 13)
  case CLIENT_RESERVED                       extends CapabilitiesFlags(1 << 14)
  case CLIENT_RESERVED2                      extends CapabilitiesFlags(1 << 15)
  case CLIENT_MULTI_STATEMENTS               extends CapabilitiesFlags(1 << 16)
  case CLIENT_MULTI_RESULTS                  extends CapabilitiesFlags(1 << 17)
  case CLIENT_PS_MULTI_RESULTS               extends CapabilitiesFlags(1 << 18)
  case CLIENT_PLUGIN_AUTH                    extends CapabilitiesFlags(1 << 19)
  case CLIENT_CONNECT_ATTRS                  extends CapabilitiesFlags(1 << 20)
  case CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA extends CapabilitiesFlags(1 << 21)
  case CLIENT_CAN_HANDLE_EXPIRED_PASSWORDS   extends CapabilitiesFlags(1 << 22)
  case CLIENT_SESSION_TRACK                  extends CapabilitiesFlags(1 << 23)
  case CLIENT_DEPRECATE_EOF                  extends CapabilitiesFlags(1 << 24)
  case CLIENT_OPTIONAL_RESULTSET_METADATA    extends CapabilitiesFlags(1 << 25)
  case CLIENT_ZSTD_COMPRESSION_ALGORITHM     extends CapabilitiesFlags(1 << 26)
  case CLIENT_QUERY_ATTRIBUTES               extends CapabilitiesFlags(1 << 27)
  case MULTI_FACTOR_AUTHENTICATION           extends CapabilitiesFlags(1 << 28)
  case CLIENT_CAPABILITY_EXTENSION           extends CapabilitiesFlags(1 << 29)
  case CLIENT_SSL_VERIFY_SERVER_CERT         extends CapabilitiesFlags(1 << 30)
  case CLIENT_REMEMBER_OPTIONS               extends CapabilitiesFlags(1 << 31)

object CapabilitiesFlags:

  /** Get bitset objects from numeric bitset. */
  def apply(bitset: Long): Seq[CapabilitiesFlags] =
    toEnumSeq(bitset)

  /** Get bitset objects from numeric bitsets. */
  def apply(bitset: Seq[Short]): Seq[CapabilitiesFlags] =
    bitset.flatMap(b => toEnumSeq(toCode(b)))

  /** Convert bitNum to BitFlag numbers */
  def toCode(bitNum: Short): Long =
    pow(2, bitNum.toDouble).toLong

  /** Calculate bitset as numeric */
  def toBitset(bitset: Seq[CapabilitiesFlags]): Long =
    bitset.foldLeft(0L)((code, cur) => code | cur.code)

  /** Calculate bitset as bit flags */
  def toEnumSeq(bitset: Long): Seq[CapabilitiesFlags] =
    CapabilitiesFlags.values.filter(p => (p.code & bitset) == p.code).toSeq

  /** Check to whether has a bit flag. */
  def hasBitFlag(bitset: Seq[CapabilitiesFlags], flag: CapabilitiesFlags): Boolean =
    (toBitset(bitset) & flag.code) == flag.code

  def hasBitFlag(bitset: Seq[CapabilitiesFlags], code: Long): Boolean = (toBitset(bitset) & code) == code

  def hasBitFlag(bitset: Long, flag: CapabilitiesFlags): Boolean = (bitset & flag.code) == flag.code

  def hasBitFlag(bitset: Long, code: Long): Boolean = (bitset & code) == code

  /** Set a specified bit flag. */
  def setBitFlag(bitset: Seq[CapabilitiesFlags], flag: CapabilitiesFlags): Seq[CapabilitiesFlags] = apply(
    toBitset(bitset) | flag.code
  )

  def setBitFlag(bitset: Seq[CapabilitiesFlags], code: Long): Seq[CapabilitiesFlags] = apply(
    toBitset(bitset) | code
  )

  def setBitFlag(bitset: Long, flag: CapabilitiesFlags): Long = bitset | flag.code

  def setBitFlag(bitset: Long, code: Long): Long = bitset | code
