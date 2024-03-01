/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import cats.syntax.all.*

import scodec.bits.ByteVector

import ldbc.connector.data.Type

trait TextCodecs:

  def char(length:    Int): Codec[String] = Codec.simple(s => s, _.asRight, Type.char(length))
  def varchar(length: Int): Codec[String] = Codec.simple(s => s, _.asRight, Type.varchar(length))

  def binary(length: Int): Codec[Array[Byte]] = Codec.simple(
    "\\x" + ByteVector.view(_).toHex,
    _.getBytes("UTF-8").asRight[String],
    Type.binary(length)
  )
  def varbinary(length: Int): Codec[Array[Byte]] = Codec.simple(
    "\\x" + ByteVector.view(_).toHex,
    _.getBytes("UTF-8").asRight[String],
    Type.varbinary(length)
  )

  def tinyblob(length: Int): Codec[Array[Byte]] = Codec.simple(
    "\\x" + ByteVector.view(_).toHex,
    _.getBytes("UTF-8").asRight[String],
    Type.tinyblob
  )
  def blob(length: Int): Codec[Array[Byte]] = Codec.simple(
    "\\x" + ByteVector.view(_).toHex,
    _.getBytes("UTF-8").asRight[String],
    Type.blob
  )
  def mediumblob(length: Int): Codec[Array[Byte]] = Codec.simple(
    "\\x" + ByteVector.view(_).toHex,
    _.getBytes("UTF-8").asRight[String],
    Type.mediumblob
  )
  def longblob(length: Int): Codec[Array[Byte]] = Codec.simple(
    "\\x" + ByteVector.view(_).toHex,
    _.getBytes("UTF-8").asRight[String],
    Type.longblob
  )

  val tinytext:   Codec[String] = Codec.simple(s => s, _.asRight, Type.tinytext)
  val text:       Codec[String] = Codec.simple(s => s, _.asRight, Type.text)
  val mediumtext: Codec[String] = Codec.simple(s => s, _.asRight, Type.mediumtext)
  val longtext:   Codec[String] = Codec.simple(s => s, _.asRight, Type.longtext)

  def `enum`(values: String*): Codec[String] = Codec.simple(s => s, _.asRight, Type.`enum`(values.toList))

  def set(values: String*): Codec[List[String]] = Codec.simple[List[String]](
    s => s.mkString(","),
    str => str.split(",").toList.asRight,
    Type.set(values.toList)
  )
  
  val json: Codec[String] = Codec.simple(s => s, _.asRight, Type.json)

object text extends TextCodecs
