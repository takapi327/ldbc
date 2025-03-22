/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec.auto

import scala.deriving.Mirror

import ldbc.dsl.codec.*

/**
 * Automatic derivation can bombard compile speeds, so be careful when using it.
 */
package object generic:

  object toSlowCompile:
    given [P <: Product](using mirror: Mirror.ProductOf[P], decoder: Decoder[mirror.MirroredElemTypes]): Decoder[P] =
      decoder.to[P]

    given [P <: Product](using mirror: Mirror.ProductOf[P], encoder: Encoder[mirror.MirroredElemTypes]): Encoder[P] =
      encoder.to[P]

    given [P <: Product](using mirror: Mirror.ProductOf[P], codec: Codec[mirror.MirroredElemTypes]): Codec[P] =
      codec.to[P]
