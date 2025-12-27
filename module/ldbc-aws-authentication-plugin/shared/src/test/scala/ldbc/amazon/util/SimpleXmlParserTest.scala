/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.util

import munit.CatsEffectSuite

class SimpleXmlParserTest extends CatsEffectSuite:

  test("decodeXmlEntities should decode all standard XML entities") {
    val input    = "&amp;&lt;&gt;&quot;&apos;"
    val expected = "&<>\"'"
    assertEquals(SimpleXmlParser.decodeXmlEntities(input), expected)
  }

  test("decodeXmlEntities should handle mixed content with entities") {
    val input    = "Hello &amp; welcome to &lt;XML&gt; parsing &quot;test&quot;"
    val expected = "Hello & welcome to <XML> parsing \"test\""
    assertEquals(SimpleXmlParser.decodeXmlEntities(input), expected)
  }

  test("decodeXmlEntities should handle text without entities") {
    val input = "Plain text without any entities"
    assertEquals(SimpleXmlParser.decodeXmlEntities(input), input)
  }

  test("decodeXmlEntities should handle empty string") {
    assertEquals(SimpleXmlParser.decodeXmlEntities(""), "")
  }

  test("extractTagContent should extract simple tag content") {
    val xml    = "<name>John Doe</name>"
    val result = SimpleXmlParser.extractTagContent("name", xml)
    assertEquals(result, Some("John Doe"))
  }

  test("extractTagContent should extract content with whitespace trimmed") {
    val xml    = "<name>  John Doe  </name>"
    val result = SimpleXmlParser.extractTagContent("name", xml)
    assertEquals(result, Some("John Doe"))
  }

  test("extractTagContent should extract content with XML entities decoded") {
    val xml    = "<message>Hello &amp; welcome to &lt;XML&gt;</message>"
    val result = SimpleXmlParser.extractTagContent("message", xml)
    assertEquals(result, Some("Hello & welcome to <XML>"))
  }

  test("extractTagContent should return None for non-existent tag") {
    val xml    = "<name>John Doe</name>"
    val result = SimpleXmlParser.extractTagContent("email", xml)
    assertEquals(result, None)
  }

  test("extractTagContent should return None for malformed XML (missing end tag)") {
    val xml    = "<name>John Doe"
    val result = SimpleXmlParser.extractTagContent("name", xml)
    assertEquals(result, None)
  }

  test("extractTagContent should extract from complex XML structure") {
    val xml         = """
      <response>
        <user>
          <name>John Doe</name>
          <email>john@example.com</email>
        </user>
      </response>
    """
    val nameResult  = SimpleXmlParser.extractTagContent("name", xml)
    val emailResult = SimpleXmlParser.extractTagContent("email", xml)
    assertEquals(nameResult, Some("John Doe"))
    assertEquals(emailResult, Some("john@example.com"))
  }

  test("extractTagContent should handle nested tags with same name") {
    val xml    = "<outer><name>Outer Name</name><inner><name>Inner Name</name></inner></outer>"
    val result = SimpleXmlParser.extractTagContent("name", xml)
    // Should extract the first occurrence
    assertEquals(result, Some("Outer Name"))
  }

  test("extractSection should extract complete XML section") {
    val xml    = """
      <response>
        <user>
          <name>John Doe</name>
          <email>john@example.com</email>
        </user>
        <status>success</status>
      </response>
    """
    val result = SimpleXmlParser.extractSection("user", xml)
    assert(result.isDefined)
    val userSection = result.get
    assert(userSection.contains("<user>"))
    assert(userSection.contains("</user>"))
    assert(userSection.contains("John Doe"))
    assert(userSection.contains("john@example.com"))
  }

  test("extractSection should return None for non-existent section") {
    val xml    = "<response><status>success</status></response>"
    val result = SimpleXmlParser.extractSection("user", xml)
    assertEquals(result, None)
  }

  test("extractSection should return None for malformed XML section") {
    val xml    = "<user><name>John Doe</name>"
    val result = SimpleXmlParser.extractSection("user", xml)
    assertEquals(result, None)
  }

  test("extractSection should handle nested sections") {
    val xml    = """
      <outer>
        <inner>
          <data>test</data>
        </inner>
      </outer>
    """
    val result = SimpleXmlParser.extractSection("inner", xml)
    assert(result.isDefined)
    val innerSection = result.get
    assertEquals(innerSection.trim, "<inner>\n          <data>test</data>\n        </inner>")
  }

  test("requireTag should return content for existing tag") {
    val xml    = "<AccessKeyId>AKIAIOSFODNN7EXAMPLE</AccessKeyId>"
    val result = SimpleXmlParser.requireTag("AccessKeyId", xml, "AccessKeyId not found")
    assertEquals(result, "AKIAIOSFODNN7EXAMPLE")
  }

  test("requireTag should throw exception for non-existent tag") {
    val xml = "<name>John Doe</name>"
    intercept[IllegalArgumentException] {
      SimpleXmlParser.requireTag("email", xml, "Email tag not found")
    }
  }

  test("requireTag should throw exception for empty tag content") {
    val xml = "<name></name>"
    intercept[IllegalArgumentException] {
      SimpleXmlParser.requireTag("name", xml, "Name cannot be empty")
    }
  }

  test("requireTag should throw exception for whitespace-only content") {
    val xml = "<name>   </name>"
    intercept[IllegalArgumentException] {
      SimpleXmlParser.requireTag("name", xml, "Name cannot be empty")
    }
  }

  test("requireTag should handle valid content with entities") {
    val xml    = "<message>Hello &amp; welcome</message>"
    val result = SimpleXmlParser.requireTag("message", xml, "Message not found")
    assertEquals(result, "Hello & welcome")
  }

  test("parse AWS STS AssumeRoleWithWebIdentity response") {
    val stsResponse = """<?xml version="1.0" encoding="UTF-8"?>
      <AssumeRoleWithWebIdentityResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
        <AssumeRoleWithWebIdentityResult>
          <Credentials>
            <AccessKeyId>ASIAIOSFODNN7EXAMPLE</AccessKeyId>
            <SecretAccessKey>wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY</SecretAccessKey>
            <SessionToken>IQoJb3JpZ2luX2VjECoaCXVzLWVhc3QtMSJHMEUCIQDtqstfDEaRfZKFK5Z2n2CnP3</SessionToken>
            <Expiration>2024-12-06T12:00:00Z</Expiration>
          </Credentials>
          <SubjectFromWebIdentityToken>amzn1.account.AF6RHO7KZU5XRVQJGXK6HB56KR2A</SubjectFromWebIdentityToken>
          <AssumedRoleUser>
            <AssumedRoleId>AROA3XFRBF535PLBQAARL:app-session-123</AssumedRoleId>
            <Arn>arn:aws:sts::123456789012:assumed-role/TestRole/app-session-123</Arn>
          </AssumedRoleUser>
        </AssumeRoleWithWebIdentityResult>
        <ResponseMetadata>
          <RequestId>c6104cbe-af31-11e0-8154-cbc7ccf896c7</RequestId>
        </ResponseMetadata>
      </AssumeRoleWithWebIdentityResponse>"""

    // Extract credentials section
    val credentialsSection = SimpleXmlParser.extractSection("Credentials", stsResponse)
    assert(credentialsSection.isDefined)

    // Extract individual credential fields
    val credentials     = credentialsSection.get
    val accessKeyId     = SimpleXmlParser.extractTagContent("AccessKeyId", credentials)
    val secretAccessKey = SimpleXmlParser.extractTagContent("SecretAccessKey", credentials)
    val sessionToken    = SimpleXmlParser.extractTagContent("SessionToken", credentials)
    val expiration      = SimpleXmlParser.extractTagContent("Expiration", credentials)

    assertEquals(accessKeyId, Some("ASIAIOSFODNN7EXAMPLE"))
    assertEquals(secretAccessKey, Some("wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"))
    assertEquals(sessionToken, Some("IQoJb3JpZ2luX2VjECoaCXVzLWVhc3QtMSJHMEUCIQDtqstfDEaRfZKFK5Z2n2CnP3"))
    assertEquals(expiration, Some("2024-12-06T12:00:00Z"))

    // Test requireTag functionality
    val requiredAccessKeyId = SimpleXmlParser.requireTag("AccessKeyId", credentials, "AccessKeyId is required")
    assertEquals(requiredAccessKeyId, "ASIAIOSFODNN7EXAMPLE")
  }

  test("parse AWS STS error response") {
    val errorResponse = """<?xml version="1.0" encoding="UTF-8"?>
      <ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
        <Error>
          <Type>Sender</Type>
          <Code>InvalidParameterValue</Code>
          <Message>The security token included in the request is invalid</Message>
        </Error>
        <RequestId>c6104cbe-af31-11e0-8154-cbc7ccf896c7</RequestId>
      </ErrorResponse>"""

    val errorSection = SimpleXmlParser.extractSection("Error", errorResponse)
    assert(errorSection.isDefined)

    val error        = errorSection.get
    val errorType    = SimpleXmlParser.extractTagContent("Type", error)
    val errorCode    = SimpleXmlParser.extractTagContent("Code", error)
    val errorMessage = SimpleXmlParser.extractTagContent("Message", error)

    assertEquals(errorType, Some("Sender"))
    assertEquals(errorCode, Some("InvalidParameterValue"))
    assertEquals(errorMessage, Some("The security token included in the request is invalid"))
  }

  test("handle XML with special characters and entities") {
    val xml    = """<message>Data contains &lt;brackets&gt; &amp; "quotes" &apos;apostrophes&apos;</message>"""
    val result = SimpleXmlParser.extractTagContent("message", xml)
    assertEquals(result, Some("Data contains <brackets> & \"quotes\" 'apostrophes'"))
  }

  test("handle empty XML documents") {
    val xml    = ""
    val result = SimpleXmlParser.extractTagContent("any", xml)
    assertEquals(result, None)
  }

  test("handle XML with CDATA sections") {
    val xml = "<data><![CDATA[Some raw data with <tags> and & entities]]></data>"
    // Note: This simple parser doesn't handle CDATA, but should still extract content
    val result = SimpleXmlParser.extractTagContent("data", xml)
    assertEquals(result, Some("<![CDATA[Some raw data with <tags> and & entities]]>"))
  }
