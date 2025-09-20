{%
laika.title = "Q: 同時実行のクエリ数を制限する機能はありますか？"
laika.metadata.language = ja
%}

# Q: 同時実行のクエリ数を制限する機能はありますか？

## A: 現在組み込みの同時実行制限機能はありませんが、Cats EffectのSemaphoreを使って実装できます。

Cats Effectの`Semaphore`を使って、同時実行数を制限する関数を実装できます。例えば、次のように`limitedConcurrency`関数を定義することで、同時実行数を制限したクエリを実行できます。

```scala 3
import cats.effect.*
import cats.effect.syntax.all.*
import ldbc.dsl.*

def limitedConcurrency[A](program: DBIO[A], connector: Connector[IO], maxConcurrent: Int): IO[A] =
  Semaphore[IO](maxConcurrent).flatMap { sem =>
    sem.permit.use(_ => program.readOnly(connector))
  }
```
