/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * There is no platform TLS context on this platform, so [[SSL.Platform]] cannot be constructed here
 * (its `context` field is uninhabited). Custom trust is expressed via [[SSL.Custom]] instead.
 */
type PlatformSSLContext = Nothing
