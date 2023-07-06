/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.core

import org.scalatest.flatspec.AnyFlatSpec

class DataTypeCompileTest extends AnyFlatSpec:

  it should "Successful TINYBLOB compile" in {
    assertCompiles(
      """
        |import ldbc.core.*
        |import ldbc.core.DataType.*
        |
        |val bit1: Tinyblob[Option[Array[Byte]]] = TINYBLOB[Option[Array[Byte]]]().DEFAULT(None)
        |""".stripMargin)
  }

  it should "Fails TINYBLOB compile" in {
    assertDoesNotCompile(
      """
        |import ldbc.core.*
        |import ldbc.core.DataType.*
        |
        |val p1: Tinyblob[Array[Byte]] = TINYBLOB[Array[Byte]]().DEFAULT(Array.emptyByteArray)
        |val p2: Tinyblob[Option[Array[Byte]]] = TINYBLOB[Option[Array[Byte]]]().DEFAULT(Some(Array.emptyByteArray))
        |""".stripMargin)
  }
