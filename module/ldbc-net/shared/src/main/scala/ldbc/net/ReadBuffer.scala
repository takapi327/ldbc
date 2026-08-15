/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * A tiny buffered-read state machine for callback/event-loop transports (e.g. the JS `net` socket).
 * Bytes pushed via [[onData]] accumulate; a [[read]] of up to `n` bytes is served immediately when
 * data is available or parked until it is. Both the immediate and the parked paths honour `n`, so a
 * read never returns more than requested. End-of-stream is signalled out of band as `None`
 * (mirroring the [[Socket]] contract), so it can never collide with a legitimate zero-length read.
 *
 * Not thread-safe: intended for a single-threaded event loop.
 */
private[net] final class ReadBuffer:

  private var pending: Array[Byte]                                            = Array.emptyByteArray
  private var waiter:  Option[Either[Throwable, Option[Array[Byte]]] => Unit] = None
  private var waiterN: Int                                                    = 0
  private var eof:     Boolean                                                = false
  private var error:   Option[Throwable]                                      = None
  private var closed:  Boolean                                                = false

  /** Appends received bytes and serves a parked reader if any. */
  def onData(bytes: Array[Byte]): Unit =
    pending = pending ++ bytes
    deliver()

  /** Marks end-of-stream and serves a parked reader (with `None`). */
  def onEof(): Unit =
    eof = true
    deliver()

  /** Records a transport error and delivers it to a parked reader. */
  def onError(e: Throwable): Unit =
    error = Some(e)
    deliver()

  /**
   * Marks the transport as locally closed (its owning [[Socket.close]] was called) and fails a parked
   * reader. Unlike [[onEof]], this does not wait for the peer's EOF: a subsequent or parked `read`
   * completes immediately with an error instead of hanging on a peer that never sends `end`.
   */
  def onClose(): Unit =
    closed = true
    deliver()

  /**
   * Requests up to `n` bytes. A `n <= 0` request completes immediately with `Some` of an empty
   * array (never confused with EOF). Otherwise the callback is served from the buffer / EOF /
   * error, or parked until data arrives. Returns a cancel action that drops a still-parked waiter.
   *
   * @param n  the maximum number of bytes to return
   * @param cb invoked once with `Some(bytes)`, `None` at end of stream, or the error
   */
  def read(n: Int, cb: Either[Throwable, Option[Array[Byte]]] => Unit): () => Unit =
    error match
      case Some(e) => cb(Left(e))
      case None =>
        if n <= 0 then cb(Right(Some(Array.emptyByteArray)))
        else if closed then cb(Left(new java.io.IOException("socket closed")))
        else if pending.nonEmpty then cb(Right(Some(takeN(n))))
        else if eof then cb(Right(None))
        else
          waiterN = n
          waiter = Some(cb)
    () => waiter = None

  /**
   * Removes and returns every buffered byte, leaving the buffer empty. Used when a transport is
   * upgraded (e.g. STARTTLS) and pre-read bytes must be handed off to the new layer.
   */
  private[net] def drainPending(): Array[Byte] =
    val out = pending
    pending = Array.emptyByteArray
    out

  /** Removes and returns up to `n` bytes from the front of the buffer. */
  private def takeN(n: Int): Array[Byte] =
    val out = pending.take(n)
    pending = pending.drop(n)
    out

  /** Serves a parked reader if one is waiting and a result is available. */
  private def deliver(): Unit =
    waiter match
      case None => ()
      case Some(w) =>
        error match
          case Some(e) =>
            waiter = None
            w(Left(e))
          case None =>
            if closed then
              waiter = None
              w(Left(new java.io.IOException("socket closed")))
            else if pending.nonEmpty then
              waiter = None
              w(Right(Some(takeN(waiterN))))
            else if eof then
              waiter = None
              w(Right(None))
