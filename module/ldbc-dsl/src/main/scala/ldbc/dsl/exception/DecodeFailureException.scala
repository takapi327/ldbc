/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.exception

/**
 * The subclass of [[LdbcException]] thrown when an attempt to read a value from the {@code ResultSet} failed due to an error.
 */
class DecodeFailureException(
  message: String,
  offset: Int
) extends LdbcException(
  message,
  Some(s"An attempt to read a value from the ${offset}th position of the ResultSet failed due to an error."),
  Some(
    """
      |      The number of records retrieved from MySQL may not match the number of Decoders.
      |      Try building a Decoder that matches the number and type of cases to be acquired as follows.
      |
      |      given Decoder[(Int, String)] = Decoder[Int] *: Decoder[String]
      |      sql"SELECT c1, c2 FROM table".query[(Int, String)]
      |""".stripMargin)
):

  override def title: String = "Decode Failure Exception"
  override protected def width = 180
