/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.sql

import org.scalatest.flatspec.AnyFlatSpec

class DataTypesTest extends AnyFlatSpec:

  it should "Successful generation of BIT" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Bit[Byte]  = BIT[Byte](1)
      val bit2: Bit[Short] = BIT[Short](64)
    """.stripMargin)
  }

  it should "If length is lower than 1 at the time of BIT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Bit[Byte] = BIT[Byte](0)
    """.stripMargin)
  }

  it should "If the length at the time of BIT generation is greater than 64, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Bit[Short] = BIT[Short](65)
    """.stripMargin)
  }

  it should "Successful generation of TINYINT" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Tinyint[Byte] = TINYINT[Byte](1)
      val bit2: Tinyint[Byte] = TINYINT[Byte](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of TINYINT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Tinyint[Byte] = TINYINT[Byte](-1)
    """.stripMargin)
  }

  it should "If the length at the time of TINYINT generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Tinyint[Byte] = TINYINT[Byte](256)
    """.stripMargin)
  }

  it should "Successful generation of SMALLINT" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Smallint[Short] = SMALLINT[Short](1)
      val bit2: Smallint[Short] = SMALLINT[Short](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of SMALLINT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Smallint[Short] = SMALLINT[Short](-1)
    """.stripMargin)
  }

  it should "If the length at the time of SMALLINT generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Smallint[Short] = SMALLINT[Short](256)
    """.stripMargin)
  }

  it should "Successful generation of MEDIUMINT" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Mediumint[Int] = MEDIUMINT[Int](1)
      val bit2: Mediumint[Int] = MEDIUMINT[Int](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of MEDIUMINT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Mediumint[Int] = MEDIUMINT[Int](-1)
    """.stripMargin)
  }

  it should "If the length at the time of MEDIUMINT generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Mediumint[Int] = MEDIUMINT[Int](256)
    """.stripMargin)
  }

  it should "Successful generation of INT" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Integer[Int] = INT[Int](1)
      val bit2: Integer[Int] = INT[Int](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of INT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Integer[Int] = INT[Int](-1)
    """.stripMargin)
  }

  it should "If the length at the time of INT generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Integer[Int] = INT[Int](256)
    """.stripMargin)
  }

  it should "Successful generation of BIGINT" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Bigint[Long] = BIGINT[Long](1)
      val bit2: Bigint[Long] = BIGINT[Long](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of BIGINT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Bigint[Long] = BIGINT[Long](-1)
    """.stripMargin)
  }

  it should "If the length at the time of BIGINT generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Bigint[Long] = BIGINT[Long](256)
    """.stripMargin)
  }

  it should "Successful generation of DECIMAL" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Decimal[BigDecimal] = DECIMAL[BigDecimal](0, 0)
      val bit2: Decimal[BigDecimal] = DECIMAL[BigDecimal](65)
    """.stripMargin)
  }

  it should "If accuracy is lower than 0 at the time of DECIMAL generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Decimal[BigDecimal] = DECIMAL[BigDecimal](-1, 0)
    """.stripMargin)
  }

  it should "If the accuracy at the time of DECIMAL generation is greater than 65, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Decimal[BigDecimal] = DECIMAL[BigDecimal](66, 0)
    """.stripMargin)
  }

  it should "If scale is lower than 0 at the time of DECIMAL generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Decimal[BigDecimal] = DECIMAL[BigDecimal](1, -1)
    """.stripMargin)
  }

  it should "Successful generation of FLOAT" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: CFloat[Float] = FLOAT[Float](0)
      val bit2: CFloat[Float] = FLOAT[Float](24)
    """.stripMargin)
  }

  it should "If accuracy is lower than 0 at the time of FLOAT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: CFloat[Float] = FLOAT[Float](-1)
    """.stripMargin)
  }

  it should "If the accuracy at the time of FLOAT generation is greater than 24, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: CFloat[Float] = FLOAT[Float](25)
    """.stripMargin)
  }

  it should "Successful generation of DOUBLE" in {
    assertCompiles(
      """
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: CFloat[Double] = DOUBLE[Double](24)
      val bit2: CFloat[Double] = DOUBLE[Double](53)
    """.stripMargin)
  }

  it should "If accuracy is lower than 24 at the time of DOUBLE generation, an error occurs." in {
    assertDoesNotCompile(
      """
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: CFloat[Double] = DOUBLE[Double](23)
    """.stripMargin)
  }

  it should "If the accuracy at the time of DOUBLE generation is greater than 53, an error occurs." in {
    assertDoesNotCompile(
      """
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: CFloat[Double] = DOUBLE[Double](54)
    """.stripMargin)
  }

  it should "Successful generation of CHAR" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: CChar[String] = CHAR[String](0)
      val bit2: CChar[String] = CHAR[String](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of CHAR generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: CChar[String] = CHAR[String](-1)
    """.stripMargin)
  }

  it should "If the length at the time of CHAR generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: CChar[String] = CHAR[String](256)
    """.stripMargin)
  }

  it should "Successful generation of VARCHAR" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Varchar[String] = VARCHAR[String](0)
      val bit2: Varchar[String] = VARCHAR[String](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of VARCHAR generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Varchar[String] = VARCHAR[String](-1)
    """.stripMargin)
  }

  it should "If the length at the time of VARCHAR generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Varchar[String] = VARCHAR[String](256)
    """.stripMargin)
  }

  it should "Successful generation of BINARY" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Binary[Array[Byte]] = BINARY[Array[Byte]](0)
      val bit2: Binary[Array[Byte]] = BINARY[Array[Byte]](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of BINARY generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Binary[Array[Byte]] = BINARY[Array[Byte]](-1)
    """.stripMargin)
  }

  it should "If the length at the time of BINARY generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Binary[Array[Byte]] = BINARY[Array[Byte]](256)
    """.stripMargin)
  }

  it should "Successful generation of VARBINARY" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Varbinary[Array[Byte]] = VARBINARY[Array[Byte]](0)
      val bit2: Varbinary[Array[Byte]] = VARBINARY[Array[Byte]](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of VARBINARY generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Varbinary[Array[Byte]] = VARBINARY[Array[Byte]](-1)
    """.stripMargin)
  }

  it should "If the length at the time of VARBINARY generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Varbinary[Array[Byte]] = VARBINARY[Array[Byte]](256)
    """.stripMargin)
  }

  it should "Successful generation of BLOB" in {
    assertCompiles("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit1: Blob[Array[Byte]] = BLOB[Array[Byte]](0)
      val bit2: Blob[Array[Byte]] = BLOB[Array[Byte]](4294967295L)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of BLOB generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Blob[Array[Byte]] = BLOB[Array[Byte]](-1)
    """.stripMargin)
  }

  it should "If the length at the time of BLOB generation is greater than 4294967295L, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.sql.DataType.*
      import ldbc.sql.DataTypes.*

      val bit: Blob[Array[Byte]] = BLOB[Array[Byte]](4294967296L)
    """.stripMargin)
  }
