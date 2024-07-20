{%
  laika.title = ロギング
  laika.metadata.language = ja
%}

# ロギング

ldbcではデータベース接続の実行ログやエラーログを任意のロギングライブラリを使用して任意の形式で書き出すことができます。

標準ではCats EffectのConsoleを使用したロガーが提供されているため開発時はこちらを使用することができます。

```scala 3
given LogHandler[IO] = LogHandler.console[IO]
```

任意のロギングライブラリを使用してログをカスタマイズする場合は`ldbc.dsl.logging.LogHandler`を使用します。

以下は標準実装のログ実装です。ldbcではデータベース接続で以下3種類のイベントが発生します。

- Success: 処理の成功
- ProcessingFailure: データ取得後もしくはデータベース接続前の処理のエラー
- ExecFailure: データベースへの接続処理のエラー

それぞれのイベントでどのようなログを書き込むかをパターンマッチングによって振り分けを行います。

```scala 3
def console[F[_]: Console: Sync]: LogHandler[F] =
  case LogEvent.Success(sql, args) =>
    Console[F].println(
      s"""Successful Statement Execution:
         |  $sql
         |
         | arguments = [${ args.mkString(",") }]
         |""".stripMargin
    )
  case LogEvent.ProcessingFailure(sql, args, failure) =>
    Console[F].errorln(
      s"""Failed ResultSet Processing:
         |  $sql
         |
         | arguments = [${ args.mkString(",") }]
         |""".stripMargin
    ) >> Console[F].printStackTrace(failure)
  case LogEvent.ExecFailure(sql, args, failure) =>
    Console[F].errorln(
      s"""Failed Statement Execution:
         |  $sql
         |
         | arguments = [${ args.mkString(",") }]
         |""".stripMargin
    ) >> Console[F].printStackTrace(failure)
```
