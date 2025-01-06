/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sbt

import scala.Console._

import sbt._

/**
 * Class for specifying the format of the logs to be spit out when the sbt project is executed.
 */
class ProcessLogger extends Logger {
  def trace(t: => Throwable): Unit = {
    t.printStackTrace()
    println(t)
  }

  def success(message: => String): Unit =
    println(s"success: $message")

  def log(level: Level.Value, message: => String): Unit = {
    val levelStr = level match {
      case Level.Debug => s"[${ GREEN }debug$RESET]"
      case Level.Info  => s"[${ BLUE }info$RESET]"
      case Level.Warn  => s"[${ YELLOW }warn$RESET]"
      case Level.Error => s"[${ RED }error$RESET]"
      case _           => ""
    }
    println(s"$levelStr $message")
  }
}

object ProcessLogger {
  def apply(): ProcessLogger = new ProcessLogger
}
