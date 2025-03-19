{%
  laika.title = "Q: コネクション確立の前後に処理を追加する方法は？"
  laika.metadata.language = ja
%}

# Q: コネクション確立の前後に処理を追加する方法は？

## A: コネクション確立の前後に処理を追加するには、`withBeforeAfter`を使用します。

Connection 生成に`withBeforeAfter`メソッドを使用することでコネクションが確立される前と後にそれぞれ処理を呼び出すことができます。例えば、次のように`before`と`after`に任意の処理を渡すことで、コネクション確立前後にログを出力することができます。

`withBeforeAfter`の第2引数には、Afterに渡すBeforeの処理結果の型を指定します。

```scala 3
def before: Connection[IO] => IO[Unit] = _ => IO.println("Connecting to...")
def after: (Unit, Connection[IO]) => IO[Unit] = (_, _) => IO.println("Connection Closed")

def connect: Resource[IO, Connection[IO]] =
  Connection.withBeforeAfter[IO, Unit](
    ...
    before = before,
    after = after
  )
```

@:callout(warning)

この機能は`ldbc-connector`を使用した場合にのみ利用可能です。

```scala 3
libraryDependencies += "@ORGANIZATION@" %% "ldbc-connector" % "@VERSION@"
```

@:@

## 参考資料
- [コネクション](/ja/tutorial/Connection.md#ldbcコネクタの使用)
