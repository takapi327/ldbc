/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import org.scalatest.flatspec.AnyFlatSpec

class DataTypesTest extends AnyFlatSpec:

  it should "Successful generation of BIT" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Bit[Byte]  = BIT[Byte](1)
      val p2: Bit[Short] = BIT[Short](64)
      val p3: Bit[Byte]  = BIT[Byte]
      val p4: Bit[Short] = BIT[Short]
    """.stripMargin)
  }

  it should "If length is lower than 1 at the time of BIT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Bit[Byte] = BIT[Byte](0)
    """.stripMargin)
  }

  it should "If the length at the time of BIT generation is greater than 64, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Bit[Short] = BIT[Short](65)
    """.stripMargin)
  }

  it should "Successful generation of TINYINT" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Tinyint[Byte] = TINYINT[Byte](1)
      val p2: Tinyint[Byte] = TINYINT[Byte](255)
      val p3: Tinyint[Byte] = TINYINT[Byte]
      val p4: Tinyint[Byte] = TINYINT[Byte]
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of TINYINT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Tinyint[Byte] = TINYINT[Byte](-1)
    """.stripMargin)
  }

  it should "If the length at the time of TINYINT generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Tinyint[Byte] = TINYINT[Byte](256)
    """.stripMargin)
  }

  it should "Successful generation of SMALLINT" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Smallint[Short] = SMALLINT[Short](1)
      val p2: Smallint[Short] = SMALLINT[Short](255)
      val p3: Smallint[Short] = SMALLINT[Short]
      val p4: Smallint[Short] = SMALLINT[Short]
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of SMALLINT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Smallint[Short] = SMALLINT[Short](-1)
    """.stripMargin)
  }

  it should "If the length at the time of SMALLINT generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Smallint[Short] = SMALLINT[Short](256)
    """.stripMargin)
  }

  it should "Successful generation of MEDIUMINT" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Mediumint[Int] = MEDIUMINT[Int](1)
      val p2: Mediumint[Int] = MEDIUMINT[Int](255)
      val p3: Mediumint[Int] = MEDIUMINT[Int]
      val p4: Mediumint[Int] = MEDIUMINT[Int]
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of MEDIUMINT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Mediumint[Int] = MEDIUMINT[Int](-1)
    """.stripMargin)
  }

  it should "If the length at the time of MEDIUMINT generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Mediumint[Int] = MEDIUMINT[Int](256)
    """.stripMargin)
  }

  it should "Successful generation of INT" in {
    assertCompiles("""
      import ldbc.core.DataType.*

      val p1: Integer[Int] = INT[Int](1)
      val p2: Integer[Int] = INT[Int](255)
      val p3: Integer[Int] = INT[Int]
      val p4: Integer[Int] = INT[Int]
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of INT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Integer[Int] = INT[Int](-1)
    """.stripMargin)
  }

  it should "If the length at the time of INT generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Integer[Int] = INT[Int](256)
    """.stripMargin)
  }

  it should "Successful generation of BIGINT" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Bigint[Long] = BIGINT[Long](1)
      val p2: Bigint[Long] = BIGINT[Long](255)
      val p3: Bigint[Long] = BIGINT[Long]
      val p4: Bigint[Long] = BIGINT[Long]
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of BIGINT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Bigint[Long] = BIGINT[Long](-1)
    """.stripMargin)
  }

  it should "If the length at the time of BIGINT generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Bigint[Long] = BIGINT[Long](256)
    """.stripMargin)
  }

  it should "Successful generation of DECIMAL" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Decimal[BigDecimal] = DECIMAL[BigDecimal](0, 0)
      val p2: Decimal[BigDecimal] = DECIMAL[BigDecimal](65)
    """.stripMargin)
  }

  it should "If accuracy is lower than 0 at the time of DECIMAL generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Decimal[BigDecimal] = DECIMAL[BigDecimal](-1, 0)
    """.stripMargin)
  }

  it should "If the accuracy at the time of DECIMAL generation is greater than 65, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Decimal[BigDecimal] = DECIMAL[BigDecimal](66, 0)
    """.stripMargin)
  }

  it should "If scale is lower than 0 at the time of DECIMAL generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Decimal[BigDecimal] = DECIMAL[BigDecimal](1, -1)
    """.stripMargin)
  }

  it should "Successful generation of FLOAT" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: CFloat[Float] = FLOAT[Float](0)
      val p2: CFloat[Float] = FLOAT[Float](24)
    """.stripMargin)
  }

  it should "If accuracy is lower than 0 at the time of FLOAT generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: CFloat[Float] = FLOAT[Float](-1)
    """.stripMargin)
  }

  it should "If the accuracy at the time of FLOAT generation is greater than 24, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: CFloat[Float] = FLOAT[Float](25)
    """.stripMargin)
  }

  it should "Successful generation of DOUBLE" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: CFloat[Double] = DOUBLE[Double](24)
      val p2: CFloat[Double] = DOUBLE[Double](53)
    """.stripMargin)
  }

  it should "If accuracy is lower than 24 at the time of DOUBLE generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: CFloat[Double] = DOUBLE[Double](23)
    """.stripMargin)
  }

  it should "If the accuracy at the time of DOUBLE generation is greater than 53, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val bit: CFloat[Double] = DOUBLE[Double](54)
    """.stripMargin)
  }

  it should "Successful generation of CHAR" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: CChar[String] = CHAR[String](0)
      val p2: CChar[String] = CHAR[String](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of CHAR generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: CChar[String] = CHAR[String](-1)
    """.stripMargin)
  }

  it should "If the length at the time of CHAR generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.DataType.*

      val p: CChar[String] = CHAR[String](256)
    """.stripMargin)
  }

  it should "Successful generation of VARCHAR" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Varchar[String] = VARCHAR[String](0)
      val p2: Varchar[String] = VARCHAR[String](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of VARCHAR generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Varchar[String] = VARCHAR[String](-1)
    """.stripMargin)
  }

  it should "If the length at the time of VARCHAR generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Varchar[String] = VARCHAR[String](256)
    """.stripMargin)
  }

  it should "Successful generation of BINARY" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Binary[Array[Byte]] = BINARY[Array[Byte]](0)
      val p2: Binary[Array[Byte]] = BINARY[Array[Byte]](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of BINARY generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Binary[Array[Byte]] = BINARY[Array[Byte]](-1)
    """.stripMargin)
  }

  it should "If the length at the time of BINARY generation is greater than 255, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Binary[Array[Byte]] = BINARY[Array[Byte]](256)
    """.stripMargin)
  }

  it should "Successful generation of VARBINARY" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Varbinary[Array[Byte]] = VARBINARY[Array[Byte]](0)
      val p2: Varbinary[Array[Byte]] = VARBINARY[Array[Byte]](255)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of VARBINARY generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Varbinary[Array[Byte]] = VARBINARY[Array[Byte]](-1)
    """.stripMargin)
  }

  it should "Successful generation of BLOB" in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Blob[Array[Byte]] = BLOB[Array[Byte]](0)
      val p2: Blob[Array[Byte]] = BLOB[Array[Byte]](4294967295L)
    """.stripMargin)
  }

  it should "If length is lower than 0 at the time of BLOB generation, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.sql.DataType.*

      val p: Blob[Array[Byte]] = BLOB[Array[Byte]](-1)
    """.stripMargin)
  }

  it should "If the length at the time of BLOB generation is greater than 4294967295L, an error occurs." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p: Blob[Array[Byte]] = BLOB[Array[Byte]](4294967296L)
    """.stripMargin)
  }

  it should "A string in hh:mm:ss or hhh:mm:ss format and in the range from '-838:59:59' to '838:59:59' must be passed to the TIME type." in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Time[java.time.LocalTime] = TIME.DEFAULT("-838:59:59")
      val p2: Time[java.time.LocalTime] = TIME.DEFAULT("838:59:59")
      val p3: Time[java.time.LocalTime] = TIME.DEFAULT("60:59:59")
    """.stripMargin)
  }

  it should "Passing a string of type TIME in hh:mm:ss or hhh:mm:ss format other than the range from '-838:59:59' to '838:59:59' will result in a compile error." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Time[java.time.LocalTime] = TIME.DEFAULT("-839:59:59")
      val p2: Time[java.time.LocalTime] = TIME.DEFAULT("839:59:59")
      val p3: Time[java.time.LocalTime] = TIME.DEFAULT("1111:59:59")
    """.stripMargin)
  }

  it should "The default value can be passed to the YEAR type as 0 or a value greater than or equal to 1901 or less than or equal to 2155." in {
    assertCompiles("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Year[java.time.Year] = YEAR.DEFAULT(0)
      val p2: Year[java.time.Year] = YEAR.DEFAULT(1901)
      val p3: Year[java.time.Year] = YEAR.DEFAULT(2155)
    """.stripMargin)
  }

  it should "If a value other than 0 or a value in the range 1901-2155 is passed, a default value of type Year will result in an error." in {
    assertDoesNotCompile("""
      import ldbc.core.*
      import ldbc.core.DataType.*

      val p1: Year[java.time.Year] = YEAR.DEFAULT(1)
      val p2: Year[java.time.Year] = YEAR.DEFAULT(1900)
      val p3: Year[java.time.Year] = YEAR.DEFAULT(2156)
    """.stripMargin)
  }
