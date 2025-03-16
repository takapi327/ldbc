{%
  laika.title = ロギング
  laika.metadata.language = ja
%}

# ロギング

[エラーハンドリング](/ja/tutorial/Error-Handling.md)でエラーを処理する方法を学びました。このページでは、ロギングを設定し、クエリの実行やエラーをログに記録する方法を説明します。

ロギングは、アプリケーションの動作を理解し、問題を診断するための重要な要素です。ldbcは、カスタマイズ可能なロギングシステムを提供し、任意のロギングライブラリと統合できます。

ldbcではデータベース接続の実行ログやエラーログを任意のロギングライブラリを使用して任意の形式で書き出すことができます。

デフォルトではCats EffectのConsoleを使用したロガーが提供されているため開発時はこちらを使用することができます。

任意のロギングライブラリを使用してログをカスタマイズする場合は`ldbc.sql.logging.LogHandler`を使用します。

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

作成されたLogHandlerはConnection生成時に引数として渡すことで使用することができます。

```scala 3
val connection: Resource[IO, Connection[IO]] =
  Connection[IO](
    ...,
    logHandler = console[IO]
  )
```

## 次のステップ

これでldbcでのロギングの設定方法がわかりました。適切なロギングを設定することで、アプリケーションの動作を監視したり、問題を診断しやすくなります。

次は[カスタムデータ型](/ja/tutorial/Custom-Data-Type.md)に進み、独自のデータ型をldbcで使用する方法を学びましょう。
