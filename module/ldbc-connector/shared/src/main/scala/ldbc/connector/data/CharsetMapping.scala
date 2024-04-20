/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.nio.charset.Charset
import java.util.Locale

import ldbc.connector.util.Version

/**
 * Mapping between MySQL charset names and Java charset names.
 * 
 * see: https://github.com/mysql/mysql-connector-j/blob/release/8.x/src/main/core-api/java/com/mysql/cj/CharsetMapping.java#L304
 */
object CharsetMapping:

  private val MAP_SIZE = 1024

  val MYSQL_CHARSET_NAME_armscii8 = "armscii8"
  val MYSQL_CHARSET_NAME_ascii    = "ascii"
  val MYSQL_CHARSET_NAME_big5     = "big5"
  val MYSQL_CHARSET_NAME_binary   = "binary"
  val MYSQL_CHARSET_NAME_cp1250   = "cp1250"
  val MYSQL_CHARSET_NAME_cp1251   = "cp1251"
  val MYSQL_CHARSET_NAME_cp1256   = "cp1256"
  val MYSQL_CHARSET_NAME_cp1257   = "cp1257"
  val MYSQL_CHARSET_NAME_cp850    = "cp850"
  val MYSQL_CHARSET_NAME_cp852    = "cp852"
  val MYSQL_CHARSET_NAME_cp866    = "cp866"
  val MYSQL_CHARSET_NAME_cp932    = "cp932"
  val MYSQL_CHARSET_NAME_dec8     = "dec8"
  val MYSQL_CHARSET_NAME_eucjpms  = "eucjpms"
  val MYSQL_CHARSET_NAME_euckr    = "euckr"
  val MYSQL_CHARSET_NAME_gb18030  = "gb18030"
  val MYSQL_CHARSET_NAME_gb2312   = "gb2312"
  val MYSQL_CHARSET_NAME_gbk      = "gbk"
  val MYSQL_CHARSET_NAME_geostd8  = "geostd8"
  val MYSQL_CHARSET_NAME_greek    = "greek"
  val MYSQL_CHARSET_NAME_hebrew   = "hebrew"
  val MYSQL_CHARSET_NAME_hp8      = "hp8"
  val MYSQL_CHARSET_NAME_keybcs2  = "keybcs2"
  val MYSQL_CHARSET_NAME_koi8r    = "koi8r"
  val MYSQL_CHARSET_NAME_koi8u    = "koi8u"
  val MYSQL_CHARSET_NAME_latin1   = "latin1"
  val MYSQL_CHARSET_NAME_latin2   = "latin2"
  val MYSQL_CHARSET_NAME_latin5   = "latin5"
  val MYSQL_CHARSET_NAME_latin7   = "latin7"
  val MYSQL_CHARSET_NAME_macce    = "macce"
  val MYSQL_CHARSET_NAME_macroman = "macroman"
  val MYSQL_CHARSET_NAME_sjis     = "sjis"
  val MYSQL_CHARSET_NAME_swe7     = "swe7"
  val MYSQL_CHARSET_NAME_tis620   = "tis620"
  val MYSQL_CHARSET_NAME_ucs2     = "ucs2"
  val MYSQL_CHARSET_NAME_ujis     = "ujis"
  val MYSQL_CHARSET_NAME_utf16    = "utf16"
  val MYSQL_CHARSET_NAME_utf16le  = "utf16le"
  val MYSQL_CHARSET_NAME_utf32    = "utf32"
  val MYSQL_CHARSET_NAME_utf8     = "utf8"
  val MYSQL_CHARSET_NAME_utf8mb3  = "utf8mb3"
  val MYSQL_CHARSET_NAME_utf8mb4  = "utf8mb4"

  val MYSQL_COLLATION_INDEX_utf8mb4_general_ci = 45
  val MYSQL_COLLATION_INDEX_utf8mb4_0900_ai_ci = 255
  val MYSQL_COLLATION_INDEX_binary             = 63

  val charsets: List[MysqlCharset] =
    List(
      MysqlCharset(MYSQL_CHARSET_NAME_ascii, 1, 0, List("US-ASCII", "ASCII")),
      MysqlCharset(MYSQL_CHARSET_NAME_big5, 2, 0, List("Big5")),
      MysqlCharset(MYSQL_CHARSET_NAME_gbk, 2, 0, List("GBK")),
      MysqlCharset(MYSQL_CHARSET_NAME_sjis, 2, 0, List("SHIFT_JIS", "Cp943", "WINDOWS-31J")),
      MysqlCharset(MYSQL_CHARSET_NAME_cp932, 2, 1, List("WINDOWS-31J")),
      MysqlCharset(MYSQL_CHARSET_NAME_gb2312, 2, 0, List("GB2312")),
      MysqlCharset(MYSQL_CHARSET_NAME_ujis, 3, 0, List("EUC-JP")),
      MysqlCharset(MYSQL_CHARSET_NAME_eucjpms, 3, 0, List("EUC_JP_Solaris"), Version(5, 0, 3)),
      MysqlCharset(MYSQL_CHARSET_NAME_gb18030, 4, 0, List("GB18030"), Version(5, 7, 4)),
      MysqlCharset(MYSQL_CHARSET_NAME_euckr, 2, 0, List("EUC-KR")),
      MysqlCharset(MYSQL_CHARSET_NAME_latin1, 1, 1, List("Cp1252", "ISO8859_1")),
      MysqlCharset(MYSQL_CHARSET_NAME_swe7, 1, 0, List("Cp1252")),
      MysqlCharset(MYSQL_CHARSET_NAME_hp8, 1, 0, List("Cp1252")),
      MysqlCharset(MYSQL_CHARSET_NAME_dec8, 1, 0, List("Cp1252")),
      MysqlCharset(MYSQL_CHARSET_NAME_armscii8, 1, 0, List("Cp1252")),
      MysqlCharset(MYSQL_CHARSET_NAME_geostd8, 1, 0, List("Cp1252")),
      MysqlCharset(MYSQL_CHARSET_NAME_latin2, 1, 0, List("ISO8859_2")),
      MysqlCharset(MYSQL_CHARSET_NAME_greek, 1, 0, List("ISO8859_7", "greek")),
      MysqlCharset(MYSQL_CHARSET_NAME_latin7, 1, 0, List("ISO-8859-13")),
      MysqlCharset(MYSQL_CHARSET_NAME_hebrew, 1, 0, List("ISO8859_8")),
      MysqlCharset(MYSQL_CHARSET_NAME_latin5, 1, 0, List("ISO8859_9")),
      MysqlCharset(MYSQL_CHARSET_NAME_cp850, 2, 0, List("Cp850", "Cp437")),
      MysqlCharset(MYSQL_CHARSET_NAME_cp852, 1, 0, List("Cp852")),
      MysqlCharset(MYSQL_CHARSET_NAME_keybcs2, 1, 0, List("Cp852")),
      MysqlCharset(MYSQL_CHARSET_NAME_cp866, 1, 0, List("Cp866")),
      MysqlCharset(MYSQL_CHARSET_NAME_koi8r, 1, 0, List("KOI8_R")),
      MysqlCharset(MYSQL_CHARSET_NAME_koi8u, 1, 0, List("KOI8_U")),
      MysqlCharset(MYSQL_CHARSET_NAME_tis620, 1, 0, List("TIS620")),
      MysqlCharset(MYSQL_CHARSET_NAME_cp1250, 1, 0, List("Cp1250")),
      MysqlCharset(MYSQL_CHARSET_NAME_cp1251, 1, 0, List("Cp1251")),
      MysqlCharset(MYSQL_CHARSET_NAME_cp1256, 1, 0, List("Cp1256")),
      MysqlCharset(MYSQL_CHARSET_NAME_cp1257, 1, 0, List("Cp1257")),
      MysqlCharset(MYSQL_CHARSET_NAME_macroman, 1, 0, List("MacRoman")),
      MysqlCharset(MYSQL_CHARSET_NAME_macce, 1, 0, List("MacCentralEurope")),
      MysqlCharset(MYSQL_CHARSET_NAME_utf8mb3, 3, 0, List("UTF-8"), List(MYSQL_CHARSET_NAME_utf8)),
      MysqlCharset(MYSQL_CHARSET_NAME_utf8mb4, 4, 1, List("UTF-8")),
      MysqlCharset(MYSQL_CHARSET_NAME_binary, 1, 1, List("ISO8859_1")),
      MysqlCharset(MYSQL_CHARSET_NAME_ucs2, 2, 0, List("UnicodeBig")),
      MysqlCharset(MYSQL_CHARSET_NAME_utf16, 4, 0, List("UTF-16")),
      MysqlCharset(MYSQL_CHARSET_NAME_utf16le, 4, 0, List("UTF-16LE")),
      MysqlCharset(MYSQL_CHARSET_NAME_utf32, 4, 0, List("UTF-32"))
    )

  val collations: List[Collation] = List(
    Collation(1, "big5_chinese_ci", 1, MYSQL_CHARSET_NAME_big5),
    Collation(2, "latin2_czech_cs", 1, MYSQL_CHARSET_NAME_latin2),
    Collation(3, "dec8_swedish_ci", 0, MYSQL_CHARSET_NAME_dec8),
    Collation(4, "cp850_general_ci", 1, MYSQL_CHARSET_NAME_cp850),
    Collation(5, "latin1_german1_ci", 0, MYSQL_CHARSET_NAME_latin1),
    Collation(6, "hp8_english_ci", 0, MYSQL_CHARSET_NAME_hp8),
    Collation(7, "koi8r_general_ci", 0, MYSQL_CHARSET_NAME_koi8r),
    Collation(8, "latin1_swedish_ci", 1, MYSQL_CHARSET_NAME_latin1),
    Collation(9, "latin2_general_ci", 1, MYSQL_CHARSET_NAME_latin2),
    Collation(10, "swe7_swedish_ci", 0, MYSQL_CHARSET_NAME_swe7),
    Collation(11, "ascii_general_ci", 1, MYSQL_CHARSET_NAME_ascii),
    Collation(12, "ujis_japanese_ci", 0, MYSQL_CHARSET_NAME_ujis),
    Collation(13, "sjis_japanese_ci", 0, MYSQL_CHARSET_NAME_sjis),
    Collation(14, "cp1251_bulgarian_ci", 1, MYSQL_CHARSET_NAME_cp1251),
    Collation(15, "latin1_danish_ci", 0, MYSQL_CHARSET_NAME_latin1),
    Collation(16, "hebrew_general_ci", 0, MYSQL_CHARSET_NAME_hebrew),
    // 17
    Collation(18, "tis620_thai_ci", 0, MYSQL_CHARSET_NAME_tis620),
    Collation(19, "euckr_korean_ci", 0, MYSQL_CHARSET_NAME_euckr),
    Collation(20, "latin7_estonian_cs", 0, MYSQL_CHARSET_NAME_latin7),
    Collation(21, "latin2_hungarian_ci", 0, MYSQL_CHARSET_NAME_latin2),
    Collation(22, "koi8u_general_ci", 0, MYSQL_CHARSET_NAME_koi8u),
    Collation(23, "cp1251_ukrainian_ci", 1, MYSQL_CHARSET_NAME_cp1251),
    Collation(24, "gb2312_chinese_ci", 0, MYSQL_CHARSET_NAME_gb2312),
    Collation(25, "greek_general_ci", 0, MYSQL_CHARSET_NAME_greek),
    Collation(26, "cp1250_general_ci", 1, MYSQL_CHARSET_NAME_cp1250),
    Collation(27, "latin2_croatian_ci", 0, MYSQL_CHARSET_NAME_latin2),
    Collation(28, "gbk_chinese_ci", 1, MYSQL_CHARSET_NAME_gbk),
    Collation(29, "cp1257_lithuanian_ci", 0, MYSQL_CHARSET_NAME_cp1257),
    Collation(30, "latin5_turkish_ci", 1, MYSQL_CHARSET_NAME_latin5),
    Collation(31, "latin1_german2_ci", 0, MYSQL_CHARSET_NAME_latin1),
    Collation(32, "armscii8_general_ci", 0, MYSQL_CHARSET_NAME_armscii8),
    Collation(33, List("utf8mb3_general_ci", "utf8_general_ci"), 1, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(34, "cp1250_czech_cs", 0, MYSQL_CHARSET_NAME_cp1250),
    Collation(35, "ucs2_general_ci", 1, MYSQL_CHARSET_NAME_ucs2),
    Collation(36, "cp866_general_ci", 1, MYSQL_CHARSET_NAME_cp866),
    Collation(37, "keybcs2_general_ci", 1, MYSQL_CHARSET_NAME_keybcs2),
    Collation(38, "macce_general_ci", 1, MYSQL_CHARSET_NAME_macce),
    Collation(39, "macroman_general_ci", 1, MYSQL_CHARSET_NAME_macroman),
    Collation(40, "cp852_general_ci", 1, MYSQL_CHARSET_NAME_cp852),
    Collation(41, "latin7_general_ci", 1, MYSQL_CHARSET_NAME_latin7),
    Collation(42, "latin7_general_cs", 0, MYSQL_CHARSET_NAME_latin7),
    Collation(43, "macce_bin", 0, MYSQL_CHARSET_NAME_macce),
    Collation(44, "cp1250_croatian_ci", 0, MYSQL_CHARSET_NAME_cp1250),
    Collation(45, "utf8mb4_general_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(46, "utf8mb4_bin", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(47, "latin1_bin", 0, MYSQL_CHARSET_NAME_latin1),
    Collation(48, "latin1_general_ci", 0, MYSQL_CHARSET_NAME_latin1),
    Collation(49, "latin1_general_cs", 0, MYSQL_CHARSET_NAME_latin1),
    Collation(50, "cp1251_bin", 0, MYSQL_CHARSET_NAME_cp1251),
    Collation(51, "cp1251_general_ci", 1, MYSQL_CHARSET_NAME_cp1251),
    Collation(52, "cp1251_general_cs", 0, MYSQL_CHARSET_NAME_cp1251),
    Collation(53, "macroman_bin", 0, MYSQL_CHARSET_NAME_macroman),
    Collation(54, "utf16_general_ci", 1, MYSQL_CHARSET_NAME_utf16),
    Collation(55, "utf16_bin", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(56, "utf16le_general_ci", 1, MYSQL_CHARSET_NAME_utf16le),
    Collation(57, "cp1256_general_ci", 1, MYSQL_CHARSET_NAME_cp1256),
    Collation(58, "cp1257_bin", 0, MYSQL_CHARSET_NAME_cp1257),
    Collation(59, "cp1257_general_ci", 1, MYSQL_CHARSET_NAME_cp1257),
    Collation(60, "utf32_general_ci", 1, MYSQL_CHARSET_NAME_utf32),
    Collation(61, "utf32_bin", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(62, "utf16le_bin", 0, MYSQL_CHARSET_NAME_utf16le),
    Collation(63, "binary", 1, MYSQL_CHARSET_NAME_binary),
    Collation(64, "armscii8_bin", 0, MYSQL_CHARSET_NAME_armscii8),
    Collation(65, "ascii_bin", 0, MYSQL_CHARSET_NAME_ascii),
    Collation(66, "cp1250_bin", 0, MYSQL_CHARSET_NAME_cp1250),
    Collation(67, "cp1256_bin", 0, MYSQL_CHARSET_NAME_cp1256),
    Collation(68, "cp866_bin", 0, MYSQL_CHARSET_NAME_cp866),
    Collation(69, "dec8_bin", 0, MYSQL_CHARSET_NAME_dec8),
    Collation(70, "greek_bin", 0, MYSQL_CHARSET_NAME_greek),
    Collation(71, "hebrew_bin", 0, MYSQL_CHARSET_NAME_hebrew),
    Collation(72, "hp8_bin", 0, MYSQL_CHARSET_NAME_hp8),
    Collation(73, "keybcs2_bin", 0, MYSQL_CHARSET_NAME_keybcs2),
    Collation(74, "koi8r_bin", 0, MYSQL_CHARSET_NAME_koi8r),
    Collation(75, "koi8u_bin", 0, MYSQL_CHARSET_NAME_koi8u),
    Collation(76, List("utf8mb3_tolower_ci", "utf8_tolower_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(77, "latin2_bin", 0, MYSQL_CHARSET_NAME_latin2),
    Collation(78, "latin5_bin", 0, MYSQL_CHARSET_NAME_latin5),
    Collation(79, "latin7_bin", 0, MYSQL_CHARSET_NAME_latin7),
    Collation(80, "cp850_bin", 0, MYSQL_CHARSET_NAME_cp850),
    Collation(81, "cp852_bin", 0, MYSQL_CHARSET_NAME_cp852),
    Collation(82, "swe7_bin", 0, MYSQL_CHARSET_NAME_swe7),
    Collation(83, List("utf8mb3_bin", "utf8_bin"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(84, "big5_bin", 0, MYSQL_CHARSET_NAME_big5),
    Collation(85, "euckr_bin", 0, MYSQL_CHARSET_NAME_euckr),
    Collation(86, "gb2312_bin", 0, MYSQL_CHARSET_NAME_gb2312),
    Collation(87, "gbk_bin", 0, MYSQL_CHARSET_NAME_gbk),
    Collation(88, "sjis_bin", 0, MYSQL_CHARSET_NAME_sjis),
    Collation(89, "tis620_bin", 0, MYSQL_CHARSET_NAME_tis620),
    Collation(90, "ucs2_bin", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(91, "ujis_bin", 0, MYSQL_CHARSET_NAME_ujis),
    Collation(92, "geostd8_general_ci", 0, MYSQL_CHARSET_NAME_geostd8),
    Collation(93, "geostd8_bin", 0, MYSQL_CHARSET_NAME_geostd8),
    Collation(94, "latin1_spanish_ci", 0, MYSQL_CHARSET_NAME_latin1),
    Collation(95, "cp932_japanese_ci", 1, MYSQL_CHARSET_NAME_cp932),
    Collation(96, "cp932_bin", 0, MYSQL_CHARSET_NAME_cp932),
    Collation(97, "eucjpms_japanese_ci", 1, MYSQL_CHARSET_NAME_eucjpms),
    Collation(98, "eucjpms_bin", 0, MYSQL_CHARSET_NAME_eucjpms),
    Collation(99, "cp1250_polish_ci", 0, MYSQL_CHARSET_NAME_cp1250),
    // 100
    Collation(101, "utf16_unicode_ci", 1, MYSQL_CHARSET_NAME_utf16),
    Collation(102, "utf16_icelandic_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(103, "utf16_latvian_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(104, "utf16_romanian_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(105, "utf16_slovenian_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(106, "utf16_polish_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(107, "utf16_estonian_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(108, "utf16_spanish_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(109, "utf16_swedish_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(110, "utf16_turkish_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(111, "utf16_czech_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(112, "utf16_danish_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(113, "utf16_lithuanian_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(114, "utf16_slovak_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(115, "utf16_spanish2_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(116, "utf16_roman_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(117, "utf16_persian_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(118, "utf16_esperanto_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(119, "utf16_hungarian_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(120, "utf16_sinhala_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(121, "utf16_german2_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(122, "utf16_croatian_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(123, "utf16_unicode_520_ci", 0, MYSQL_CHARSET_NAME_utf16),
    Collation(124, "utf16_vietnamese_ci", 0, MYSQL_CHARSET_NAME_utf16),
    // 125..127
    Collation(128, "ucs2_unicode_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(129, "ucs2_icelandic_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(130, "ucs2_latvian_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(131, "ucs2_romanian_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(132, "ucs2_slovenian_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(133, "ucs2_polish_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(134, "ucs2_estonian_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(135, "ucs2_spanish_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(136, "ucs2_swedish_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(137, "ucs2_turkish_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(138, "ucs2_czech_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(139, "ucs2_danish_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(140, "ucs2_lithuanian_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(141, "ucs2_slovak_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(142, "ucs2_spanish2_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(143, "ucs2_roman_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(144, "ucs2_persian_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(145, "ucs2_esperanto_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(146, "ucs2_hungarian_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(147, "ucs2_sinhala_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(148, "ucs2_german2_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(149, "ucs2_croatian_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(150, "ucs2_unicode_520_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(151, "ucs2_vietnamese_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    // 152..158
    Collation(159, "ucs2_general_mysql500_ci", 0, MYSQL_CHARSET_NAME_ucs2),
    Collation(160, "utf32_unicode_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(161, "utf32_icelandic_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(162, "utf32_latvian_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(163, "utf32_romanian_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(164, "utf32_slovenian_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(165, "utf32_polish_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(166, "utf32_estonian_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(167, "utf32_spanish_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(168, "utf32_swedish_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(169, "utf32_turkish_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(170, "utf32_czech_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(171, "utf32_danish_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(172, "utf32_lithuanian_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(173, "utf32_slovak_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(174, "utf32_spanish2_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(175, "utf32_roman_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(176, "utf32_persian_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(177, "utf32_esperanto_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(178, "utf32_hungarian_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(179, "utf32_sinhala_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(180, "utf32_german2_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(181, "utf32_croatian_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(182, "utf32_unicode_520_ci", 0, MYSQL_CHARSET_NAME_utf32),
    Collation(183, "utf32_vietnamese_ci", 0, MYSQL_CHARSET_NAME_utf32),
    // 184..191
    Collation(192, List("utf8mb3_unicode_ci", "utf8_unicode_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(193, List("utf8mb3_icelandic_ci", "utf8_icelandic_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(194, List("utf8mb3_latvian_ci", "utf8_latvian_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(195, List("utf8mb3_romanian_ci", "utf8_romanian_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(196, List("utf8mb3_slovenian_ci", "utf8_slovenian_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(197, List("utf8mb3_polish_ci", "utf8_polish_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(198, List("utf8mb3_estonian_ci", "utf8_estonian_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(199, List("utf8mb3_spanish_ci", "utf8_spanish_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(200, List("utf8mb3_swedish_ci", "utf8_swedish_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(201, List("utf8mb3_turkish_ci", "utf8_turkish_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(202, List("utf8mb3_czech_ci", "utf8_czech_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(203, List("utf8mb3_danish_ci", "utf8_danish_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(204, List("utf8mb3_lithuanian_ci", "utf8_lithuanian_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(205, List("utf8mb3_slovak_ci", "utf8_slovak_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(206, List("utf8mb3_spanish2_ci", "utf8_spanish2_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(207, List("utf8mb3_roman_ci", "utf8_roman_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(208, List("utf8mb3_persian_ci", "utf8_persian_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(209, List("utf8mb3_esperanto_ci", "utf8_esperanto_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(210, List("utf8mb3_hungarian_ci", "utf8_hungarian_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(211, List("utf8mb3_sinhala_ci", "utf8_sinhala_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(212, List("utf8mb3_german2_ci", "utf8_german2_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(213, List("utf8mb3_croatian_ci", "utf8_croatian_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(214, List("utf8mb3_unicode_520_ci", "utf8_unicode_520_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(215, List("utf8mb3_vietnamese_ci", "utf8_vietnamese_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    // 216..222
    Collation(223, List("utf8mb3_general_mysql500_ci", "utf8_general_mysql500_ci"), 0, MYSQL_CHARSET_NAME_utf8mb3),
    Collation(224, "utf8mb4_unicode_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(225, "utf8mb4_icelandic_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(226, "utf8mb4_latvian_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(227, "utf8mb4_romanian_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(228, "utf8mb4_slovenian_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(229, "utf8mb4_polish_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(230, "utf8mb4_estonian_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(231, "utf8mb4_spanish_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(232, "utf8mb4_swedish_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(233, "utf8mb4_turkish_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(234, "utf8mb4_czech_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(235, "utf8mb4_danish_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(236, "utf8mb4_lithuanian_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(237, "utf8mb4_slovak_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(238, "utf8mb4_spanish2_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(239, "utf8mb4_roman_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(240, "utf8mb4_persian_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(241, "utf8mb4_esperanto_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(242, "utf8mb4_hungarian_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(243, "utf8mb4_sinhala_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(244, "utf8mb4_german2_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(245, "utf8mb4_croatian_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(246, "utf8mb4_unicode_520_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(247, "utf8mb4_vietnamese_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(248, "gb18030_chinese_ci", 1, MYSQL_CHARSET_NAME_gb18030),
    Collation(249, "gb18030_bin", 0, MYSQL_CHARSET_NAME_gb18030),
    Collation(250, "gb18030_unicode_520_ci", 0, MYSQL_CHARSET_NAME_gb18030),
    // 251..254
    Collation(255, "utf8mb4_0900_ai_ci", 1, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(256, "utf8mb4_de_pb_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(257, "utf8mb4_is_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(258, "utf8mb4_lv_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(259, "utf8mb4_ro_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(260, "utf8mb4_sl_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(261, "utf8mb4_pl_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(262, "utf8mb4_et_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(263, "utf8mb4_es_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(264, "utf8mb4_sv_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(265, "utf8mb4_tr_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(266, "utf8mb4_cs_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(267, "utf8mb4_da_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(268, "utf8mb4_lt_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(269, "utf8mb4_sk_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(270, "utf8mb4_es_trad_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(271, "utf8mb4_la_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    // 272
    Collation(273, "utf8mb4_eo_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(274, "utf8mb4_hu_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(275, "utf8mb4_hr_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    // 276
    Collation(277, "utf8mb4_vi_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(278, "utf8mb4_0900_as_cs", 1, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(279, "utf8mb4_de_pb_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(280, "utf8mb4_is_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(281, "utf8mb4_lv_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(282, "utf8mb4_ro_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(283, "utf8mb4_sl_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(284, "utf8mb4_pl_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(285, "utf8mb4_et_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(286, "utf8mb4_es_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(287, "utf8mb4_sv_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(288, "utf8mb4_tr_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(289, "utf8mb4_cs_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(290, "utf8mb4_da_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(291, "utf8mb4_lt_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(292, "utf8mb4_sk_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(293, "utf8mb4_es_trad_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(294, "utf8mb4_la_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    // 295
    Collation(296, "utf8mb4_eo_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(297, "utf8mb4_hu_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(298, "utf8mb4_hr_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    // 299
    Collation(300, "utf8mb4_vi_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    // 301,302
    Collation(303, "utf8mb4_ja_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(304, "utf8mb4_ja_0900_as_cs_ks", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(305, "utf8mb4_0900_as_ci", 1, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(306, "utf8mb4_ru_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(307, "utf8mb4_ru_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(308, "utf8mb4_zh_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(309, "utf8mb4_0900_bin", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(310, "utf8mb4_nb_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(311, "utf8mb4_nb_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(312, "utf8mb4_nn_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(313, "utf8mb4_nn_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(314, "utf8mb4_sr_latn_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(315, "utf8mb4_sr_latn_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(316, "utf8mb4_bs_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(317, "utf8mb4_bs_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(318, "utf8mb4_hr_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(319, "utf8mb4_hr_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(320, "utf8mb4_gl_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(321, "utf8mb4_gl_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(322, "utf8mb4_mn_cyrl_0900_ai_ci", 0, MYSQL_CHARSET_NAME_utf8mb4),
    Collation(323, "utf8mb4_mn_cyrl_0900_as_cs", 0, MYSQL_CHARSET_NAME_utf8mb4)
  )

  val COLLATION_INDEX_TO_COLLATION_NAME: List[String] = collations.map(_.charset.charsetName)
  val COLLATION_INDEX_TO_CHARSET: Map[Int, MysqlCharset] =
    collations.map(collation => collation.index -> collation.charset).toMap

  val CHARSET_NAME_TO_CHARSET: Map[String, MysqlCharset] = charsets.map(charset => charset.charsetName -> charset).toMap
  val JAVA_ENCODING_UC_TO_MYSQL_CHARSET: Map[String, List[MysqlCharset]] = ???
  val CHARSET_NAME_TO_COLLATION_INDEX: Map[String, Int] = charsets
    .map(charset =>
      charset.charsetName -> collations.find(_.charset.charsetName == charset.charsetName).fold(0)(_.index)
    )
    .toMap
  val COLLATION_NAME_TO_COLLATION_INDEX: Map[String, Int] =
    collations.map(collation => collation.collationNames.headOption.getOrElse("") -> collation.index).toMap

  def getStaticMysqlCharsetNameForCollationIndex(collationIndex: Int): Option[String] =
    COLLATION_INDEX_TO_CHARSET.get(collationIndex).map(_.charsetName)

  def getStaticMysqlCharsetByName(charsetName: String): Option[MysqlCharset] = CHARSET_NAME_TO_CHARSET.get(charsetName)

  def getStaticCollationNameForCollationIndex(collationIndex: Int): Option[String] =
    if collationIndex > 0 && collationIndex < MAP_SIZE then COLLATION_INDEX_TO_COLLATION_NAME.lift(collationIndex)
    else None

  def getStaticMblen(charsetName: String): Int = getStaticMysqlCharsetByName(charsetName).fold(0)(_.mblen)

  def getStaticMysqlCharsetForJavaEncoding(javaEncoding: String, version: Option[Version]): Option[String] =
    val mysqlCharsets = JAVA_ENCODING_UC_TO_MYSQL_CHARSET.get(javaEncoding.toUpperCase(Locale.ENGLISH))
    mysqlCharsets.flatMap { charsets =>
      version match
        case Some(v) =>
          charsets
            .foldLeft[Option[MysqlCharset]](None) {
              case (acc, charset) =>
                if charset.isOkayForVersion(v) && (acc.isEmpty || acc.get.minimumVersion.compare(
                    charset.minimumVersion
                  ) < 0 || acc.get.priority < charset.priority && acc.get.minimumVersion.compare(
                    charset.minimumVersion
                  ) == 0)
                then Some(charset)
                else acc
            }
            .map(_.charsetName)
        case None => charsets.headOption.map(_.charsetName)
    }

  def getStaticCollationIndexForMysqlCharsetName(charsetName: Option[String]): Int =
    charsetName match
      case Some(name) => CHARSET_NAME_TO_COLLATION_INDEX.getOrElse(name, 0)
      case None       => 0

case class MysqlCharset(
  charsetName:     String,
  mblen:           Int,
  priority:        Int,
  javaEncodingsUc: List[String],
  aliases:         List[String],
  minimumVersion:  Version
):

  def isOkayForVersion(version: Version): Boolean = minimumVersion.compare(version) match
    case -1 => false
    case 0  => true
    case 1  => true

  override def toString: String = s"[charsetName=$charsetName,mblen=$mblen]"

object MysqlCharset:

  def apply(charsetName: String, mblen: Int, priority: Int, javaEncodings: List[String]): MysqlCharset =
    MysqlCharset(charsetName, mblen, priority, addEncodingMapping(javaEncodings, mblen), List.empty, Version(0, 0, 0))

  def apply(
    charsetName:   String,
    mblen:         Int,
    priority:      Int,
    javaEncodings: List[String],
    aliases:       List[String]
  ): MysqlCharset =
    MysqlCharset(charsetName, mblen, priority, addEncodingMapping(javaEncodings, mblen), aliases, Version(0, 0, 0))

  def apply(
    charsetName:    String,
    mblen:          Int,
    priority:       Int,
    javaEncodings:  List[String],
    minimumVersion: Version
  ): MysqlCharset =
    MysqlCharset(charsetName, mblen, priority, addEncodingMapping(javaEncodings, mblen), List.empty, minimumVersion)

  def apply(
    charsetName:    String,
    mblen:          Int,
    priority:       Int,
    javaEncodings:  List[String],
    aliases:        List[String],
    minimumVersion: Version
  ): MysqlCharset =
    val encodings =
      if javaEncodings.isEmpty then if mblen > 1 then List("UTF-8") else List("Cp1252")
      else addEncodingMapping(javaEncodings, mblen)
    new MysqlCharset(charsetName, mblen, priority, encodings, aliases, minimumVersion)

  private def addEncodingMapping(encodings: List[String], mblen: Int): List[String] =
    encodings.flatMap { encoding =>
      try
        val cs = Charset.forName(encoding)
        List(cs.name().toUpperCase(Locale.ENGLISH)) ++ cs
          .aliases()
          .toArray
          .toList
          .asInstanceOf[List[String]]
          .map(_.toUpperCase(Locale.ENGLISH))
      catch
        case _: Exception =>
          if mblen == 1 then List(encoding.toUpperCase(Locale.ENGLISH))
          else List.empty
    }

case class Collation(
  index:          Int,
  collationNames: List[String],
  priority:       Int,
  charset:        MysqlCharset
):

  override def toString: String =
    s"[index=$index,collationNames=${ collationNames.mkString(",") },charsetName=${ charset.charsetName }]"

object Collation:

  def apply(index: Int, collationName: String, priority: Int, charsetName: String): Collation =
    this.apply(index, List(collationName), priority, charsetName)

  def apply(index: Int, collationNames: List[String], priority: Int, charsetName: String): Collation =
    new Collation(index, collationNames, priority, CharsetMapping.CHARSET_NAME_TO_CHARSET(charsetName))
