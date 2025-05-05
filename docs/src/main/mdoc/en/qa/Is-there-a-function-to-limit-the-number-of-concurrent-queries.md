{%
laika.title = "Q: Is there a function to limit the number of concurrent queries?"
laika.metadata.language = en
%}

# Q: Is there a function to limit the number of concurrent queries?

## A: Currently there is no built-in concurrency limit feature, but you can implement it using Cats Effect's Semaphore.

You can implement a function to limit concurrent executions using Cats Effect's `Semaphore`. For example, by defining a `limitedConcurrency` function as follows, you can execute queries with a limited number of concurrent executions.

```scala 3
import cats.effect.*
import cats.effect.syntax.all.*
import ldbc.dsl.*

def limitedConcurrency[A](program: DBIO[A], conn: Connection[IO], maxConcurrent: Int): IO[A] =
  Semaphore[IO](maxConcurrent).flatMap { sem =>
    sem.permit.use(_ => program.readOnly(conn))
  }
```
