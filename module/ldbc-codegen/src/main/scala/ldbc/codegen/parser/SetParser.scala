/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.codegen.parser

import ldbc.codegen.model.SetDefinition

/** Parser for parsing SET definitions.
 */
trait SetParser extends SqlParser:

  private def systemVariable(str: "global" | "persist" | "persist_only" | "session" | "local"): Parser[String] =
    opt("@@" | "@") ~> caseSensitivity(str) <~ opt(".")

  private def global: Parser[String] = systemVariable("global")
  private def persist: Parser[String] = systemVariable("persist")
  private def persistOnly: Parser[String] = systemVariable("persist_only")
  private def session: Parser[String] = systemVariable("session") | "@@"
  private def local: Parser[String] = systemVariable("local")
  private def userVariable: Parser[String] = opt("@@" | "@") ~> specialChars <~ "."

  private def expr: Parser[String] = opt(global | persist | session | local | userVariable) ~> opt("'") ~> specialChars <~ opt("'")

  private def globalVariableStatement: Parser[SetDefinition] =
    customError(
      global ~> ident ~ "=" ~ expr ^^ {
        case variable ~ _ ~ expr => SetDefinition("GLOBAL", expr)
      },
      failureMessage("set global variable", "{GLOBAL | @@GLOBAL.} system_var_name = expr")
    )

  private def persistVariableStatement: Parser[SetDefinition] =
    customError(
      persist ~> ident ~ "=" ~ expr ^^ {
        case variable ~ _ ~ expr => SetDefinition("PERSIST", expr)
      },
      failureMessage("set persist variable", "{PERSIST | @@PERSIST.} system_var_name = expr")
    )

  private def persistOnlyVariableStatement: Parser[SetDefinition] =
    customError(
      persistOnly ~> ident ~ "=" ~ expr ^^ {
        case variable ~ _ ~ expr => SetDefinition("PERSIST_ONLY", expr)
      },
      failureMessage("set persist_only variable", "{PERSIST_ONLY | @@PERSIST_ONLY.} system_var_name = expr")
    )

  private def sessionVariableStatement: Parser[SetDefinition] =
    customError(
      session ~> ident ~ "=" ~ expr ^^ {
        case variable ~ _ ~ expr => SetDefinition("SESSION", expr)
      },
      failureMessage("set session variable", "[SESSION | @@SESSION. | @@] system_var_name = expr")
    )

  private def localVariableStatement: Parser[SetDefinition] =
    customError(
      local ~> ident ~ "=" ~ expr ^^ {
        case variable ~ _ ~ expr => SetDefinition("LOCAL", expr)
      },
      failureMessage("set local variable", "[LOCAL | @@LOCAL.] system_var_name = expr")
    )

  private def variableStatement: Parser[SetDefinition] =
    customError(
      opt("@") ~ ident ~ "=" ~ expr ^^ {
        case symbol ~ variable ~ _ ~ expr => SetDefinition(symbol.getOrElse("") + variable, expr)
      },
      failureMessage("set variable", "user_var_name | param_name | local_var_name")
    )

  protected def setStatements: Parser[List[SetDefinition]] =
    customError(
      caseSensitivity("set") ~> rep1sep(globalVariableStatement | persistVariableStatement | persistOnlyVariableStatement | sessionVariableStatement | localVariableStatement | variableStatement, ",") <~ ";",
      failureMessage("set", "SET variable = expr [, variable = expr] ...")
    )
