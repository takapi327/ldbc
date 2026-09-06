/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.telemetry

/**
 * The completeness guard for [[DbMetricSpecs]]: every declared spec must be reachable from
 * [[DbMetricSpecs.all]], because the backends' conformance tests iterate `all` — a spec that is declared
 * but never registered there would go unverified in both `ldbc-otel4s` and `ldbc-zio-telemetry`.
 *
 * This needs JVM reflection to enumerate the declared specs, so it lives in the JVM-only source set;
 * the platform-independent invariants are in [[DbMetricSpecsTest]].
 */
class DbMetricSpecsCompletenessTest extends munit.FunSuite:

  test("every spec is reachable from `all`, by reflection over the declared specs") {
    val declared = DbMetricSpecs.getClass.getMethods.toList
      .filter(method => method.getParameterCount == 0 && method.getReturnType == classOf[MetricSpec])
      .map(_.invoke(DbMetricSpecs).asInstanceOf[MetricSpec])
      .distinct
    assertEquals(
      declared.map(_.name).toSet,
      DbMetricSpecs.all.map(_.name).toSet,
      "a spec was declared but not added to DbMetricSpecs.all, so no backend test would check it"
    )
  }
