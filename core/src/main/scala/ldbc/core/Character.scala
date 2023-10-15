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

  object Armscii8 extends Character:
    override def name:        String = "armscii8"
    override def description: String = "ARMSCII-8 Armenian"
    override def maxLen:      Int    = 1

  object Ascii extends Character:
    override def name:        String = "ascii"
    override def description: String = "US ASCII"
    override def maxLen:      Int    = 1

  object Big5 extends Character:
    override def name:        String = "big5"
    override def description: String = "Big5 Traditional Chinese"
    override def maxLen:      Int    = 2

  object Binary extends Character:
    override def name:        String = "binary"
    override def description: String = "Binary pseudo charset"
    override def maxLen:      Int    = 1

  object CP1250 extends Character:
    override def name:        String = "cp1250"
    override def description: String = "Windows Central European"
    override def maxLen:      Int    = 1

  object CP1251 extends Character:
    override def name:        String = "cp1251"
    override def description: String = "Windows Cyrillic"
    override def maxLen:      Int    = 1

  object CP1256 extends Character:
    override def name:        String = "cp1256"
    override def description: String = "Windows Arabic"
    override def maxLen:      Int    = 1

  object CP1257 extends Character:
    override def name:        String = "cp1257"
    override def description: String = "Windows Baltic"
    override def maxLen:      Int    = 1

  object CP850 extends Character:
    override def name:        String = "cp850"
    override def description: String = "DOS West European"
    override def maxLen:      Int    = 1

  object CP852 extends Character:
    override def name:        String = "cp852"
    override def description: String = "DOS Central European"
    override def maxLen:      Int    = 1

  object CP866 extends Character:
    override def name:        String = "cp866"
    override def description: String = "DOS Russian"
    override def maxLen:      Int    = 1

  object CP932 extends Character:
    override def name:        String = "cp932"
    override def description: String = "SJIS for Windows Japanese"
    override def maxLen:      Int    = 2

  object Dec8 extends Character:
    override def name:        String = "dec8"
    override def description: String = "DEC West European"
    override def maxLen:      Int    = 2

  object Eucjpms extends Character:
    override def name:        String = "eucjpms"
    override def description: String = "UJIS for Windows Japanese"
    override def maxLen:      Int    = 3

  object Euckr extends Character:
    override def name:        String = "euckr"
    override def description: String = "EUC-KR Korean"
    override def maxLen:      Int    = 2

  object GB18030 extends Character:
    override def name:        String = "gb18030"
    override def description: String = "China National Standard GB18030"
    override def maxLen:      Int    = 4

  object GB2312 extends Character:
    override def name:        String = "gb2312"
    override def description: String = "GB2312 Simplified Chinese"
    override def maxLen:      Int    = 2

  object Gbk extends Character:
    override def name:        String = "gbk"
    override def description: String = "GBK Simplified Chinese"
    override def maxLen:      Int    = 2

  object Geostd8 extends Character:
    override def name:        String = "geostd8"
    override def description: String = "GEOSTD8 Georgian"
    override def maxLen:      Int    = 1

  object Greek extends Character:
    override def name:        String = "greek"
    override def description: String = "ISO 8859-7 Greek"
    override def maxLen:      Int    = 1

  object Hebrew extends Character:
    override def name:        String = "hebrew"
    override def description: String = "ISO 8859-8 Hebrew"
    override def maxLen:      Int    = 1

  object HP8 extends Character:
    override def name:        String = "hp8"
    override def description: String = "HP West European"
    override def maxLen:      Int    = 1

  object Keybcs2 extends Character:
    override def name:        String = "keybcs2"
    override def description: String = "DOS Kamenicky Czech-Slovak"
    override def maxLen:      Int    = 1

  object Koi8r extends Character:
    override def name:        String = "koi8r"
    override def description: String = "KOI8-R Relcom Russian"
    override def maxLen:      Int    = 1

  object Koi8u extends Character:
    override def name:        String = "koi8u"
    override def description: String = "KOI8-U Ukrainian"
    override def maxLen:      Int    = 1

  object Latin1 extends Character:
    override def name:        String = "latin1"
    override def description: String = "cp1252 West European"
    override def maxLen:      Int    = 1

  object Latin2 extends Character:
    override def name:        String = "latin2"
    override def description: String = "ISO 8859-2 Central European"
    override def maxLen:      Int    = 1

  object Latin5 extends Character:
    override def name:        String = "latin5"
    override def description: String = "ISO 8859-9 Turkish"
    override def maxLen:      Int    = 1

  object Latin7 extends Character:
    override def name:        String = "latin7"
    override def description: String = "ISO 8859-13 Baltic"
    override def maxLen:      Int    = 1

  object Macce extends Character:
    override def name:        String = "macce"
    override def description: String = "Mac Central European"
    override def maxLen:      Int    = 1

  object Macroman extends Character:
    override def name:        String = "macroman"
    override def description: String = "Mac West European"
    override def maxLen:      Int    = 1

  object Sjis extends Character:
    override def name:        String = "sjis"
    override def description: String = "Shift-JIS Japanese"
    override def maxLen:      Int    = 2

  object SWE7 extends Character:
    override def name:        String = "swe7"
    override def description: String = "7bit Swedish"
    override def maxLen:      Int    = 1

  object Tis620 extends Character:
    override def name:        String = "tis620"
    override def description: String = "TIS620 Thai"
    override def maxLen:      Int    = 1

  object UCS2 extends Character:
    override def name:        String = "ucs2"
    override def description: String = "UCS-2 Unicode"
    override def maxLen:      Int    = 2

  object Ujis extends Character:
    override def name:        String = "ujis"
    override def description: String = "EUC-JP Japanese"
    override def maxLen:      Int    = 3

  object Utf16 extends Character:
    override def name:        String = "utf16"
    override def description: String = "UTF-16 Unicode"
    override def maxLen:      Int    = 4

  object Utf16le extends Character:
    override def name:        String = "utf16le"
    override def description: String = "UTF-16LE Unicode"
    override def maxLen:      Int    = 4

  object Utf32 extends Character:
    override def name:        String = "utf32"
    override def description: String = "UTF-32 Unicode"
    override def maxLen:      Int    = 4

  object Utf8mb3 extends Character:
    override def name:        String = "utf8mb3"
    override def description: String = "UTF-8 Unicode"
    override def maxLen:      Int    = 3

  object Utf8mb4 extends Character:
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

  def Armscii8Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("armscii8_bin", Character.Armscii8, 64, false, true, 1)
  def Armscii8GeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("armscii8_general_ci", Character.Armscii8, 32, true, true, 1)

  def AsciiBin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("ascii_bin", Character.Ascii, 65, false, true, 1)
  def AsciiGeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("ascii_general_ci", Character.Ascii, 11, true, true, 1)

  def Big5Bin[T <: COLLATION_TYPE]:       Collate[T] = Collate[T]("big5_bin", Character.Big5, 84, false, true, 1)
  def Big5ChineseCI[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("big5_chinese_ci", Character.Big5, 1, true, true, 1)

  def Binary[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("binary", Character.Binary, 63, true, true, 1)

  def CP1250Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp1250_bin", Character.CP1250, 66, false, true, 1)
  def CP1250CroatianCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1250_croatian_ci", Character.CP1250, 44, false, true, 1)
  def CP1250CzechCs[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1250_czech_cs", Character.CP1250, 34, false, true, 2)
  def CP1250GeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1250_general_ci", Character.CP1250, 26, true, true, 1)
  def CP1250PolishCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1250_polish_ci", Character.CP1250, 99, false, true, 1)

  def CP1251Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp1251_bin", Character.CP1251, 50, false, true, 1)
  def CP1251BulgarianCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1251_bulgarian_ci", Character.CP1251, 14, false, true, 1)
  def CP1251GeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1251_general_ci", Character.CP1251, 51, true, true, 1)
  def CP1251GeneralCs[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1251_general_cs", Character.CP1251, 52, false, true, 1)
  def CP1251UkrainianCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1251_ukrainian_ci", Character.CP1251, 23, false, true, 1)

  def CP1256Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp1256_bin", Character.CP1256, 67, false, true, 1)
  def CP1256GeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1256_general_ci", Character.CP1256, 57, true, true, 1)

  def CP1257Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp1257_bin", Character.CP1257, 58, false, true, 1)
  def CP1257GeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1257_general_ci", Character.CP1257, 59, true, true, 1)
  def CP1257LithuanianCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp1257_lithuanian_ci", Character.CP1257, 29, false, true, 1)

  def CP850Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp850_bin", Character.CP850, 80, false, true, 1)
  def CP850GeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp850_general_ci", Character.CP850, 4, true, true, 1)

  def CP852Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp852_bin", Character.CP852, 81, false, true, 1)
  def CP852GeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp852_general_ci", Character.CP852, 40, true, true, 1)

  def CP866Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp866_bin", Character.CP866, 68, false, true, 1)
  def CP866GeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp866_general_ci", Character.CP866, 36, true, true, 1)

  def CP932Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("cp932_bin", Character.CP932, 96, false, true, 1)
  def CP932JapaneseCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("cp932_japanese_ci", Character.CP932, 95, true, true, 1)

  def Dec8Bin[T <: COLLATION_TYPE]:       Collate[T] = Collate[T]("dec8_bin", Character.Dec8, 69, false, true, 1)
  def Dec8SwedishCI[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("dec8_swedish_ci", Character.Dec8, 3, true, true, 1)

  def EucjpmsBin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("eucjpms_bin", Character.Eucjpms, 98, false, true, 1)
  def EucjpmsJapaneseCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("eucjpms_japanese_ci", Character.Eucjpms, 97, true, true, 1)

  def EuckrBin[T <: COLLATION_TYPE]:      Collate[T] = Collate[T]("euckr_bin", Character.Euckr, 85, false, true, 1)
  def EuckrKoreanCI[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("euckr_korean_ci", Character.Euckr, 19, true, true, 1)

  def GB18030Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("gb18030_bin", Character.GB18030, 249, false, true, 1)
  def GB18030ChineseXU[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("gb18030_chinese_ci", Character.GB18030, 248, true, true, 1)
  def GB18030uUnicode520CI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("gb18030_unicode_520_ci", Character.GB18030, 250, false, true, 1)

  def GB2312Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("gb2312_bin", Character.GB18030, 86, false, true, 1)
  def GB2312ChineseCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("gb2312_chinese_ci", Character.GB18030, 24, true, true, 1)

  def GbkBin[T <: COLLATION_TYPE]:       Collate[T] = Collate[T]("gbk_bin", Character.Gbk, 87, false, true, 1)
  def GbkChineseCI[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("gbk_chinese_ci", Character.Gbk, 28, true, true, 1)

  def Geostd8Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("geostd8_bin", Character.Geostd8, 93, false, true, 1)
  def Geostd8GeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("geostd8_general_ci", Character.Geostd8, 92, true, true, 1)

  def GreekBin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("greek_bin", Character.Greek, 70, false, true, 1)
  def GreekGeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("greek_general_ci", Character.Greek, 25, true, true, 1)

  def HebrewBin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("hebrew_bin", Character.Hebrew, 71, false, true, 1)
  def HebrewGeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("hebrew_general_ci", Character.Hebrew, 16, true, true, 1)

  def HP8Bin[T <: COLLATION_TYPE]:       Collate[T] = Collate[T]("hp8_bin", Character.HP8, 72, false, true, 1)
  def HP8EnglishCI[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("hp8_english_ci", Character.HP8, 6, true, true, 1)

  def Keybcs2Bin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("keybcs2_bin", Character.Keybcs2, 73, false, true, 1)
  def Keybcs2GeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("keybcs2_general_ci", Character.Keybcs2, 37, true, true, 1)

  def Koi8rBin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("koi8r_bin", Character.Koi8r, 74, false, true, 1)
  def Koi8rGeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("koi8r_general_ci", Character.Koi8r, 7, true, true, 1)

  def Koi8uBin[T <: COLLATION_TYPE]: Collate[T] = Collate[T]("koi8u_bin", Character.Koi8u, 75, false, true, 1)
  def Koi8uGeneralCI[T <: COLLATION_TYPE]: Collate[T] =
    Collate[T]("koi8u_general_ci", Character.Koi8u, 22, true, true, 1)
