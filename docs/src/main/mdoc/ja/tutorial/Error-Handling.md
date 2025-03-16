{%
  laika.title = エラーハンドリング
  laika.metadata.language = ja
%}

# エラーハンドリング

[データベース操作](/ja/tutorial/Database-Operations.md)で基本的なトランザクション管理を学びました。しかし、実際のアプリケーションでは、さまざまな理由でエラーが発生する可能性があります。このページでは、ldbcでエラーを適切に処理する方法を説明します。

堅牢なアプリケーションを構築するには、期待通りに動作しない状況に対処できる必要があります。ldbcはCats Effectと統合されているため、関数型エラー処理パターンを活用できます。

この章では、例外をトラップしたり処理したりするプログラムを構築するためのコンビネーター一式を検討します。

## 例外について

ある操作が成功するかどうかは、ネットワークの健全性、テーブルの現在の内容、ロックの状態など、予測できない要因に依存します。そのため、`EitherT[DBIO, Throwable, A]`のような論理和ですべてを計算するか、明示的に捕捉されるまで例外の伝播を許可するかを決めなければならない。つまり、ldbcのアクション（ターゲット・モナドに変換される）が実行されると、例外が発生する可能性がある。

発生しやすい例外は主に3種類あります：

1. あらゆる種類のI/Oで様々なタイプのIOExceptionが発生する可能性があり、これらの例外は回復できない傾向があります。
2. データベース例外は、通常、ベンダー固有のSQLStateで特定のエラーを識別する一般的なSQLExceptionとして、キー違反のような一般的な状況で発生します。エラーコードは伝承として伝えられるか、実験によって発見されなければなりません。XOPENとSQL:2003の標準がありますが、どのベンダーもこれらの仕様に忠実ではないようです。これらのエラーには回復可能なものとそうでないものがあります。
3. ldbcは、無効な型マッピング、ドライバから返される未知の JDBC 定数、観測される NULL 値、その他 ldbc が想定している不変条件の違反に対して InvariantViolation を発生させます。これらの例外はプログラマのエラーかドライバの不適合を示し、一般に回復不可能です。

## モナド・エラーと派生コンバイネーター

すべてのldbcモナドは、`MonadError[?[_], Throwable]`を拡張したAsyncインスタンスを提供しています。つまり、DBIOなどは以下のような基本的な操作を持っています：

- `raiseError`: 例外を発生させる (Throwableを`M[A]`に変換する)
- `handleErrorWith`: 例外を処理する (`M[A]`を`M[B]`に変換する)
- `attempt`: 例外を捕捉する (`M[A]`を`M[Either[Throwable, A]]`に変換する)

つまり、どんなldbcプログラムでも`attempt`を加えるだけで例外を捕捉することができます。

```scala
val program = DBIO.pure[IO, Int](1)

program.attempt
// DBIO[IO, Either[Throwable, Int]]
```

### 具体的なエラー処理の例

実際のアプリケーションでは、以下のようにしてエラーを処理できます。

#### 例外の発生

`raiseError`を使って明示的に例外を発生させることができます：

```scala
import cats.effect.IO
import ldbc.dsl.io.*

// 特定の条件でエラーを発生させる
val program: DBIO[String] = for
  id <- DBIO.pure[IO, Int](0)
  result <- if id <= 0 then
    DBIO.raiseError[IO, String](new IllegalArgumentException("IDは正の値である必要があります"))
  else
    DBIO.pure[IO, String](s"有効なID: $id")
yield result
```

#### エラー処理

`handleErrorWith`を使ってエラーを処理する例：

```scala 3
import cats.effect.IO
import ldbc.dsl.io.*
import java.sql.SQLException

// IDによるユーザー検索と、エラー処理
val findUserById: DBIO[String] = for
  userId <- DBIO.pure[IO, Int](123)
  query = s"SELECT name FROM users WHERE id = $userId"
  result <- DBIO.single[IO](query).handleErrorWith {
    case e: SQLException if e.getMessage.contains("Table 'users' doesn't exist") =>
      // テーブルが存在しない場合の処理
      DBIO.pure[IO, String]("ユーザーテーブルがまだ作成されていません")
    case e: SQLException =>
      // その他のSQLエラーの処理
      DBIO.pure[IO, String](s"データベースエラー: ${e.getMessage}")
    case e: Throwable =>
      // 予期しないエラーの処理
      DBIO.pure[IO, String](s"予期しないエラー: ${e.getMessage}")
  }
yield result
```

