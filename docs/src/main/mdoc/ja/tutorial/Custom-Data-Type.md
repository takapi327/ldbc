{%
  laika.title = カスタム データ型
  laika.metadata.language = ja
%}

# カスタム データ型

この章では、ldbcで構築したテーブル定義でユーザー独自の型もしくはサポートされていない型を使用するための方法について説明します。

セットアップで作成したテーブル定義に新たにカラムを追加します。

```sql
ALTER TABLE user ADD COLUMN status BOOLEAN NOT NULL DEFAULT TRUE;
```

## 受け渡し

ldbcではstatementに受け渡す値を`Parameter`で表現しています。`Parameter`はstatementへのバインドする値を表現するためのtraitです。

`Parameter`を実装することでstatementに受け渡す値をカスタム型で表現することができます。

ユーザー情報にそのユーザーのステータスを表す`Status`を追加します。

```scala 3
enum Status(val done: Boolean, val name: String):
  case Active   extends TaskStatus(false, "Active")
  case InActive extends TaskStatus(true, "InActive")
```

以下のコード例では、`Parameter`を実装した`CustomParameter`を定義しています。

これによりstatementにカスタム型をバインドすることができるようになります。

```scala 3
given Parameter[Status] with
  override def bind[F[_]](
    statement: PreparedStatement[F],
    index: Int,
    status: Status
  ): F[Unit] =
    status match
      case Status.Active   => statement.setBoolean(index, true)
      case Status.InActive => statement.setBoolean(index, false)
```

カスタム型は他のパラメーターと同じようにstatementにバインドすることができます。

```scala
val program1: Executor[IO, Int] =
  sql"INSERT INTO user (name, email, status) VALUES (${ "user 1" }, ${ "user@example.com" }, ${ Status.Active })".update
```

これでstatementにカスタム型をバインドすることができるようになりました。

## 読み取り

ldbcではパラメーターの他に実行結果から独自の型を取得するための`ResultSetReader`も提供しています。

`ResultSetReader`を実装することでstatementの実行結果から独自の型を取得することができます。

以下のコード例では、`ResultSetReader`を実装した`CustomResultSetReader`を定義しています。

```scala 3
given ResultSetReader[IO, Status] =
  ResultSetReader.mapping[IO, Boolean, Status] {
    case true  => Status.Active
    case false => Status.InActive
  }
```

```scala 3
val program2: Executor[IO, (String, String, Status)] =
  sql"SELECT name, email, status FROM user WHERE id = 1".query[(String, String, Status)].unsafe
```

これでstatementの実行結果からカスタム型を取得することができるようになりました。
