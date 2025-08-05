package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.effect.*

import ldbc.DataSource
import ldbc.sql.Connection

/**
 * A DataSource implementation that manages a pool of reusable database connections.
 * 
 * PooledDataSource extends the basic [[ldbc.DataSource]] interface to provide connection
 * pooling capabilities, which significantly improve performance by reusing existing
 * connections rather than creating new ones for each request.
 * 
 * Key features include:
 * - Connection reuse to minimize the overhead of connection establishment
 * - Configurable pool size with minimum and maximum connections
 * - Connection validation to ensure healthy connections
 * - Idle timeout management to release unused connections
 * - Leak detection to identify connections not properly returned to the pool
 * - Adaptive sizing to dynamically adjust pool size based on load
 * - Comprehensive metrics tracking for monitoring pool health
 * 
 * The pool maintains connections in different states:
 * - Available: Ready for use
 * - In-use: Currently borrowed by a client
 * - Invalid: Failed validation and awaiting removal
 * 
 * @tparam F the effect type (e.g., IO) that wraps asynchronous operations
 */
trait PooledDataSource[F[_]] extends DataSource[F]:
  
  /** The minimum number of connections to maintain in the pool. */
  def minConnections: Int
  
  /** The maximum number of connections allowed in the pool. */
  def maxConnections: Int
  
  /** The maximum time to wait for a connection to become available. */
  def connectionTimeout: FiniteDuration
  
  /** The maximum time a connection can remain idle before being closed. */
  def idleTimeout: FiniteDuration
  
  /** The maximum lifetime of a connection in the pool. */
  def maxLifetime: FiniteDuration
  
  /** The maximum time to wait for connection validation. */
  def validationTimeout: FiniteDuration
  
  /** Optional threshold for detecting connection leaks. */
  def leakDetectionThreshold: Option[FiniteDuration]
  
  /** Whether adaptive pool sizing is enabled. */
  def adaptiveSizing: Boolean
  
  /** The interval at which the adaptive sizing algorithm runs. */
  def adaptiveInterval: FiniteDuration
  
  /** The metrics tracker for monitoring pool performance. */
  def metricsTracker: PoolMetricsTracker[F]
  
  /** Internal state of the connection pool. */
  def poolState: Ref[F, PoolState[F]]
  
  /** Generates unique identifiers for connections. */
  def idGenerator: F[String]
  
  /** Background fiber performing pool maintenance tasks. */
  def houseKeeper: Option[Fiber[F, Throwable, Unit]]
  
  /** Background fiber performing adaptive pool sizing. */
  def adaptiveSizer: Option[Fiber[F, Throwable, Unit]]
  
  /**
   * Returns the current status of the pool.
   * 
   * @return a PoolStatus containing information about available, in-use, and total connections
   */
  def status: F[PoolStatus]
  
  /**
   * Returns comprehensive metrics about pool performance.
   * 
   * @return a PoolMetrics object with detailed statistics
   */
  def metrics: F[PoolMetrics]
  
  /**
   * Gracefully shuts down the pool, closing all connections.
   * 
   * This method will:
   * - Stop accepting new connection requests
   * - Wait for in-use connections to be returned
   * - Close all connections
   * - Cancel background maintenance tasks
   */
  def close: F[Unit]
  
  /**
   * Creates a new pooled connection.
   * 
   * @return a new PooledConnection wrapped in the effect type
   */
  def createNewConnection(): F[PooledConnection[F]]
  
  /**
   * Returns a connection to the pool for reuse.
   * 
   * @param pooled the connection to return to the pool
   */
  def returnToPool(pooled: PooledConnection[F]): F[Unit]
  
  /**
   * Removes a connection from the pool permanently.
   * 
   * @param pooled the connection to remove
   */
  def removeConnection(pooled: PooledConnection[F]): F[Unit]
  
  /**
   * Validates that a connection is still healthy and usable.
   * 
   * @param conn the connection to validate
   * @return true if the connection is valid, false otherwise
   */
  def validateConnection(conn: Connection[F]): F[Boolean]
