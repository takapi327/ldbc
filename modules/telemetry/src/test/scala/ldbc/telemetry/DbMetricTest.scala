/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.telemetry

/**
 * Guards the invariants of [[DbMetric]], the single definition both `ldbc-otel4s` and
 * `ldbc-zio-telemetry` build their instruments from.
 *
 * Completeness needs no test: the backends' conformance tests iterate [[DbMetric.values]], which the
 * compiler generates from the enum cases, so a metric cannot be declared and then left unchecked.
 */
class DbMetricTest extends munit.FunSuite:

  test("metric names are unique") {
    val names = DbMetric.values.map(_.name).toList
    assertEquals(names.distinct, names, s"duplicate metric names: ${ names.diff(names.distinct) }")
  }

  test("every histogram carries explicit bucket boundaries, and nothing else does") {
    DbMetric.values.foreach { metric =>
      metric.kind match
        case InstrumentKind.Histogram =>
          assert(metric.boundaries.nonEmpty, s"${ metric.name } is a histogram but has no bucket boundaries")
        case _ =>
          assertEquals(metric.boundaries, Nil, s"${ metric.name } is not a histogram but carries boundaries")
    }
  }

  test("bucket boundaries are strictly increasing and positive") {
    DbMetric.values.filter(_.kind == InstrumentKind.Histogram).foreach { metric =>
      assertEquals(metric.boundaries.sorted, metric.boundaries, s"boundaries of ${ metric.name } are not ascending")
      assertEquals(metric.boundaries.distinct, metric.boundaries, s"boundaries of ${ metric.name } contain duplicates")
      assert(metric.boundaries.forall(_ > 0), s"boundaries of ${ metric.name } must be positive")
    }
  }

  test("names follow the db.* semantic convention and every metric has a unit and a description") {
    DbMetric.values.foreach { metric =>
      assert(metric.name.startsWith("db."), s"${ metric.name } is not a db.* metric")
      assert(metric.unit.nonEmpty, s"${ metric.name } has no unit")
      assert(metric.description.nonEmpty, s"${ metric.name } has no description")
    }
  }

  test("duration histograms are expressed in seconds and share one set of boundaries") {
    val durations = DbMetric.values.filter(_.unit == "s").toList
    assert(durations.nonEmpty, "there is at least one duration metric")
    durations.foreach { metric =>
      assertEquals(metric.kind, InstrumentKind.Histogram, s"${ metric.name } is in seconds but is not a histogram")
    }
    assertEquals(durations.map(_.boundaries).distinct.size, 1, "the duration metrics disagree on their boundaries")
  }

  test("poolStateGauges is exactly the set of observable instruments") {
    assertEquals(
      DbMetric.poolStateGauges,
      DbMetric.values.filter(_.kind == InstrumentKind.ObservableUpDownCounter).toList
    )
  }