#### 例外の捕捉

`attempt`を使って例外を捕捉し、エラーハンドリングを行う例：

```scala 3
import cats.effect.IO
import cats.syntax.all.*
import ldbc.dsl.io.*

// データベースからの結果を安全に処理する
val safeOperation: DBIO[String] =
  val riskyOperation = DBIO.single[IO]("SELECT * FROM non_existent_table")
  
  riskyOperation.attempt.flatMap {
    case Right(result) => 
      DBIO.pure[IO, String]("操作成功: " + result.toString)
    case Left(error) => 
      DBIO.pure[IO, String](s"エラーが発生しました: ${error.getMessage}")
  }
```

### 実用的なエラー処理パターン

実際のアプリケーションでよく使われるエラー処理パターンの例を紹介します：

#### リトライ機能の実装

一時的なデータベース接続エラーに対してリトライを行う例：

```scala 3
import scala.concurrent.duration.*
import cats.effect.IO
import cats.syntax.all.*
import ldbc.dsl.io.*

def retryOnConnectionError[A](operation: DBIO[A], maxRetries: Int, delay: FiniteDuration): DBIO[A] =
  def retry(remainingAttempts: Int): DBIO[A] =
    if remainingAttempts <= 0 then
      operation
    else
      operation.handleErrorWith { error =>
        // 接続エラーの場合のみリトライする
        if isConnectionError(error) && remainingAttempts > 0 then
          DBIO.pure[IO, Unit](()) // 遅延のための空のアクション
            .flatMap(_ => IO.sleep(delay).to[DBIO])
            .flatMap(_ => retry(remainingAttempts - 1))
        else
          DBIO.raiseError[IO, A](error) // その他のエラーは再スロー
      }
  
  retry(maxRetries)

// 接続エラーかどうかを判定する補助関数
def isConnectionError(error: Throwable): Boolean =
  error.getMessage.contains("Connection reset") ||
  error.getMessage.contains("Connection refused") ||
  error.getMessage.contains("Connection timed out")
```

#### ユーザー定義エラー型によるエラー処理

独自のエラー型を定義して、より明確なエラー処理を行う例：

```scala
import cats.effect.IO
import cats.syntax.all.*
import ldbc.dsl.io.*

// アプリケーション固有のエラー型の定義
sealed trait AppError
case class UserNotFound(id: Int) extends AppError
case class DatabaseError(cause: Throwable) extends AppError
case class ValidationError(message: String) extends AppError

// EitherTを使ったエラー処理
import cats.data.EitherT

// ユーザーを検索する関数（エラーハンドリング付き）
def findUser(id: Int): EitherT[DBIO, AppError, User] =
  val query = DBIO.single[IO](s"SELECT * FROM users WHERE id = $id")
  
  EitherT(
    query.attempt.flatMap {
      case Right(resultSet) =>
        if resultSet.next() then
          // ユーザーが見つかった場合
          val name = resultSet.getString("name")
          val email = resultSet.getString("email")
          val user = User(id, name, email)
          DBIO.pure[IO, Either[AppError, User]](Right(user))
        else
          // ユーザーが見つからない場合
          DBIO.pure[IO, Either[AppError, User]](Left(UserNotFound(id)))
      case Left(e) =>
        // データベースエラーの場合
        DBIO.pure[IO, Either[AppError, User]](Left(DatabaseError(e)))
    }
  )

// ユーザーデータモデル
case class User(id: Int, name: String, email: String)
```

## 次のステップ

これでldbcでのエラー処理方法がわかりました。適切なエラー処理は堅牢なアプリケーションの基盤となります。ldbcが提供する`raiseError`、`handleErrorWith`、`attempt`などの基本的なコンビネーターを使って、様々なエラー状況に対応できます。また、より高度なエラー処理パターンとして、リトライ機構や独自のエラー型の定義なども検討すると良いでしょう。

実際のアプリケーションでは、これらのパターンを使って堅牢なエラー処理を実装できます。

次は[ロギング](/ja/tutorial/Logging.md)に進み、クエリの実行やエラーをログに記録する方法を学びましょう。
