{%
  laika.title = エラーハンドリング
  laika.metadata.language = ja
%}

# エラーハンドリング

[データベース操作](/ja/tutorial/Database-Operations.md)で基本的なトランザクション管理を学びました。しかし、実際のアプリケーションでは、データベース接続の問題、SQL構文エラー、一意制約違反など、様々な理由でエラーが発生する可能性があります。このページでは、ldbcでエラーを適切に処理する方法を説明します。

## エラーハンドリングの基本

ldbcは関数型プログラミングの原則に従っており、Cats Effectのエラー処理機能と統合されています。`DBIO` モナドは `MonadThrow` のインスタンスを持っているため、例外の発生や処理のための標準的な関数型インターフェースを提供します。

主に使用する3つの基本的なエラー処理メソッドは次のとおりです：

- `raiseError`: 例外を発生させる - `Throwable`を`DBIO[A]`に変換します
- `handleErrorWith`: 例外を処理する - エラー処理関数を使って`DBIO[A]`を別の`DBIO[A]`に変換します
- `attempt`: 例外を捕捉する - `DBIO[A]`を`DBIO[Either[Throwable, A]]`に変換します

## データベース操作での例外の種類

ldbcを使用する際に遭遇する可能性のある主な例外の種類は次のとおりです：

1. **接続エラー**: データベースサーバーに接続できない場合に発生します（例：`ConnectException`）
2. **SQL例外**: SQLの実行中に発生するエラー（例：`SQLException`）
   - 構文エラー
   - 一意キー制約違反
   - 外部キー制約違反
   - タイムアウト
3. **型変換エラー**: データベースの結果を期待する型に変換できない場合（例：`ldbc.dsl.exception.DecodeFailureException`）

## エラー処理の基本的な使い方

### 例外の発生（raiseError）

特定の条件で明示的に例外を発生させる場合は、`raiseError`を使用します：

```scala
import cats.syntax.all.*
import ldbc.dsl.*

// 特定の条件でエラーを発生させる例
def validateUserId(id: Int): DBIO[Int] = 
  if id <= 0 then 
    DBIO.raiseError[Int](new IllegalArgumentException("IDは正の値である必要があります"))
  else 
    DBIO.pure(id)

// 使用例
val program: DBIO[String] = for
  id <- validateUserId(0)
  result <- sql"SELECT name FROM users WHERE id = $id".query[String].unsafe
yield result

// この例では、idが0のためエラーが発生し、後続のSQLは実行されません
```

### エラーの処理（handleErrorWith）

発生したエラーを処理するには、`handleErrorWith`メソッドを使用します：

```scala
import ldbc.dsl.*
import java.sql.SQLException

// エラー処理の例
val findUserById: DBIO[String] = for
  userId <- DBIO.pure(123)
  result <- sql"SELECT name FROM users WHERE id = $userId".query[String].unsafe.handleErrorWith {
    case e: SQLException if e.getMessage.contains("table 'users' doesn't exist") =>
      // テーブルが存在しない場合のフォールバック
      DBIO.pure("ユーザーテーブルがまだ作成されていません")
    case e: SQLException =>
      // その他のSQLエラーの処理
      DBIO.pure(s"データベースエラー: ${e.getMessage}")
    case e: Throwable =>
      // その他のエラーの処理
      DBIO.pure(s"予期しないエラー: ${e.getMessage}")
  }
yield result
```

### 例外の捕捉（attempt）

エラーを`Either`型で捕捉するには、`attempt`メソッドを使用します：

```scala
import cats.syntax.all.*
import ldbc.dsl.*

// attempt を使って例外を捕捉する例
val safeOperation: DBIO[String] = {
  val riskyOperation = sql"SELECT * FROM potentially_missing_table".query[String].unsafe
  
  riskyOperation.attempt.flatMap {
    case Right(result) => 
      DBIO.pure(s"操作成功: $result")
    case Left(error) => 
      DBIO.pure(s"エラーが発生しました: ${error.getMessage}")
  }
}
```

## 実践的なエラー処理パターン

実際のアプリケーションで役立つエラー処理パターンをいくつか紹介します。

### リトライ機能の実装

一時的なデータベース接続エラーに対して自動的にリトライを行う例：

```scala
import scala.concurrent.duration.*
import cats.effect.{IO, Sync}
import cats.effect.syntax.all.*
import cats.syntax.all.*
import ldbc.dsl.*

// リトライ処理の実装例 - IOとDBIOを組み合わせた場合
def retryWithBackoff[F[_]: Sync, A](
  dbioOperation: DBIO[A], 
  connection: Connection[F],
  maxRetries: Int = 3, 
  initialDelay: FiniteDuration = 100.millis,
  maxDelay: FiniteDuration = 2.seconds
): F[A] =
  def retryLoop(remainingRetries: Int, delay: FiniteDuration): F[A] =
    // DBIOをF型（例：IO）に変換して実行
    dbioOperation.run(connection).handleErrorWith { error =>
      if remainingRetries > 0 && isTransientError(error) then
        // 一時的なエラーの場合は遅延してリトライ
        val nextDelay = (delay * 2).min(maxDelay)
        Sync[F].sleep(delay) >> retryLoop(remainingRetries - 1, nextDelay)
      else
        // リトライ回数を超えた場合や永続的なエラーの場合は例外を再スロー
        Sync[F].raiseError[A](error)
    }
  
  retryLoop(maxRetries, initialDelay)

// 具体的なIO型での使用例
def retryDatabaseOperation[A](
  operation: DBIO[A],
  connection: Connection[IO],
  maxRetries: Int = 3
): IO[A] =
  retryWithBackoff(operation, connection, maxRetries)

// 一時的なエラーかどうかを判断するヘルパーメソッド
def isTransientError(error: Throwable): Boolean =
  error match
    case e: SQLException if e.getSQLState == "40001" => true // デッドロックの場合
    case e: SQLException if e.getSQLState == "08006" => true // 接続喪失の場合
    case e: Exception if e.getMessage.contains("connection reset") => true
    case _ => false

// 使用例
val query = sql"SELECT * FROM users".query[User].unsafe
val result = retryDatabaseOperation(query, myConnection)
```

