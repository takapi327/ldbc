/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import ldbc.core.model.Enum
import ldbc.core.attribute.Attribute

/** A model for representing character sets to be set in column definitions for the string data types CHAR, VARCHAR,
  * TEXT, ENUM, SET, and any synonym.
  */
trait Character:

  /** Character name */
  def name: String

  /** Character description */
  def description: String

  /** Maximum number of bytes required to store a single character. */
  def maxLen: Int

  /** Variable that contains the SQL string of Character
    *
    * @return
    *   SQL query string
    */
  val queryString: String = s"CHARACTER SET $name"

object Character:

  object armscii8 extends Character:
    override def name:        String = "armscii8"
    override def description: String = "ARMSCII-8 Armenian"
    override def maxLen:      Int    = 1

  object ascii extends Character:
    override def name:        String = "ascii"
    override def description: String = "US ASCII"
    override def maxLen:      Int    = 1

  object big5 extends Character:
    override def name:        String = "big5"
    override def description: String = "Big5 Traditional Chinese"
    override def maxLen:      Int    = 2

  object binary extends Character:
    override def name:        String = "binary"
    override def description: String = "Binary pseudo charset"
    override def maxLen:      Int    = 1

  object cp1250 extends Character:
    override def name:        String = "cp1250"
    override def description: String = "Windows Central European"
    override def maxLen:      Int    = 1

  object cp1251 extends Character:
    override def name:        String = "cp1251"
    override def description: String = "Windows Cyrillic"
    override def maxLen:      Int    = 1

  object cp1256 extends Character:
    override def name:        String = "cp1256"
    override def description: String = "Windows Arabic"
    override def maxLen:      Int    = 1

  object cp1257 extends Character:
    override def name:        String = "cp1257"
    override def description: String = "Windows Baltic"
    override def maxLen:      Int    = 1

  object cp850 extends Character:
    override def name:        String = "cp850"
    override def description: String = "DOS West European"
    override def maxLen:      Int    = 1

  object cp852 extends Character:
    override def name:        String = "cp852"
    override def description: String = "DOS Central European"
    override def maxLen:      Int    = 1

  object cp866 extends Character:
    override def name:        String = "cp866"
    override def description: String = "DOS Russian"
    override def maxLen:      Int    = 1

  object cp932 extends Character:
    override def name:        String = "cp932"
    override def description: String = "SJIS for Windows Japanese"
    override def maxLen:      Int    = 2

  object dec8 extends Character:
    override def name:        String = "dec8"
    override def description: String = "DEC West European"
    override def maxLen:      Int    = 2

  object eucjpms extends Character:
    override def name:        String = "eucjpms"
    override def description: String = "UJIS for Windows Japanese"
    override def maxLen:      Int    = 3

  object euckr extends Character:
    override def name:        String = "euckr"
    override def description: String = "EUC-KR Korean"
    override def maxLen:      Int    = 2

  object gb18030 extends Character:
    override def name:        String = "gb18030"
    override def description: String = "China National Standard GB18030"
    override def maxLen:      Int    = 4

  object gb2312 extends Character:
    override def name:        String = "gb2312"
    override def description: String = "GB2312 Simplified Chinese"
    override def maxLen:      Int    = 2

  object gbk extends Character:
    override def name:        String = "gbk"
    override def description: String = "GBK Simplified Chinese"
    override def maxLen:      Int    = 2

  object geostd8 extends Character:
    override def name:        String = "geostd8"
    override def description: String = "GEOSTD8 Georgian"
    override def maxLen:      Int    = 1

  object greek extends Character:
    override def name:        String = "greek"
    override def description: String = "ISO 8859-7 Greek"
    override def maxLen:      Int    = 1

  object hebrew extends Character:
    override def name:        String = "hebrew"
    override def description: String = "ISO 8859-8 Hebrew"
    override def maxLen:      Int    = 1

  object hp8 extends Character:
    override def name:        String = "hp8"
    override def description: String = "HP West European"
    override def maxLen:      Int    = 1

  object keybcs2 extends Character:
    override def name:        String = "keybcs2"
    override def description: String = "DOS Kamenicky Czech-Slovak"
    override def maxLen:      Int    = 1

  object koi8r extends Character:
    override def name:        String = "koi8r"
    override def description: String = "KOI8-R Relcom Russian"
    override def maxLen:      Int    = 1

  object koi8u extends Character:
    override def name:        String = "koi8u"
    override def description: String = "KOI8-U Ukrainian"
    override def maxLen:      Int    = 1

  object latin1 extends Character:
    override def name:        String = "latin1"
    override def description: String = "cp1252 West European"
    override def maxLen:      Int    = 1

  object latin2 extends Character:
    override def name:        String = "latin2"
    override def description: String = "ISO 8859-2 Central European"
    override def maxLen:      Int    = 1

  object latin5 extends Character:
    override def name:        String = "latin5"
    override def description: String = "ISO 8859-9 Turkish"
    override def maxLen:      Int    = 1

  object latin7 extends Character:
    override def name:        String = "latin7"
    override def description: String = "ISO 8859-13 Baltic"
    override def maxLen:      Int    = 1

  object macce extends Character:
    override def name:        String = "macce"
    override def description: String = "Mac Central European"
    override def maxLen:      Int    = 1

  object macroman extends Character:
    override def name:        String = "macroman"
    override def description: String = "Mac West European"
    override def maxLen:      Int    = 1

  object sjis extends Character:
    override def name:        String = "sjis"
    override def description: String = "Shift-JIS Japanese"
    override def maxLen:      Int    = 2

  object swe7 extends Character:
    override def name:        String = "swe7"
    override def description: String = "7bit Swedish"
    override def maxLen:      Int    = 1

  object tis620 extends Character:
    override def name:        String = "tis620"
    override def description: String = "TIS620 Thai"
    override def maxLen:      Int    = 1

  object ucs2 extends Character:
    override def name:        String = "ucs2"
    override def description: String = "UCS-2 Unicode"
    override def maxLen:      Int    = 2

  object ujis extends Character:
    override def name:        String = "ujis"
    override def description: String = "EUC-JP Japanese"
    override def maxLen:      Int    = 3

  object utf16 extends Character:
    override def name:        String = "utf16"
    override def description: String = "UTF-16 Unicode"
    override def maxLen:      Int    = 4

  object utf16le extends Character:
    override def name:        String = "utf16le"
    override def description: String = "UTF-16LE Unicode"
    override def maxLen:      Int    = 4

  object utf32 extends Character:
    override def name:        String = "utf32"
    override def description: String = "UTF-32 Unicode"
    override def maxLen:      Int    = 4

  object utf8mb3 extends Character:
    override def name:        String = "utf8mb3"
    override def description: String = "UTF-8 Unicode"
    override def maxLen:      Int    = 3

  object utf8mb4 extends Character:
    override def name:        String = "utf8mb4"
    override def description: String = "UTF-8 Unicode"
    override def maxLen:      Int    = 4

