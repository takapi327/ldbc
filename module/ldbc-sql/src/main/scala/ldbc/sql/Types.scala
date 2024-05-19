/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * The object that defines the constants that are used to identify generic
 * SQL types, called JDBC types.
 */
object Types:

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>BIT</code>.
   */
  val BIT: Int = -7

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>TINYINT</code>.
   */
  val TINYINT: Int = -6

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>SMALLINT</code>.
   */
  val SMALLINT = 5

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>INTEGER</code>.
   */
  val INTEGER = 4

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>BIGINT</code>.
   */
  val BIGINT: Int = -5

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>FLOAT</code>.
   */
  val FLOAT = 6

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>REAL</code>.
   */
  val REAL = 7

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>DOUBLE</code>.
   */
  val DOUBLE = 8

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>NUMERIC</code>.
   */
  val NUMERIC = 2

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>DECIMAL</code>.
   */
  val DECIMAL = 3

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>CHAR</code>.
   */
  val CHAR = 1

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>VARCHAR</code>.
   */
  val VARCHAR = 12

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>LONGVARCHAR</code>.
   */
  val LONGVARCHAR: Int = -1

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>DATE</code>.
   */
  val DATE = 91

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>TIME</code>.
   */
  val TIME = 92

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>TIMESTAMP</code>.
   */
  val TIMESTAMP = 93

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>BINARY</code>.
   */
  val BINARY: Int = -2

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>VARBINARY</code>.
   */
  val VARBINARY: Int = -3

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type
   * <code>LONGVARBINARY</code>.
   */
  val LONGVARBINARY: Int = -4

  /**
   * <P>The constant in the Java programming language
   * that identifies the generic SQL value
   * <code>NULL</code>.
   */
  val NULL = 0

  /**
   * The constant in the Java programming language that indicates
   * that the SQL type is database-specific and
   * gets mapped to a Java object that can be accessed via
   * the methods <code>getObject</code> and <code>setObject</code>.
   */
  val OTHER = 1111

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>JAVA_OBJECT</code>.
   */
  val JAVA_OBJECT = 2000

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>DISTINCT</code>.
   */
  val DISTINCT = 2001

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>STRUCT</code>.
   */
  val STRUCT = 2002

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>ARRAY</code>.
   */
  val ARRAY = 2003

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>BLOB</code>.
   */
  val BLOB = 2004

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>CLOB</code>.
   */
  val CLOB = 2005

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>REF</code>.
   */
  val REF = 2006

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type <code>DATALINK</code>.
   */
  val DATALINK = 70

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type <code>BOOLEAN</code>.
   */
  val BOOLEAN = 16

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type <code>ROWID</code>
   */
  val ROWID: Int = -8

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type <code>NCHAR</code>
   */
  val NCHAR: Int = -15

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type <code>NVARCHAR</code>.
   */
  val NVARCHAR: Int = -9

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type <code>LONGNVARCHAR</code>.
   */
  val LONGNVARCHAR: Int = -16

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type <code>NCLOB</code>.
   */
  val NCLOB = 2011

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type <code>XML</code>.
   */
  val SQLXML = 2009

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type {@code REF CURSOR}.
   */
  val REF_CURSOR = 2012

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * {@code TIME WITH TIMEZONE}.
   */
  val TIME_WITH_TIMEZONE = 2013

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * {@code TIMESTAMP WITH TIMEZONE}.
   */
  val TIMESTAMP_WITH_TIMEZONE = 2014
