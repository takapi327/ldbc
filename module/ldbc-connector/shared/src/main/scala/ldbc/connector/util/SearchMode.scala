/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

/**
 * Search mode flags enumeration. Primarily used by [[StringInspector]].
 */
enum SearchMode:

  // Allow backslash escapes.
  case ALLOW_BACKSLASH_ESCAPE

  // Skip between markers (quoted text, quoted identifiers, text between parentheses).
  case SKIP_BETWEEN_MARKERS
  
  // Skip block comments ("/* text... *\/").
  case SKIP_BLOCK_COMMENTS

  // Skip line comments ("-- text...", "# text...").
  case SKIP_LINE_COMMENTS
  
  // Skip MySQL specific markers ("/*![12345]" and "*\/") but not their contents.
  case SKIP_MYSQL_MARKERS

  // Skip hint blocks ("/*+ text... *\/").
  case SKIP_HINT_BLOCKS

  // Skip white space.
  case SKIP_WHITE_SPACE

  // Dummy search mode. Does nothing.
  case VOID
