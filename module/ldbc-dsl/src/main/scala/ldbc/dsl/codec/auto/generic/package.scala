/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec.auto

import scala.deriving.Mirror

import ldbc.dsl.codec.*

package object generic:
  
  trait AutoDecoder:

    given [A, B](using da: Decoder[A], db: Decoder[B]): Decoder[(A, B)] =
      da.product(db)

    given [H, T <: Tuple](using dh: Decoder[H], dt: Decoder[T]): Decoder[H *: T] =
      dh.product(dt).map { case (h, t) => h *: t }

    given [P <: Product](using mirror: Mirror.ProductOf[P], decoder: Decoder[mirror.MirroredElemTypes]): Decoder[P] =
      decoder.to[P]

  trait AutoEncoder:

    given [A, B](using ea: Encoder[A], eb: Encoder[B]): Encoder[(A, B)] =
      ea.product(eb)

    given [H, T <: Tuple](using eh: Encoder[H], et: Encoder[T]): Encoder[H *: T] =
      eh.product(et).contramap { case h *: t => (h, t) }

    given [P <: Product](using mirror: Mirror.ProductOf[P], encoder: Encoder[mirror.MirroredElemTypes]): Encoder[P] =
      encoder.to[P]
      
  trait AutoCodec extends AutoDecoder, AutoEncoder:

    given [A, B](using ca: Codec[A], cb: Codec[B]): Codec[(A, B)] = ca product cb

    given [H, T <: Tuple](using dh: Codec[H], dt: Codec[T]): Codec[H *: T] =
      dh.product(dt).imap { case (h, t) => h *: t }(tuple => (tuple.head, tuple.tail))

    given [P <: Product](using mirror: Mirror.ProductOf[P], codec: Codec[mirror.MirroredElemTypes]): Codec[P] =
      codec.to[P]

  object toSlowCompile extends AutoCodec
