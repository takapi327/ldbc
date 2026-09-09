/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.connector.mysql

import java.time.*
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

import scala.compiletime.uninitialized
import scala.concurrent.duration.*
import scala.concurrent.Await

import org.openjdk.jmh.annotations.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.sql.{ BufferedResultSet, Connection, ResultSet }

import ldbc.catseffect.concurrentIO
import ldbc.effect.syntax.*
import ldbc.effect.MonadThrow
import ldbc.future.toFuture
import ldbc.fx.{ concurrentFx, Fx }
import ldbc.mysql.syntax.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.zio.concurrentTask

import zio.{ Runtime, Task, Unsafe }

/**
 * Throughput of the effect-generic `ldbc-mysql` driver running the same read-only `SELECT` at the raw
 * [[ldbc.sql.Connection]] level (`prepareStatement` / `executeQuery` / column-wise decode), matching the
 * measurement level of the `jdbc` and `ldbc` benchmarks in this package rather than going through the
 * `DBIO` / DSL layer.
 *
 * `io`, `fx` and `zio` run natively on a `Connection[IO]` / `Connection[Fx]` / `Connection[Task]`. `Future` is
 * not `Concurrent`, so it has no native `Connection[Future]`; `future` therefore runs the identical program on
 * the `Fx` connection and bridges the single result with [[ldbc.future.toFuture]] — the cost a `Future` caller
 * actually pays.
 */
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = Array("-Xms4G", "-Xmx4G", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200"))
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Threads(1)
class Select:

  type BenchmarkType = (
    Long,
    Short,
    Int,
    Int,
    Int,
    Long,
    Float,
    Double,
    BigDecimal,
    String,
    String,
    Boolean,
    LocalDate,
    LocalTime,
    LocalDateTime,
    LocalDateTime
  )

  @volatile
  var ioConnection: Connection[IO] = uninitialized

  @volatile
  var fxConnection: Connection[Fx] = uninitialized

  @volatile
  var zioConnection: Connection[Task] = uninitialized

  @volatile
  var ioRelease: IO[Unit] = uninitialized

  @volatile
  var fxRelease: Fx[Unit] = uninitialized

  @volatile
  var zioRelease: Task[Unit] = uninitialized

  private val zioRuntime = Runtime.default

  @Setup
  def setup(): Unit =
    val ioDataSource = MySQLDataSource
      .build[IO]("127.0.0.1", 13306, "ldbc")
      .setPassword("password")
      .setDatabase("benchmark")
      .setSSL(SSL.Trusted)

    val fxDataSource = MySQLDataSource
      .build[Fx]("127.0.0.1", 13306, "ldbc")
      .setPassword("password")
      .setDatabase("benchmark")
      .setSSL(SSL.Trusted)

    val zioDataSource = MySQLDataSource
      .build[Task]("127.0.0.1", 13306, "ldbc")
      .setPassword("password")
      .setDatabase("benchmark")
      .setSSL(SSL.Trusted)

    val (ioConn, ioClose)   = ioDataSource.getConnection.unsafeRunSync()
    val (fxConn, fxClose)   = unsafeRunFx(fxDataSource.getConnection)
    val (zioConn, zioClose) = unsafeRunZio(zioDataSource.getConnection)

    ioConnection  = ioConn
    ioRelease     = ioClose
    fxConnection  = fxConn
    fxRelease     = fxClose
    zioConnection = zioConn
    zioRelease    = zioClose

  @TearDown(Level.Trial)
  def tearDown(): Unit =
    ioRelease.unsafeRunSync()
    unsafeRunFx(fxRelease)
    unsafeRunZio(zioRelease)

  @Param(Array("500", "1000", "1500", "2000"))
  var len: Int = uninitialized

  @Benchmark
  def io: List[BenchmarkType] =
    programBatch(ioConnection).unsafeRunSync()

  @Benchmark
  def fx: List[BenchmarkType] =
    unsafeRunFx(programBatch(fxConnection))

  @Benchmark
  def future: List[BenchmarkType] =
    Await.result(toFuture(programBatch(fxConnection)), 60.seconds)

  @Benchmark
  def zio: List[BenchmarkType] =
    unsafeRunZio(programBatch(zioConnection))

  /**
   * The shared read-only program. A fully-buffered result set is decoded through the single-effect synchronous
   * drain (`BufferedResultSet.foldRowsSync`) instead of per-column `F` reads; any non-buffered result set falls
   * back to the per-column [[consume]] loop.
   */
  private def programBatch[F[_]: MonadThrow](connection: Connection[F]): F[List[BenchmarkType]] =
    for
      statement <- connection.prepareStatement("SELECT * FROM jdbc_prepare_statement_test LIMIT ?")
      _         <- statement.setInt(1, len)
      resultSet <- statement.executeQuery()
      decoded   <- resultSet match
                   case buffered: BufferedResultSet[F] =>
                     buffered
                       .foldRowsSync(List.newBuilder[BenchmarkType]) { (builder, row) =>
                         builder += (
                           (
                             row.getLong(1),
                             row.getShort(2),
                             row.getInt(3),
                             row.getInt(4),
                             row.getInt(5),
                             row.getLong(6),
                             row.getFloat(7),
                             row.getDouble(8),
                             row.getBigDecimal(9),
                             row.getString(10),
                             row.getString(11),
                             row.getBoolean(12),
                             row.getDate(13),
                             row.getTime(14),
                             row.getTimestamp(15),
                             row.getTimestamp(16)
                           )
                         )
                       }
                       .map(_.result())
                   case other => consume(other)
      _ <- statement.close()
    yield decoded

  private def consume[F[_]: MonadThrow](resultSet: ResultSet[F]): F[List[BenchmarkType]] =
    resultSet.whileM[List, BenchmarkType] {
      for
        c1  <- resultSet.getLong(1)
        c2  <- resultSet.getShort(2)
        c3  <- resultSet.getInt(3)
        c4  <- resultSet.getInt(4)
        c5  <- resultSet.getInt(5)
        c6  <- resultSet.getLong(6)
        c7  <- resultSet.getFloat(7)
        c8  <- resultSet.getDouble(8)
        c9  <- resultSet.getBigDecimal(9)
        c10 <- resultSet.getString(10)
        c11 <- resultSet.getString(11)
        c12 <- resultSet.getBoolean(12)
        c13 <- resultSet.getDate(13)
        c14 <- resultSet.getTime(14)
        c15 <- resultSet.getTimestamp(15)
        c16 <- resultSet.getTimestamp(16)
      yield (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16)
    }

  /** Runs an `Fx` to a value on the current runtime, blocking the calling thread until it completes. */
  private def unsafeRunFx[A](fx: Fx[A]): A =
    val latch  = new CountDownLatch(1)
    val result = new AtomicReference[Either[Throwable, A]]()
    fx.unsafeRun { outcome =>
      result.set(outcome)
      latch.countDown()
    }
    latch.await()
    result.get.fold(throw _, identity)

  /** Runs a `Task` to a value on ZIO's default runtime, blocking the calling thread until it completes. */
  private def unsafeRunZio[A](task: Task[A]): A =
    Unsafe.unsafe(implicit unsafe => zioRuntime.unsafe.run(task).getOrThrowFiberFailure())
