/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.util

/**
 * Simple XML parser utility for extracting content from XML documents.
 * 
 * This parser provides basic XML parsing functionality without requiring a full XML library.
 * It's designed specifically for parsing AWS service responses and handles common XML patterns.
 * The parser performs basic entity decoding and content extraction but does not validate
 * XML structure or handle complex XML features like namespaces, CDATA, or processing instructions.
 */
object SimpleXmlParser:

  /**
   * Decodes standard XML entities to their corresponding characters.
   * 
   * This method replaces common XML entities with their actual character representations:
   * - &amp; → &
   * - &lt; → <
   * - &gt; → >
   * - &quot; → "
   * - &apos; → '
   * 
   * @param s The string containing XML entities to decode
   * @return The string with XML entities decoded to their character equivalents
   */
  def decodeXmlEntities(s: String): String =
    s.replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&apos;", "'")

  /**
   * Extracts the content between XML tags.
   * 
   * Finds the first occurrence of the specified XML tag and extracts its content.
   * The content is trimmed and XML entities are decoded. This method only handles
   * simple tags without attributes and does not support nested tags with the same name.
   * 
   * @param tagName The name of the XML tag (without angle brackets)
   * @param xml The XML string to search in
   * @return Some(content) if the tag is found with content, None otherwise
   */
  def extractTagContent(tagName: String, xml: String): Option[String] = {
    val startTag = s"<$tagName>"
    val endTag   = s"</$tagName>"
    val startIdx = xml.indexOf(startTag)

    if startIdx < 0 then None
    else {
      val contentStart = startIdx + startTag.length
      val endIdx       = xml.indexOf(endTag, contentStart)
      if endIdx < 0 then None
      else Some(decodeXmlEntities(xml.substring(contentStart, endIdx).trim))
    }
  }

  /**
   * Extracts a complete XML section including the opening and closing tags.
   * 
   * Finds the first occurrence of the specified XML tag and extracts the entire
   * section including the tags themselves. This is useful for extracting nested
   * XML structures that need further processing.
   * 
   * @param tagName The name of the XML tag (without angle brackets)
   * @param xml The XML string to search in
   * @return Some(section) if the tag section is found, None otherwise
   */
  def extractSection(tagName: String, xml: String): Option[String] = {
    val startTag = s"<$tagName>"
    val endTag   = s"</$tagName>"
    val startIdx = xml.indexOf(startTag)

    if startIdx < 0 then None
    else {
      val endIdx = xml.indexOf(endTag, startIdx)
      if endIdx < 0 then None
      else Some(xml.substring(startIdx, endIdx + endTag.length))
    }
  }

  /**
   * Extracts tag content and throws an exception if the tag is missing or empty.
   * 
   * This is a strict version of extractTagContent that requires the tag to exist
   * and have non-empty content. If the tag is missing, empty, or contains only
   * whitespace, an IllegalArgumentException is thrown with the provided error message.
   * 
   * @param tagName The name of the XML tag (without angle brackets)
   * @param xml The XML string to search in
   * @param errorMsg The error message to use if the tag is missing or empty
   * @return The tag content if found and non-empty
   * @throws IllegalArgumentException if the tag is missing, empty, or contains only whitespace
   */
  def requireTag(tagName: String, xml: String, errorMsg: String): String =
    extractTagContent(tagName, xml)
      .filter(_.nonEmpty)
      .getOrElse(throw new IllegalArgumentException(errorMsg))
