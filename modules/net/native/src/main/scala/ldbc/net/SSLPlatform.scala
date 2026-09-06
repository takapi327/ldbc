/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * This platform has no `SSLContext`/`KeyStore`, so the [[SSL]] companion exposes no platform-native
 * TLS factories here. Custom trust is expressed cross-platform via [[SSL.Custom]].
 */
private[net] trait SSLPlatform
