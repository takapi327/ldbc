/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.client

import java.net.URI

trait HttpClient[F[_]]:

  def get(uri: URI, headers: Map[String, String]): F[HttpResponse]

  def put(uri: URI, headers: Map[String, String], body: String): F[HttpResponse]
