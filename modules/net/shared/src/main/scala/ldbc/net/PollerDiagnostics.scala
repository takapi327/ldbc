/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * Observation points on a poller-backed [[RawIoEngine]].
 *
 * The engines recover from failures on their own — a dying thread is replaced, a terminated engine is
 * rebuilt on the next connect — which means the interesting behaviour leaves no trace a test could
 * otherwise assert on. These counters and hooks make those paths observable, and let one test cover
 * both platform engines instead of each growing its own copy.
 */
private[net] trait PollerDiagnostics:

  /**
   * Installs a hook run at the top of each loop iteration, waking the poller so an already-parked
   * loop reaches it. Throwing something non-fatal exercises the recoverable branch, something fatal
   * the handover. `null` removes it.
   */
  def injectFault(fault: () => Unit): Unit

  /**
   * Installs a hook run while a socket's readiness callback is being dispatched. This is the only way
   * to reach the window where a registration has been disarmed but its continuation has not run yet,
   * which is the state the handover has to account for.
   */
  def injectDispatchFault(fault: () => Unit): Unit

  /** Recoverable loop errors swallowed so far. */
  def loopErrorCount: Long

  /** Times a terminated engine has been rebuilt by a subsequent connect. */
  def revivalCount: Int

  /**
   * Multiplexers this engine has released.
   *
   * A rebuild replaces the selector or poller, and the one being replaced owns descriptors that only
   * an explicit close returns. Counting the releases is how a test can tell "replaced it" apart from
   * "replaced it and let the old one leak" without measuring process-wide descriptors, which any
   * other test running alongside would perturb.
   */
  def releasedMultiplexers: Int

  /** True once the engine has given up replacing its poller thread. */
  def isTerminated: Boolean

  /** Sockets the engine still tracks. A connect that fails or is cancelled must not leave one behind. */
  def liveSocketCount: Int

  /** Name of the current poller thread, so tests can find and interrupt it. */
  def pollerThreadName: String
