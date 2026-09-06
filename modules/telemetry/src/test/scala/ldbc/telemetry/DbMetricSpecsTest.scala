/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.telemetry

/**
 * Guards the invariants of [[DbMetricSpecs]], which is the single definition both `ldbc-otel4s` and
 * `ldbc-zio-telemetry` build their instruments from.
 *
 * These are the platform-independent invariants. The completeness guard — that every declared spec is
 * reachable from [[DbMetricSpecs.all]] — needs JVM reflection and lives in `DbMetricSpecsCompletenessTest`
 * under the JVM-only test source set.
 */
class DbMetricSpecsTest extends munit.FunSuite:

  test("metric names are unique") {
    val names = DbMetricSpecs.all.map(_.name)
    assertEquals(names.distinct, names, s"duplicate metric names: ${ names.diff(names.distinct) }")
  }

  test("every histogram carries explicit bucket boundaries, and nothing else does") {
    DbMetricSpecs.all.foreach { spec =>
      spec.kind match
        case InstrumentKind.Histogram =>
          assert(spec.boundaries.nonEmpty, s"${ spec.name } is a histogram but has no bucket boundaries")
        case _ =>
          assertEquals(spec.boundaries, Nil, s"${ spec.name } is not a histogram but carries boundaries")
    }
  }

  test("bucket boundaries are strictly increasing and positive") {
    DbMetricSpecs.all.filter(_.kind == InstrumentKind.Histogram).foreach { spec =>
      assertEquals(spec.boundaries.sorted, spec.boundaries, s"boundaries of ${ spec.name } are not ascending")
      assertEquals(spec.boundaries.distinct, spec.boundaries, s"boundaries of ${ spec.name } contain duplicates")
      assert(spec.boundaries.forall(_ > 0), s"boundaries of ${ spec.name } must be positive")
    }
  }

  test("names follow the db.* semantic convention and every spec has a unit and a description") {
    DbMetricSpecs.all.foreach { spec =>
      assert(spec.name.startsWith("db."), s"${ spec.name } is not a db.* metric")
      assert(spec.unit.nonEmpty, s"${ spec.name } has no unit")
      assert(spec.description.nonEmpty, s"${ spec.name } has no description")
    }
  }

  test("duration histograms are expressed in seconds and share the same boundaries") {
    val durations = DbMetricSpecs.all.filter(_.unit == "s")
    assert(durations.nonEmpty, "there is at least one duration metric")
    durations.foreach { spec =>
      assertEquals(spec.kind, InstrumentKind.Histogram, s"${ spec.name } is measured in seconds but is not a histogram")
      assertEquals(spec.boundaries, DbMetricSpecs.durationBoundaries, s"boundaries of ${ spec.name }")
    }
  }

  test("poolStateGauges is a subset of `all` and contains only observable instruments") {
    DbMetricSpecs.poolStateGauges.foreach { spec =>
      assert(DbMetricSpecs.all.contains(spec), s"${ spec.name } is not registered in DbMetricSpecs.all")
      assertEquals(spec.kind, InstrumentKind.ObservableUpDownCounter, s"${ spec.name } is not an observable gauge")
    }
  }

  test("every observable instrument is registered as a pool state gauge") {
    val observables = DbMetricSpecs.all.filter(_.kind == InstrumentKind.ObservableUpDownCounter)
    assertEquals(
      observables.map(_.name).toSet,
      DbMetricSpecs.poolStateGauges.map(_.name).toSet,
      "an observable spec exists that registerPoolStateCallback does not register"
    )
  }
