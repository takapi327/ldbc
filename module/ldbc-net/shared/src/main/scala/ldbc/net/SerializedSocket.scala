/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import ldbc.fx.{ Fx, Mutex }

/**
 * A [[Socket]] decorator that serialises concurrent reads and concurrent writes with two separate
 * [[ldbc.fx.Mutex]]es, mirroring fs2's socket (`readMutex` / `writeMutex`). A read and a write may
 * still proceed concurrently — the underlying engines use disjoint buffers for each direction — but
 * two reads (or two writes) never overlap, so the single-slot NIO read registration cannot be
 * overwritten and the TLS engines' per-direction state cannot be corrupted. Enforcing this makes the
 * "reads/writes are each sequential" contract structural rather than advisory (review R2-8).
 *
 * @param underlying the socket whose access is serialised
 * @param readMutex  guards [[read]]
 * @param writeMutex guards [[write]]
 */
private[net] final class SerializedSocket(
  private[net] val underlying: Socket,
  readMutex:                   Mutex,
  writeMutex:                  Mutex
) extends Socket:

  override def read(n: Int): Fx[Option[Array[Byte]]] = readMutex.surround(underlying.read(n))

  override def write(bytes: Array[Byte]): Fx[Unit] = writeMutex.surround(underlying.write(bytes))

  override def close(): Fx[Unit] = underlying.close()

/** Wraps a [[Socket]] so its reads and writes are each serialised. */
private[net] object SerializedSocket:

  /**
   * Decorates `underlying` with fresh read/write mutexes.
   *
   * @param underlying the socket to serialise
   */
  def apply(underlying: Socket): Fx[Socket] =
    Mutex.create.flatMap(readMutex =>
      Mutex.create.map(writeMutex => new SerializedSocket(underlying, readMutex, writeMutex))
    )

  /**
   * Returns the raw socket beneath a [[SerializedSocket]] (or `socket` itself if it is not one), so a
   * transport upgrade such as TLS can drive the concrete platform socket directly rather than through
   * the serialising wrapper.
   *
   * @param socket the possibly-wrapped socket
   */
  def unwrap(socket: Socket): Socket = socket match
    case wrapped: SerializedSocket => wrapped.underlying
    case other                     => other
