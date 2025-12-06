/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.util

import munit.CatsEffectSuite

class SimpleJsonParserTest extends CatsEffectSuite:

  test("parse empty object") {
    val result = SimpleJsonParser.parse("{}")
    assert(result.isRight)
    assertEquals(result.map(_.fields.size).toOption.get, 0)
  }

  test("parse object with single string field") {
    val json   = """{"key": "value"}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("key")).toOption.flatten, Some("value"))
    assertEquals(result.map(_.getOrEmpty("key")).toOption.get, "value")
  }

  test("parse object with multiple string fields") {
    val json   = """{"AccessKeyId": "AKIAIOSFODNN7EXAMPLE", "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("AccessKeyId")).toOption.flatten, Some("AKIAIOSFODNN7EXAMPLE"))
    assertEquals(result.map(_.get("SecretAccessKey")).toOption.flatten, Some("wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"))
  }

  test("parse object with number values") {
    val json   = """{"port": 3306, "timeout": 30.5}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("port")).toOption.flatten, Some("3306"))
    assertEquals(result.map(_.get("timeout")).toOption.flatten, Some("30.5"))
  }

  test("parse object with boolean values") {
    val json   = """{"enabled": true, "disabled": false}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("enabled")).toOption.flatten, Some("true"))
    assertEquals(result.map(_.get("disabled")).toOption.flatten, Some("false"))
  }

  test("parse object with null values") {
    val json   = """{"nullValue": null}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("nullValue")).toOption.flatten, None)
    assertEquals(result.map(_.getOrEmpty("nullValue")).toOption.get, "")
  }

  test("parse object with escaped string values") {
    val json   = """{"escaped": "Hello \"World\"", "newline": "Line1\nLine2"}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("escaped")).toOption.flatten, Some("Hello \"World\""))
    assertEquals(result.map(_.get("newline")).toOption.flatten, Some("Line1\nLine2"))
  }

  test("parse object with unicode escape sequences") {
    val json   = """{"unicode": "\u0041\u0042\u0043"}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("unicode")).toOption.flatten, Some("ABC"))
  }

  test("parse object with whitespace") {
    val json   = """  {  "key"  :  "value"  ,  "another"  :  42  }  """
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("key")).toOption.flatten, Some("value"))
    assertEquals(result.map(_.get("another")).toOption.flatten, Some("42"))
  }

  test("parse object with nested objects (should skip)") {
    val json   = """{"nested": {"inner": "value"}, "simple": "text"}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("nested")).toOption.flatten, Some("{...}"))
    assertEquals(result.map(_.get("simple")).toOption.flatten, Some("text"))
  }

  test("parse object with arrays (should skip)") {
    val json   = """{"array": [1, 2, 3], "simple": "text"}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("array")).toOption.flatten, Some("[...]"))
    assertEquals(result.map(_.get("simple")).toOption.flatten, Some("text"))
  }

  test("JsonObject.require returns Right for existing key") {
    val json   = """{"AccessKeyId": "AKIAIOSFODNN7EXAMPLE"}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    val required = result.map(_.require("AccessKeyId"))
    assert(required.toOption.get.isRight)
    assertEquals(required.toOption.get.toOption.get, "AKIAIOSFODNN7EXAMPLE")
  }

  test("JsonObject.require returns Left for null value") {
    val json   = """{"nullValue": null}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    val required = result.map(_.require("nullValue"))
    assert(required.toOption.get.isLeft)
    assertEquals(required.toOption.get.left.toOption.get, "Field 'nullValue' is null")
  }

  test("JsonObject.require returns Left for missing key") {
    val json   = """{"AccessKeyId": "AKIAIOSFODNN7EXAMPLE"}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    val required = result.map(_.require("MissingKey"))
    assert(required.toOption.get.isLeft)
    assertEquals(required.toOption.get.left.toOption.get, "Required field 'MissingKey' not found")
  }

  test("JsonObject.getOrEmpty returns empty string for missing key") {
    val json   = """{"AccessKeyId": "AKIAIOSFODNN7EXAMPLE"}"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.getOrEmpty("MissingKey")).toOption.get, "")
  }

  test("fail to parse invalid JSON - not an object") {
    val result = SimpleJsonParser.parse("\"just a string\"")
    assert(result.isLeft)
    assert(result.left.toOption.get.contains("Invalid JSON: must be an object"))
  }

  test("fail to parse invalid JSON - missing closing brace") {
    val result = SimpleJsonParser.parse("""{"key": "value"""")
    assert(result.isLeft)
  }

  test("fail to parse invalid JSON - missing colon") {
    val result = SimpleJsonParser.parse("""{"key" "value"}""")
    assert(result.isLeft)
  }

  test("fail to parse invalid JSON - unterminated string") {
    val result = SimpleJsonParser.parse("""{"key": "value}""")
    assert(result.isLeft)
  }

  test("fail to parse invalid JSON - invalid escape sequence") {
    val result = SimpleJsonParser.parse("""{"key": "value\q"}""")
    assert(result.isLeft)
  }

  test("fail to parse invalid JSON - incomplete unicode escape") {
    val result = SimpleJsonParser.parse("""{"key": "value\u00"}""")
    assert(result.isLeft)
  }

  test("parse complex AWS credentials response") {
    val json = """{
      "AccessKeyId": "ASIAIOSFODNN7EXAMPLE",
      "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY",
      "Token": "IQoJb3JpZ2luX2VjECoaCXVzLWVhc3QtMSJIMEYCIQD6m6XYcCTgK8jELjQXqKE",
      "Expiration": "2024-12-06T12:34:56Z"
    }"""
    val result = SimpleJsonParser.parse(json)
    assert(result.isRight)
    assertEquals(result.map(_.get("AccessKeyId")).toOption.flatten, Some("ASIAIOSFODNN7EXAMPLE"))
    assertEquals(result.map(_.get("SecretAccessKey")).toOption.flatten, Some("wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"))
    assertEquals(result.map(_.get("Token")).toOption.flatten, Some("IQoJb3JpZ2luX2VjECoaCXVzLWVhc3QtMSJIMEYCIQD6m6XYcCTgK8jELjQXqKE"))
    assertEquals(result.map(_.get("Expiration")).toOption.flatten, Some("2024-12-06T12:34:56Z"))
  }