### ユーザー定義エラー型と EitherT を使用したエラー処理

より詳細なエラー処理のために、アプリケーション固有のエラー型を定義する例：

```scala
import cats.data.EitherT
import cats.syntax.all.*
import ldbc.dsl.*

// アプリケーション固有のエラー型
sealed trait AppDatabaseError
case class UserNotFoundError(id: Int) extends AppDatabaseError
case class DuplicateUserError(email: String) extends AppDatabaseError
case class DatabaseConnectionError(cause: Throwable) extends AppDatabaseError
case class UnexpectedDatabaseError(message: String, cause: Throwable) extends AppDatabaseError

// ユーザーモデル
case class User(id: Int, name: String, email: String)

// EitherTを使ったエラー処理の例
def findUserById(id: Int): EitherT[DBIO, AppDatabaseError, User] =
  val query = sql"SELECT id, name, email FROM users WHERE id = $id".query[User].to[Option]

  EitherT(
    query.attempt.map {
      case Right(user) => user.toRight(UserNotFoundError(id))
      case Left(e: SQLException) if e.getMessage.contains("Connection refused") =>
        Left(DatabaseConnectionError(e))
      case Left(e) =>
        Left(UnexpectedDatabaseError(e.getMessage, e))
    }
  )

// 使用例
val program = for
  user <- findUserById(123)
  // 他の操作...
yield user

// 結果の処理
val result: DBIO[Either[AppDatabaseError, User]] = program.value

// 最終的な処理
val finalResult: DBIO[String] = result.flatMap {
  case Right(user) => DBIO.pure(s"ユーザーが見つかりました: ${user.name}")
  case Left(UserNotFoundError(id)) => DBIO.pure(s"ID ${id} のユーザーは存在しません")
  case Left(DatabaseConnectionError(_)) => DBIO.pure("データベース接続エラーが発生しました")
  case Left(error) => DBIO.pure(s"エラー: $error")
}
```

### トランザクションとエラー処理の組み合わせ

トランザクション内でのエラー処理の例：

```scala
import cats.effect.IO
import cats.syntax.all.*
import ldbc.dsl.*
import java.sql.Connection

// トランザクション内でのエラー処理
def transferMoney(fromAccount: Int, toAccount: Int, amount: BigDecimal): DBIO[String] =
  val operation = for
    // 送金元の残高を確認
    balance <- sql"SELECT balance FROM accounts WHERE id = $fromAccount".query[BigDecimal].unsafe
    
    _ <- if balance < amount then
           DBIO.raiseError[Unit](new IllegalStateException(s"口座残高が不足しています: $balance < $amount"))
         else
           DBIO.pure(())
           
    // 送金元から引き落とし
    _ <- sql"UPDATE accounts SET balance = balance - $amount WHERE id = $fromAccount".update.unsafe
    
    // 送金先に入金
    _ <- sql"UPDATE accounts SET balance = balance + $amount WHERE id = $toAccount".update.unsafe
    
    // 取引記録を作成
    _ <- sql"""INSERT INTO transactions (from_account, to_account, amount, timestamp) 
         VALUES ($fromAccount, $toAccount, $amount, NOW())""".update.unsafe
  yield "送金が完了しました"
  
  // トランザクションとしてラップ（エラー発生時は自動的にロールバック）
  operation.handleErrorWith { error =>
    DBIO.pure(s"送金エラー: ${error.getMessage}")
  }

// 使用例
val fromAccount: Int = ???
val toAccount: Int = ???
val amount: BigDecimal = ???

provider.use { conn =>
  transferMoney(fromAccount, toAccount, amount).transaction(conn)
}
```

## まとめ

エラー処理は堅牢なデータベースアプリケーションの重要な側面です。ldbcでは、関数型プログラミングの原則に基づいた明示的なエラー処理が可能です。主なポイントは次のとおりです：

- `raiseError`で例外を発生させる
- `handleErrorWith`で例外を処理する
- `attempt`で例外を`Either`として捕捉する
- 適切なエラー型を定義してアプリケーション固有のエラー処理を行う
- トランザクションとエラー処理を組み合わせて、データの整合性を保つ

これらのテクニックを活用することで、予期せぬエラーに強い、メンテナンスしやすいデータベース操作を実装できます。

## 次のステップ

エラー処理について理解したら、次は[ロギング](/ja/tutorial/Logging.md)に進み、クエリの実行やエラーをログに記録する方法を学びましょう。ロギングはデバッグやモニタリングのために重要です。
