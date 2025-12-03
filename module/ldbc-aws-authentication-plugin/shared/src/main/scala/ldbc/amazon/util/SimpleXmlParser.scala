/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.util

object SimpleXmlParser:
  def decodeXmlEntities(s: String): String =
    s.replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&apos;", "'")

  def extractTagContent(tagName: String, xml: String): Option[String] = {
    val startTag = s"<$tagName>"
    val endTag = s"</$tagName>"
    val startIdx = xml.indexOf(startTag)

    if (startIdx < 0) None
    else {
      val contentStart = startIdx + startTag.length
      val endIdx = xml.indexOf(endTag, contentStart)
      if (endIdx < 0) None
      else Some(decodeXmlEntities(xml.substring(contentStart, endIdx).trim))
    }
  }

  def extractSection(tagName: String, xml: String): Option[String] = {
    val startTag = s"<$tagName>"
    val endTag = s"</$tagName>"
    val startIdx = xml.indexOf(startTag)

    if (startIdx < 0) None
    else {
      val endIdx = xml.indexOf(endTag, startIdx)
      if (endIdx < 0) None
      else Some(xml.substring(startIdx, endIdx + endTag.length))
    }
  }

  def requireTag(tagName: String, xml: String, errorMsg: String): String =
    extractTagContent(tagName, xml)
      .filter(_.nonEmpty)
      .getOrElse(throw new IllegalArgumentException(errorMsg))
