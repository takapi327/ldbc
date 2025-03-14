/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import java.util.concurrent.{ ExecutorService, Executors }

import scala.concurrent.ExecutionContext

import cats.effect.kernel.{ Resource, Sync }

/**
 * copied from doobie-core:
 * https://github.com/tpolecat/doobie/blob/v1.0.0-RC2/modules/core/src/main/scala/doobie/util/ExecutionContexts.scala#L11
 */
object ExecutionContexts:

  /** Resource yielding an `ExecutionContext` backed by a fixed-size pool. */
  def fixedThreadPool[F[_]](size: Int)(using sf: Sync[F]): Resource[F, ExecutionContext] =
    val alloc = sf.delay(Executors.newFixedThreadPool(size))
    val free  = (es: ExecutorService) => sf.delay(es.shutdown())
    Resource.make(alloc)(free).map(ExecutionContext.fromExecutor)

  /** Resource yielding an `ExecutionContext` backed by an unbounded thread pool. */
  def cachedThreadPool[F[_]](using sf: Sync[F]): Resource[F, ExecutionContext] =
    val alloc = sf.delay(Executors.newCachedThreadPool)
    val free  = (es: ExecutorService) => sf.delay(es.shutdown())
    Resource.make(alloc)(free).map(ExecutionContext.fromExecutor)

  /** Execution context that runs everything synchronously. This can be useful for testing. */
  object synchronous extends ExecutionContext:
    def execute(runnable: Runnable): Unit = runnable.run()

    def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
