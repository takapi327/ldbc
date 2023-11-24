/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.benchmark

import java.io.{ ByteArrayOutputStream, PrintStream }

import dotty.tools.dotc.core.Contexts.inContext
import dotty.tools.repl.{ State, ParseResult, Parsed, Command, ReplDriver as DottyReplDriver }

class Compiler:

  val out = new ByteArrayOutputStream()

  val driver = new Compiler.ReplDriver(new PrintStream(out))

  given initialState: State = driver.initialState

  def compile(parsed: Parsed): State =
    val state = driver.interpret(parsed)

    if state.context.reporter.hasErrors then
      inContext(state.context) {
        state.context.reporter.allErrors.foreach { err =>
          state.context.reporter.report(err)
        }
        throw Compiler.TypeError(out.toString())
      }

    state

  def compile(source: String): State =
    ParseResult.complete(source) match
      case parsed: Parsed => compile(parsed)
      case _: Command => throw new UnsupportedOperationException("Command is not supported")
      case _ => driver.initialState

object Compiler:
  class ReplDriver(out: PrintStream) extends DottyReplDriver(
    Array(
      "-classpath",
      "",
      "-usejavacp",
      "-color:never",
      "-Xrepl-disable-display",
      "-Xmax-inlines",
      "1000",
    ),
    out,
    None,
  ):
    override def interpret(res: ParseResult, quiet: Boolean = false)(using state: State): State =
      super.interpret(res, quiet)

  class TypeError(msg: String) extends Exception(msg)
  class SyntaxError(msg: String) extends Exception(msg)
