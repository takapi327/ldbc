/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.exception

/**
 * The subclass of [[LdbcException]] thrown when an attempt to read a value from the [[ldbc.sql.ResultSet]] failed due to an error.
 */
class DecodeFailureException(
  message:   String,
  offset:    Int,
  statement: String,
  cause:     Option[Throwable] = None
) extends LdbcException(
    message,
    cause.map(_ =>
      s"""
         |    ${ Console.CYAN }I tried to read a value from the ${ Console.RED + offset + Console.CYAN } th position of the ResultSet,
         |    but it does not seem to match the number I am trying to retrieve from the database.
         |
         |      ${ Console.GREEN + statement + Console.RESET }
         |""".stripMargin
    ),
    cause.map(_ =>
      s"""${ Console.CYAN }Try building a Decoder that matches the number and type of cases to be acquired as follows.
      |
      |      given Decoder[(Int, String)] = Decoder[Int] *: Decoder[String]
      |      ${ Console.RED + " " * 28 }^${ Console.RESET }${ Console.RED + " " * 8 }^${ Console.CYAN }
      |      sql"SELECT c1, c2 FROM table".query[(Int, String)]${ Console.RESET }
      |      ${ Console.RED + " " * 22 }^${ Console.RESET }${ Console.RED + " " * 3 }^${ Console.RESET }
      |""".stripMargin
    )
  ):

  override def title: String = "Decode Failure Exception"
  override protected def width = 180