/** A model for representing collations to be set in column definitions for the string data types CHAR, VARCHAR, TEXT,
  * ENUM, SET, and any synonym.
  */
trait Collate[T <: Collate.COLLATION_TYPE] extends Attribute[T]:

  /** Collate name */
  def name: String

  /** Name of the character set with which the collation is associated. */
  def charset: Character

  /** Collation ID. */
  def id: Int

  /** Whether the collation is the default for the character set. */
  def default: Boolean

  /** Whether the character set is compiled to the server. */
  def compiled: Boolean

  /** It relates to the amount of memory required to sort a string represented by a character set. */
  def sortLen: Int

  /** Variable that contains the SQL string of Collate
    *
    * @return
    *   SQL query string
    */
  val queryString: String = s"COLLATE $name"

object Collate:

  type COLLATION_TYPE = Byte | Array[Byte] | String | Enum | Option[Byte | Array[Byte] | String | Enum]

  def apply[T <: COLLATION_TYPE](
    _name:     String,
    _charset:  Character,
    _id:       Int,
    _default:  Boolean,
    _compiled: Boolean,
    _sortLen:  Int
  ): Collate[T] = new Collate[T]:
    override def name:     String    = _name
    override def charset:  Character = _charset
    override def id:       Int       = _id
    override def default:  Boolean   = _default
    override def compiled: Boolean   = _compiled
    override def sortLen:  Int       = _sortLen

  def armscii8_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("armscii8_bin", Character.armscii8, 64, false, true, 1)
  def armscii8_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("armscii8_general_ci", Character.armscii8, 32, true, true, 1)

  def ascii_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("ascii_bin", Character.ascii, 65, false, true, 1)
  def ascii_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("ascii_general_ci", Character.ascii, 11, true, true, 1)

  def big5_bin[T <: COLLATION_TYPE]:       Collate[T] = Collate[T]("big5_bin", Character.big5, 84, false, true, 1)
  def big5_chinese_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("big5_chinese_ci", Character.big5, 1, true, true, 1)

  def binary[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("binary", Character.binary, 63, true, true, 1)

  def cp1250_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp1250_bin", Character.cp1250, 66, false, true, 1)
  def cp1250_croatian_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1250_croatian_ci", Character.cp1250, 44, false, true, 1)
  def cp1250_czech_cs[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1250_czech_cs", Character.cp1250, 34, false, true, 2)
  def cp1250_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1250_general_ci", Character.cp1250, 26, true, true, 1)
  def cp1250_polish_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1250_polish_ci", Character.cp1250, 99, false, true, 1)

  def cp1251_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp1251_bin", Character.cp1251, 50, false, true, 1)
  def cp1251_bulgarian_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1251_bulgarian_ci", Character.cp1251, 14, false, true, 1)
  def cp1251_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1251_general_ci", Character.cp1251, 51, true, true, 1)
  def cp1251_general_cs[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1251_general_cs", Character.cp1251, 52, false, true, 1)
  def cp1251_ukrainian_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1251_ukrainian_ci", Character.cp1251, 23, false, true, 1)

  def cp1256_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp1256_bin", Character.cp1256, 67, false, true, 1)
  def cp1256_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1256_general_ci", Character.cp1256, 57, true, true, 1)

  def cp1257_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp1257_bin", Character.cp1257, 58, false, true, 1)
  def cp1257_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1257_general_ci", Character.cp1257, 59, true, true, 1)
  def cp1257_lithuanian_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1257_lithuanian_ci", Character.cp1257, 29, false, true, 1)

  def cp850_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp850_bin", Character.cp850, 80, false, true, 1)
  def cp850_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp850_general_ci", Character.cp850, 4, true, true, 1)

  def cp852_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp852_bin", Character.cp852, 81, false, true, 1)
  def cp852_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp852_general_ci", Character.cp852, 40, true, true, 1)

  def cp866_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp866_bin", Character.cp866, 68, false, true, 1)
  def cp866_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp866_general_ci", Character.cp866, 36, true, true, 1)

  def cp932_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp932_bin", Character.cp932, 96, false, true, 1)
  def cp932_japanese_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp932_japanese_ci", Character.cp932, 95, true, true, 1)

  def dec8_bin[T <: COLLATION_TYPE]:       Collate[T] = Collate[T]("dec8_bin", Character.dec8, 69, false, true, 1)
  def dec8_swedish_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("dec8_swedish_ci", Character.dec8, 3, true, true, 1)

  def eucjpms_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("eucjpms_bin", Character.eucjpms, 98, false, true, 1)
  def eucjpms_japanese_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("eucjpms_japanese_ci", Character.eucjpms, 97, true, true, 1)

  def euckr_bin[T <: COLLATION_TYPE]:      Collate[T] = Collate[T]("euckr_bin", Character.euckr, 85, false, true, 1)
  def euckr_korean_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("euckr_korean_ci", Character.euckr, 19, true, true, 1)

  def gb18030_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("gb18030_bin", Character.gb18030, 249, false, true, 1)
  def gb18030_chinese_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("gb18030_chinese_ci", Character.gb18030, 248, true, true, 1)
  def gb18030_unicode_520_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("gb18030_unicode_520_ci", Character.gb18030, 250, false, true, 1)

  def gb2312_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("gb2312_bin", Character.gb18030, 86, false, true, 1)
  def gb2312_chinese_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("gb2312_chinese_ci", Character.gb18030, 24, true, true, 1)

  def gbk_bin[T <: COLLATION_TYPE]:       Collate[T] = Collate[T]("gbk_bin", Character.gbk, 87, false, true, 1)
  def gbk_chinese_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("gbk_chinese_ci", Character.gbk, 28, true, true, 1)

  def geostd8_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("geostd8_bin", Character.geostd8, 93, false, true, 1)
  def geostd8_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("geostd8_general_ci", Character.geostd8, 92, true, true, 1)

  def greek_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("greek_bin", Character.greek, 70, false, true, 1)
  def greek_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("greek_general_ci", Character.greek, 25, true, true, 1)

  def hebrew_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("hebrew_bin", Character.hebrew, 71, false, true, 1)
  def hebrew_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("hebrew_general_ci", Character.hebrew, 16, true, true, 1)

  def hp8_bin[T <: COLLATION_TYPE]:       Collate[T] = Collate[T]("hp8_bin", Character.hp8, 72, false, true, 1)
  def hp8_english_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("hp8_english_ci", Character.hp8, 6, true, true, 1)

  def keybcs2_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("keybcs2_bin", Character.keybcs2, 73, false, true, 1)
  def keybcs2_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("keybcs2_general_ci", Character.keybcs2, 37, true, true, 1)

  def koi8r_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("koi8r_bin", Character.koi8r, 74, false, true, 1)
  def koi8r_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("koi8r_general_ci", Character.koi8r, 7, true, true, 1)

  def koi8u_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("koi8u_bin", Character.koi8u, 75, false, true, 1)
  def koi8u_general_ci[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("koi8u_general_ci", Character.koi8u, 22, true, true, 1)

  def latin1_bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("latin1_bin", Character.latin1, 47, false, true, 1)
  def latin1_danish_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("latin1_danish_ci", Character.latin1, 15, false, true, 1)
  def latin1_general_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("latin1_general_ci", Character.latin1, 48, false, true, 1)
  def latin1_general_cs[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("latin1_general_cs", Character.latin1, 49, false, true, 1)
  def latin1_german1_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("latin1_german1_ci", Character.latin1, 5, false, true, 1)
  def latin1_german2_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("latin1_german2_ci", Character.latin1, 31, false, true, 1)
  def latin1_spanish_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("latin1_spanish_ci", Character.latin1, 94, false, true, 1)
  def latin1_swedish_ci[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("latin1_swedish_ci", Character.latin1, 8, true, true, 1)
