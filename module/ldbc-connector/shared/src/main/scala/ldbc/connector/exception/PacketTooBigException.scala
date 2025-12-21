/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

/**
 * Exception thrown when a MySQL protocol packet exceeds the configured maximum packet size limit.
 * 
 * This exception serves as a security mechanism to prevent memory exhaustion attacks and ensures
 * compatibility with MySQL protocol constraints. It is thrown when the connector attempts to
 * send or receive a packet that exceeds the `maxAllowedPacket` configuration setting.
 * 
 * The `maxAllowedPacket` setting corresponds to MySQL's `max_allowed_packet` system variable
 * and provides protection against:
 * 
 * - **Memory exhaustion attacks**: Prevents attackers from sending oversized packets to consume server memory
 * - **Denial of Service (DoS)**: Blocks attempts to overwhelm the system with large data payloads  
 * - **Accidental large data transfers**: Catches unintentional transmission of extremely large datasets
 * - **Protocol violations**: Enforces MySQL protocol packet size constraints (max 16MB)
 * 
 * @param packetLength the actual size of the packet that exceeded the limit, in bytes
 * @param maxAllowed the configured maximum packet size limit, in bytes
 * 
 * @example {{{
 * // Typical usage when packet size validation fails
 * try {
 *   // Some operation that sends a large packet
 *   connection.executeUpdate(queryWithLargeData)
 * } catch {
 *   case ex: PacketTooBigException =>
 *     println(s"Packet too large: ${ex.getMessage}")
 *     // Consider increasing maxAllowedPacket or reducing data size
 * }
 * }}}
 * 
 * @example {{{
 * // Configure larger packet size to handle bigger data
 * val config = MySQLConfig.default
 *   .setMaxAllowedPacket(1048576) // 1MB limit
 * 
 * // Or use MySQL protocol maximum
 * val config = MySQLConfig.default
 *   .setMaxAllowedPacket(16777215) // 16MB - protocol maximum
 * }}}
 * 
 * @see [[ldbc.connector.MySQLConfig.maxAllowedPacket]] for configuration details
 * @see [[ldbc.connector.MySQLConfig.setMaxAllowedPacket]] for setting the packet size limit
 * @see [[https://dev.mysql.com/doc/refman/en/packet-too-large.html MySQL Protocol Packet Limits]]
 * 
 * @note The default packet size limit is 65,535 bytes (64KB), which matches MySQL JDBC Driver defaults
 *       and provides good security against packet-based attacks while accommodating most use cases.
 * 
 * @note This exception extends SQLException to maintain compatibility with JDBC error handling patterns.
 *       The error message includes both the actual packet size and the configured limit for debugging.
 */
class PacketTooBigException(
  packetLength: Int,
  maxAllowed:   Int
) extends SQLException(
    s"Packet for query is too large ($packetLength > $maxAllowed). " +
      s"You can change the value by setting the 'maxAllowedPacket' configuration."
  )
