/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.sql.{ Connection, DataSource }

import ldbc.effect.{ Ref, Resource }
import ldbc.fx.concurrentFx
import ldbc.fx.Fx
import ldbc.fx.FxSuite

/**
 * Pins how the `idGenerator` parameter of the [[PooledDataSource]] factories behaves: omitting it must
 * yield distinct generated ids, and supplying one must make the pool use it.
 *
 * Written as a safety net around turning the parameter from a `null`-sentinel default into an `Option`,
 * so both spellings are held to the same observable behaviour.
 */
class PoolIdGeneratorTest extends FxSuite:

  private def mockCreate: Resource[Fx, Connection[Fx]] =
    Resource.make(MockConnection().map(c => (c: Connection[Fx])))(c => c.close())

  private def mockDataSource: DataSource[Fx] = new DataSource[Fx]:
    override def getConnection: Fx[(Connection[Fx], Fx[Unit])] = mockCreate.allocatedCase

  private def config(min: Int): ConnectionPoolConfig =
    ConnectionPoolConfig(minConnections = min, maxConnections = 5, adaptiveSizing = false)

  /** The ids the pool assigned to the connections it holds. */
  private def connectionIds(pool: PooledDataSource[Fx]): Fx[List[String]] =
    pool.poolState.get.map(_.connections.map(_.id).toList)

  /** A generator handing out `prefix-1`, `prefix-2`, ... so a test can tell it apart from the default. */
  private def countingGenerator(prefix: String): Fx[Fx[String]] =
    Ref.of[Fx, Int](0).map(counter => counter.updateAndGet(_ + 1).map(n => s"$prefix-$n"))

  test("without an id generator the pool assigns a distinct non-empty id to each connection") {
    PooledDataSource.fromConfig(config(3), mockCreate).use { pool =>
      connectionIds(pool).map { ids =>
        assertEquals(ids.size, 3)
        assertEquals(ids.distinct.size, 3, s"ids must be distinct, but got $ids")
        assert(ids.forall(_.nonEmpty), s"ids must be non-empty, but got $ids")
      }
    }
  }

  test("the default id generator produces UUID-shaped ids") {
    PooledDataSource.fromConfig(config(1), mockCreate).use { pool =>
      connectionIds(pool).map { ids =>
        val uuid = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$".r
        assert(ids.forall(id => uuid.matches(id)), s"expected version-4 UUIDs, but got $ids")
      }
    }
  }

  test("fromConfig uses a supplied id generator") {
    countingGenerator("cfg").flatMap { generator =>
      PooledDataSource.fromConfig(config(2), mockCreate, idGenerator = Some(generator)).use { pool =>
        connectionIds(pool).map(ids => assertEquals(ids.sorted, List("cfg-1", "cfg-2")))
      }
    }
  }

  test("fromDataSource uses a supplied id generator") {
    countingGenerator("ds").flatMap { generator =>
      PooledDataSource.fromDataSource(config(2), mockDataSource, idGenerator = Some(generator)).use { pool =>
        connectionIds(pool).map(ids => assertEquals(ids.sorted, List("ds-1", "ds-2")))
      }
    }
  }

  test("fromConfigWithBeforeAfter uses a supplied id generator") {
    countingGenerator("hook").flatMap { generator =>
      PooledDataSource
        .fromConfigWithBeforeAfter[Fx, Unit](
          config(2),
          mockCreate,
          before      = _ => Fx.unit,
          after       = (_, _) => Fx.unit,
          idGenerator = Some(generator)
        )
        .use(pool => connectionIds(pool).map(ids => assertEquals(ids.sorted, List("hook-1", "hook-2"))))
    }
  }

  test("fromDataSourceWithBeforeAfter uses a supplied id generator") {
    countingGenerator("dshook").flatMap { generator =>
      PooledDataSource
        .fromDataSourceWithBeforeAfter[Fx, Unit](
          config(2),
          mockDataSource,
          before      = _ => Fx.unit,
          after       = (_, _) => Fx.unit,
          idGenerator = Some(generator)
        )
        .use(pool => connectionIds(pool).map(ids => assertEquals(ids.sorted, List("dshook-1", "dshook-2"))))
    }
  }
