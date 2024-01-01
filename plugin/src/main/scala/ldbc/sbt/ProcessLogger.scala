/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
  */

package ldbc.sbt

import sbt._

import scala.Console._

/** Class for specifying the format of the logs to be spit out when the sbt project is executed.
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
