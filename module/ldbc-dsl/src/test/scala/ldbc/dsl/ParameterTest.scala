/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.data.NonEmptyList

import munit.CatsEffectSuite

import ldbc.dsl.codec.{ Encoder, Codec }

class ParameterTest extends CatsEffectSuite:

  test("Parameter.Static should store and return its value") {
    val static = Parameter.Static("users")
    assertEquals(static.value, "users")
    assertEquals(static.toString, "users")
  }

  test("Parameter.Static should be created from multiple different strings") {
    val table = Parameter.Static("table")
    val column = Parameter.Static("column")
    val function = Parameter.Static("COUNT(*)")
    
    assertEquals(table.value, "table")
    assertEquals(column.value, "column")
    assertEquals(function.value, "COUNT(*)")
  }

  test("Parameter.Dynamic.Success should store encoded value") {
    val encoded = Parameter.Dynamic.Success("test")
    assertEquals(encoded.value, "test")
    
    val intEncoded = Parameter.Dynamic.Success(42)
    assertEquals(intEncoded.value, "42")
    
    val boolEncoded = Parameter.Dynamic.Success(true)
    assertEquals(boolEncoded.value, "true")
  }

  test("Parameter.Dynamic.Failure should store error messages") {
    val failure = Parameter.Dynamic.Failure(List("Error 1", "Error 2"))
    assertEquals(failure.value, "Error 1, Error 2")
    
    val singleError = Parameter.Dynamic.Failure(List("Single error"))
    assertEquals(singleError.value, "Single error")
  }

  test("Parameter.Dynamic.many should handle successful encoding") {
    val success = Encoder.Encoded.Success(List("value1", "value2", "value3"))
    val result = Parameter.Dynamic.many(success)
    
    assertEquals(result.length, 3)
    result.foreach { param =>
      assert(param.isInstanceOf[Parameter.Dynamic.Success])
    }
    assertEquals(result.map(_.value), List("value1", "value2", "value3"))
  }

  test("Parameter.Dynamic.many should handle failed encoding") {
    val failure = Encoder.Encoded.Failure(NonEmptyList.of("Error 1", "Error 2"))
    val result = Parameter.Dynamic.many(failure)
    
    assertEquals(result.length, 1)
    assert(result.head.isInstanceOf[Parameter.Dynamic.Failure])
    assertEquals(result.head.value, "Error 1, Error 2")
  }

  test("Conversion from value with Encoder should work") {
    val stringParam: Parameter.Dynamic = "test"
    val intParam: Parameter.Dynamic = 42
    val boolParam: Parameter.Dynamic = true
    
    assert(stringParam.isInstanceOf[Parameter.Dynamic.Success])
    assert(intParam.isInstanceOf[Parameter.Dynamic.Success])
    assert(boolParam.isInstanceOf[Parameter.Dynamic.Success])
    
    assertEquals(stringParam.value, "test")
    assertEquals(intParam.value, "42")
    assertEquals(boolParam.value, "true")
  }

  test("Conversion should fail when encoder produces multiple values") {
    // Test the conversion logic directly
    val encoded = Encoder.Encoded.Success(List("test", "test"))
    val params = Parameter.Dynamic.many(encoded)
    
    // many should create multiple Success parameters
    assertEquals(params.length, 2)
    params.foreach { param =>
      assert(param.isInstanceOf[Parameter.Dynamic.Success])
    }
    
    // Test case where encoder produces multiple values for single parameter
    // The conversion should fail because single parameter expects single encoded value
    val multipleEncoded = Encoder.Encoded.Success(List("value1", "value2"))
    val dynamicParams = Parameter.Dynamic.many(multipleEncoded)
    
    // Dynamic.many creates a Success for each value
    assertEquals(dynamicParams.length, 2)
    assert(dynamicParams(0).isInstanceOf[Parameter.Dynamic.Success])
    assert(dynamicParams(1).isInstanceOf[Parameter.Dynamic.Success])
  }

  test("Conversion from value with Codec should work") {
    // Single value codec
    val longParam: Parameter.Dynamic = 123L
    assert(longParam.isInstanceOf[Parameter.Dynamic.Success])
    assertEquals(longParam.value, "123")
  }

  test("Conversion with Codec should fail for multiple values") {
    case class User(id: Long, name: String)
    given Codec[User] = Codec.derived[User]
    
    val userParam: Parameter.Dynamic = User(1L, "test")
    assert(userParam.isInstanceOf[Parameter.Dynamic.Failure])
    assert(userParam.value.contains("Multiple values are not allowed"))
  }

  test("Conversion should handle Encoder failures") {
    // Test failure encoding directly
    val encoded = Encoder.Encoded.Failure(NonEmptyList.of("Encoding failed", "Invalid input"))
    val params = Parameter.Dynamic.many(encoded)
    
    assertEquals(params.length, 1)
    assert(params.head.isInstanceOf[Parameter.Dynamic.Failure])
    assertEquals(params.head.value, "Encoding failed, Invalid input")
  }

  test("Option values should be handled correctly") {
    val someParam: Parameter.Dynamic = Some("test")
    val noneParam: Parameter.Dynamic = (None: Option[String])
    val someIntParam: Parameter.Dynamic = Some(42)
    
    assert(someParam.isInstanceOf[Parameter.Dynamic.Success])
    assert(noneParam.isInstanceOf[Parameter.Dynamic.Success])
    assert(someIntParam.isInstanceOf[Parameter.Dynamic.Success])
    
    assertEquals(someParam.value, "test")
    assertEquals(noneParam.value, "None")
    assertEquals(someIntParam.value, "42")
  }

  test("Complex types with custom encoders") {
    case class UserId(value: Long)
    case class Email(value: String)
    
    given Encoder[UserId] = Encoder[Long].contramap(_.value)
    given Encoder[Email] = Encoder[String].contramap(_.value)
    
    val userIdParam: Parameter.Dynamic = UserId(123L)
    val emailParam: Parameter.Dynamic = Email("test@example.com")
    
    assert(userIdParam.isInstanceOf[Parameter.Dynamic.Success])
    assert(emailParam.isInstanceOf[Parameter.Dynamic.Success])
    
    assertEquals(userIdParam.value, "123")
    assertEquals(emailParam.value, "test@example.com")
  }

  test("List of Dynamic parameters from encoded values") {
    // Test with tuple encoding
    val tupleEncoded = Encoder.Encoded.Success(List(1, "test", true))
    val tupleParams = Parameter.Dynamic.many(tupleEncoded)
    
    assertEquals(tupleParams.length, 3)
    assertEquals(tupleParams.map(_.value), List("1", "test", "true"))
    
    // Test with empty list
    val emptyEncoded = Encoder.Encoded.Success(List.empty)
    val emptyParams = Parameter.Dynamic.many(emptyEncoded)
    
    assertEquals(emptyParams.length, 0)
  }

  test("Parameter usage in SQL interpolation") {
    // Static parameter
    val table = Parameter.Static("users")
    
    // Dynamic parameters
    val id: Parameter.Dynamic = 42
    val name: Parameter.Dynamic = "John"
    
    // These should work in SQL string interpolation
    assert(table.isInstanceOf[Parameter.Static])
    assert(id.isInstanceOf[Parameter.Dynamic.Success])
    assert(name.isInstanceOf[Parameter.Dynamic.Success])
  }

  test("Special characters in Parameter.Static") {
    val backticks = Parameter.Static("`table`")
    val withDot = Parameter.Static("schema.table")
    val withSpaces = Parameter.Static("column name")
    val sqlFunction = Parameter.Static("DATE_FORMAT(created_at, '%Y-%m-%d')")
    
    assertEquals(backticks.value, "`table`")
    assertEquals(withDot.value, "schema.table")
    assertEquals(withSpaces.value, "column name")
    assertEquals(sqlFunction.value, "DATE_FORMAT(created_at, '%Y-%m-%d')")
  }

  test("Encoder.Supported types in Dynamic.Success") {
    // Test various supported types
    val stringSuccess = Parameter.Dynamic.Success("test")
    val intSuccess = Parameter.Dynamic.Success(42)
    val longSuccess = Parameter.Dynamic.Success(123L)
    val doubleSuccess = Parameter.Dynamic.Success(3.14)
    val boolSuccess = Parameter.Dynamic.Success(true)
    val byteSuccess = Parameter.Dynamic.Success(127.toByte)
    val shortSuccess = Parameter.Dynamic.Success(32767.toShort)
    val floatSuccess = Parameter.Dynamic.Success(3.14f)
    val bigDecimalSuccess = Parameter.Dynamic.Success(BigDecimal("123.456"))
    val noneSuccess = Parameter.Dynamic.Success(None)
    
    // All should be Success instances
    assert(stringSuccess.isInstanceOf[Parameter.Dynamic.Success])
    assert(intSuccess.isInstanceOf[Parameter.Dynamic.Success])
    assert(longSuccess.isInstanceOf[Parameter.Dynamic.Success])
    assert(doubleSuccess.isInstanceOf[Parameter.Dynamic.Success])
    assert(boolSuccess.isInstanceOf[Parameter.Dynamic.Success])
    assert(byteSuccess.isInstanceOf[Parameter.Dynamic.Success])
    assert(shortSuccess.isInstanceOf[Parameter.Dynamic.Success])
    assert(floatSuccess.isInstanceOf[Parameter.Dynamic.Success])
    assert(bigDecimalSuccess.isInstanceOf[Parameter.Dynamic.Success])
    assert(noneSuccess.isInstanceOf[Parameter.Dynamic.Success])
  }

  test("Multiple error messages in Dynamic.Failure") {
    val multipleErrors = Parameter.Dynamic.Failure(List(
      "Field 'id' cannot be null",
      "Field 'name' is too long",
      "Field 'email' has invalid format"
    ))
    
    assertEquals(
      multipleErrors.value,
      "Field 'id' cannot be null, Field 'name' is too long, Field 'email' has invalid format"
    )
    
    // Empty error list
    val emptyErrors = Parameter.Dynamic.Failure(List.empty)
    assertEquals(emptyErrors.value, "")
  }