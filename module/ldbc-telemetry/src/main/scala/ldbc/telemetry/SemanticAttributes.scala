/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.telemetry

import ldbc.sql.AttributeKey

/**
 * The subset of OpenTelemetry database semantic-convention attribute keys the driver emits, inlined as
 * CE-free [[AttributeKey]]s. Values match the OpenTelemetry semantic conventions (v1.39.0). This
 * replaces the otel4s `semconv` attribute keys so the core carries no otel4s (and therefore no
 * cats-effect) dependency.
 *
 * @see [[https://opentelemetry.io/docs/specs/semconv/database/ Database Semantic Conventions]]
 */
object DbAttributes:

  /** `db.query.text` — the database query being executed. */
  val DbQueryText: AttributeKey[String] = AttributeKey("db.query.text")

  /** `db.operation.name` — the name of the operation or command being executed. */
  val DbOperationName: AttributeKey[String] = AttributeKey("db.operation.name")

  /** `db.namespace` — the name of the database/schema. */
  val DbNamespace: AttributeKey[String] = AttributeKey("db.namespace")

  /** `db.system.name` — the database management system product. */
  val DbSystemName: AttributeKey[String] = AttributeKey("db.system.name")

  /** `db.collection.name` — the name of the table (collection) being operated on. */
  val DbCollectionName: AttributeKey[String] = AttributeKey("db.collection.name")

  /** `db.operation.batch.size` — the number of queries in a batch (set only when >= 2). */
  val DbOperationBatchSize: AttributeKey[Long] = AttributeKey("db.operation.batch.size")

  /** Well-known values for [[DbSystemName]]. */
  object DbSystemNameValue:
    /** The MySQL system name value. */
    object Mysql:
      /** The literal `"mysql"`. */
      val value: String = "mysql"

/**
 * The subset of OpenTelemetry server semantic-convention attribute keys the driver emits.
 */
object ServerAttributes:

  /** `server.address` — the server hostname or IP. */
  val ServerAddress: AttributeKey[String] = AttributeKey("server.address")

  /** `server.port` — the server port. */
  val ServerPort: AttributeKey[Long] = AttributeKey("server.port")

/**
 * The subset of OpenTelemetry error semantic-convention attribute keys the driver emits.
 */
object ErrorAttributes:

  /** `error.type` — the class of error (typically the exception class name). */
  val ErrorType: AttributeKey[String] = AttributeKey("error.type")
