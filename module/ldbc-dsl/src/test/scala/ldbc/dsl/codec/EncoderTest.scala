/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import cats.data.NonEmptyList

import munit.CatsEffectSuite

class EncoderTest extends CatsEffectSuite:

  private val string = Encoder[String]

  test("Encoder contramap should transform the input") {
    val long = string.contramap[Long](_.toString)
    assertEquals(long.encode(123L), Encoder.Encoded.Success(List("123")))
  }

  test("Encoder contraemap should handle Either transformations") {
    val longRight = string.contraemap[Long](long => Right(long.toString))
    val longLeft  = string.contraemap[Long](long => Left(long.toString))
    assertEquals(longRight.encode(123L), Encoder.Encoded.Success(List("123")))
    assertEquals(longLeft.encode(123L), Encoder.Encoded.Failure(NonEmptyList.of("123")))
  }

  test("Encoder opt should handle Option values") {
    val stringOpt: Encoder[Option[String]] = string.opt
    assertEquals(stringOpt.encode(None), Encoder.Encoded.Success(List(None)))
    assertEquals(stringOpt.encode(Some("test")), Encoder.Encoded.Success(List("test")))
  }

  test("Encoder derived should work with case classes") {
    case class User(id: Long, name: String)
    given Encoder[User] = Encoder.derived[User]
    val user            = User(1L, "test")
    assertEquals(
      Encoder[User].encode(user),
      Encoder.Encoded.Success(List(1, "test"))
    )
  }

  test("Encoder product should combine two encoders") {
    val tuple = Encoder[String].product(Encoder[Long])
    assertEquals(tuple.encode(("test", 123L)), Encoder.Encoded.Success(List("test", 123L)))
  }

  test("Encoder for tuple should work with predefined instances") {
    val tuple = Encoder[(String, Long)]
    assertEquals(tuple.encode(("test", 123L)), Encoder.Encoded.Success(List("test", 123L)))
  }

  test("Encoder.Encoded product with success and failure should result in failure") {
    val success = Encoder.Encoded.Success(List("test"))
    val failure = Encoder.Encoded.Failure(NonEmptyList.of("test"))
    val result  = success.product(failure)
    assertEquals(result, Encoder.Encoded.Failure(NonEmptyList.of("test")))
  }

  test("Encoder.Encoded product with failure and success should result in failure") {
    val failure = Encoder.Encoded.Failure(NonEmptyList.of("test"))
    val success = Encoder.Encoded.Success(List("test"))
    val result  = failure.product(success)
    assertEquals(result, Encoder.Encoded.Failure(NonEmptyList.of("test")))
  }

  test("Encoder.Encoded product with two successes should combine their values") {
    val success1 = Encoder.Encoded.success(List("test1"))
    val success2 = Encoder.Encoded.success(List("test2"))
    val result   = success1.product(success2)
    assertEquals(result, Encoder.Encoded.Success(List("test1", "test2")))
  }

  test("Encoder.Encoded product with two failures should combine their error messages") {
    val failure1 = Encoder.Encoded.failure("test1")
    val failure2 = Encoder.Encoded.failure("test2")
    val result   = failure1.product(failure2)
    assertEquals(result, Encoder.Encoded.Failure(NonEmptyList.of("test2", "test1")))
  }
