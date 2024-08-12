package ldbc

import cats.effect.unsafe.IORuntime

import epollcat.unsafe.EpollRuntime
import munit.CatsEffectSuite

package object connector:

  trait FTestPlatform extends CatsEffectSuite:
    override def munitIORuntime: IORuntime = EpollRuntime.global
