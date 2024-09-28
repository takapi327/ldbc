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

## Encoder

ldbcではstatementに受け渡す値を`Encoder`で表現しています。`Encoder`はstatementへのバインドする値を表現するためのtraitです。

`Encoder`を実装することでstatementに受け渡す値をカスタム型で表現することができます。

ユーザー情報にそのユーザーのステータスを表す`Status`を追加します。

```scala 3
enum Status(val done: Boolean, val name: String):
  case Active   extends Status(false, "Active")
  case InActive extends Status(true, "InActive")
```

以下のコード例では、カスタム型の`Encoder`を定義しています。

これによりstatementにカスタム型をバインドすることができるようになります。

```scala 3
given Encoder[Status] with
  override def encode(status: Status): Boolean = status.done
```

カスタム型は他のパラメーターと同じようにstatementにバインドすることができます。

```scala
val program1: Executor[IO, Int] =
  sql"INSERT INTO user (name, email, status) VALUES (${ "user 1" }, ${ "user@example.com" }, ${ Status.Active })".update
```

これでstatementにカスタム型をバインドすることができるようになりました。

## Decoder

ldbcではパラメーターの他に実行結果から独自の型を取得するための`Decoder`も提供しています。

`Decoder`を実装することでstatementの実行結果から独自の型を取得することができます。

以下のコード例では、`Decoder.Elem`を使用して単一のデータ型を取得する方法を示しています。

```scala 3
  given Decoder.Elem[Status] = Decoder.Elem.mapping[Boolean, Status] {
  case true  => Status.Active
  case false => Status.InActive
}
```

```scala 3
val program2: Executor[IO, (String, String, Status)] =
  sql"SELECT name, email, status FROM user WHERE id = 1".query[(String, String, Status)].unsafe
```

これでstatementの実行結果からカスタム型を取得することができるようになりました。
