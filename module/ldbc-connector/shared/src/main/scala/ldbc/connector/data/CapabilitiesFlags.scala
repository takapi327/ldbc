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
 * ※ There is a 32-bit limit, but the Long type is used because Scala's Int type is out of range.
 */
opaque type CapabilitiesFlags = Long

object CapabilitiesFlags:

  val CLIENT_LONG_PASSWORD:                  CapabilitiesFlags = 1 << 0
  val CLIENT_FOUND_ROWS:                     CapabilitiesFlags = 1 << 1
  val CLIENT_LONG_FLAG:                      CapabilitiesFlags = 1 << 2
  val CLIENT_CONNECT_WITH_DB:                CapabilitiesFlags = 1 << 3
  val CLIENT_NO_SCHEMA:                      CapabilitiesFlags = 1 << 4
  val CLIENT_COMPRESS:                       CapabilitiesFlags = 1 << 5
  val CLIENT_ODBC:                           CapabilitiesFlags = 1 << 6
  val CLIENT_LOCAL_FILES:                    CapabilitiesFlags = 1 << 7
  val CLIENT_IGNORE_SPACE:                   CapabilitiesFlags = 1 << 8
  val CLIENT_PROTOCOL_41:                    CapabilitiesFlags = 1 << 9
  val CLIENT_INTERACTIVE:                    CapabilitiesFlags = 1 << 10
  val CLIENT_SSL:                            CapabilitiesFlags = 1 << 11
  val CLIENT_IGNORE_SIGPIPE:                 CapabilitiesFlags = 1 << 12
  val CLIENT_TRANSACTIONS:                   CapabilitiesFlags = 1 << 13
  val CLIENT_RESERVED:                       CapabilitiesFlags = 1 << 14
  val CLIENT_RESERVED2:                      CapabilitiesFlags = 1 << 15
  val CLIENT_MULTI_STATEMENTS:               CapabilitiesFlags = 1 << 16
  val CLIENT_MULTI_RESULTS:                  CapabilitiesFlags = 1 << 17
  val CLIENT_PS_MULTI_RESULTS:               CapabilitiesFlags = 1 << 18
  val CLIENT_PLUGIN_AUTH:                    CapabilitiesFlags = 1 << 19
  val CLIENT_CONNECT_ATTRS:                  CapabilitiesFlags = 1 << 20
  val CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA: CapabilitiesFlags = 1 << 21
  val CLIENT_CAN_HANDLE_EXPIRED_PASSWORDS:   CapabilitiesFlags = 1 << 22
  val CLIENT_SESSION_TRACK:                  CapabilitiesFlags = 1 << 23
  val CLIENT_DEPRECATE_EOF:                  CapabilitiesFlags = 1 << 24
  val CLIENT_OPTIONAL_RESULTSET_METADATA:    CapabilitiesFlags = 1 << 25
  val CLIENT_ZSTD_COMPRESSION_ALGORITHM:     CapabilitiesFlags = 1 << 26
  val CLIENT_QUERY_ATTRIBUTES:               CapabilitiesFlags = 1 << 27
  val MULTI_FACTOR_AUTHENTICATION:           CapabilitiesFlags = 1 << 28
  val CLIENT_CAPABILITY_EXTENSION:           CapabilitiesFlags = 1 << 29
  val CLIENT_SSL_VERIFY_SERVER_CERT:         CapabilitiesFlags = 1 << 30
  val CLIENT_REMEMBER_OPTIONS:               CapabilitiesFlags = 1 << 31

  val values: Set[CapabilitiesFlags] = Set(
    CLIENT_LONG_PASSWORD,
    CLIENT_FOUND_ROWS,
    CLIENT_LONG_FLAG,
    CLIENT_CONNECT_WITH_DB,
    CLIENT_NO_SCHEMA,
    CLIENT_COMPRESS,
    CLIENT_ODBC,
    CLIENT_LOCAL_FILES,
    CLIENT_IGNORE_SPACE,
    CLIENT_PROTOCOL_41,
    CLIENT_INTERACTIVE,
    CLIENT_SSL,
    CLIENT_IGNORE_SIGPIPE,
    CLIENT_TRANSACTIONS,
    CLIENT_RESERVED,
    CLIENT_RESERVED2,
    CLIENT_MULTI_STATEMENTS,
    CLIENT_MULTI_RESULTS,
    CLIENT_PS_MULTI_RESULTS,
    CLIENT_PLUGIN_AUTH,
    CLIENT_CONNECT_ATTRS,
    CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA,
    CLIENT_CAN_HANDLE_EXPIRED_PASSWORDS,
    CLIENT_SESSION_TRACK,
    CLIENT_DEPRECATE_EOF,
    CLIENT_OPTIONAL_RESULTSET_METADATA,
    CLIENT_ZSTD_COMPRESSION_ALGORITHM,
    CLIENT_QUERY_ATTRIBUTES,
    MULTI_FACTOR_AUTHENTICATION,
    CLIENT_CAPABILITY_EXTENSION,
    CLIENT_SSL_VERIFY_SERVER_CERT,
    CLIENT_REMEMBER_OPTIONS
  )

  /** Get bitset objects from numeric bitset. */
  def apply(bitset: Long): Set[CapabilitiesFlags] =
    toEnumSet(bitset)

  /** Get bitset objects from numeric bitsets. */
  def apply(bitset: Set[Short]): Set[CapabilitiesFlags] =
    bitset.flatMap(b => toEnumSet(toCode(b)))

  /** Convert bitNum to BitFlag numbers */
  def toCode(bitNum: Short): Long =
    pow(2, bitNum.toDouble).toLong

  /** Calculate bitset as numeric */
  def toBitset(bitset: Set[CapabilitiesFlags]): Long =
    bitset.foldLeft(0L)((code, cur) => code | cur)

  /** Calculate bitset as bit flags */
  def toEnumSet(bitset: Long): Set[CapabilitiesFlags] =
    CapabilitiesFlags.values.filter(p => (bitset & p) == p)

  /** Check to whether has a bit flag. */
  def hasBitFlag(bitset: Set[CapabilitiesFlags], flag: CapabilitiesFlags): Boolean =
    (toBitset(bitset) & flag) == flag

  def hasBitFlag(bitset: Long, flag: CapabilitiesFlags): Boolean = (bitset & flag) == flag

  /** Set a specified bit flag. */
  def setBitFlag(bitset: Set[CapabilitiesFlags], flag: CapabilitiesFlags): Set[CapabilitiesFlags] = apply(
    toBitset(bitset) | flag
  )

  def setBitFlag(bitset: Long, flag: CapabilitiesFlags): Long = bitset | flag
