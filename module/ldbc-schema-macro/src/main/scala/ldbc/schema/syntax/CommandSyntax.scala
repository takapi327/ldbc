package ldbc.schema.syntax

import ldbc.dsl.*
import ldbc.dsl.codec.Decoder
import ldbc.schema.statement.Command

trait CommandSyntax[F[_]]:

  extension (command: Command)

    /**
     * A method to execute an update operation against the MySQL server.
     *
     * {{{
     *   Table[User]
     *     .update("id", 1L)
     *     .set("name", "Alice")
     *     .set("age", 20)
     *     .where(_.id === 1L)
     *     .update
     * }}}
     *
     * @return
     *   The number of rows updated
     */
    def update: Executor[F, Int]

    /**
     * A method to execute an insert operation against the MySQL server.
     *
     * {{{
     *   Table[User]
     *     .insertInto(user => (user.name, user.age))
     *     .values(("Alice", 20))
     *     .returning[Long]
     * }}}
     *
     * @tparam T
     *   The type of the primary key
     * @return
     *   The primary key value
     */
    def returning[T <: String | Int | Long](using decoder: Decoder.Elem[T]): Executor[F, T]
