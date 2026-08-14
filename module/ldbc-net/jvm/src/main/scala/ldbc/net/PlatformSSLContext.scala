/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/** On the JVM a platform TLS context is a JSSE `SSLContext`. */
type PlatformSSLContext = javax.net.ssl.SSLContext
