/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.catseffect

import cats.effect.IO

/**
 * Adapts an [[ldbc.effect.Resource]] over `IO` into a Cats Effect `Resource[IO, *]`.
 *
 * The driver / pool build their lifecycles as `ldbc.effect.Resource[IO, A]` (running natively on the
 * `Concurrent[IO]` instance); this exposes them as the `cats.effect.Resource[IO, A]` that Cats Effect
 * users compose with. It is a structural adaptation only — both sides run on `IO`, so there is no
 * cross-effect bridging (design step 7, "bridge removed").
 *
 * @param r the `IO`-native resource
 */
def toIOResource[A](r: ldbc.effect.Resource[IO, A]): cats.effect.Resource[IO, A] =
  cats.effect.Resource
    .make(r.allocatedCase)((pair: (A, IO[Unit])) => pair._2)
    .map(_._1)
