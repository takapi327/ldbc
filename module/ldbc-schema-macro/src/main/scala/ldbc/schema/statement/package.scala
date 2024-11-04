package ldbc.schema

import ldbc.dsl.{Parameter, SQL}
import ldbc.dsl.codec.Decoder

// TODO: Scheduled to migrate to schema
import ldbc.query.builder.Column
import ldbc.query.builder.statement.Expression
import ldbc.query.builder.statement.Query as QuerySQL
import ldbc.query.builder.statement.Command as CommandSQL

package object statement:

  trait Query[A, B] extends QuerySQL[B]:

    def table: A

    def columns: Column[B]

    def params: List[Parameter.Dynamic]

    override def decoder: Decoder[B] = columns.decoder

  trait Command extends CommandSQL
