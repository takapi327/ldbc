/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import cats.effect.unsafe.IORuntime

import munit.CatsEffectSuite

import epollcat.unsafe.EpollRuntime

package object connector:

  trait FTestPlatform extends CatsEffectSuite:
    override def munitIORuntime: IORuntime = EpollRuntime.global
