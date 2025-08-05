/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

package object pool:

  /**
   * State of a pooled connection.
   */
  enum ConnectionState:
    case Idle
    case InUse
    case Removed
    case Reserved // Intermediate state during acquisition

  /**
   * Pool adjustment decision for adaptive sizing.
   */
  enum PoolAdjustment:
    case Grow(by: Int)
    case Shrink(by: Int)
    case NoChange
