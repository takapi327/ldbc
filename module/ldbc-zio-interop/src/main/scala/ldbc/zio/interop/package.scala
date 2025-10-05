/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.zio

import java.util.UUID

import cats.effect.std.UUIDGen

import fs2.hashing.Hashing
import fs2.io.net.Network

import zio.*
import zio.interop.catz.*

package object interop:
  implicit def consoleToZIO: cats.effect.std.Console[Task] = cats.effect.std.Console.make[Task]
  implicit def uuidGenToZIO: UUIDGen[Task] = new UUIDGen[Task]:
    override def randomUUID: Task[UUID] = ZIO.attempt(UUID.randomUUID())
  implicit def hashingToZIO: Hashing[Task] = Hashing.forSync[Task]
  implicit def networkToZIO: Network[Task] = Network.forAsync[Task]
