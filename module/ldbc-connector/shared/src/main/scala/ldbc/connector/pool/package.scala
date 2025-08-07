/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

package object pool:

  /**
   * Represents the lifecycle state of a connection within the pool.
   * 
   * A connection transitions through these states during its lifetime:
   * - Created connections start in `Idle` state
   * - When acquired by a client, they move to `Reserved` then `InUse`
   * - After release, they return to `Idle`
   * - Connections marked for removal enter `Removed` state
   */
  enum ConnectionState:
    /** Connection is available in the pool and ready to be acquired. */
    case Idle
    
    /** Connection is currently being used by a client. */
    case InUse
    
    /** Connection has been marked for removal and will be closed. */
    case Removed
    
    /** Intermediate state when a connection is being acquired but not yet in use. */
    case Reserved

  /**
   * Represents a decision made by the adaptive pool sizing algorithm.
   * 
   * The adaptive sizer monitors pool metrics and usage patterns to determine
   * whether the pool size should be adjusted to optimize performance and
   * resource utilization.
   */
  enum PoolAdjustment:
    /**
     * Increase the pool size by the specified number of connections.
     * 
     * @param by the number of connections to add to the pool
     */
    case Grow(by: Int)
    
    /**
     * Decrease the pool size by the specified number of connections.
     * 
     * @param by the number of connections to remove from the pool
     */
    case Shrink(by: Int)
    
    /** No adjustment needed - pool size remains unchanged. */
    case NoChange
