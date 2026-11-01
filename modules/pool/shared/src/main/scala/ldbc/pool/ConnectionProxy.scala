/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentLinkedQueue

import ldbc.sql.*

import ldbc.effect.syntax.*
import ldbc.effect.Sync

/**
 * A [[ldbc.sql.Connection]] proxy handed to pool clients. It delegates every operation to the pooled
 * connection, except `close()`, which returns the connection to the pool via `releaseCallback` rather
 * than actually closing it.
 *
 * @param pooled          the underlying pooled connection
 * @param releaseCallback the action that returns this proxy to the pool
 */
private[pool] class ConnectionProxy[F[_]](
  val pooled:      PooledConnection[F],
  releaseCallback: ConnectionProxy[F] => F[Unit]
)(using F: Sync[F])
  extends Connection[F]:

  private def delegate: Connection[F] = pooled.connection

  private val openStatements     = new ConcurrentLinkedQueue[Statement[F]]()
  private val originalAutoCommit = new AtomicReference[Option[Boolean]](None)
  private val originalReadOnly   = new AtomicReference[Option[Boolean]](None)
  private val originalIsolation  = new AtomicReference[Option[Int]](None)
  private val originalCatalog    = new AtomicReference[Option[String]](None)
  private val originalSchema     = new AtomicReference[Option[String]](None)

  private def track[S <: Statement[F]](statement: F[S]): F[S] =
    statement.flatTap(s => F.delay(openStatements.add(s)).void)

  private def remember[A](slot: AtomicReference[Option[A]], current: => F[A]): F[Unit] =
    F.delay(slot.get().isDefined).flatMap { known =>
      if known then F.unit
      else current.flatMap(value => F.delay(slot.compareAndSet(None, Some(value))).void)
    }

  private def restore[A](slot: AtomicReference[Option[A]])(set: A => F[Unit]): F[Unit] =
    F.delay(slot.getAndSet(None)).flatMap {
      case Some(value) => set(value)
      case None        => F.unit
    }

  private def closeStatements: F[Unit] =
    F.delay(Iterator.continually(openStatements.poll()).takeWhile(_ != null).toList).flatMap { statements =>
      statements
        .foldLeft(F.pure(Option.empty[Throwable])) { (acc, statement) =>
          acc.flatMap(first => statement.close().attempt.map(result => first.orElse(result.left.toOption)))
        }
        .flatMap {
          case Some(error) => F.raiseError(error)
          case None        => F.unit
        }
    }

  private[pool] def resetState: F[Unit] =
    for
      _          <- closeStatements
      autoCommit <- delegate.getAutoCommit()
      _          <- if autoCommit then F.unit else delegate.rollback()
      _          <- restore(originalAutoCommit)(delegate.setAutoCommit)
      _          <- restore(originalReadOnly)(delegate.setReadOnly)
      _          <- restore(originalIsolation)(delegate.setTransactionIsolation)
      _          <- restore(originalSchema)(delegate.setSchema)
      _          <- restore(originalCatalog)(delegate.setCatalog)
    yield ()

  override def close(): F[Unit] = releaseCallback(this)

  override def createStatement(): F[Statement[F]] = track(delegate.createStatement())

  override def prepareStatement(sql: String): F[PreparedStatement[F]] = track(delegate.prepareStatement(sql))

  override def prepareCall(sql: String): F[CallableStatement[F]] = track(delegate.prepareCall(sql))

  override def nativeSQL(sql: String): F[String] = delegate.nativeSQL(sql)

  override def getAutoCommit(): F[Boolean] = delegate.getAutoCommit()

  override def setAutoCommit(autoCommit: Boolean): F[Unit] =
    remember(originalAutoCommit, delegate.getAutoCommit()) *> delegate.setAutoCommit(autoCommit)

  override def commit(): F[Unit] = delegate.commit()

  override def rollback(): F[Unit] = delegate.rollback()

  override def getTransactionIsolation(): F[Int] = delegate.getTransactionIsolation()

  override def setTransactionIsolation(level: Int): F[Unit] =
    remember(originalIsolation, delegate.getTransactionIsolation()) *> delegate.setTransactionIsolation(level)

  override def isReadOnly: F[Boolean] = delegate.isReadOnly

  override def setReadOnly(readOnly: Boolean): F[Unit] =
    remember(originalReadOnly, delegate.isReadOnly) *> delegate.setReadOnly(readOnly)

  override def getCatalog(): F[String] = delegate.getCatalog()

  override def setCatalog(catalog: String): F[Unit] =
    remember(originalCatalog, delegate.getCatalog()) *> delegate.setCatalog(catalog)

  override def isClosed(): F[Boolean] = delegate.isClosed()

  override def isValid(timeout: Int): F[Boolean] = delegate.isValid(timeout)

  override def getMetaData(): F[DatabaseMetaData[F]] = delegate.getMetaData()

  override def setSavepoint(): F[Savepoint] = delegate.setSavepoint()

  override def setSavepoint(name: String): F[Savepoint] = delegate.setSavepoint(name)

  override def releaseSavepoint(savepoint: Savepoint): F[Unit] = delegate.releaseSavepoint(savepoint)

  override def rollback(savepoint: Savepoint): F[Unit] = delegate.rollback(savepoint)

  override def createStatement(resultSetType: Int, resultSetConcurrency: Int): F[Statement[F]] =
    track(delegate.createStatement(resultSetType, resultSetConcurrency))

  override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int): F[PreparedStatement[F]] =
    track(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency))

  override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int): F[CallableStatement[F]] =
    track(delegate.prepareCall(sql, resultSetType, resultSetConcurrency))

  override def prepareStatement(sql: String, autoGeneratedKeys: Int): F[PreparedStatement[F]] =
    track(delegate.prepareStatement(sql, autoGeneratedKeys))

  override def setSchema(schema: String): F[Unit] =
    remember(originalSchema, delegate.getSchema()) *> delegate.setSchema(schema)

  override def getSchema(): F[String] = delegate.getSchema()
