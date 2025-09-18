/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import ldbc.connector.*

class EncodedTest extends FTestPlatform:

  test("Encoded case class should create instance with value and redacted flag") {
    val encoded1 = Encoded("test value", false)
    assertEquals(encoded1.value, "test value")
    assertEquals(encoded1.redacted, false)
    
    val encoded2 = Encoded("secret value", true)
    assertEquals(encoded2.value, "secret value")
    assertEquals(encoded2.redacted, true)
  }

  test("Encoded.apply(value: String) should create instance with redacted = false") {
    val encoded = Encoded("test value")
    assertEquals(encoded.value, "test value")
    assertEquals(encoded.redacted, false)
  }

  test("toString should return value when not redacted") {
    val encoded = Encoded("visible value", false)
    assertEquals(encoded.toString, "visible value")
    
    val encodedViaApply = Encoded("another visible value")
    assertEquals(encodedViaApply.toString, "another visible value")
  }

  test("toString should return REDACTED_TEXT when redacted") {
    val encoded = Encoded("secret password", true)
    assertEquals(encoded.toString, "?")
    assertEquals(encoded.toString, Encoded.REDACTED_TEXT)
    
    // Verify the actual value is still preserved
    assertEquals(encoded.value, "secret password")
  }

  test("REDACTED_TEXT constant should be '?'") {
    assertEquals(Encoded.REDACTED_TEXT, "?")
  }

  test("multiple Encoded instances should be independent") {
    val encoded1 = Encoded("value1", true)
    val encoded2 = Encoded("value2", false)
    val encoded3 = Encoded("value3")
    
    assertEquals(encoded1.toString, "?")
    assertEquals(encoded2.toString, "value2")
    assertEquals(encoded3.toString, "value3")
    
    assertEquals(encoded1.value, "value1")
    assertEquals(encoded2.value, "value2")
    assertEquals(encoded3.value, "value3")
  }

  test("Encoded should handle empty strings") {
    val encodedEmpty = Encoded("", false)
    assertEquals(encodedEmpty.value, "")
    assertEquals(encodedEmpty.toString, "")
    
    val redactedEmpty = Encoded("", true)
    assertEquals(redactedEmpty.value, "")
    assertEquals(redactedEmpty.toString, "?")
  }

  test("Encoded should handle special characters") {
    val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
    
    val encoded = Encoded(specialChars, false)
    assertEquals(encoded.value, specialChars)
    assertEquals(encoded.toString, specialChars)
    
    val redacted = Encoded(specialChars, true)
    assertEquals(redacted.value, specialChars)
    assertEquals(redacted.toString, "?")
  }

  test("Encoded should handle multi-line strings") {
    val multiLine = """Line 1
                      |Line 2
                      |Line 3""".stripMargin
    
    val encoded = Encoded(multiLine, false)
    assertEquals(encoded.value, multiLine)
    assertEquals(encoded.toString, multiLine)
    
    val redacted = Encoded(multiLine, true)
    assertEquals(redacted.value, multiLine)
    assertEquals(redacted.toString, "?")
  }

  test("Encoded should handle unicode characters") {
    val unicode = "Hello 世界 🌍"
    
    val encoded = Encoded(unicode, false)
    assertEquals(encoded.value, unicode)
    assertEquals(encoded.toString, unicode)
    
    val redacted = Encoded(unicode, true)
    assertEquals(redacted.value, unicode)
    assertEquals(redacted.toString, "?")
  }

  test("Encoded equals and hashCode should work correctly") {
    val encoded1 = Encoded("test", false)
    val encoded2 = Encoded("test", false)
    val encoded3 = Encoded("test", true)
    val encoded4 = Encoded("other", false)
    
    // Same value and redacted flag
    assertEquals(encoded1, encoded2)
    assertEquals(encoded1.hashCode, encoded2.hashCode)
    
    // Same value, different redacted flag
    assert(encoded1 != encoded3)
    
    // Different value, same redacted flag
    assert(encoded1 != encoded4)
  }

  test("pattern matching should work with Encoded") {
    val encoded = Encoded("test", true)
    
    encoded match {
      case Encoded(value, true) =>
        assertEquals(value, "test")
      case _ =>
        fail("Pattern match failed")
    }
    
    val result = encoded match {
      case Encoded(_, true) => "redacted"
      case Encoded(_, false) => "not redacted"
    }
    assertEquals(result, "redacted")
  }