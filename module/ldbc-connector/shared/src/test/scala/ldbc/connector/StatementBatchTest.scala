/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import org.typelevel.otel4s.trace.Tracer

import cats.effect.*

import munit.CatsEffectSuite

class StatementBatchTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )
  
  test("It will be the same as the list of updates specified as a result of executing the batch process.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _ <- statement.addBatch("CREATE TABLE `batch_test` (`c1` INT)")
          _ <- statement.addBatch("INSERT INTO `batch_test` VALUES (1)")
          _ <- statement.addBatch("DROP TABLE `batch_test`")
          result <- statement.executeBatch()
        yield result
      },
      List(0, 1, 0)
    )
  }
