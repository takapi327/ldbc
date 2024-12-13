package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.*

import ldbc.sql.*
import ldbc.connector.SSL
import ldbc.schema.syntax.io.*

import ldbc.tests.model.*

class LdbcTableQuerySelectConnectionTest extends TableSchemaSelectConnectionTest:

  override def prefix: "jdbc" | "ldbc" = "ldbc"

  override def connection: Resource[IO, Connection[IO]] =
    ldbc.connector.Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("world"),
      ssl      = SSL.Trusted
    )

class JdbcTableQuerySelectConnectionTest extends TableSchemaSelectConnectionTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world")
  ds.setUser("ldbc")
  ds.setPassword("password")

  override def prefix: "jdbc" | "ldbc" = "jdbc"

  override def connection: Resource[IO, Connection[IO]] =
    Resource.make(jdbc.connector.MysqlDataSource[IO](ds).getConnection)(_.close())

trait TableSchemaSelectConnectionTest extends TableQuerySelectConnectionTest:

  override private val country = TableQuery[CountryTable]
  override private val city = TableQuery[CityTable]
  override private val countryLanguage = TableQuery[CountryLanguageTable]
  override private val governmentOffice = TableQuery[GovernmentOfficeTable]
